package com.fanguskdev.carofarty.audio.music

import com.fanguskdev.carofarty.game.Scenario
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The music of a run as the game will use it (AUD-19, AUD-25, AUD-26), on every platform. */
class MusicStreamTest {
    /** A short run with everything in it: the layers come in, the tense version, back to the music and the end. */
    private val quickRun = listOf(
        Cue(4.0) { it.setLevel(2) },
        Cue(8.0) { it.setLevel(3) },
        Cue(12.0) { it.setLevel(4) },
        Cue(18.0) { it.setTense(true) },
        Cue(26.0) { it.setTense(false) },
        Cue(30.0) { it.setLevel(5) },
        Cue(36.0) { it.end() },
    )

    @Test
    fun everyScenarioStaysUnderMinusOneDecibelAndWithoutDcOffset() {
        for (scenario in Scenario.entries) {
            val stream = Runs.stream(scenario, seed = 5, rate = 48_000)
            val pcm = Runs.render(stream, 40.0, quickRun)
            assertTrue(Runs.peakDb(pcm) <= -1.0, "$scenario: peak at ${Runs.peakDb(pcm)} dBFS")
            assertTrue(Runs.dcDb(pcm) < -60.0, "$scenario: DC at ${Runs.dcDb(pcm)} dBFS")
            assertTrue(Runs.rmsDb(pcm, 12 * 48_000, 36 * 48_000) > -30.0, "$scenario: too quiet")
            assertEquals(0, stream.engine.stats.pitchClamps, "$scenario: a note over the top of the pitch register")
            assertEquals(0, stream.engine.stats.lateCuts, "$scenario: a note was cut too late")
        }
    }

    @Test
    fun theSameSeedGivesTheSameMusicWhateverTheBuffers() {
        for (scenario in Scenario.entries) {
            val a = Runs.render(Runs.stream(scenario, seed = 9, rate = 44_100), 6.0, quickRun, buffer = 4096)
            val b = Runs.render(Runs.stream(scenario, seed = 9, rate = 44_100), 6.0, quickRun, buffer = 333)
            assertContentEquals(a, b, "$scenario")
            val c = Runs.render(Runs.stream(scenario, seed = 10, rate = 44_100), 6.0, quickRun)
            assertFalse(a.contentEquals(c), "$scenario: another seed should give another song")
        }
    }

    /**
     * The music is the same, sample for sample, on the JVM, Android and iOS: these fingerprints were taken on the JVM
     * and this test runs on the three. If the music is changed on purpose, they change: print the new ones and update
     * them here.
     */
    @Test
    fun theMusicIsTheSameOnEveryPlatform() {
        val expected = mapOf(
            Scenario.Bosque to 0xd85045027aafdba9uL.toLong(),
            Scenario.Nieve to 0x637981b9a20b9505uL.toLong(),
            Scenario.Desierto to 0xe65e873ba518ed3auL.toLong(),
        )
        val found = HashMap<Scenario, Long>()
        for (scenario in Scenario.entries) {
            val pcm = Runs.render(Runs.stream(scenario, seed = 2026, rate = 48_000), 8.0, listOf(Cue(1.0) { it.setLevel(4) }))
            found[scenario] = Runs.fingerprint(pcm)
        }
        val printed = found.entries.joinToString { "${it.key}: 0x${it.value.toULong().toString(16)}" }
        assertEquals(expected, found.toMap(), "the fingerprints here are $printed")
    }

    @Test
    fun theWarningComesOnTheNextBeatAndTheTenseVersionOnTheNextBar() {
        val stream = Runs.stream(Scenario.Nieve, rate = Chip.RATE)
        val engine = stream.engine
        Runs.render(stream, 10.3)
        assertEquals(Composer.A, engine.currentSection)
        val asked = engine.now
        stream.setTense(true)
        stream.render(ShortArray(0), 0, 0)
        val beat = Chip.RATE * 60.0 / 100.0 // a crotchet of the Nieve at 100 BPM
        val warning = engine.warningTime
        assertTrue(warning >= asked && warning - asked <= beat, "the warning should come on the next beat")
        // On a beat of the bar: a whole number of beats from the start (the tempo has not moved at level 1).
        val beats = warning / beat
        assertEquals(kotlin.math.round(beats), beats, 1e-9)
        Runs.render(stream, 2.0)
        assertEquals(Composer.TENSE, engine.currentSection)
        stream.setTense(false)
        Runs.render(stream, 2.0)
        assertEquals(Composer.A, engine.currentSection)
    }

    @Test
    fun theLayersComeInWithTheSpeed() {
        val stream = Runs.stream(Scenario.Desierto, rate = Chip.RATE)
        val engine = stream.engine
        Runs.render(stream, 3.0)
        val layers = engine.style.layers
        assertTrue(layers[0].all { engine.channelGain(it) == 1.0 }, "C0 always plays")
        for (l in 1 until 4) assertTrue(layers[l].all { engine.channelGain(it) == 0.0 }, "C$l before its level")
        stream.setLevel(4)
        Runs.render(stream, 6.0)
        for (l in 0 until 4) assertTrue(layers[l].all { engine.channelGain(it) == 1.0 }, "C$l at level 4")
        // Going down waits two bars.
        stream.setLevel(1)
        Runs.render(stream, 0.1)
        assertTrue(layers[3].all { engine.channelGain(it) == 1.0 }, "the melody leaves two bars late")
        Runs.render(stream, 8.0)
        assertTrue(layers[3].all { engine.channelGain(it) == 0.0 }, "the melody has left")
    }

    @Test
    fun afterTheEndOnlySilenceIsLeft() {
        for (scenario in Scenario.entries) {
            val stream = Runs.stream(scenario, seed = 4, rate = 22_050)
            Runs.render(stream, 5.0, listOf(Cue(4.0) { it.end() }))
            assertFalse(stream.isFinished)
            val tail = Runs.render(stream, 3.0)
            assertTrue(stream.isFinished, "$scenario should have finished")
            val last = tail.copyOfRange(tail.size - 22_050 / 2, tail.size)
            assertTrue(last.all { it.toInt() == 0 }, "$scenario: not silent after the end")
        }
    }
}
