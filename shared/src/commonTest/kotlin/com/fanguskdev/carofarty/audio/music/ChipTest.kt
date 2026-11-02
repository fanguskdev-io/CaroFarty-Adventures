package com.fanguskdev.carofarty.audio.music

import com.fanguskdev.carofarty.game.Scenario
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The chip the music imitates (AUD-24), piece by piece, with the checks of the prototype's verificar.py. */
class ChipTest {
    @Test
    fun theEchoFilterIsTheManualsScaledToAddUpTo128() {
        assertEquals(136, Chip.manualFir.sumOf { abs(it) }, "the manual's filter goes over the limit")
        assertTrue(Chip.echoFir.sumOf { abs(it) } <= 128)
        // Scaled by 128/136 and rounded so that the sum stays at 128 (8 · 128/136 = 7.5 becomes 7).
        for (i in 0 until 8) assertTrue(abs(Chip.manualFir[i] * 128.0 / 136 - Chip.echoFir[i]) < 0.6)
    }

    @Test
    fun everyScenarioKeepsItsEchoWithinTheChip() {
        for (scenario in Scenario.entries) {
            val echo = styleOf(scenario).echo
            assertTrue(echo.fir.sumOf { abs(it) } <= 128, "$scenario: Σ|FIR| > 128")
            assertTrue(abs(echo.efb) <= Chip.MAX_FEEDBACK, "$scenario: EFB over \$50")
            assertTrue(echo.edl in 1..15, "$scenario: EDL")
        }
    }

    @Test
    fun theMusicHasSixVoicesAndTheEffectsTwo() {
        assertEquals(6, Chip.MUSIC_VOICES)
        assertEquals(2, Chip.EFFECT_VOICES)
        assertEquals(8, Chip.VOICES)
        for (scenario in Scenario.entries) {
            val style = styleOf(scenario)
            val channels = style.layers.flatMap { it.toList() }
            assertEquals(channels.size, channels.toSet().size, "$scenario: a channel in two layers")
            assertTrue(channels.all { it in 0 until Chip.MUSIC_VOICES }, "$scenario: more channels than voices")
        }
    }

    @Test
    fun theGaussianWeightsAddUpToOneAndCutTheNyquist() {
        for (k in 0 until 256) {
            val sum = Chip.gauss[4 * k] + Chip.gauss[4 * k + 1] + Chip.gauss[4 * k + 2] + Chip.gauss[4 * k + 3]
            assertEquals(1.0, sum, 1e-12)
        }
        assertEquals(0.18, Chip.gauss[0], 0.005)
        assertEquals(0.64, Chip.gauss[1], 0.005)
        assertEquals(0.18, Chip.gauss[2], 0.005)
        // A sample at the Nyquist frequency (+1, −1, +1…) comes out at 0.64 − 2·0.18 ≈ 0.28: about 11 dB less.
        val nyquist = Chip.gauss[1] - Chip.gauss[0] - Chip.gauss[2] + Chip.gauss[3]
        assertTrue(20 * kotlin.math.log10(nyquist) < -10, "only ${20 * kotlin.math.log10(nyquist)} dB at the Nyquist frequency")
    }

    @Test
    fun brrKeepsASineCloseAndSilenceSilent() {
        val sine = DoubleArray(512) { 0.9 * sin(2 * PI * it / 64) }
        val coded = Brr.roundTrip(sine)
        var error = 0.0
        for (i in sine.indices) error += (coded[i] - sine[i]) * (coded[i] - sine[i])
        val snr = 10 * kotlin.math.log10(sine.sumOf { it * it } / error)
        assertTrue(snr > 30, "BRR loses too much: $snr dB")
        assertContentEquals(DoubleArray(64), Brr.roundTrip(DoubleArray(64)))
        // 4 bits per sample: each decoded sample is the prediction plus k · 2^shift, with k in −8…7.
        assertEquals(512, coded.size)
    }

    @Test
    fun aSampleWithALoopMustLastAMultipleOf16() {
        val looped = Sample("test", DoubleArray(32) { sin(2 * PI * it / 32) }, 16000, 500.0, loopStart = 0)
        assertEquals(32, looped.length)
        // The interpolation reads three samples past the end: the loop's first ones.
        assertEquals(looped.played[0], looped.played[32])
        val unlooped = Sample("test", DoubleArray(20) { 0.5 }, 16000, 16000.0, brr = false)
        assertEquals(32, unlooped.length)
        assertEquals(0f, unlooped.played[33])
    }

    @Test
    fun theNoiseGeneratorRepeatsEvery32767Steps() {
        var lfsr = 0x4000
        var period = 0
        var sum = 0.0
        do {
            lfsr = Chip.nextNoise(lfsr)
            val v = Chip.noiseLevel(lfsr)
            assertTrue(v >= -1.0 && v < 1.0)
            sum += v
            period++
        } while (lfsr != 0x4000)
        assertEquals(32767, period)
        assertTrue(abs(sum / period) < 0.01, "the noise has an offset")
    }

