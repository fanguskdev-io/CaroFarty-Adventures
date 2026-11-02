package com.fanguskdev.carofarty.ui.render

import com.fanguskdev.carofarty.engine.PixelViewport
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The animals of the title screen (UI-07): each one reached on its turn, never on top of another and never popping in. */
class IntroShowTest {
    private val widths = PixelViewport.MIN_WIDTH..PixelViewport.MAX_WIDTH

    @Test
    fun caroReachesEachAnimalOnItsTurnWhereTheirBoxesTouch() {
        for (width in widths) {
            val caroX = width / 4
            for (k in 0L until 20L) {
                assertEquals(caroX + IntroShow.REACH, IntroShow.x(k, IntroShow.reachedAt(k), caroX), "animal $k on $width")
                assertEquals(k + 1, IntroShow.next(IntroShow.reachedAt(k)), "after animal $k, the next")
            }
        }
        assertEquals(-1L, IntroShow.sinceRescue(IntroShow.EVERY_MILLIS - 1), "before the first rescue")
        assertEquals(0L, IntroShow.sinceRescue(IntroShow.reachedAt(3)), "at a rescue")
    }

    @Test
    fun twoAnimalsAreNeverOnTopOfEachOther() {
        for (width in widths step 16) {
            val caroX = width / 4
            for (show in IntroShow.START_MILLIS until 60_000L step 50L) {
                val next = IntroShow.next(show)
                val shown = (next until next + IntroShow.AT_ONCE).map { IntroShow.x(it, show, caroX) }.filter { it - HALF < width }
                for (i in shown.indices) {
                    for (j in i + 1 until shown.size) assertTrue(abs(shown[i] - shown[j]) >= 2 * HALF, "at $show on $width: $shown")
                }
            }
        }
    }

    @Test
    fun anAnimalComesInFromBeyondTheRightEdge() {
        // When an animal becomes one of those drawn, it is still out of the widest screen.
        for (width in widths) {
            val caroX = width / 4
            for (k in IntroShow.AT_ONCE.toLong() until 20L) {
                val from = (k - IntroShow.AT_ONCE + 1) * IntroShow.EVERY_MILLIS
                assertTrue(IntroShow.x(k, from, caroX) - HALF >= width, "animal $k on $width")
            }
        }
    }

    private companion object {
        const val HALF = 8
    }
}
