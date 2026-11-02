package com.fanguskdev.carofarty.audio.music

import com.fanguskdev.carofarty.game.Scenario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Half an hour of music never plays the same 8 bars twice (AUD-19, AUD-26): in a run of 30 minutes, with the levels of
 * the speeds proposed for AUD-04 (2 at 14 s, 3 at 34 s, 4 at 1.2 min and 5 at 2.7 min), no two sections have the same
 * notes in all their channels. The tense version is a loop on purpose and is not counted.
 */
class MusicVarietyTest {
    @Test
    fun halfAnHourDoesNotRepeatASection() {
        for (scenario in Scenario.entries) {
            val stream = Runs.stream(scenario, seed = 2024)
            val seen = HashSet<String>()
            var sections = 0
            var repeats = 0
            stream.engine.onSection = { kind, _, notes ->
                if (kind != Composer.TENSE) {
                    sections++
                    if (!seen.add(contentOf(notes))) repeats++
                }
            }
            val cues = listOf(
                Cue(14.0) { it.setLevel(2) },
                Cue(34.0) { it.setLevel(3) },
                Cue(72.0) { it.setLevel(4) },
                Cue(162.0) { it.setLevel(5) },
            )
            Runs.render(stream, 30 * 60.0, cues, buffer = 4096)
            println("${scenario.name}: $sections sections in 30 minutes, $repeats repeated")
            assertTrue(sections > 100, "${scenario.name}: only $sections sections")
            assertEquals(0, repeats, "${scenario.name}: a section repeated note for note")
            assertEquals(0, stream.engine.stats.pitchClamps, "${scenario.name}: a note over the pitch register's top")
            assertEquals(0, stream.engine.stats.lateCuts, "${scenario.name}: a note cut too late")
        }
    }

    /** The notes of a section as they are written: every channel's positions, lengths, pitches and instruments. */
    private fun contentOf(notes: Array<NoteList>): String {
        val text = StringBuilder()
        for (list in notes) {
            for (i in 0 until list.size) {
                text.append(list.pos[i]).append(',').append(list.dur[i]).append(',').append(list.pitch[i]).append(',')
                    .append(list.instrument[i]).append(',').append(list.quality[i]).append(',').append(list.ornament[i]).append(';')
            }
            text.append('|')
        }
        return text.toString()
    }
}
