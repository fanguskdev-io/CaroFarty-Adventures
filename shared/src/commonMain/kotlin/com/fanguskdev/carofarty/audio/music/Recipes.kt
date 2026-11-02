package com.fanguskdev.carofarty.audio.music

import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sqrt

/**
 * How each sample of the bank is made when the music starts, as in the prototype's sinte.py (`muestra_*`): additive,
 * FM, Karplus-Strong, chords, drums, metal, winds with breath, birds and the rain stick. Pitched samples are an attack
 * and a looped cycle: the long decay is the envelope's job, not the sample's. Everything is computed here; nothing is
 * read from a file. A recipe is plain data, so a new instrument is a new recipe.
 */
internal sealed interface SampleRecipe {
    fun build(): Sample
}

/** A harmonic: number [k], amplitude [amplitude] and phase [phase] in radians. */
internal class Harmonic(val k: Double, val amplitude: Double, val phase: Double = 0.0)

/** A partial of an additive sound: ratio [k], amplitude at the start, time constant [tau] (s) and amplitude in the loop. */
internal class Partial(
    val k: Double,
    val start: Double,
    val tau: Double,
    val loop: Double,
)

/** A partial of a metal: [hz], [amplitude] and its decay [tau] in seconds. */
internal class MetalPartial(val hz: Double, val amplitude: Double, val tau: Double)

/** The noise of a mallet or a pick at the start: [level], how long ([ms]) and its low-pass [cutoff] in Hz. */
internal class Strike(val level: Double, val ms: Double, val cutoff: Double)

/** One cycle of [n] samples at [rate] Hz made of [harmonics], looped: it sounds at rate / n Hz. */
internal class CycleRecipe(
    val name: String,
    val harmonics: List<Harmonic>,
    val n: Int = 64,
    val rate: Int = 16_000,
    val brr: Boolean = true,
) : SampleRecipe {
    override fun build(): Sample = Sample(name, Synthesis.cycle(harmonics, n), rate, rate.toDouble() / n, loopStart = 0, brr = brr)
}

/**
 * An additive attack of [attackCycles] cycles and one cycle in a loop (`muestra_parciales`): whole partials go on in the
 * loop at their loop amplitude, the others reach zero by the end of the attack. It sounds at rate / [cycle] Hz, times
 * [rootScale] when the pitch heard is another partial's (a bell's).
 */
internal class PartialsRecipe(
    val name: String,
    val cycle: Int,
    val rate: Int,
    val attackCycles: Int,
    val partials: List<Partial>,
    val strike: Strike? = null,
    val rootScale: Double = 1.0,
) : SampleRecipe {
    override fun build(): Sample = Synthesis.partials(this)
}

/** Two-operator FM with whole ratios, its index falling from [indexStart] to [indexLoop] during the attack (`muestra_fm`). */
internal class FmRecipe(
    val name: String,
    val cycle: Int,
    val rate: Int,
    val attackCycles: Int,
    val modulatorRatio: Double,
    val indexStart: Double,
    val tau: Double,
    val indexLoop: Double,
    val carrierRatio: Double = 1.0,
    val strike: Strike? = null,
) : SampleRecipe {
    override fun build(): Sample = Synthesis.fm(this)
}

/**
 * A plucked string by Karplus-Strong with period [period] (it sounds at rate / period Hz) and its last period looped
 * (`muestra_karplus`): a triangular pluck at [position] with 20 % noise, low-passed by [brightness], a symmetric loop
 * filter of [damping] and, optionally, some pure fundamental ([body]) for round basses.
 */
internal class PluckRecipe(
    val name: String,
    val period: Int,
    val rate: Int,
    val seconds: Double,
    val brightness: Double = 0.5,
    val rho: Double = 0.996,
    val position: Double = 0.2,
    val seed: Long = 1,
    val body: Double = 0.0,
    val damping: Double = 0.5,
) : SampleRecipe {
    override fun build(): Sample = Synthesis.pluck(this)
}

/** A long loop of two oscillators a whole number of cycles apart: a chorus that loops seamlessly (`muestra_pad`). */
internal class PadRecipe(
    val name: String,
    val rate: Int,
    val seconds: Double,
    val cycles: Int,
    val harmonics: List<Harmonic>,
    val detune: Int = 1,
) : SampleRecipe {
    override fun build(): Sample = Synthesis.pad(this)
}

