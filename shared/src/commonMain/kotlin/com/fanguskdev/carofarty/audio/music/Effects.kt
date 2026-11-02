package com.fanguskdev.carofarty.audio.music

import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sqrt

/**
 * One of the two effect voices: it plays a sound made in advance (an effect is a fixed sound in the prototype too),
 * from a given sample, over the music and after its fades.
 */
internal class EffectVoice {
    private var sound: FloatArray = FloatArray(0)
    private var startAt = Long.MAX_VALUE
    private var gain = 0.0

    val busy: Boolean get() = startAt != Long.MAX_VALUE

    /** Plays [sound] times [gain] from time [at] (samples at 32 kHz), instead of whatever this voice was playing. */
    fun play(sound: FloatArray, at: Long, gain: Double) {
        this.sound = sound
        this.startAt = at
        this.gain = gain
    }

    fun stop() {
        startAt = Long.MAX_VALUE
    }

    /** Adds [count] samples from time [from] to [out]. */
    fun mix(from: Long, out: DoubleArray, count: Int) {
        if (startAt == Long.MAX_VALUE) return
        for (k in 0 until count) {
            val i = from + k - startAt
            if (i < 0) continue
            if (i >= sound.size) {
                startAt = Long.MAX_VALUE
                return
            }
            out[k] += sound[i.toInt()] * gain
        }
    }
}

/** The effects the music plays itself (sfx.py): for now the warning of few points left. */
internal object Effects {
    /** sfx.env_sfxr: a linear attack up to 1 + 2·punch, a hold that comes down to 1 and a linear decay to 0. */
    fun sfxrEnvelope(
        attackMs: Double,
        holdMs: Double,
        decayMs: Double,
        punch: Double = 0.0,
    ): DoubleArray {
        val na = Chip.ms(attackMs)
        val ns = Chip.ms(holdMs)
        val nd = maxOf(1, Chip.ms(decayMs))
        val out = DoubleArray(na + ns + nd)
        for (i in 0 until na) out[i] = (1.0 + 2.0 * punch) * i / na
        for (i in 0 until ns) out[na + i] = 1.0 + 2.0 * punch * (1.0 - i.toDouble() / ns)
        for (i in 0 until nd) out[na + ns + i] = 1.0 - (i + 1).toDouble() / nd
        return out
    }

    /** sinte.tocar with a fixed pitch: [n] samples of [sample] at [hz], through the pitch register and the gaussian. */
    fun play(sample: Sample, hz: Double, n: Int): DoubleArray {
        val out = DoubleArray(n)
        val data = sample.played
        val end = sample.length * Chip.PITCH_ONE
        val loop = if (sample.loopStart >= 0) (sample.length - sample.loopStart) * Chip.PITCH_ONE else 0
        val ratio = sample.rate / (sample.root * Chip.RATE)
        val step = minOf(Chip.PITCH_MAX, round(hz * ratio * Chip.PITCH_ONE).toInt())
        var pos = 0
        val g = Chip.gauss
        for (k in 0 until n) {
            val at = pos ushr 12
            val w = ((pos and 0xFFF) ushr 4) * 4
            out[k] = data[at] * g[w] + data[at + 1] * g[w + 1] + data[at + 2] * g[w + 2] + data[at + 3] * g[w + 3]
            pos += step
            if (pos >= end) {
                if (loop == 0) return out
                pos -= loop
            }
        }
        return out
    }

    /** sfx.K_para: how many harmonics fit under [ceiling] Hz at [highestHz]. */
    private fun harmonicsFor(highestHz: Double, ceiling: Double): Int = maxOf(1, minOf(32, (ceiling / highestHz).toInt()))

    /**
     * sfx.aviso_pocos_puntos as sfx.renderizar gives it: «ti-tu, ti-tu», A5 and E5 every 105 ms on a soft pulse wave
     * (the falling fourth warns without frightening), with 60 ms of silence after it and the DC taken out at 20 Hz.
     */
    fun fewPointsWarning(): FloatArray {
        val notes = doubleArrayOf(Chip.hz(midi("A5").toDouble()), Chip.hz(midi("E5").toDouble()))
        val envelope = sfxrEnvelope(2.0, 45.0, 45.0)
        val length = Chip.ms(3 * 105.0) + envelope.size
        val sum = DoubleArray(length)
        for (i in 0 until 4) {
            val hz = notes[i % 2]
            val k = harmonicsFor(hz, 6000.0)
            val wave = CycleRecipe("pulso50-$k-64", Synthesis.pulseHarmonics(0.5, k)).build()
            val x = play(wave, hz, envelope.size)
            val from = Chip.ms(i * 105.0)
            for (j in envelope.indices) sum[from + j] += x[j] * envelope[j] * 0.40
        }
        val withTail = sum.copyOf(length + Chip.ms(60.0))
        val dc = DcBlocker(20.0)
        return FloatArray(withTail.size) { dc.process(withTail[it]).toFloat() }
    }

    /** The RMS of the part of [x] that sounds (|x| > 1e-4), as the prototype measures effects. */
    fun activeRms(x: FloatArray): Double {
        var sum = 0.0
        var n = 0
        for (v in x) {
            if (abs(v) > 1e-4) {
                sum += v.toDouble() * v
                n++
            }
        }
        return if (n == 0) 0.0 else sqrt(sum / n)
    }
}
