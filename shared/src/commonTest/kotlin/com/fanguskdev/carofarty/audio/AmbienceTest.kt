package com.fanguskdev.carofarty.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The ambience under the music of a run (AUD-17): its fades, its loop and its level against the music. */
class AmbienceTest {
    private val rate = 1_000

    /** A loop of [length] samples all at [level], at [loopRate] samples a second. */
    private fun loop(level: Short, length: Int = 300, loopRate: Int = rate) = Pcm(ShortArray(length) { level }, 1, loopRate)

    private fun Ambience.render(frames: Int, music: Short = 0): ShortArray = ShortArray(frames) { music }.also { mixInto(it, 0, frames) }

    @Test
    fun itComesInOverTwoSeconds() {
        val ambience = Ambience(loop(1_000), rate)
        val first = ambience.render(rate)
        assertEquals(500.0, first.last().toDouble(), 2.0)
        val second = ambience.render(rate)
        assertEquals(1_000.0, second.last().toDouble(), 2.0)
    }

    @Test
    fun itGoesOutInLessThanASecondFromTheEnd() {
        val ambience = Ambience(loop(1_000), rate)
        ambience.render(3 * rate)
        ambience.end()
        val out = ambience.render(rate)
        assertEquals(500.0, out[rate * 4 / 10].toDouble(), 3.0)
        assertTrue(out.drop(rate * 8 / 10 + 1).all { it.toInt() == 0 }, "gone after 0.8 s")
    }

    @Test
    fun itGoesRoundAndRoundAtTheRateOfThePlayer() {
        // A ramp of 100 samples at half the rate of the player: each of its samples lasts two of the player's.
        val ramp = Pcm(ShortArray(100) { (it * 10).toShort() }, 1, rate / 2)
        val ambience = Ambience(ramp, rate)
        ambience.render(3 * rate)
        val out = ambience.render(400)
        assertEquals(out[0].toInt(), out[200].toInt(), "two hundred samples later, round again")
    }

    @Test
    fun itAddsToTheMusicAndNeverGoesPastItsCeiling() {
        val ambience = Ambience(loop(10_000), rate)
        ambience.render(3 * rate)
        val loud = ambience.render(10, music = 30_000)
        assertTrue(loud.all { it < 32_767 * 0.9 }, "under −1 dBFS")
        val quiet = ambience.render(10, music = 1_000)
        assertEquals(11_000, quiet.last().toInt())
    }
}