/** A drum: a sine falling from [startHz] to [endHz], a partial at [partialRatio] and noise (`muestra_membrana`). */
internal class MembraneRecipe(
    val name: String,
    val rate: Int,
    val startHz: Double,
    val endHz: Double,
    val pitchTau: Double,
    val ampTau: Double,
    val seconds: Double,
    val noise: Double = 0.0,
    val noiseTau: Double = 0.01,
    val noiseCutoff: Double = 4000.0,
    val partial: Double = 0.0,
    val seed: Long = 3,
    val partialRatio: Double = 1.6,
) : SampleRecipe {
    override fun build(): Sample = Synthesis.membrane(this)
}

/** A metal: inharmonic partials and high noise, struck once or shaken at [shakes] seconds (`muestra_metal`). */
internal class MetalRecipe(
    val name: String,
    val rate: Int,
    val partials: List<MetalPartial>,
    val seconds: Double,
    val noise: Double = 0.0,
    val noiseTau: Double = 0.02,
    val highPassHz: Double = 3000.0,
    val shakes: DoubleArray = doubleArrayOf(0.0),
    val seed: Long = 5,
) : SampleRecipe {
    override fun build(): Sample = Synthesis.metal(this)
}

/**
 * A whole chord in one looped sample, as many games of the chip did to save voices (`muestra_acorde`): each note gives a
 * whole number of cycles per loop, with a double one cycle apart for a chorus that loops. The root sounds at
 * ([rootCycles] + [detune] / 2) / [seconds] Hz.
 */
internal class ChordRecipe(
    val name: String,
    val intervals: IntArray,
    val harmonics: List<Harmonic>,
    val rate: Int = 16_000,
    val seconds: Double = 1.0,
    val rootCycles: Int = 220,
    val detune: Int = 1,
) : SampleRecipe {
    override fun build(): Sample = Synthesis.chord(this)
}

/** A wind with breath: harmonics with band noise in the loop as well (it loops) and a puff of air at the start (`muestra_soplo`). */
internal class BreathRecipe(
    val name: String,
    val cycle: Int,
    val rate: Int,
    val attackCycles: Int,
    val loopCycles: Int,
    val harmonics: List<Harmonic>,
    val breath: Double = 0.15,
    val bandLow: Double = 800.0,
    val bandHigh: Double = 4000.0,
    val chiff: Double = 0.3,
    val chiffMs: Double = 35.0,
    val seed: Long = 17,
) : SampleRecipe {
    override fun build(): Sample = Synthesis.breath(this)
}

/** The calls of the birds and the frog (`muestra_pajaro`). */
internal enum class Call { Whistle, Trill, Warble, Coo, Frog }

/** A bird or a frog, synthesized; no loop, played at its own speed. */
internal class BirdRecipe(
    val name: String,
    val call: Call,
    val seed: Long,
    val rate: Int = 32_000,
) : SampleRecipe {
    override fun build(): Sample = Synthesis.bird(this)
}

/** The rain stick: a cascade of grains that grows and fades (`muestra_palo_lluvia`). */
internal class RainStickRecipe(
    val name: String,
    val rate: Int = 32_000,
    val seconds: Double = 2.4,
    val seed: Long = 5,
) : SampleRecipe {
    override fun build(): Sample = Synthesis.rainStick(this)
}

/** The arithmetic of the recipes, line by line as in sinte.py, with the platform-independent [DetMath]. */
internal object Synthesis {
    private const val PEAK = 0.95

    /** `_lanczos`: the sigma factor that smooths the Gibbs ripple of a wave with [harmonics] harmonics. */
    fun lanczos(k: Int, harmonics: Int): Double {
        val x = DetMath.PI * k / (harmonics + 1)
        return DetMath.sin(x) / x
    }

    /** `armonicos_pulso`: a pulse of [duty] with [count] harmonics and no DC, the «filtered square». */
    fun pulseHarmonics(duty: Double, count: Int): List<Harmonic> = (1..count).map { k ->
        val amplitude = 2.0 / (k * DetMath.PI) * DetMath.sin(DetMath.PI * k * duty) * lanczos(k, count)
        Harmonic(k.toDouble(), amplitude, DetMath.PI / 2 - DetMath.PI * k * duty)
    }

