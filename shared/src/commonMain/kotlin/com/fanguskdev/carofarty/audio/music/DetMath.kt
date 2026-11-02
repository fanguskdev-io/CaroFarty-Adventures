package com.fanguskdev.carofarty.audio.music

import kotlin.math.round

/**
 * sin, cos, exp, ln and pow written with + − × ÷ only, so the music comes out the same, sample for sample, on the JVM,
 * Android and iOS. The platforms' own functions may differ in the last bit, and one bit can flip a quantized pitch, a BRR
 * choice or a rounded note time. sqrt and round are exact everywhere (IEEE 754), so they are used as they are.
 *
 * They are accurate to a couple of units in the last place, like the C library the prototype ran on.
 */
internal object DetMath {
    const val PI = 3.141592653589793
    const val TWO_PI = 6.283185307179586
    const val LN2 = 0.6931471805599453

    // π/2 in pieces whose products by a whole number of quarter turns below 2^20 are exact (Cody and Waite).
    private val pio2Hi = Double.fromBits(0x3FF921FB54400000L)
    private val pio2Mid = Double.fromBits(0x3DD0B4611A600000L)
    private val pio2Lo = Double.fromBits(0x3BA3198A2E000000L)
    private val pio2Tail = Double.fromBits(0x397B839A252049C1L)
    private const val TWO_OVER_PI = 0.6366197723675814

    // ln 2 in two pieces, the first with zeros at the end so that k × it is exact for any exponent.
    private val ln2Hi = Double.fromBits(0x3FE62E42FEE00000L)
    private val ln2Lo = Double.fromBits(0x3DEA39EF35793C76L)
    private const val INV_LN2 = 1.4426950408889634
    private const val SQRT2 = 1.4142135623730951
    private const val EXP_OVERFLOW = 709.782712893384
    private const val EXP_UNDERFLOW = -745.1332191019411

    fun sin(x: Double): Double {
        if (x.isNaN() || x.isInfinite()) return Double.NaN
        val k = round(x * TWO_OVER_PI)
        val r = reduce(x, k)
        return when (quadrant(k)) {
            0 -> sinKernel(r)
            1 -> cosKernel(r)
            2 -> -sinKernel(r)
            else -> -cosKernel(r)
        }
    }

    fun cos(x: Double): Double {
        if (x.isNaN() || x.isInfinite()) return Double.NaN
        val k = round(x * TWO_OVER_PI)
        val r = reduce(x, k)
        return when (quadrant(k)) {
            0 -> cosKernel(r)
            1 -> -sinKernel(r)
            2 -> -cosKernel(r)
            else -> sinKernel(r)
        }
    }

    /** x minus k quarter turns, in the pieces of π/2 so that nothing is lost. */
    private fun reduce(x: Double, k: Double): Double = (((x - k * pio2Hi) - k * pio2Mid) - k * pio2Lo) - k * pio2Tail

    private fun quadrant(k: Double): Int = (k.toLong() and 3L).toInt()

    /** sin on [−π/4, π/4]: the Taylor series up to x^17, whose next term is below 1e-19. */
    private fun sinKernel(x: Double): Double {
        val z = x * x
        val p = S1 + z * (S2 + z * (S3 + z * (S4 + z * (S5 + z * (S6 + z * (S7 + z * S8))))))
        return x + x * z * p
    }

    /** cos on [−π/4, π/4]: the Taylor series up to x^18. */
    private fun cosKernel(x: Double): Double {
        val z = x * x
        val p = C1 + z * (C2 + z * (C3 + z * (C4 + z * (C5 + z * (C6 + z * (C7 + z * (C8 + z * C9)))))))
        return 1.0 + z * p
    }

    fun exp(x: Double): Double {
        if (x.isNaN()) return x
        if (x > EXP_OVERFLOW) return Double.POSITIVE_INFINITY
        if (x < EXP_UNDERFLOW) return 0.0
        val k = round(x * INV_LN2)
        val r = (x - k * ln2Hi) - k * ln2Lo
        return scale(expKernel(r), k.toInt())
    }

    /** 2^y. */
    fun exp2(y: Double): Double {
        if (y.isNaN()) return y
        if (y > 1023.0) return Double.POSITIVE_INFINITY
        if (y < -1074.0) return 0.0
        val k = round(y)
        return scale(expKernel((y - k) * LN2), k.toInt())
    }

