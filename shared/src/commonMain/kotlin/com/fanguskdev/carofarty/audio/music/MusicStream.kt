package com.fanguskdev.carofarty.audio.music

import com.fanguskdev.carofarty.game.Scenario
import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round

/** The music of each scenario. */
internal fun styleOf(scenario: Scenario): MusicStyle = when (scenario) {
    Scenario.Bosque -> Forest.style
    Scenario.Nieve -> Snow.style
    Scenario.Desierto -> Desert.style
}

/**
 * The music of a run (AUD-19, AUD-24, AUD-25, AUD-26): a song for [scenario] that [seed] chooses and never repeats,
 * made as it plays, with layers that come in as the speed rises, a tense version when few points are left and its
 * end. The platform's player pulls it with [render]: mono, 16-bit, at [outputRate] Hz.
 *
 * - The music is made at 32 kHz with the chip's 6 voices of music and 2 of effects, and resampled to [outputRate].
 * - The same scenario and seed give the same samples on every platform, whatever the calls' timing within a buffer.
 * - [render] allocates nothing and is meant for the audio thread; [setLevel], [setTense] and [end] may be called from
 *   any thread and take effect at the next [render].
 * - [bank] is the scenario's instruments: pass the same one to every run of that scenario so they are made only once.
 * - With [tempoRises] false the tempo stays at the scenario's base (the prototype's `--tempo-fijo`).
 */
class MusicStream(
    val scenario: Scenario,
    seed: Long,
    val outputRate: Int,
    bank: MusicBank = MusicBank(scenario),
    tempoRises: Boolean = true,
) {
    internal val engine: MusicEngine

    private val resampler: Resampler?
    private val scratch = DoubleArray(CHUNK)

    // What the game asks for, written from any thread and read by render: the latest of each is what counts.
    @Volatile private var level = 1

    @Volatile private var tense = false

    @Volatile private var over = false

    init {
        require(outputRate in 8_000..192_000) { "unsupported rate: $outputRate Hz" }
        require(bank.style === styleOf(scenario)) { "the bank is not the instruments of $scenario" }
        engine = MusicEngine(bank.style, bank, seed, tempoRises)
        val source = SampleSource { out, count -> engine.render(out, count) }
        resampler = if (outputRate == Chip.RATE) null else Resampler(source, Chip.RATE, outputRate)
    }

    /** The speed level, 1 to 5: C0 always plays, C1 from 2, C2 from 3 and C3 (the melody) from 4. It goes down 2 bars late. */
    fun setLevel(level: Int) {
        this.level = level
    }

    /**
     * Few points left: the warning (played here, on an effect voice) on the next beat and the tense version from the next
     * bar; false goes back to the music on the next bar.
     */
    fun setTense(tense: Boolean) {
        this.tense = tense
    }

    /** The run is over: on the next beat the layers fade out in 200 ms. */
    fun end() {
        over = true
    }

    /** Whether the music has faded out and its echo has died away: from here on [render] only gives silence. */
    val isFinished: Boolean get() = engine.finished

    /** Writes [frames] samples, mono 16-bit at [outputRate], into [out] from [offset]. */
    fun render(out: ShortArray, offset: Int, frames: Int) {
        applyRequests()
        var done = 0
        while (done < frames) {
            val n = min(CHUNK, frames - done)
            if (resampler == null) engine.render(scratch, n) else resampler.process(scratch, n)
            for (i in 0 until n) out[offset + done + i] = toPcm(scratch[i])
            done += n
        }
    }

    /** The game's requests, as they are now; the engine ignores what has not changed. */
    private fun applyRequests() {
        engine.setLevel(level)
        engine.setTense(tense)
        if (over) engine.end()
    }

    internal companion object {
        private const val CHUNK = 512

        /** The output never goes over −1 dBFS: 0.891 of full scale. */
        val CEILING = Chip.fromDb(-1.0)

        /** Below this (−2 dBFS) the signal passes untouched; above, it bends smoothly towards [CEILING] and never reaches it. */
        const val KNEE = 0.794

        /** A soft limit that only touches the rare peaks above [KNEE]. */
        fun limit(x: Double): Double {
            val a = abs(x)
            if (a <= KNEE) return x
            val room = CEILING - KNEE
            val u = (a - KNEE) / room
            val y = KNEE + room * u / (1.0 + u)
            return if (x < 0) -y else y
        }

        /** To 16 bits, rounded like the prototype (half to even), after [limit]. */
        fun toPcm(x: Double): Short = round(limit(x) * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
    }
}