    /** `armonicos_triangulo`. */
    fun triangleHarmonics(count: Int): List<Harmonic> = (1..count step 2).map { k ->
        val sign = if (((k - 1) / 2) % 2 == 0) 1.0 else -1.0
        Harmonic(k.toDouble(), 8.0 / (DetMath.PI * DetMath.PI) * sign / (k * k))
    }

    /** `armonicos_sierra`. */
    fun sawHarmonics(count: Int): List<Harmonic> = (1..count).map { k ->
        val sign = if ((k + 1) % 2 == 0) 1.0 else -1.0
        Harmonic(k.toDouble(), 2.0 / DetMath.PI * sign / k * lanczos(k, count))
    }

    /** `normalizar_pico`: scales so the peak is [peak]. */
    fun normalizePeak(data: DoubleArray, peak: Double = 1.0): DoubleArray {
        var high = data[0]
        var low = data[0]
        for (v in data) {
            if (v > high) high = v
            if (v < low) low = v
        }
        var m = maxOf(high, -low)
        if (m == 0.0) m = 1.0
        return DoubleArray(data.size) { data[it] * peak / m }
    }

    /** `ciclo`: one cycle of [n] samples made of [harmonics], with peak 1. */
    fun cycle(harmonics: List<Harmonic>, n: Int): DoubleArray {
        val data = DoubleArray(n)
        for (h in harmonics) {
            val w = 2 * DetMath.PI * h.k / n
            for (i in 0 until n) data[i] += h.amplitude * DetMath.sin(w * i + h.phase)
        }
        return normalizePeak(data)
    }

    /** `_caida_hasta`: from 1 to 0 between t = 0 and t = [end], exponential with constant [tau]; exactly 0 at the end. */
    fun decayUntil(t: Double, tau: Double, end: Double): Double {
        if (tau <= 0) return 0.0
        val e = DetMath.exp(-end / tau)
        return maxOf(0.0, (DetMath.exp(-t / tau) - e) / (1.0 - e))
    }

    /** `_ruido_blanco`: [n] uniform values in [−1, 1] from Python's generator with [seed]. */
    fun whiteNoise(n: Int, seed: Long): DoubleArray {
        val rng = PyRandom(seed)
        return DoubleArray(n) { rng.uniform(-1.0, 1.0) }
    }

    /** `_paso_bajo`: a one-pole low-pass at [cutoff] Hz. */
    fun lowPass(x: DoubleArray, cutoff: Double, rate: Int): DoubleArray {
        val a = 1.0 - DetMath.exp(-2.0 * DetMath.PI * cutoff / rate)
        var y = 0.0
        return DoubleArray(x.size) {
            y += a * (x[it] - y)
            y
        }
    }

    /** `_semilla`: a seed from a name that never changes (CRC-32 of its UTF-8 bytes, low 16 bits). */
    fun seedOf(name: String): Long = (crc32(name.encodeToByteArray()) and 0xFFFFL)

    private fun crc32(bytes: ByteArray): Long {
        var crc = 0xFFFFFFFFL
        for (b in bytes) {
            crc = crc xor (b.toLong() and 0xFF)
            repeat(8) { crc = if ((crc and 1L) != 0L) (crc ushr 1) xor 0xEDB88320L else crc ushr 1 }
        }
        return crc xor 0xFFFFFFFFL
    }

    /** `fundido` (only the fade out, which is all the recipes use): the last [samples] samples fade to zero. */
    private fun fadeOut(x: DoubleArray, samples: Int) {
        val n = x.size
        for (i in 0 until minOf(samples, n)) x[n - 1 - i] *= i.toDouble() / samples
    }

    private fun addStrike(
        data: DoubleArray,
        strike: Strike,
        attack: Int,
        rate: Int,
        name: String,
    ) {
        val ng = minOf(attack, (rate * strike.ms / 1000).toInt())
        val r = lowPass(whiteNoise(ng, seedOf(name)), strike.cutoff, rate)
        for (i in 0 until ng) {
            val fade = 1.0 - i.toDouble() / ng
            data[i] += strike.level * r[i] * (fade * fade)
        }
    }

