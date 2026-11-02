package com.fanguskdev.carofarty.audio.music

import kotlin.math.abs
import kotlin.math.round

/**
 * The sound chip the music imitates (the Super Nintendo's, as the prototype's sinte.py describes it): a 32 kHz mix of 8
 * voices, samples played through a 14-bit pitch register and a 4-point gaussian interpolation, an ADSR envelope, a 15-bit
 * noise generator and an echo with an 8-tap FIR. Only its documented behaviour is imitated; no code or data of any game.
 */
internal object Chip {
    /** The mixing rate. */
    const val RATE = 32_000

    /** Voices for the music (bass, two of pad, percussion, C2 and C3) and for the effects. */
    const val MUSIC_VOICES = 6
    const val EFFECT_VOICES = 2
    const val VOICES = MUSIC_VOICES + EFFECT_VOICES

    /** Samples between pitch updates when the pitch moves (vibrato, slides): 1 ms, like a driver's tick. */
    const val PITCH_BLOCK = 32

    /** The pitch register counts in 1/4096 of a sample; its top is $3FFF, almost 4 samples of the source per sample. */
    const val PITCH_ONE = 0x1000
    const val PITCH_MAX = 0x3FFF

    /** The ramp that silences a voice before it is reused (8 ms), as a driver did. */
    const val CUT = 256

    /** The noise generator's 32 rates, in Hz. */
    val noiseRates = intArrayOf(
        0, 16, 21, 25, 31, 42, 50, 63, 83, 100, 125, 167, 200, 250, 333, 400, 500, 667, 800,
        1000, 1300, 1600, 2000, 2700, 3200, 4000, 5300, 6400, 8000, 10700, 16000, 32000,
    )

    /** The low-pass of the chip's manual (−1, 8, 23, 36, 36, 23, 8, −1) adds up to 136 in absolute value, over the 128 allowed. */
    val manualFir = intArrayOf(-1, 8, 23, 36, 36, 23, 8, -1)

    /** The same filter scaled by 128/136 and rounded: it adds up to 128. */
    val echoFir = intArrayOf(-1, 7, 22, 34, 34, 22, 7, -1)

    /** The highest echo feedback allowed: $50, well away from the $7F that runs away. */
    const val MAX_FEEDBACK = 0x50

    /** The width of the gaussian bell, in samples. */
    const val GAUSS_SIGMA = 0.63

    /**
     * The 4 weights of the gaussian interpolation for each of 256 fractional positions: for position i+1+f the samples i,
     * i+1, i+2 and i+3, at distances 1+f, f, 1−f and 2−f, weigh exp(−d²/2σ²), normalized. At f = 0 they are 0.18, 0.64 and
     * 0.18: about 11 dB less at the Nyquist frequency of the sample, the muffled sound of the chip.
     */
    val gauss: DoubleArray = DoubleArray(256 * 4).also { table ->
        val twoSigmaSquared = 2 * GAUSS_SIGMA * GAUSS_SIGMA
        for (k in 0 until 256) {
            val f = k / 256.0
            val w0 = DetMath.exp(-((1 + f) * (1 + f)) / twoSigmaSquared)
            val w1 = DetMath.exp(-(f * f) / twoSigmaSquared)
            val w2 = DetMath.exp(-((1 - f) * (1 - f)) / twoSigmaSquared)
            val w3 = DetMath.exp(-((2 - f) * (2 - f)) / twoSigmaSquared)
            val sum = w0 + w1 + w2 + w3
            table[4 * k] = w0 / sum
            table[4 * k + 1] = w1 / sum
            table[4 * k + 2] = w2 / sum
            table[4 * k + 3] = w3 / sum
        }
    }

    /** One step of the noise generator: a 15-bit LFSR whose new bit 14 is bit 0 XOR bit 1. */
    fun nextNoise(lfsr: Int): Int = (lfsr ushr 1) or (((lfsr xor (lfsr ushr 1)) and 1) shl 14)

