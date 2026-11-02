package com.fanguskdev.carofarty.audio.music

/**
 * The random numbers of the prototype: Python's `random.Random` (a Mersenne Twister, MT19937) with the same seeding and
 * the same `random`, `uniform`, `choice`, `randrange` and `gauss`. A seed gives the same choices as in Python, which lets
 * the port be checked note for note against the prototype, and only integer and IEEE arithmetic is involved, so it is
 * the same on every platform. Nothing here allocates after construction.
 */
internal class PyRandom(seed: Long) {
    private val mt = IntArray(N)
    private var index = N + 1
    private var gaussNext = 0.0
    private var hasGaussNext = false
    private val key = IntArray(2)

    init {
        seed(seed)
    }

    /** As `random.seed(n)` for an integer: the absolute value cut into 32-bit words, lowest first. */
    fun seed(seed: Long) {
        val n = if (seed < 0) -seed else seed // Long.MIN_VALUE stays negative but its bits are those of 2^63
        key[0] = (n and MASK32).toInt()
        key[1] = (n ushr 32).toInt()
        initByArray(if (key[1] == 0) 1 else 2)
        hasGaussNext = false
    }

    private fun initGenrand(s: Int) {
        mt[0] = s
        for (i in 1 until N) {
            val previous = mt[i - 1]
            mt[i] = 1812433253 * (previous xor (previous ushr 30)) + i
        }
        index = N
    }

    private fun initByArray(keyLength: Int) {
        initGenrand(19650218)
        var i = 1
        var j = 0
        var k = if (N > keyLength) N else keyLength
        while (k > 0) {
            val previous = mt[i - 1]
            mt[i] = (mt[i] xor ((previous xor (previous ushr 30)) * 1664525)) + key[j] + j
            i++
            j++
            if (i >= N) {
                mt[0] = mt[N - 1]
                i = 1
            }
            if (j >= keyLength) j = 0
            k--
        }
        k = N - 1
        while (k > 0) {
            val previous = mt[i - 1]
            mt[i] = (mt[i] xor ((previous xor (previous ushr 30)) * 1566083941)) - i
            i++
            if (i >= N) {
                mt[0] = mt[N - 1]
                i = 1
            }
            k--
        }
        mt[0] = UPPER_MASK
    }

    /** Becomes a copy of [other]: the same numbers will come out of both (to peek at a choice without making it). */
    fun copyFrom(other: PyRandom) {
        other.mt.copyInto(mt)
        index = other.index
        gaussNext = other.gaussNext
        hasGaussNext = other.hasGaussNext
    }

    /** The next 32 random bits (as an Int whose bits are the unsigned value). */
    fun nextBits32(): Int {
        if (index >= N) twist()
        var y = mt[index++]
        y = y xor (y ushr 11)
        y = y xor ((y shl 7) and TEMPER_B)
        y = y xor ((y shl 15) and TEMPER_C)
        y = y xor (y ushr 18)
        return y
    }

    private fun twist() {
        var kk = 0
        while (kk < N - M) {
            val y = (mt[kk] and UPPER_MASK) or (mt[kk + 1] and LOWER_MASK)
            mt[kk] = mt[kk + M] xor (y ushr 1) xor (if ((y and 1) != 0) MATRIX_A else 0)
            kk++
        }
        while (kk < N - 1) {
            val y = (mt[kk] and UPPER_MASK) or (mt[kk + 1] and LOWER_MASK)
            mt[kk] = mt[kk + (M - N)] xor (y ushr 1) xor (if ((y and 1) != 0) MATRIX_A else 0)
            kk++
        }
        val y = (mt[N - 1] and UPPER_MASK) or (mt[0] and LOWER_MASK)
        mt[N - 1] = mt[M - 1] xor (y ushr 1) xor (if ((y and 1) != 0) MATRIX_A else 0)
        index = 0
    }

    /** `random()`: a double in [0, 1) made of 53 random bits. */
    fun random(): Double {
        val a = nextBits32() ushr 5
        val b = nextBits32() ushr 6
        return (a * 67108864.0 + b) * (1.0 / 9007199254740992.0)
    }

    /** `uniform(a, b)`. */
    fun uniform(a: Double, b: Double): Double = a + (b - a) * random()

    /** `getrandbits(k)` for 1 ≤ k ≤ 31. */
    private fun bits(k: Int): Int = nextBits32() ushr (32 - k)

    /** `_randbelow(n)`: an integer in [0, n), by rejection like Python, so the same seed gives the same number. */
    fun below(n: Int): Int {
        if (n <= 0) return 0
        val k = 32 - n.countLeadingZeroBits()
        var r = bits(k)
        while (r >= n) r = bits(k)
        return r
    }

    /** `randrange(start, stop)`. */
    fun range(start: Int, stop: Int): Int = start + below(stop - start)

    /** `gauss(mu, sigma)`, with the second value of each pair kept for the next call, as Python does. */
    fun gauss(mu: Double, sigma: Double): Double {
        var z: Double
        if (hasGaussNext) {
            z = gaussNext
            hasGaussNext = false
        } else {
            val x2pi = random() * DetMath.TWO_PI
            val g2rad = kotlin.math.sqrt(-2.0 * DetMath.ln(1.0 - random()))
            z = DetMath.cos(x2pi) * g2rad
            gaussNext = DetMath.sin(x2pi) * g2rad
            hasGaussNext = true
        }
        return mu + z * sigma
    }

    private companion object {
        const val N = 624
        const val M = 397
        const val MATRIX_A = -0x66f74f21 // 0x9908B0DF
        const val UPPER_MASK = Int.MIN_VALUE // 0x80000000
        const val LOWER_MASK = 0x7FFFFFFF
        const val TEMPER_B = -0x62d3a980 // 0x9D2C5680
        const val TEMPER_C = -0x103a0000 // 0xEFC60000
        const val MASK32 = 0xFFFFFFFFL
    }
}