    fun partials(recipe: PartialsRecipe): Sample {
        val rate = recipe.rate
        val n = recipe.cycle
        val f0 = rate.toDouble() / n
        val attack = recipe.attackCycles * n
        val end = attack.toDouble() / rate
        val data = DoubleArray(attack + n)
        for (p in recipe.partials) {
            val harmonic = abs(p.k - round(p.k)) < 1e-9
            val w = 2.0 * DetMath.PI * p.k * f0 / rate
            for (i in 0 until attack) {
                val d = decayUntil(i.toDouble() / rate, p.tau, end)
                val amplitude = if (harmonic) p.loop + (p.start - p.loop) * d else p.start * d
                data[i] += amplitude * DetMath.sin(w * i)
            }
            if (harmonic) for (i in attack until attack + n) data[i] += p.loop * DetMath.sin(w * i)
        }
        recipe.strike?.let { addStrike(data, it, attack, rate, recipe.name) }
        return Sample(recipe.name, normalizePeak(data, PEAK), rate, f0 * recipe.rootScale, loopStart = attack)
    }

    fun fm(recipe: FmRecipe): Sample {
        val rate = recipe.rate
        val n = recipe.cycle
        val f0 = rate.toDouble() / n
        val attack = recipe.attackCycles * n
        val end = attack.toDouble() / rate
        val data = DoubleArray(attack + n) { i ->
            val t = i.toDouble() / rate
            val fall = if (i < attack) decayUntil(t, recipe.tau, end) else 0.0
            val index = recipe.indexLoop + (recipe.indexStart - recipe.indexLoop) * fall
            val modulator = DetMath.sin(2 * DetMath.PI * recipe.modulatorRatio * f0 * t)
            DetMath.sin(2 * DetMath.PI * recipe.carrierRatio * f0 * t + index * modulator)
        }
        recipe.strike?.let { addStrike(data, it, attack, rate, recipe.name) }
        return Sample(recipe.name, normalizePeak(data, PEAK), rate, f0, loopStart = attack)
    }

    fun pluck(recipe: PluckRecipe): Sample {
        val period = recipe.period
        val rate = recipe.rate
        val length = ceilDiv((recipe.seconds * rate).toInt(), period) * period
        val noise = whiteNoise(period, recipe.seed)
        val d = minOf(period - 1, maxOf(1, (recipe.position * period).toInt()))
        var excitation = DoubleArray(period) { i ->
            (if (i < d) i.toDouble() / d else (period - i).toDouble() / (period - d)) + 0.2 * noise[i]
        }
        excitation = minusMean(excitation)
        val a = 0.05 + 0.9 * recipe.brightness
        val filtered = DoubleArray(period)
        var y = 0.0
        repeat(3) {
            // A circular low-pass: three turns, and the last one stays.
            for (i in 0 until period) {
                y += a * (excitation[i] - y)
                filtered[i] = y
            }
        }
        excitation = minusMean(filtered)
        var data = DoubleArray(length)
        excitation.copyInto(data)
        val side = recipe.damping / 2
        val centre = 1.0 - recipe.damping
        for (i in period until length) {
            val before = if (i > period) data[i - period - 1] else excitation[period - 1]
            data[i] = recipe.rho * (side * data[i - period + 1] + centre * data[i - period] + side * before)
        }
        data = normalizePeak(data, PEAK)
        if (recipe.body != 0.0) {
            val w = 2.0 * DetMath.PI / period
            for (i in 0 until length) {
                data[i] += recipe.body * DetMath.sin(w * i) * maxOf(0.0, 1.0 - i / (0.7 * length))
            }
            data = normalizePeak(data, PEAK)
        }
        return Sample(recipe.name, data, rate, rate.toDouble() / period, loopStart = length - period)
    }

    private fun minusMean(x: DoubleArray): DoubleArray {
        var sum = 0.0
        for (v in x) sum += v
        val mean = sum / x.size
        return DoubleArray(x.size) { x[it] - mean }
    }

    private fun ceilDiv(a: Int, b: Int): Int = -((-a).floorDiv(b))