    /** The noise generator's output: its 15 bits as a signed value, over 16 384. */
    fun noiseLevel(lfsr: Int): Double = (if ((lfsr and 0x4000) != 0) lfsr - 0x8000 else lfsr) / 16384.0

    /** Milliseconds as samples at 32 kHz, rounded like the prototype (half to even). */
    fun ms(millis: Double): Int = round(millis * RATE / 1000.0).toInt()

    /** The frequency of a MIDI note (fractions of a semitone allowed). */
    fun hz(note: Double): Double = 440.0 * DetMath.exp2((note - 69) / 12.0)

    /** The frequency factor of [cents] cents. */
    fun cents(cents: Double): Double = DetMath.exp2(cents / 1200.0)

    /** decibels as a factor. */
    fun fromDb(db: Double): Double = DetMath.exp(db / 20.0 * LN10)

    private const val LN10 = 2.302585092994046
}

/**
 * A sample of the bank: [data] at [rate] Hz that sounds at [root] Hz when played at its own speed. [loopStart] is where
 * it goes back to when it reaches its end, or −1 if it stops. As on the chip, a loop starts at a multiple of 16 samples
 * and lasts a multiple of 16, and the data goes through the BRR codec (unless [brr] is false) for its grain.
 */
internal class Sample(
    val name: String,
    data: DoubleArray,
    val rate: Int,
    val root: Double,
    val loopStart: Int = -1,
    brr: Boolean = true,
) {
    /**
     * The data as played, after the codec and in 32 bits: [length] samples, a multiple of 16, and 3 more (the loop's
     * first ones, or silence) so that the interpolation never reads outside.
     */
    val played: FloatArray

    val length: Int

    init {
        var raw = data
        if (raw.size % 16 != 0) {
            require(loopStart < 0) { "$name: a sample with a loop must last a multiple of 16 samples" }
            raw = raw.copyOf(raw.size + 16 - raw.size % 16)
        }
        if (loopStart >= 0) require(loopStart % 16 == 0 && loopStart < raw.size) { "$name: loop at $loopStart of ${raw.size}" }
        val coded = if (brr) Brr.roundTrip(raw, loopStart) else raw
        length = coded.size
        played = FloatArray(length + 3)
        for (i in coded.indices) played[i] = coded[i].toFloat()
        if (loopStart >= 0) for (i in 0 until 3) played[length + i] = coded[loopStart + i].toFloat()
    }

    /** The samples, without the 3 of more. */
    fun values(): DoubleArray = DoubleArray(length) { played[it].toDouble() }
}

/**
 * The BRR codec of the chip, applied when the bank is made so that the samples get its grain: blocks of 16 samples of
 * 4 bits (−8…7) with a shift of 0–12 and one of 4 prediction filters, the one with the least error for each block. The
 * first block and the block of the loop point use filter 0, which does not depend on the samples before it.
 */
internal object Brr {
    private val c1 = doubleArrayOf(0.0, 15.0 / 16, 61.0 / 32, 115.0 / 64)
    private val c2 = doubleArrayOf(0.0, 0.0, -15.0 / 16, -13.0 / 16)
    private const val SCALE = 32767.0

