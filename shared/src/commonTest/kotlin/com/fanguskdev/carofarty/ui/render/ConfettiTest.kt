package com.fanguskdev.carofarty.ui.render

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The confetti of a new record (UI-15), as GameMaster designed it: how many, for how long, and only once. */
class ConfettiTest {
    private val frame = 1f / 60

    @Test
    fun aBurstAndThenADrizzleForEveryFourHundredPixels() {
        // On a screen so tall that none leaves it: 60 at once and 20 a second for 2 s, a hundred in all.
        val confetti = Confetti(Random(7))
        confetti.start(400, 100_000)
        assertEquals(60, confetti.size)
        repeat(60) { confetti.step(frame) }
        assertEquals(80, confetti.size, "after a second")
        repeat(120) { confetti.step(frame) }
        assertEquals(100, confetti.size, "on 400 px")
        val wide = Confetti(Random(7)).apply { start(448, 252) }
        assertEquals(67, wide.size, "on 448 px, 60 × 448 / 400")
    }

    @Test
    fun itFloatsDownAndIsGoneInAboutFiveSecondsAndAHalf() {
        val confetti = Confetti(Random(3))
        confetti.start(400, 180)
        var seconds = 0f
        while (confetti.falling && seconds < 20f) {
            confetti.step(frame)
            seconds += frame
        }
        assertFalse(confetti.falling)
        assertTrue(seconds in 4.5f..7f, "gone after $seconds s")
    }

    @Test
    fun stoppedThereIsNoneLeft() {
        val confetti = Confetti(Random(1))
        confetti.start(320, 180)
        confetti.step(frame)
        confetti.stop()
        assertFalse(confetti.falling)
        assertEquals(0, confetti.size)
    }
}
