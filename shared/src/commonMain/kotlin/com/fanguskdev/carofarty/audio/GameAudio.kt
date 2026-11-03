package com.fanguskdev.carofarty.audio

import carofarty.shared.generated.resources.Res
import com.fanguskdev.carofarty.audio.music.MusicBank
import com.fanguskdev.carofarty.audio.music.MusicStream
import com.fanguskdev.carofarty.game.Scenario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    Pajaro("pajaro"),
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
 * The music and the sounds (AUD), as GameMaster designed them and Fangusk approved (ADR 0006: each scenario has «su
 * decorado, su paleta y su música»). In a run, the music of its scenario made as she plays (docs/musica.md): a song the
 * run's seed chooses, its layers coming in as the speed rises ([speed]), its tense version with few points left
 * ([lowPoints]) and its end. Around the runs, the loop of the scenario in `files/musica/<escenario>.wav`; and the sounds
 * of `files/sonidos/` over both. What is missing stays silent; until the instruments of a scenario are made, its run
 * plays the loop.
 */
internal class GameAudio(private val music: MusicPlayer, private val sounds: SoundPlayer, private val scope: CoroutineScope) {
    /** The scenario whose loop the player has, or null if it has a stream or nothing. */
    private var current: Scenario? = null
    private var playing = false

    /** The instruments of each scenario, made once in the background; and the music of the run, while there is one. */
    private val banks = arrayOfNulls<MusicBank>(Scenario.entries.size)
    private var stream: MusicStream? = null
    private var level = 1
    private var tense = false

    /**
     * Whether the music is off: by the button of the sound by the pause, which silences everything, or in the settings
     * (UI-11), with the jingles, which are music too.
     */
    var muted = false
        set(value) {
            field = value
            if (value) {
                music.pause()
            } else if (playing) {
                music.resume()
            }
        }

    /** Whether the sounds are off: by the same button, or on their own in the settings (UI-11). */
    var effectsMuted = false
    private var loading: Job? = null
    private var after: Job? = null
    private val random = Random.Default
    private var jumpRate = 1f
    private var jumpVolume = 1f

    /** The files loaded, and how long each lasts in milliseconds. */
    private val millis = HashMap<String, Long>()

