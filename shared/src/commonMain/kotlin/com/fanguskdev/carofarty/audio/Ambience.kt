package com.fanguskdev.carofarty.audio

import com.fanguskdev.carofarty.audio.music.MusicStream
import kotlin.concurrent.Volatile
import kotlin.math.max
import kotlin.math.min

/**
 * The ambience of a scenario under its music in a run (AUD-17), GameMaster's loops: birds in the Bosque, wind in the Nieve
 * and dry wind in the Desierto, round and round at their own level (they come some 16 dB under the music). It is mixed
 * into the music's own samples, so it goes on the music's bus, as GameMaster asked: off with the music, paused with it.
 * It comes in over [FADE_IN] s when the music of the run starts and goes out over [FADE_OUT] s from the end of the run,
 * so it does not sound under its jingle. [pcm] is its loop, mono (or the left of two channels), played at [outputRate]
 * samples a second whatever its own rate. [mixInto] allocates nothing and runs on the player's thread; [end] may be
 * called from any.
 */
internal class Ambience(private val pcm: Pcm, private val outputRate: Int) {
    private val length = pcm.samples.size / pcm.channels
    private val step = pcm.rate.toDouble() / outputRate
    private val rise = 1.0 / (FADE_IN * outputRate)
    private val fall = 1.0 / (FADE_OUT * outputRate)
    private var position = 0.0
    private var gain = 0.0

    @Volatile private var ending = false

    /** The run is over: it goes out. */
    fun end() {
        ending = true
    }

    /** Adds [frames] of it to the music in [out] from [offset], and keeps the sum under the music's ceiling. */
    fun mixInto(out: ShortArray, offset: Int, frames: Int) {
        if (length == 0) return
        val samples = pcm.samples
        val channels = pcm.channels
        for (i in 0 until frames) {
            gain = if (ending) max(0.0, gain - fall) else min(1.0, gain + rise)
            val at = position.toInt()
            val share = position - at
            val a = samples[at * channels]
            val b = samples[((at + 1) % length) * channels]
            val ambience = (a + (b - a) * share) * gain
            if (gain > 0.0) out[offset + i] = MusicStream.toPcm((out[offset + i] + ambience) / FULL_SCALE)
            position += step
            if (position >= length) position -= length
        }
    }

    private companion object {
        /** GameMaster's fades: 2 s in, 0.8 s out. */
        const val FADE_IN = 2.0
        const val FADE_OUT = 0.8
        const val FULL_SCALE = 32_767.0
    }
}
