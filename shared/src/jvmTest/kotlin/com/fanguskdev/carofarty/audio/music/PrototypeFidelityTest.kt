package com.fanguskdev.carofarty.audio.music

import com.fanguskdev.carofarty.game.Scenario
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The port composes exactly what the approved prototype composes (AUD-26). tools/musica_referencia.py drives the
 * prototype's own generador.py as MusicEngine drives the composer in real time (the same demo run, seed 7) and prints
 * a fingerprint of every note it schedules: start sample, channel, instrument, pitch, length and gain. These are those
 * fingerprints. The music rendered from the two note lists also agrees to about 1e-8 (see docs/musica.md).
 *
 * If the music is changed on purpose, this test is expected to fail: update the fingerprints, or drop the test once
 * the music no longer follows the prototype.
 */
class PrototypeFidelityTest {
    private val expected = mapOf(
        Scenario.Nieve to (0x0c73f51c450e95bfuL.toLong() to 1083),
        Scenario.Desierto to (0xa696322ec45e556euL.toLong() to 1525),
        Scenario.Bosque to (0xfa230e49e138e113uL.toLong() to 1577),
    )

    @Test
    fun theDemoRunHasTheNotesOfThePrototype() {
        for (scenario in Scenario.entries) {
            val stream = Runs.stream(scenario)
            val engine = stream.engine
            val text = StringBuilder()
            var notes = 0
            engine.onNote = { c, q, s ->
                val instrument = engine.bank.instruments[q.instrument[s]]
                text.append(q.start[s]).append(',').append(c).append(',').append(instrument.recipe.name).append(',')
                    .append(q.pitch[s]).append(',').append(q.length[s]).append(',').append(round(q.gain[s] * 1e9).toLong()).append(';')
                notes++
            }
            Runs.render(stream, Runs.SECONDS, Runs.demo)
            val (fingerprint, count) = expected.getValue(scenario)
            assertEquals(count, notes, "${scenario.name}: notes")
            assertEquals(fingerprint, Runs.fingerprint(text.toString()), "${scenario.name}: the notes differ from the prototype's")
        }
    }
}