    init {
        // The three at once, each on a core of its own: on a phone they take a moment.
        for (scenario in Scenario.entries) {
            scope.launch {
                banks[scenario.ordinal] = runCatching { withContext(Dispatchers.Default) { MusicBank(scenario) } }
                    .getOrNull()
            }
        }
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

    /** The loop of [scenario] from its start, or on with it if [restart] is false and it is already the one playing. */
    fun music(scenario: Scenario, restart: Boolean = false) {
        after?.cancel()
        stream = null
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
     * a machine (GameMaster's mix); the jingles and the buttons, always the same. A rescue goes up the steps of the
     * pentatonic with its [streak] ([RescueStreak]), played that much faster.
     */
    fun play(sound: Sound, scenario: Scenario? = null, streak: Int = 0) {
        // The jingles go with the music; the rest are the sounds.
        if (if (sound.byScenario) muted else effectsMuted) return
        val file = fileOf(sound, scenario)
        if (sound.byScenario || sound == Sound.Tap || sound == Sound.Back) {
            sounds.play(file)
            return
        }
        // The tail of a long jump splices onto its short jump: it goes at the same pitch and volume.
        if (sound == Sound.LongJump) {
            sounds.play(file, jumpRate, jumpVolume)
            return
        }
        val steps = when (sound) {
            Sound.Perrito -> PERRITO_STEPS
            Sound.Gatito -> GATITO_STEPS
            else -> null
        }
        val semitones = steps?.get(streak.coerceIn(0, steps.size - 1)) ?: 0
        val cents = random.nextDouble(-MAX_CENTS, MAX_CENTS) + semitones * CENTS_IN_SEMITONE
        val decibels = random.nextDouble(-MAX_DECIBELS, MAX_DECIBELS)
        val rate = 2.0.pow(cents / CENTS_IN_OCTAVE).toFloat()
        val volume = (EFFECTS_VOLUME * 10.0.pow(decibels / DECIBELS_TO_AMPLITUDE)).toFloat().coerceAtMost(1f)
        if (sound == Sound.Jump) {
            jumpRate = rate
            jumpVolume = volume
        }
        sounds.play(file, rate, volume)
    }

    private fun fileOf(sound: Sound, scenario: Scenario?): String {
        val own = scenario?.let { sound.file(it) }
        return if (own != null && own in millis) own else sound.file
    }

    /**
     * A run begins: the music stops for the start jingle of [scenario] and, when it ends, the music of the run starts,
     * the song that [seed] chooses; or the loop, if the instruments of the scenario are not made yet.
     */
    fun start(scenario: Scenario, seed: Long) {
        pause()
        play(Sound.StartJingle, scenario)
        val wait = millis[fileOf(Sound.StartJingle, scenario)] ?: 0L
        after =
            scope.launch {
                delay(wait)
                // The instruments, made since the game opened, are ready by now but for the very first seconds.
                val made = banks[scenario.ordinal]?.let {
                    runCatching { withContext(Dispatchers.Default) { MusicStream(scenario, seed, music.streamRate, it) } }
                        .getOrNull()
                }
                if (made == null) {
                    music(scenario, restart = true)
                    return@launch
                }
                current = null
                stream = made
                level = 1
                tense = false
                playing = true
                music.stream(PcmSource { out, offset, frames -> made.render(out, offset, frames) }, music.streamRate, MUSIC_VOLUME)
                if (muted) music.pause()
            }
    }

    /** Whether the instruments of [scenario] are made: until they are, its runs play the loop. */
    fun hasMusic(scenario: Scenario): Boolean = banks[scenario.ordinal] != null

    /** How fast she runs, in pixels a second: the layers of the music come in at GameMaster's speeds (AUD-04). */
    fun speed(pixelsPerSecond: Float) {
        var next = 1
        for (threshold in LEVEL_SPEEDS) if (pixelsPerSecond >= threshold) next++
        if (next == level) return
        level = next
        stream?.setLevel(next)
    }

    /** Whether few points are left: the music plays its warning and its tense version, and leaves it when she recovers. */
    fun lowPoints(low: Boolean) {
        if (low == tense) return
        tense = low
        stream?.setTense(low)
    }

    /**
     * A run in [scenario] ends: the music of the run fades out (a loop stops at once) and, [wait] ms later, its jingle of
     * the end, or of a new [record].
     */
    fun end(scenario: Scenario, record: Boolean, wait: Long) {
        after?.cancel()
        playing = false
        val ending = stream
        if (ending != null) ending.end() else music.pause()
        after =
            scope.launch {
                delay(wait)
                music.pause()
                play(if (record) Sound.RecordJingle else Sound.EndJingle, scenario)
            }
    }

    fun pause() {
        after?.cancel()
        playing = false
        music.pause()
    }

    fun resume() {
        if (current == null && stream == null) return
        playing = true
        if (!muted) music.resume()
    }

    fun release() {
        loading?.cancel()
        current = null
        stream = null
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

        /** The steps of a streak of rescues, in semitones: the pentatonic of each sound (GameMaster's sfx.py). */
        val PERRITO_STEPS = intArrayOf(0, 2, 5, 7, 9, 12)
        val GATITO_STEPS = intArrayOf(0, 2, 4, 7, 9, 12)
        const val CENTS_IN_SEMITONE = 100.0

        /** The speeds of the levels 2 to 5 of the music, in pixels a second (GameMaster's proposal for AUD-04). */
        val LEVEL_SPEEDS = floatArrayOf(200f, 240f, 280f, 320f)
    }
}