    @Test
    fun theEchoRepeatsDullerEachTime() {
        val echo = Echo(EchoSettings(1, 0x40, 0x7F))
        val out = DoubleArray(512 * 4)
        for (i in out.indices) out[i] = echo.process(if (i == 0) 1.0 else 0.0)
        // Nothing comes back before the delay (512 samples) plus the filter's first taps.
        for (i in 0 until 512) assertEquals(0.0, out[i])
        val first = (512 until 1024).sumOf { out[it] * out[it] }
        val second = (1024 + 8 until 1536 + 8).sumOf { out[it] * out[it] }
        assertTrue(first > 0 && second < first * 0.5, "each repeat should be weaker: $first then $second")
    }

    @Test
    fun theWarningIsALongSoundOfFourNotes() {
        val warning = Effects.fewPointsWarning()
        // la5 mi5 la5 mi5 every 105 ms, 92 ms each, and 60 ms of silence after them.
        assertEquals(Chip.ms(315.0) + Chip.ms(92.0) + Chip.ms(60.0), warning.size)
        assertTrue(Effects.activeRms(warning) > 0.05)
    }

    @Test
    fun theLimitNeverReachesMinusOneDecibel() {
        assertEquals(0.5, MusicStream.limit(0.5))
        assertEquals(-0.7, MusicStream.limit(-0.7))
        for (x in listOf(0.8, 0.9, 1.0, 2.0, 10.0, 1e6)) {
            val y = MusicStream.limit(x)
            assertTrue(y < MusicStream.CEILING && y > 0.79, "limit($x) = $y")
            assertEquals(-y, MusicStream.limit(-x))
        }
        assertTrue(MusicStream.toPcm(5.0) <= (MusicStream.CEILING * 32767).toInt())
        assertEquals(0, MusicStream.toPcm(0.0).toInt())
    }

    @Test
    fun milliSecondsRoundLikeThePrototype() {
        assertEquals(256, Chip.ms(8.0))
        assertEquals(16, Chip.ms(0.5))
        assertEquals(160, Chip.ms(5.0))
        assertEquals(48, Chip.ms(1.5))
    }

    @Test
    fun theResamplerIsExactForTheRatesOfThePhones() {
        for ((rate, phases) in listOf(48_000 to 3, 44_100 to 441, 22_050 to 441)) {
            val g = Resampler.gcd(Chip.RATE, rate)
            assertEquals(phases, rate / g, "$rate Hz")
            val table = Resampler.kernels(Chip.RATE, phases, 64, 0.445 * minOf(Chip.RATE, rate), 8.6)
            for (p in 0 until phases) {
                var sum = 0.0
                for (k in 0 until 64) sum += table[p * 64 + k]
                assertEquals(1.0, sum, 1e-12, "$rate Hz, phase $p")
            }
        }
    }

    @Test
    fun aToneKeepsItsPitchAndLevelThroughTheResampler() {
        for (rate in listOf(48_000, 44_100, 22_050)) {
            var n = 0L
            val source = SampleSource { out, count ->
                for (i in 0 until count) out[i] = sin(2 * PI * 1000.0 * (n + i) / Chip.RATE)
                n += count
            }
            val resampler = Resampler(source, Chip.RATE, rate)
            val out = DoubleArray(rate / 4)
            resampler.process(out, out.size)
            var worst = 0.0
            for (i in 200 until out.size) worst = maxOf(worst, abs(out[i] - sin(2 * PI * 1000.0 * i / rate)))
            assertTrue(worst < 2e-3, "$rate Hz: the tone strays $worst")
        }
    }

    @Test
    fun theResamplerGivesTheSameWhateverTheBuffers() {
        fun run(chunks: IntArray): DoubleArray {
            var n = 0L
            val source = SampleSource { out, count ->
                for (i in 0 until count) out[i] = sin(0.01 * (n + i)) + 0.3 * sin(0.37 * (n + i))
                n += count
            }
            val resampler = Resampler(source, Chip.RATE, 44_100)
            val out = DoubleArray(10_000)
            var done = 0
            var k = 0
            while (done < out.size) {
                val count = minOf(chunks[k++ % chunks.size], out.size - done)
                val part = DoubleArray(count)
                resampler.process(part, count)
                part.copyInto(out, done)
                done += count
            }
            return out
        }
        assertContentEquals(run(intArrayOf(10_000)), run(intArrayOf(1, 7, 300, 64, 1023)))
    }

    @Test
    fun theDeterministicSquareRootIsTheIeeeOne() {
        // The maths of the port lean on sqrt being exact everywhere (IEEE 754 requires it).
        assertEquals(1.4142135623730951, sqrt(2.0))
    }
}
