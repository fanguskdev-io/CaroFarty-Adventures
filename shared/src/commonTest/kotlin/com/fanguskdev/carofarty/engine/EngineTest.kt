package com.fanguskdev.carofarty.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PixelViewportTest {
    @Test
    fun eachScreenGetsTheLargestScaleThatShowsTheMinimumArea() {
        // The table of ADR 0003.
        assertEquals(PixelViewport(4, 320, 180, 0, 0), PixelViewport.fit(1280, 720))
        assertEquals(PixelViewport(6, 400, 180, 0, 0), PixelViewport.fit(2400, 1080))
        assertEquals(PixelViewport(6, 422, 195, 0, 0), PixelViewport.fit(2532, 1170))
        assertEquals(PixelViewport(6, 437, 201, 0, 0), PixelViewport.fit(2622, 1206))
        assertEquals(7, PixelViewport.fit(2992, 1344).scale)
        assertEquals(PixelViewport(8, 344, 258, 0, 0), PixelViewport.fit(2752, 2064))
    }

    @Test
    fun aVeryWideScreenGetsBandsBeyondTheMaximumArea() {
        val viewport = PixelViewport.fit(3200, 1080)

        assertEquals(6, viewport.scale)
        assertEquals(PixelViewport.MAX_WIDTH, viewport.width)
        assertEquals((3200 - 448 * 6) / 2, viewport.offsetX)
    }

    @Test
    fun thePixelsThatDoNotDivideEvenlyAreSharedOutAsBands() {
        val viewport = PixelViewport.fit(2405, 1083)

        assertEquals(6, viewport.scale)
        assertEquals(400, viewport.width)
        assertEquals(180, viewport.height)
        assertEquals(2, viewport.offsetX)
        assertEquals(1, viewport.offsetY)
    }
}

class FixedStepTest {
    @Test
    fun itRunsOneUpdatePerStepAndKeepsTheRest() {
        val clock = FixedStep(step = 0.01)
        var updates = 0

        val alpha = clock.advance(0.035) { updates++ }

        assertEquals(3, updates)
        assertEquals(0.5f, alpha, 0.001f)
        clock.advance(0.005) { updates++ }
        assertEquals(4, updates)
    }

    @Test
    fun afterALongPauseItDoesNotTryToCatchUp() {
        val clock = FixedStep(step = 0.01, maxSteps = 5)
        var updates = 0

        clock.advance(10.0) { updates++ }

        assertEquals(5, updates)
    }

    @Test
    fun sixtyAndOneHundredTwentyHertzGiveTheSameSteps() {
        fun stepsIn(frames: Int, frame: Double): Int {
            val clock = FixedStep()
            var updates = 0
            repeat(frames) { clock.advance(frame) { updates++ } }
            return updates
        }
        assertEquals(stepsIn(60, 1.0 / 60), stepsIn(120, 1.0 / 120))
    }
}

class TouchInputTest {
    @Test
    fun aTapIsReadOnceEvenIfTheFingerIsAlreadyUp() {
        val input = TouchInput()
        input.press()
        input.release()

        assertTrue(input.consumePress())
        assertFalse(input.consumePress())
        assertFalse(input.isHeld)
    }

    @Test
    fun aSecondFingerIsNotASecondTap() {
        val input = TouchInput()
        input.press()
        input.consumePress()

        input.press()

        assertFalse(input.consumePress())
        assertTrue(input.isHeld)
    }
}