    fun pad(recipe: PadRecipe): Sample {
        val length = (recipe.rate * recipe.seconds).toInt()
        require(length % 16 == 0) { "${recipe.name}: the loop must last a multiple of 16 samples" }
        val data = DoubleArray(length)
        for ((cycles, phase) in listOf(recipe.cycles to 0.0, recipe.cycles + recipe.detune to 1.3)) {
            for (h in recipe.harmonics) {
                val w = 2.0 * DetMath.PI * h.k * cycles / length
                val f = phase * h.k
                for (i in 0 until length) data[i] += h.amplitude * DetMath.sin(w * i + f)
            }
        }
        val root = (recipe.cycles + recipe.detune / 2.0) / recipe.seconds
        return Sample(recipe.name, normalizePeak(data, PEAK), recipe.rate, root, loopStart = 0)
    }

    fun membrane(recipe: MembraneRecipe): Sample {
        val rate = recipe.rate
        val n = (recipe.seconds * rate).toInt()
        val noise = if (recipe.noise != 0.0) lowPass(whiteNoise(n, recipe.seed), recipe.noiseCutoff, rate) else null
        val data = DoubleArray(n)
        var phase = 0.0
        var phase2 = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            val f = recipe.endHz + (recipe.startHz - recipe.endHz) * DetMath.exp(-t / recipe.pitchTau)
            phase += 2.0 * DetMath.PI * f / rate
            phase2 += 2.0 * DetMath.PI * recipe.partialRatio * f / rate
            var v = DetMath.sin(phase) * DetMath.exp(-t / recipe.ampTau)
            if (recipe.partial != 0.0) v += recipe.partial * DetMath.sin(phase2) * DetMath.exp(-t / (recipe.ampTau * 0.4))
            if (noise != null) v += recipe.noise * noise[i] * DetMath.exp(-t / recipe.noiseTau)
            data[i] = v * minOf(1.0, i / (0.0006 * rate))
        }
        fadeOut(data, (0.004 * rate).toInt())
        return Sample(recipe.name, normalizePeak(data, PEAK), rate, rate.toDouble())
    }

    fun metal(recipe: MetalRecipe): Sample {
        val rate = recipe.rate
        val n = (recipe.seconds * rate).toInt()
        var data = DoubleArray(n)
        val white = whiteNoise(n, recipe.seed)
        val low = lowPass(white, recipe.highPassHz, rate)
        for ((j, t0) in recipe.shakes.withIndex()) {
            val i0 = (t0 * rate).toInt()
            val g = if (j == 0) 1.0 else 0.75
            for (p in recipe.partials) {
                val w = 2.0 * DetMath.PI * p.hz * (1.0 + 0.004 * j) / rate
                for (i in i0 until n) {
                    val t = (i - i0).toDouble() / rate
                    data[i] += g * p.amplitude * DetMath.sin(w * (i - i0)) * DetMath.exp(-t / p.tau)
                }
            }
            if (recipe.noise != 0.0) {
                for (i in i0 until n) {
                    data[i] += g * recipe.noise * (white[i] - low[i]) * DetMath.exp(-(i - i0).toDouble() / rate / recipe.noiseTau)
                }
            }
        }
        data = DoubleArray(n) { data[it] * minOf(1.0, it / (0.0005 * rate)) }
        fadeOut(data, (0.01 * rate).toInt())
        return Sample(recipe.name, normalizePeak(data, PEAK), rate, rate.toDouble())
    }

    fun chord(recipe: ChordRecipe): Sample {
        val length = (recipe.rate * recipe.seconds).toInt()
        require(length % 16 == 0) { "${recipe.name}: the loop must last a multiple of 16 samples" }
        val table = DoubleArray(length) { DetMath.sin(2.0 * DetMath.PI * it / length) }
        val data = DoubleArray(length)
        for ((j, semitones) in recipe.intervals.withIndex()) {
            val c = round(recipe.rootCycles * DetMath.exp2(semitones / 12.0)).toInt()
            for ((copy, cycles) in intArrayOf(c, c + recipe.detune).withIndex()) {
                for (h in recipe.harmonics) {
                    val k = h.k.toInt()
                    val m = k * cycles
                    if (m >= length / 2) continue
                    val offset = (j * 997 + copy * 1499 + k * 331) % length
                    val amplitude = h.amplitude / (1.0 + 0.12 * j)
                    for (i in 0 until length) data[i] = data[i] + amplitude * table[((m.toLong() * i + offset) % length).toInt()]
                }
            }
        }
        val root = (recipe.rootCycles + recipe.detune / 2.0) / recipe.seconds
        return Sample(recipe.name, normalizePeak(data, PEAK), recipe.rate, root, loopStart = 0)
    }

    /** `_formantes`: harmonics shaped by formants (Hz, width, weight), an «oo» of a choir. */
    fun formants(rootHz: Double, peaks: List<Triple<Double, Double, Double>>, count: Int = 14): List<Harmonic> = (1..count).map { k ->
        val fk = k * rootHz
        var a = 0.0
        for ((f, width, weight) in peaks) {
            val x = (fk - f) / width
            a += weight * DetMath.exp(-(x * x))
        }
        Harmonic(k.toDouble(), a + 0.08 / k)
    }

    /** `_ruido_banda_circular`: band noise from [low] to [high] Hz that repeats every [n] samples without a seam. */
    private fun circularBandNoise(
        n: Int,
        low: Double,
        high: Double,
        rate: Int,
        seed: Long,
    ): DoubleArray {
        val white = whiteNoise(n, seed)
        fun circularLowPass(cutoff: Double): DoubleArray {
            val a = 1.0 - DetMath.exp(-2.0 * DetMath.PI * cutoff / rate)
            var y = 0.0
            val out = DoubleArray(n)
            repeat(3) {
                for (i in 0 until n) {
                    y += a * (white[i] - y)
                    out[i] = y
                }
            }
            return out
        }
        val upper = circularLowPass(high)
        val lower = circularLowPass(low)
        val band = DoubleArray(n) { upper[it] - lower[it] }
        var r = rms(band)
        if (r == 0.0) r = 1.0
        return DoubleArray(n) { band[it] / r }
    }

    fun rms(x: DoubleArray): Double {
        if (x.isEmpty()) return 0.0
        var sum = 0.0
        for (v in x) sum += v * v
        return sqrt(sum / x.size)
    }

    fun breath(recipe: BreathRecipe): Sample {
        val rate = recipe.rate
        val f0 = rate.toDouble() / recipe.cycle
        val attack = recipe.attackCycles * recipe.cycle
        val loop = recipe.loopCycles * recipe.cycle
        val total = attack + loop
        val data = DoubleArray(total)
        for (h in recipe.harmonics) {
            val w = 2.0 * DetMath.PI * h.k * f0 / rate
            for (i in 0 until total) data[i] += h.amplitude * DetMath.sin(w * i)
        }
        var tonePeak = 0.0
        for (v in data) tonePeak = maxOf(tonePeak, abs(v))
        if (tonePeak == 0.0) tonePeak = 1.0
        val band = circularBandNoise(loop, recipe.bandLow, recipe.bandHigh, rate, recipe.seed)
        for (i in 0 until total) data[i] += recipe.breath * tonePeak * 0.33 * band[(i - attack).mod(loop)]
        val nc = minOf(attack, (rate * recipe.chiffMs / 1000).toInt())
        val puff = lowPass(whiteNoise(nc, recipe.seed + 1), recipe.bandHigh * 1.5, rate)
        for (i in 0 until nc) {
            val fade = 1.0 - i.toDouble() / nc
            data[i] += recipe.chiff * tonePeak * puff[i] * (fade * fade)
        }
        return Sample(recipe.name, normalizePeak(data, PEAK), rate, f0, loopStart = attack)
    }

    /** One stretch of a call: start (s), length (s), from Hz, to Hz and trill rate (0 = none). */
    private class Stretch(
        val start: Double,
        val length: Double,
        val fromHz: Double,
        val toHz: Double,
        val trill: Double,
    )

    private val stretches = mapOf(
        Call.Whistle to listOf(Stretch(0.0, 0.085, 2700.0, 3500.0, 0.0), Stretch(0.13, 0.12, 3400.0, 2800.0, 0.0)),
        Call.Trill to listOf(Stretch(0.0, 0.42, 4000.0, 4000.0, 18.0)),
        Call.Warble to listOf(
            Stretch(0.0, 0.045, 5200.0, 3600.0, 0.0),
            Stretch(0.09, 0.045, 5300.0, 3700.0, 0.0),
            Stretch(0.18, 0.05, 5000.0, 3400.0, 0.0),
        ),
        Call.Coo to listOf(Stretch(0.0, 0.16, 560.0, 520.0, 0.0), Stretch(0.24, 0.22, 600.0, 500.0, 0.0)),
    )

    fun bird(recipe: BirdRecipe): Sample {
        val rate = recipe.rate
        val rng = PyRandom(recipe.seed)
        if (recipe.call == Call.Frog) {
            val n = (0.34 * rate).toInt()
            val data = DoubleArray(n)
            for (p in 0 until 13) {
                val i0 = (p * rate / 38.0).toInt()
                for (i in i0 until minOf(n, i0 + (0.02 * rate).toInt())) {
                    val t = (i - i0).toDouble() / rate
                    val tone = DetMath.sin(2 * DetMath.PI * 430 * t) + 0.5 * DetMath.sin(2 * DetMath.PI * 1150 * t)
                    data[i] += tone * DetMath.exp(-t / 0.006) * minOf(1.0, (i - i0) / 24.0)
                }
            }
            val shaped = DoubleArray(n) { data[it] * DetMath.sin(DetMath.PI * it / n) }
            return Sample(recipe.name, normalizePeak(shaped, PEAK), rate, rate.toDouble(), brr = false)
        }
        val parts = stretches.getValue(recipe.call)
        var end = 0.0
        for (s in parts) end = maxOf(end, s.start + s.length)
        val n = ((end + 0.03) * rate).toInt()
        val data = DoubleArray(n)
        for (s in parts) {
            val i0 = (s.start * rate).toInt()
            val nd = (s.length * rate).toInt()
            var phase = rng.uniform(0.0, 2 * DetMath.PI)
            for (j in 0 until nd) {
                val u = j.toDouble() / nd
                var f = s.fromHz * DetMath.pow(s.toHz / s.fromHz, u)
                if (s.trill != 0.0) f += 350.0 * DetMath.sin(2 * DetMath.PI * s.trill * j / rate)
                phase += 2.0 * DetMath.PI * f / rate
                var envelope = minOf(1.0, j / (0.006 * rate), (nd - j) / (0.012 * rate))
                if (s.trill != 0.0) envelope *= 0.6 + 0.4 * DetMath.sin(2 * DetMath.PI * s.trill * j / rate + 1.0)
                data[i0 + j] += envelope * (DetMath.sin(phase) + 0.12 * DetMath.sin(2 * phase))
            }
        }
        return Sample(recipe.name, normalizePeak(data, PEAK), rate, rate.toDouble(), brr = false)
    }

    fun rainStick(recipe: RainStickRecipe): Sample {
        val rate = recipe.rate
        val seconds = recipe.seconds
        val rng = PyRandom(recipe.seed)
        val n = (seconds * rate).toInt()
        val data = DoubleArray(n)
        val white = whiteNoise(n, recipe.seed + 3)
        val low = lowPass(white, 2500.0, rate)

        fun density(t: Double): Double {
            val tail = maxOf(0.0, 1.0 - maxOf(0.0, t - 0.6) / (seconds - 0.6))
            return minOf(1.0, t / 0.35) * DetMath.pow(tail, 1.5)
        }
        repeat(900) {
            val t = rng.uniform(0.0, seconds)
            if (rng.random() > density(t)) return@repeat
            val i0 = (t * rate).toInt()
            val amplitude = rng.uniform(0.2, 1.0) * density(t)
            if (rng.random() < 0.6) {
                for (j in 0 until minOf(n - i0, 40)) {
                    data[i0 + j] += amplitude * (white[i0 + j] - low[i0 + j]) * (1.0 - j / 40.0) * minOf(1.0, (j + 1) / 6.0)
                }
            } else {
                val f = rng.uniform(2500.0, 7000.0)
                for (j in 0 until minOf(n - i0, (0.006 * rate).toInt())) {
                    data[i0 + j] += 0.5 * amplitude * DetMath.sin(2 * DetMath.PI * f * j / rate) * DetMath.exp(-j / (0.0015 * rate))
                }
            }
        }
        val fadeIn = (0.01 * rate).toInt()
        for (i in 0 until minOf(fadeIn, n)) data[i] *= i.toDouble() / fadeIn
        fadeOut(data, (0.05 * rate).toInt())
        return Sample(recipe.name, normalizePeak(data, PEAK), rate, rate.toDouble(), brr = false)
    }
}
