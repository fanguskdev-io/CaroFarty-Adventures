package com.fanguskdev.carofarty.audio.music

import com.fanguskdev.carofarty.game.Scenario
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.round
import kotlin.math.sqrt

/** Something the game does at a moment of a run: the time in seconds and what it asks of the music. */
internal class Cue(val seconds: Double, val action: (MusicStream) -> Unit)

/** Runs of the music as the tests play them, and what is measured on them. */
internal object Runs {
    const val SECONDS = 88.4

    /**
     * The run of the prototype's demos (maquetas.py): the speed goes up a level every 20 s, at 72 s few points are left
     * and at 84 s the run is over. The prototype decides each bar knowing the level at its start; here a bar is decided
     * 16 ms before it starts, so the levels are given 16 ms early, and the bars are the prototype's (in the Bosque a bar
     * starts exactly at 20 s).
     */
    val demo: List<Cue> = listOf(
        Cue(early(20.0)) { it.setLevel(2) },
        Cue(early(40.0)) { it.setLevel(3) },
        Cue(early(60.0)) { it.setLevel(4) },
        Cue(72.0) { it.setTense(true) },
        Cue(84.0) { it.end() },
    )

    private fun early(seconds: Double) = seconds - MusicEngine.LOOKAHEAD.toDouble() / Chip.RATE

    /** [seconds] of [stream] with [cues] at their sample, in buffers of [buffer] frames. */
    fun render(
        stream: MusicStream,
        seconds: Double,
        cues: List<Cue> = emptyList(),
        buffer: Int = 1024,
    ): ShortArray {
        val total = (seconds * stream.outputRate).toInt()
        val out = ShortArray(total)
        var done = 0
        var next = 0
        while (done < total) {
            while (next < cues.size && sampleOf(cues[next], stream) <= done) cues[next++].action(stream)
            var n = minOf(buffer, total - done)
            if (next < cues.size) n = minOf(n, sampleOf(cues[next], stream) - done)
            stream.render(out, done, n)
            done += n
        }
        return out
    }

    private fun sampleOf(cue: Cue, stream: MusicStream): Int = round(cue.seconds * stream.outputRate).toInt()

    fun stream(scenario: Scenario, seed: Long = 7, rate: Int = Chip.RATE): MusicStream = MusicStream(scenario, seed, rate, bank(scenario))

    private val banks = HashMap<Scenario, MusicBank>()

    /** The instruments of each scenario, made once for all the tests. */
    fun bank(scenario: Scenario): MusicBank = banks.getOrPut(scenario) { MusicBank(scenario) }

    fun peakDb(x: ShortArray, from: Int = 0, to: Int = x.size): Double {
        var peak = 0
        for (i in from until to) peak = maxOf(peak, abs(x[i].toInt()))
        return 20 * log10(peak / 32767.0)
    }

    fun rmsDb(x: ShortArray, from: Int = 0, to: Int = x.size): Double {
        var sum = 0.0
        for (i in from until to) sum += x[i].toDouble() * x[i]
        return 20 * log10(sqrt(sum / (to - from)) / 32767.0)
    }

    /** The mean as dBFS (the DC offset). */
    fun dcDb(x: ShortArray): Double {
        var sum = 0.0
        for (v in x) sum += v
        return 20 * log10(abs(sum / x.size) / 32767.0 + 1e-12)
    }

    /** FNV-1a, 64 bits: a fingerprint of a run that is the same on every platform. */
    fun fingerprint(x: ShortArray): Long {
        var h = -0x340d631b7bdddcdbL // 0xcbf29ce484222325
        for (v in x) {
            h = (h xor (v.toLong() and 0xFF)) * 0x100000001b3L
            h = (h xor ((v.toLong() shr 8) and 0xFF)) * 0x100000001b3L
        }
        return h
    }

    /** FNV-1a, 64 bits, of the UTF-8 bytes of [text]. */
    fun fingerprint(text: String): Long {
        var h = -0x340d631b7bdddcdbL
        for (b in text.encodeToByteArray()) h = (h xor (b.toLong() and 0xFF)) * 0x100000001b3L
        return h
    }
}
