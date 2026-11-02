package com.fanguskdev.carofarty.audio

import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopMusicPlayerTest {
    @Test
    fun theStreamIsAskedForMoreWhileItPlays() {
        val player = musicPlayer()
        val format = AudioFormat(player.streamRate.toFloat(), 16, 1, true, false)
        // A computer without a sound card has nothing to test here.
        if (!AudioSystem.isLineSupported(DataLine.Info(SourceDataLine::class.java, format))) return
        val asked = AtomicInteger(0)
        // At volume 0: nothing is heard on the computer's speakers while the tests run.
        player.stream(
            { out, offset, frames ->
                out.fill(0, offset, offset + frames)
                asked.incrementAndGet()
            },
            player.streamRate,
            volume = 0f,
        )
        var waited = 0
        while (asked.get() < ENOUGH && waited < TIMEOUT_MILLIS) {
            Thread.sleep(STEP_MILLIS.toLong())
            waited += STEP_MILLIS
        }
        player.stop()
        assertTrue(asked.get() >= ENOUGH, "it kept asking for music while it played: ${asked.get()}")
    }

    private companion object {
        /** Some 400 ms of music, more than the line holds. */
        const val ENOUGH = 10
        const val TIMEOUT_MILLIS = 3000
        const val STEP_MILLIS = 20
    }
}