    /** [x] encoded and decoded again. */
    fun roundTrip(x: DoubleArray, loopStart: Int = -1): DoubleArray {
        require(x.size % 16 == 0) { "BRR works on blocks of 16 samples" }
        val out = DoubleArray(x.size)
        val block = DoubleArray(16)
        val trial = DoubleArray(16)
        val best = DoubleArray(16)
        var p1 = 0.0
        var p2 = 0.0
        var b = 0
        while (b < x.size) {
            for (i in 0 until 16) block[i] = x[b + i] * SCALE
            val filters = if (b == 0 || b == loopStart) 1 else 4
            var bestError = Double.MAX_VALUE
            var found = false
            for (fi in 0 until filters) {
                val a1 = c1[fi]
                val a2 = c2[fi]
                var q1 = p1
                var q2 = p2
                var largest = 0.0
                for (s in block) {
                    val r = abs(s - (a1 * q1 + a2 * q2))
                    if (r > largest) largest = r
                    q2 = q1
                    q1 = s
                }
                var base = 0
                while (base < 12 && 7.5 * (1 shl base) < largest) base++
                val low = maxOf(0, base - 1)
                val high = minOf(12, base + 1)
                for (shift in low..high) {
                    val step = (1 shl shift).toDouble()
                    q1 = p1
                    q2 = p2
                    var error = 0.0
                    for (i in 0 until 16) {
                        val s = block[i]
                        val prediction = a1 * q1 + a2 * q2
                        val k = round((s - prediction) / step).coerceIn(-8.0, 7.0)
                        val y = (prediction + k * step).coerceIn(-32768.0, 32767.0)
                        error += (s - y) * (s - y)
                        trial[i] = y
                        q2 = q1
                        q1 = y
                    }
                    if (!found || error < bestError) {
                        found = true
                        bestError = error
                        trial.copyInto(best)
                    }
                }
            }
            for (i in 0 until 16) out[b + i] = best[i] / SCALE
            p2 = best[14]
            p1 = best[15]
            b += 16
        }
        return out
    }
}

/**
 * The echo of the chip: a delay of EDL × 16 ms, an 8-tap FIR on what comes back and a feedback EFB; the output is the
 * FIR's times EVOL. EFB and EVOL are register units (−128…127; $40 = 64 is a half). The feedback goes through the FIR,
 * so each repeat comes back a little duller than the one before.
 */
internal class EchoSettings(
    val edl: Int,
    val efb: Int,
    val evol: Int,
    val fir: IntArray = Chip.echoFir,
) {
    init {
        require(edl in 1..15) { "EDL $edl: 16–240 ms" }
        require(fir.size == 8 && fir.sumOf { abs(it) } <= 128) { "the FIR adds up to more than 128" }
        require(abs(efb) <= Chip.MAX_FEEDBACK) { "EFB $efb: too close to \$7F" }
    }

    /** The delay in samples: EDL × 512 (16 ms at 32 kHz). */
    val delay: Int get() = edl * 512
}

/** The echo running sample by sample, with its delay line allocated once. */
internal class Echo(settings: EchoSettings) {
    private val buffer = DoubleArray(settings.delay)
    private var p = 0
    private val c0 = settings.fir[0] / 128.0
    private val c1 = settings.fir[1] / 128.0
    private val c2 = settings.fir[2] / 128.0
    private val c3 = settings.fir[3] / 128.0
    private val c4 = settings.fir[4] / 128.0
    private val c5 = settings.fir[5] / 128.0
    private val c6 = settings.fir[6] / 128.0
    private val c7 = settings.fir[7] / 128.0
    private val feedback = settings.efb / 128.0
    private val volume = settings.evol / 128.0
    private var h0 = 0.0
    private var h1 = 0.0
    private var h2 = 0.0
    private var h3 = 0.0
    private var h4 = 0.0
    private var h5 = 0.0
    private var h6 = 0.0
    private var h7 = 0.0

    /** Takes what is sent to the echo and gives back only the echo. */
    fun process(send: Double): Double {
        h7 = h6
        h6 = h5
        h5 = h4
        h4 = h3
        h3 = h2
        h2 = h1
        h1 = h0
        h0 = buffer[p]
        val y = c0 * h0 + c1 * h1 + c2 * h2 + c3 * h3 + c4 * h4 + c5 * h5 + c6 * h6 + c7 * h7
        buffer[p] = send + y * feedback
        p++
        if (p == buffer.size) p = 0
        return y * volume
    }
}

/** A first-order high-pass at [cutoff] Hz that takes the DC offset out, as the prototype does with every mix. */
internal class DcBlocker(cutoff: Double = 10.0, rate: Int = Chip.RATE) {
    private val r = 1.0 - 2.0 * DetMath.PI * cutoff / rate
    private var previousIn = 0.0
    private var previousOut = 0.0

    fun process(x: Double): Double {
        previousOut = x - previousIn + r * previousOut
        previousIn = x
        return previousOut
    }
}
