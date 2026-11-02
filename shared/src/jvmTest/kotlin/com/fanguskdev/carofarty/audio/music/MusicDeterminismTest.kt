package com.fanguskdev.carofarty.audio.music

import com.fanguskdev.carofarty.game.Scenario
import kotlin.test.Test
import kotlin.test.assertEquals

/** The same seed and the same calls give the same music (AUD-26); MusicStreamTest checks that it is so on every platform. */
class MusicDeterminismTest {
    /** A rendered minute is the same every time with the same seed and the same calls, whatever the buffers. */
    @Test
    fun aMinuteIsTheSameEveryTime() {
        val cues = listOf(Cue(10.0) { it.setLevel(3) }, Cue(30.0) { it.setLevel(4) }, Cue(45.0) { it.setTense(true) })
        for (scenario in Scenario.entries) {
            val first = Runs.fingerprint(Runs.render(Runs.stream(scenario, seed = 77, rate = 48_000), 60.0, cues, buffer = 480))
            val second = Runs.fingerprint(Runs.render(Runs.stream(scenario, seed = 77, rate = 48_000), 60.0, cues, buffer = 4096))
            assertEquals(first, second, "${scenario.name}: the same minute came out different")
        }
    }
}
