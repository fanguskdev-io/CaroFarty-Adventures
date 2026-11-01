package com.fanguskdev.carofarty.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class FpsMeterTest {
    @Test
    fun aSteadySixtyReadsSixty() {
        val meter = FpsMeter()
        repeat(200) { meter.add(1.0 / 60) }
        assertEquals(60, meter.value)
        assertEquals(17, meter.worstMillis)
    }

    @Test
    fun aStutterShowsForTwoSecondsAndThenGoes() {
        val meter = FpsMeter()
        repeat(200) { meter.add(1.0 / 60) }
        meter.add(0.05)
        assertEquals(50, meter.worstMillis)
        repeat(119) { meter.add(1.0 / 60) }
        assertEquals(50, meter.worstMillis)
        meter.add(1.0 / 60)
        assertEquals(17, meter.worstMillis)
    }

    @Test
    fun slowFramesAreCountedFromTheStart() {
        val meter = FpsMeter()
        meter.add(0.0)
        repeat(99) { meter.add(1.0 / 60) }
        meter.add(0.034)
        assertEquals(1.0, meter.slowPercent, 0.001)
    }

    @Test
    fun smoothedMillisFollowWhatTheyAreGiven() {
        val smoothed = Smoothed()
        repeat(100) { smoothed.add(4.milliseconds) }
        assertEquals(4.0, smoothed.millis, 0.01)
    }
}
