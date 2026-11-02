package com.fanguskdev.carofarty.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/** The sample rate of the synthesized sounds: enough for 16-bit era instruments and half the memory of CD quality. */
const val SAMPLE_RATE = 22_050

internal const val TWO_PI = 6.2831855f
private const val MAX_CUTOFF = 5_000f

/**
 * A state-variable filter (Chamberlin's): low-pass and band-pass at once, and cheap to retune on every sample, which a
 * sound that changes pitch needs. It runs twice per sample (oversampled) so it stays stable up to [MAX_CUTOFF].
 */
internal class Svf {
    var low = 0f
        private set
    var band = 0f
        private set

    fun run(input: Float, cutoffHz: Float, q: Float) {
        val f = 2f * sin(PI.toFloat() * min(cutoffHz, MAX_CUTOFF) / (2 * SAMPLE_RATE))
        val damping = 1f / q
        repeat(2) {
            low += f * band
            val high = input - low - damping * band
            band += f * high
        }
    }
}

/** The simplest low-pass: smooths what goes through it above [cutoffHz]. */
internal class OnePole(cutoffHz: Float) {
    private val a = 1f - exp(-TWO_PI * cutoffHz / SAMPLE_RATE)
    private var y = 0f

    fun run(x: Float): Float {
        y += a * (x - y)
        return y
    }
}

/** The correction that takes the clicks out of a saw or a square at each jump (PolyBLEP). */
private fun polyBlep(t: Float, dt: Float): Float = when {
    t < dt -> {
        val x = t / dt
        x + x - x * x - 1f
    }
    t > 1f - dt -> {
        val x = (t - 1f) / dt
        x * x + x + x + 1f
    }
    else -> 0f
}

/** A saw wave without aliasing at [phase] (0–1), advancing [dt] per sample. */
internal fun saw(phase: Float, dt: Float): Float = 2f * phase - 1f - polyBlep(phase, dt)

/** A pulse wave of [duty] (0–1) without aliasing, made of two saws. */
internal fun pulse(phase: Float, dt: Float, duty: Float): Float {
    val shifted = (phase + duty) % 1f
    return 0.5f * (saw(phase, dt) - saw(shifted, dt))
}

/** A triangle wave at [phase] (0–1). */
internal fun triangle(phase: Float): Float = 1f - 4f * abs(phase - 0.5f)

/** The frequency of a MIDI note (69 is the A at 440 Hz). */
internal fun midiHz(note: Int): Float = 440f * 2f.pow((note - 69) / 12f)

/** Samples for [seconds]. */
internal fun samples(seconds: Float): Int = (seconds * SAMPLE_RATE).toInt()

/**
 * The last touch of a rendered sound: warms it with a soft clip, brings its peak to [peak] and fades the first and last
 * milliseconds so it never clicks.
 */
internal fun FloatArray.finished(peak: Float = 0.92f, drive: Float = 1.4f): FloatArray {
    for (i in indices) this[i] = tanh(this[i] * drive)
    var loudest = 0f
    for (value in this) loudest = max(loudest, abs(value))
    if (loudest > 0f) {
        val gain = peak / loudest
        for (i in indices) this[i] *= gain
    }
    val fade = min(samples(0.004f), size / 2)
    for (i in 0 until fade) {
        val g = i.toFloat() / fade
        this[i] *= g
        this[size - 1 - i] *= g
    }
    return this
}

/** 16-bit PCM, what the phones play. */
fun toPcm16(samples: FloatArray): ShortArray = ShortArray(samples.size) { (samples[it].coerceIn(-1f, 1f) * 32_767f).toInt().toShort() }

/** [pcm] as a mono 16-bit WAV file at [rate] Hz, which every platform knows how to play. */
fun wav(pcm: ShortArray, rate: Int = SAMPLE_RATE): ByteArray {
    val dataBytes = pcm.size * 2
    val out = ByteArray(WAV_HEADER + dataBytes)

    fun text(offset: Int, value: String) = value.encodeToByteArray().copyInto(out, offset)

    fun int32(offset: Int, value: Int) {
        for (b in 0 until 4) out[offset + b] = (value shr (8 * b)).toByte()
    }

    fun int16(offset: Int, value: Int) {
        out[offset] = value.toByte()
        out[offset + 1] = (value shr 8).toByte()
    }
    text(0, "RIFF")
    int32(4, WAV_HEADER - 8 + dataBytes)
    text(8, "WAVE")
    text(12, "fmt ")
    int32(16, 16)
    int16(20, 1) // PCM
    int16(22, 1) // mono
    int32(24, rate)
    int32(28, rate * 2)
    int16(32, 2)
    int16(34, 16)
    text(36, "data")
    int32(40, dataBytes)
    for (i in pcm.indices) int16(WAV_HEADER + i * 2, pcm[i].toInt())
    return out
}

private const val WAV_HEADER = 44
