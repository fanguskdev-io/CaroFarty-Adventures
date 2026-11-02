package com.fanguskdev.carofarty.audio

import carofarty.shared.generated.resources.Res
import com.fanguskdev.carofarty.game.Scenario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** The sounds of the game, by the names of their files in `files/sonidos/` (GameMaster's beta set). */
enum class Sound(val file: String) {
    Jump("salto-corto"),
    LongJump("salto-largo"),
    Landing("aterrizaje"),
    Perrito("rescate-perrito"),
    Gatito("rescate-gatito"),
    Huevo("huevo"),
    Pescado("pescado"),
    IntoTheGap("caida-barranco"),
    NoPoints("fin-partida"),
    Record("record"),
    Tap("ui-pulsar"),
    Back("ui-volver"),
    StartJingle("jingle-inicio"),
    RecordJingle("jingle-record"),
    EndJingle("jingle-fin"),
}

/**
 * The music and the sounds (AUD): the music of each scenario round and round while she plays there (ADR 0006: each one
 * has «su decorado, su paleta y su música»), and the sounds over it. For the beta, GameMaster's provisional loops in
 * `files/musica/<escenario>.wav` and sounds in `files/sonidos/`; the music with layers that never bores is M4's. What is
 * missing stays silent.
 */
internal class GameAudio(private val music: MusicPlayer, private val sounds: SoundPlayer, private val scope: CoroutineScope) {
    private var current: Scenario? = null
    private var playing = false
    private var loading: Job? = null

    init {
        scope.launch {
            for (sound in Sound.entries) {
                val wav = runCatching { Res.readBytes("$SOUNDS/${sound.file}.wav") }.getOrNull() ?: continue
                sounds.load(sound.file, wav)
            }
        }
    }

    /** The music of [scenario] from its start, or on with it if [restart] is false and it is already the one playing. */
    fun music(scenario: Scenario, restart: Boolean = false) {
        playing = true
        if (current == scenario && !restart) {
            music.resume()
            return
        }
        current = scenario
        loading?.cancel()
        loading =
            scope.launch {
                val track = runCatching { Res.readBytes("$MUSIC/${scenario.name.lowercase()}.wav") }.getOrNull()
                if (track == null) {
                    music.stop()
                    return@launch
                }
                music.play(track)
                if (!playing) music.pause()
            }
    }

    fun play(sound: Sound) = sounds.play(sound.file)

    fun pause() {
        playing = false
        music.pause()
    }

    fun resume() {
        if (current == null) return
        playing = true
        music.resume()
    }

    fun release() {
        loading?.cancel()
        current = null
        playing = false
        music.stop()
        sounds.release()
    }

    private companion object {
        const val MUSIC = "files/musica"
        const val SOUNDS = "files/sonidos"
    }
}
