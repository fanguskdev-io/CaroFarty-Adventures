package com.fanguskdev.carofarty.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WalkTest {
    private val config = ItemConfig()
    private val step = 1f / 60f

    @Test
    fun itWalksBackAndForthWithinItsStretchAndStopsAWhile() {
        val walk = Walk(-20f, 30f, config, seed = 7)
        var lowest = 0f
        var highest = 0f
        var stopped = false
        repeat(60 * 60) {
            walk.step(step)
            lowest = minOf(lowest, walk.offset)
            highest = maxOf(highest, walk.offset)
            if (!walk.walking) stopped = true
        }
        assertTrue(lowest >= -20f && highest <= 30f, "it never leaves its stretch")
        assertTrue(lowest == -20f && highest == 30f, "it walks to both ends")
        assertTrue(stopped, "it stops now and then")
    }

    @Test
    fun theSameSeedWalksTheSameOnEveryPhone() {
        val one = Walk(-20f, 30f, config, seed = 99)
        val other = Walk(-20f, 30f, config, seed = 99)
        repeat(600) {
            one.step(step)
            other.step(step)
            assertEquals(one.offset, other.offset)
        }
    }
}
