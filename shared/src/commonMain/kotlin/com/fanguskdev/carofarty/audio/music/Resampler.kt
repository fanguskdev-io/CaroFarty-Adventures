package com.fanguskdev.carofarty.audio.music

import kotlin.math.abs
import kotlin.math.sqrt

/** Something that gives samples at the input rate of a [Resampler], [count] at a time. */
internal fun interface SampleSource {
    fun read(out: DoubleArray, count: Int)
}

/**
 * From 32 kHz to the device's rate (AUD-25, sinte.remuestrear): a polyphase windowed sinc, a Kaiser window with β = 8.6
 * and 64 taps, cut at 44.5 % of the lower rate. It is exact for rational ratios: 32 000 → 48 000 is 3/2 (3 phases),
 * → 44 100 is 441/320 and → 22 050 is 441/640. It reads 32 samples ahead of what it gives (1 ms) and allocates nothing
 * once built.
 */
internal class Resampler(
    private val source: SampleSource,
    inRate: Int,
    outRate: Int,
    private val taps: Int = 64,
    beta: Double = 8.6,
) {
    private val up: Int
    private val down: Int
    private val table: DoubleArray
    private val half = taps / 2
    private val buffer = DoubleArray(BUFFER)
    private val chunk = DoubleArray(CHUNK)

    /** The input index of buffer[0], and how many input samples are in the buffer. */
    private var bufferStart = -half.toLong()
    private var filled = half

    /** The next output sample: n·down = base·up + phase. */
    private var base = 0L
    private var phase = 0

    init {
        val g = gcd(inRate, outRate)
        up = outRate / g
        down = inRate / g
        require(up <= MAX_PHASES) { "$inRate → $outRate Hz needs $up phases" }
        table = kernels(inRate, up, taps, 0.445 * minOf(inRate, outRate), beta)
    }

    /** Fills [out] with [count] samples at the output rate. */
    fun process(out: DoubleArray, count: Int) {
        for (n in 0 until count) {
            val last = base + half
            while (bufferStart + filled <= last) pull()
            val from = (base + 1 - half - bufferStart).toInt()
            val k0 = phase * taps
            var sum = 0.0
            for (k in 0 until taps) sum += table[k0 + k] * buffer[from + k]
            out[n] = sum
            phase += down
            while (phase >= up) {
                phase -= up
                base++
            }
        }
    }

    /** Reads more input, first moving what is still needed to the start of the buffer if it is full. */
    private fun pull() {
        if (filled + CHUNK > BUFFER) {
            val keep = (base + 1 - half - bufferStart).toInt()
            buffer.copyInto(buffer, 0, keep, filled)
            filled -= keep
            bufferStart += keep
        }
        source.read(chunk, CHUNK)
        chunk.copyInto(buffer, filled, 0, CHUNK)
        filled += CHUNK
    }

    companion object {
        private const val CHUNK = 256
        private const val BUFFER = 4096
        private const val MAX_PHASES = 4096

        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

        /** The modified Bessel function I0, by its series (sinte._bessel_i0). */
        fun besselI0(x: Double): Double {
            var s = 1.0
            var t = 1.0
            var k = 1
            while (t > 1e-14 * s) {
                val q = x / (2.0 * k)
                t *= q * q
                s += t
                k++
            }
            return s
        }

        /** One kernel of [taps] taps for each of the [phases] fractional positions, each adding up to 1 (sinte._nucleos). */
        fun kernels(
            inRate: Int,
            phases: Int,
            taps: Int,
            cutoff: Double,
            beta: Double,
        ): DoubleArray {
            val fc = cutoff / inRate
            val half = taps / 2
            val i0b = besselI0(beta)
            val table = DoubleArray(phases * taps)
            for (p in 0 until phases) {
                val frac = p.toDouble() / phases
                var total = 0.0
                for (k in 0 until taps) {
                    val d = (k - half + 1) - frac
                    val x = 2.0 * fc * d
                    val sinc = 2.0 * fc * (if (x != 0.0) DetMath.sin(DetMath.PI * x) / (DetMath.PI * x) else 1.0)
                    val r = d / half
                    val w = if (abs(r) < 1.0) besselI0(beta * sqrt(1.0 - r * r)) / i0b else 0.0
                    table[p * taps + k] = sinc * w
                    total += sinc * w
                }
                for (k in 0 until taps) table[p * taps + k] /= total
            }
            return table
        }
    }
}
