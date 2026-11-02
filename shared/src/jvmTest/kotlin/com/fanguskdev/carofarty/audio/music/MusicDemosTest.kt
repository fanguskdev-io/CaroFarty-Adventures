package com.fanguskdev.carofarty.audio.music

import com.fanguskdev.carofarty.audio.wav
import com.fanguskdev.carofarty.game.Scenario
import java.io.File
import java.util.Locale
import kotlin.math.round
import kotlin.test.Test

/**
 * The demos to listen to (AUD-24 to AUD-26): the prototype's simulated run of 88.4 s for each scenario, with the same
 * seed (7), into build/musica/<escenario>.wav at 32 kHz, and its timing as Audacity labels beside it
 * (<escenario>-marcas.txt), to compare by ear with the prototype's demos/. The music only: the end-of-run effect and
 * jingle are the game's.
 */
class MusicDemosTest {
    @Test
    fun theDemosAreWritten() {
        val folder = File("build/musica").apply { mkdirs() }
        for (scenario in Scenario.entries) {
            val stream = Runs.stream(scenario)
            val marks = ArrayList<Pair<Double, String>>()
            fun mark(seconds: Double, text: String) = marks.add(seconds to text)
            var bpm = 0.0
            stream.engine.onSection = { kind, start, _ ->
                bpm = stream.engine.sectionBpm
                mark(start / Chip.RATE, "sección ${SECTION_NAMES[kind]} (%.1f BPM)".format(Locale.ROOT, bpm))
            }
            val cues = Runs.demo.map { cue ->
                val nominal = round(cue.seconds)
                Cue(cue.seconds) {
                    cue.action(it)
                    mark(nominal, describe(nominal))
                }
            }
            val pcm = Runs.render(stream, Runs.SECONDS, cues)
            mark(stream.engine.warningTime / Chip.RATE, "aviso de pocos puntos, en el pulso siguiente")
            mark(stream.engine.endTime / Chip.RATE, "la música se retira (200 ms), en el pulso siguiente")
            val name = scenario.name.lowercase()
            File(folder, "$name.wav").writeBytes(wav(pcm, Chip.RATE))
            File(folder, "$name-marcas.txt").writeText(
                marks.sortedBy { it.first }.joinToString("") { (t, text) -> String.format(Locale.ROOT, "%.3f\t%.3f\t%s\n", t, t, text) },
            )
            println(
                String.format(
                    Locale.ROOT,
                    "%s: %.1f s, peak %.2f dBFS, RMS %.1f dBFS (first 84 s %.1f), DC %.0f dBFS, %d notes, %d sections",
                    name,
                    pcm.size.toDouble() / Chip.RATE,
                    Runs.peakDb(pcm),
                    Runs.rmsDb(pcm),
                    Runs.rmsDb(pcm, 0, 84 * Chip.RATE),
                    Runs.dcDb(pcm),
                    stream.engine.stats.notes,
                    stream.engine.sections,
                ),
            )
        }
    }

    private fun describe(seconds: Double): String = when (seconds) {
        72.0 -> "quedan 3 puntos o menos"
        84.0 -> "sin puntos: fin de la partida"
        else -> "la velocidad sube"
    }

    private companion object {
        val SECTION_NAMES = listOf("A", "A'", "B", "puente", "tensión")
    }
}
