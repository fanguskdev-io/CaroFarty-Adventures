package com.fanguskdev.carofarty.audio

import platform.posix.usleep
import kotlin.concurrent.AtomicInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The music player of iOS with its engine offline: the simulator gives a test no sound card, so the test asks the engine
 * for the samples the sound card would, and sees the buffers made again as they play.
 */
class IosMusicPlayerTest {
    private val asked = AtomicInt(0)

    private fun waitFor(count: Int) {
        var waited = 0
        while (asked.value < count && waited < TIMEOUT_MILLIS) {
            usleep(STEP_MICROS)
            waited += STEP_MILLIS
        }
    }

    @Test
    fun theStreamIsAskedForMoreAsItPlays() {
        val player = IosMusicPlayer(offline = true)
        player.stream(
            { out, offset, frames ->
                out.fill(1, offset, offset + frames)
                asked.incrementAndGet()
            },
            player.streamRate,
        )
        waitFor(QUEUED)
        assertEquals(QUEUED, asked.value, "the buffers queued at the start")
        repeat(QUEUED) { player.pull(FRAMES) }
        waitFor(2 * QUEUED)
        assertTrue(asked.value >= 2 * QUEUED, "each one made again when it had played: ${asked.value}")
        player.stop()
        val stopped = asked.value
        repeat(QUEUED) { player.pull(FRAMES) }
        usleep(STEP_MICROS * 10u)
        assertEquals(stopped, asked.value, "and none after it stops")
    }

    private companion object {
        const val QUEUED = 3
        const val FRAMES = 4096
        const val TIMEOUT_MILLIS = 3000
        const val STEP_MILLIS = 20
        const val STEP_MICROS = 20_000u
    }
}
