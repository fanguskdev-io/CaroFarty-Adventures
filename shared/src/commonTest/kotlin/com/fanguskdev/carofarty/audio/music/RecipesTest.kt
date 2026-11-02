package com.fanguskdev.carofarty.audio.music

import com.fanguskdev.carofarty.game.Scenario
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The recipes of the bank (sinte.py's `muestra_*`): what each makes, and that every scenario's instruments are made. */
class RecipesTest {
    private fun peak(x: DoubleArray) = x.maxOf { abs(it) }

    private fun largestStep(x: DoubleArray): Double = (1 until x.size).maxOf { abs(x[it] - x[it - 1]) }

    @Test
    fun aCycleOfAWaveLoopsOnItself() {
        for (harmonics in listOf(Synthesis.pulseHarmonics(0.25, 8), Synthesis.triangleHarmonics(9), Synthesis.sawHarmonics(8))) {
            val cycle = CycleRecipe("onda", harmonics, n = 64).build()
            assertEquals(0, cycle.loopStart)
            assertEquals(250.0, cycle.root)
            // Normalized to 1, then through the codec, whose loudest blocks take steps of 1/8.
            assertTrue(peak(cycle.values()) in 0.85..1.0, "a cycle's peak: ${peak(cycle.values())}")
        }
        // A triangle only has odd harmonics, with alternating signs.
        assertEquals(listOf(1.0, 3.0, 5.0, 7.0, 9.0), Synthesis.triangleHarmonics(9).map { it.k })
        assertTrue(Synthesis.triangleHarmonics(3)[1].amplitude < 0)
    }

    @Test
    fun aPadIsALongLoopWithAChorus() {
        val pad = PadRecipe("colchon", 16000, 1.0, 250, listOf(Harmonic(1.0, 1.0), Harmonic(2.0, 0.5))).build()
        assertEquals(16000, pad.length)
        assertEquals(0, pad.loopStart)
        assertEquals(250.5, pad.root)
        // A whole number of cycles per loop: from the end to the start is a step like any other.
        assertTrue(abs(pad.values()[pad.length - 1] - pad.values()[0]) <= largestStep(pad.values()), "the loop jumps")
    }

    @Test
    fun aPluckedStringLoopsItsLastPeriod() {
        val string = PluckRecipe("cuerda", 64, 16000, 0.3, seed = 3).build()
        assertEquals(string.length - 64, string.loopStart)
        assertEquals(250.0, string.root)
        // Normalized to 0.95; the codec may overshoot a little (up to its −32768).
        assertTrue(peak(string.values()) in 0.85..1.001, "the string's peak: ${peak(string.values())}")
        // It decays: the loop is quieter than the pluck.
        val loop = string.values().copyOfRange(string.loopStart, string.length)
        assertTrue(peak(loop) < peak(string.values()), "${peak(loop)} vs ${peak(string.values())}")
    }

    @Test
    fun drumsAndMetalsStartAndEndInSilence() {
        val drum = MembraneRecipe("tambor", 16000, 150.0, 48.0, 0.028, 0.13, 0.32, noise = 0.25, noiseTau = 0.003).build()
        val bell = MetalRecipe("campanilla", 32000, listOf(MetalPartial(3150.0, 0.5, 0.9)), 0.5, noise = 0.05).build()
        for (s in listOf(drum, bell)) {
            assertEquals(-1, s.loopStart)
            assertEquals(s.rate.toDouble(), s.root)
            assertTrue(abs(s.values()[0]) < 0.01 && abs(s.values()[s.length - 1]) < 0.01, "${s.name} clicks at its edges")
        }
    }

    @Test
    fun aChordHasEveryNoteAsAWholeNumberOfCycles() {
        val chord = ChordRecipe("acorde", Quality.Major.intervals, listOf(Harmonic(1.0, 1.0))).build()
        assertEquals(220.5, chord.root)
        assertEquals(0, chord.loopStart)
        assertTrue(abs(chord.values()[chord.length - 1] - chord.values()[0]) <= largestStep(chord.values()), "the loop jumps")
    }

    @Test
    fun windsBirdsAndTheRainStickAreMade() {
        val wind = BreathRecipe("viento", 32, 16000, 40, 250, listOf(Harmonic(1.0, 1.0))).build()
        assertEquals(40 * 32, wind.loopStart)
        for (call in Call.entries) assertTrue(peak(BirdRecipe("ave", call, 5).build().values()) > 0.9, "$call")
        assertTrue(RainStickRecipe("palo").build().length > 32000)
    }

    @Test
    fun theSeedOfANameIsItsCrc32() {
        // zlib.crc32(b'marimba') & 0xFFFF, as the prototype computes it.
        assertEquals(0x251AL, Synthesis.seedOf("marimba"))
    }

    @Test
    fun everyScenarioHasItsInstruments() {
        for (scenario in Scenario.entries) {
            val bank = Runs.bank(scenario)
            assertTrue(bank.instruments.size >= 10, "$scenario")
            for (instrument in bank.instruments) {
                assertTrue(instrument.recipe.noise >= 0 || instrument.samples.isNotEmpty(), instrument.recipe.name)
                for (sample in instrument.samples) assertTrue(sample.length % 16 == 0, sample.name)
            }
        }
    }
}
