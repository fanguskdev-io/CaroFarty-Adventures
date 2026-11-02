package com.fanguskdev.carofarty.audio

import carofarty.shared.generated.resources.Res
import com.fanguskdev.carofarty.game.Scenario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.random.Random

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
    ;

    /** The jingles have one for each scenario, with its instruments (Fangusk); the rest are the same everywhere. */
    val byScenario: Boolean get() = this == StartJingle || this == RecordJingle || this == EndJingle

    /** The name of this sound's file for [scenario]: `jingle-inicio-bosque` for the jingles, its own name for the rest. */
    fun file(scenario: Scenario): String = if (byScenario) "$file-${scenario.name.lowercase()}" else file
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

    /** Whether she turned the sound off (the button by the pause): no music and no sounds until she turns it on. */
    var muted = false
        set(value) {
            field = value
            if (value) {
                music.pause()
            } else if (playing) {
                music.resume()
            }
        }
    private var loading: Job? = null
    private var after: Job? = null
    private val random = Random.Default

    /** The files loaded, and how long each lasts in milliseconds. */
    private val millis = HashMap<String, Long>()

    init {
        scope.launch {
            val names = Sound.entries.map { it.file } +
                Sound.entries.filter { it.byScenario }.flatMap { sound -> Scenario.entries.map { sound.file(it) } }
            for (name in names) {
                val wav = runCatching { Res.readBytes("$SOUNDS/$name.wav") }.getOrNull() ?: continue
                sounds.load(name, wav)
                millis[name] = readWav(wav)?.let { it.samples.size * MILLIS / (it.channels * it.rate) } ?: 0L
            }
        }
    }

    /** The music of [scenario] from its start, or on with it if [restart] is false and it is already the one playing. */
    fun music(scenario: Scenario, restart: Boolean = false) {
        after?.cancel()
        playing = true
        // The same track is never read again: that would take a frame or two in the middle of a run.
        if (current == scenario) {
            if (restart) music.restart() else music.resume()
            if (muted) music.pause()
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
                music.play(track, MUSIC_VOLUME)
                if (!playing || muted) music.pause()
            }
    }

    /**
     * Plays [sound], in its version for [scenario] if it has one and it is there, or the one for everywhere. The sounds of
     * the run come out each time a little different, ±40 cents and ±1.5 dB at random, so a row of them does not sound like
     * a machine (GameMaster's mix); the jingles and the buttons, always the same.
     */
    fun play(sound: Sound, scenario: Scenario? = null) {
        if (muted) return
        val file = fileOf(sound, scenario)
        if (sound.byScenario || sound == Sound.Tap || sound == Sound.Back) {
            sounds.play(file)
            return
        }
        val cents = random.nextDouble(-MAX_CENTS, MAX_CENTS)
        val decibels = random.nextDouble(-MAX_DECIBELS, MAX_DECIBELS)
        val rate = 2.0.pow(cents / CENTS_IN_OCTAVE).toFloat()
        val volume = (EFFECTS_VOLUME * 10.0.pow(decibels / DECIBELS_TO_AMPLITUDE)).toFloat().coerceAtMost(1f)
        sounds.play(file, rate, volume)
    }

    private fun fileOf(sound: Sound, scenario: Scenario?): String {
        val own = scenario?.let { sound.file(it) }
        return if (own != null && own in millis) own else sound.file
    }

    /** A run begins: the music stops for the start jingle of [scenario] and comes back from its start when it ends. */
    fun start(scenario: Scenario) {
        pause()
        play(Sound.StartJingle, scenario)
        val wait = millis[fileOf(Sound.StartJingle, scenario)] ?: 0L
        after =
            scope.launch {
                delay(wait)
                music(scenario, restart = true)
            }
    }

    /** A run in [scenario] ends: the music stops and, [wait] ms later, its jingle of the end, or of a new [record]. */
    fun end(scenario: Scenario, record: Boolean, wait: Long) {
        pause()
        after =
            scope.launch {
                delay(wait)
                play(if (record) Sound.RecordJingle else Sound.EndJingle, scenario)
            }
    }

    fun pause() {
        after?.cancel()
        playing = false
        music.pause()
    }

    fun resume() {
        if (current == null) return
        playing = true
        if (!muted) music.resume()
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
        const val MILLIS = 1000L

        /** The mix GameMaster proposed to start with: the music at 0.8 and the sounds at 1, varied by ±40 cents and ±1.5 dB. */
        const val MUSIC_VOLUME = 0.8f
        const val EFFECTS_VOLUME = 0.85
        const val MAX_CENTS = 40.0
        const val MAX_DECIBELS = 1.5
        const val CENTS_IN_OCTAVE = 1200.0
        const val DECIBELS_TO_AMPLITUDE = 20.0
    }
}