    /** e^r for |r| ≤ ln2/2: the Taylor series up to r^14, whose next term is below 1e-19. */
    private fun expKernel(r: Double): Double {
        val high = E8 + r * (E9 + r * (E10 + r * (E11 + r * (E12 + r * (E13 + r * E14)))))
        return 1.0 + r * (1.0 + r * (E2 + r * (E3 + r * (E4 + r * (E5 + r * (E6 + r * (E7 + r * high)))))))
    }

    /** p × 2^k, by building the power of two from its bits. */
    private fun scale(p: Double, k: Int): Double = when {
        k > 1023 -> p * powerOfTwo(1023) * powerOfTwo(k - 1023)
        k < -1022 -> p * powerOfTwo(k + 600) * powerOfTwo(-600)
        else -> p * powerOfTwo(k)
    }

    private fun powerOfTwo(k: Int): Double = Double.fromBits((k + 1023).toLong() shl 52)

    /** The natural logarithm: ln(m · 2^e) = e·ln 2 + 2·atanh((m − 1)/(m + 1)), with m in [√2/2, √2]. */
    fun ln(x: Double): Double {
        if (x.isNaN() || x < 0.0) return Double.NaN
        if (x == 0.0) return Double.NEGATIVE_INFINITY
        if (x.isInfinite()) return x
        var bits = x.toRawBits()
        var e = ((bits ushr 52) and 0x7FFL).toInt()
        if (e == 0) {
            bits = (x * powerOfTwo(54)).toRawBits()
            e = ((bits ushr 52) and 0x7FFL).toInt() - 54
        }
        e -= 1023
        var m = Double.fromBits((bits and 0x000FFFFFFFFFFFFFL) or 0x3FF0000000000000L)
        if (m > SQRT2) {
            m *= 0.5
            e += 1
        }
        val f = m - 1.0
        val s = f / (2.0 + f)
        val z = s * s
        val high = L11 + z * (L13 + z * (L15 + z * (L17 + z * (L19 + z * L21))))
        val series = s * (L1 + z * (L3 + z * (L5 + z * (L7 + z * (L9 + z * high)))))
        return e * ln2Hi + (series + e * ln2Lo)
    }

    /** base^power for a positive base. */
    fun pow(base: Double, power: Double): Double {
        if (power == 0.0 || base == 1.0) return 1.0
        if (base == 0.0) return if (power > 0.0) 0.0 else Double.POSITIVE_INFINITY
        return exp(power * ln(base))
    }

    // Taylor coefficients: ±1/n!.
    private const val S1 = -1.0 / 6.0
    private const val S2 = 1.0 / 120.0
    private const val S3 = -1.0 / 5040.0
    private const val S4 = 1.0 / 362880.0
    private const val S5 = -1.0 / 39916800.0
    private const val S6 = 1.0 / 6227020800.0
    private const val S7 = -1.0 / 1307674368000.0
    private const val S8 = 1.0 / 355687428096000.0
    private const val C1 = -1.0 / 2.0
    private const val C2 = 1.0 / 24.0
    private const val C3 = -1.0 / 720.0
    private const val C4 = 1.0 / 40320.0
    private const val C5 = -1.0 / 3628800.0
    private const val C6 = 1.0 / 479001600.0
    private const val C7 = -1.0 / 87178291200.0
    private const val C8 = 1.0 / 20922789888000.0
    private const val C9 = -1.0 / 6402373705728000.0
    private const val E2 = 1.0 / 2.0
    private const val E3 = 1.0 / 6.0
    private const val E4 = 1.0 / 24.0
    private const val E5 = 1.0 / 120.0
    private const val E6 = 1.0 / 720.0
    private const val E7 = 1.0 / 5040.0
    private const val E8 = 1.0 / 40320.0
    private const val E9 = 1.0 / 362880.0
    private const val E10 = 1.0 / 3628800.0
    private const val E11 = 1.0 / 39916800.0
    private const val E12 = 1.0 / 479001600.0
    private const val E13 = 1.0 / 6227020800.0
    private const val E14 = 1.0 / 87178291200.0

    // 2·atanh(s) = 2(s + s³/3 + s⁵/5 + …): the coefficients 2/(2n + 1).
    private const val L1 = 2.0
    private const val L3 = 2.0 / 3.0
    private const val L5 = 2.0 / 5.0
    private const val L7 = 2.0 / 7.0
    private const val L9 = 2.0 / 9.0
    private const val L11 = 2.0 / 11.0
    private const val L13 = 2.0 / 13.0
    private const val L15 = 2.0 / 15.0
    private const val L17 = 2.0 / 17.0
    private const val L19 = 2.0 / 19.0
    private const val L21 = 2.0 / 21.0
}
