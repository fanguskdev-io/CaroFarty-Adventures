package com.fanguskdev.carofarty.audio

import com.fanguskdev.carofarty.game.Scenario
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The music and the sounds that tools/sonido.py left in the resources are WAV files every platform reads (AUD). */
class AudioFilesTest {
    private val resources = File("src/commonMain/composeResources/files")

    private fun wavs(folder: String): List<File> = File(resources, folder).listFiles()?.filter { it.extension == "wav" }.orEmpty()

    @Test
    fun everyTrackAndSoundIsSixteenBitPcm() {
        for (file in wavs("musica") + wavs("sonidos")) {
            val sound = assertNotNull(readWav(file.readBytes()), "${file.name} is not a WAV of 16-bit PCM")
            assertTrue(sound.samples.isNotEmpty(), "${file.name} is empty")
        }
    }

    @Test
    fun withMusicEachScenarioHasItsOwn() {
        val tracks = wavs("musica").map { it.nameWithoutExtension }.toSet()
        if (tracks.isEmpty()) return
        for (scenario in Scenario.entries) assertTrue(scenario.name.lowercase() in tracks, "no music for $scenario")
    }

    @Test
    fun eachScenarioHasItsAmbienceToMixUnderTheMusic() {
        // AUD-17: mono, so it mixes into the music's own samples ([Ambience]), and a loop of several seconds.
        val tracks = wavs("musica").associateBy { it.nameWithoutExtension }
        if (tracks.isEmpty()) return
        for (scenario in Scenario.entries) {
            val file = assertNotNull(tracks["ambiente-${scenario.name.lowercase()}"], "no ambience for $scenario")
            val loop = assertNotNull(readWav(file.readBytes()))
            assertTrue(loop.channels == 1 && loop.samples.size / loop.rate >= 10, "${file.name}: mono and a long loop")
        }
    }

    @Test
    fun theSoundsHaveTheNamesTheGameLooksFor() {
        val known = Sound.entries.map { it.file }.toSet() + Sound.entries.flatMap { sound -> Scenario.entries.map { sound.file(it) } }
        for (file in wavs("sonidos")) assertTrue(file.nameWithoutExtension in known, "${file.name} is not a sound of the game")
    }
}
