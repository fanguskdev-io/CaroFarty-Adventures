package com.fanguskdev.carofarty.audio

import kotlin.test.Test
import kotlin.test.assertEquals

class RescueStreakTest {
    @Test
    fun eachRescueInARowGoesAStepUpToTheTop() {
        val streak = RescueStreak()
        val steps = List(8) { streak.rescued(it * 1.0) }
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 5, 5), steps)
    }

    @Test
    fun itBreaksWithMoreThanTwoSecondsWithoutARescue() {
        val streak = RescueStreak()
        streak.rescued(0.0)
        assertEquals(1, streak.rescued(2.0))
        assertEquals(0, streak.rescued(4.5))
    }

    @Test
    fun anEggOrAFishBreaksIt() {
        val streak = RescueStreak()
        streak.rescued(0.0)
        streak.rescued(0.5)
        streak.broken()
        assertEquals(0, streak.rescued(1.0))
    }
}
