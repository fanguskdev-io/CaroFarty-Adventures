package com.fanguskdev.carofarty.game

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The validator's shortcut: trying every hold of a jump at once ([JumpLab.tryHolds]), with the run-up and the steps with the
 * finger down simulated a single time, lands exactly where trying each hold on its own ([JumpLab.tryJump]) does, on a real
 * course, with and without an animal to watch, a height to give up at and steps to run on after landing.
 */
class JumpLabHoldsTest {
    private val config = GameConfig()
    private val lab = JumpLab(config.movement, config.items.negativeBox, config.items.positiveBox)

    @Test
    fun everyHoldAtOnceLandsWhereEachHoldOnItsOwnDoes() {
        val generator = CourseGenerator(config, seed = 5, runStart = GameRun.START_X)
        val course = Course()
        repeat(PIECES) { course.append(generator.next()) }
        val maxHold = MAX_HOLD
        for (runAfter in listOf(0, RUN_AFTER)) {
            for (ground in course.grounds.take(GROUNDS)) {
                val watch = course.items.firstOrNull { it.kind.adds && it.x > ground.end - FAR }
                val below = ground.top - GIVE_UP
                for (from in listOf(NEAR, FAR)) {
                    val x = ground.end - from
                    if (x < ground.start) continue
                    for (press in PRESSES) {
                        val one = (0..maxHold).map { hold ->
                            val landing = lab.tryJump(
                                course,
                                course.items,
                                x,
                                ground.top.toFloat(),
                                GameRun.START_X,
                                press,
                                hold,
                                time = 0.0,
                                watch = watch,
                                runAfter = runAfter,
                                giveUpBelow = below,
                            )
                            describe(landing)
                        }
                        val all = ArrayList<List<Any>>()
                        val top = ground.top.toFloat()
                        val start = GameRun.START_X
                        lab.tryHolds(course, course.items, x, top, start, press, maxHold, 0.0, watch, runAfter, below) { _, landing ->
                            all += describe(landing)
                            true
                        }
                        assertEquals(one, all, "from $x, pressing after $press steps, running on $runAfter")
                    }
                }
            }
        }
    }

    /** What a landing says, without where she was when she did not land: then that is left from an earlier try. */
    private fun describe(landing: JumpLab.Landing): List<Any> = if (landing.landed) {
        listOf(true, landing.x, landing.top, landing.time, landing.touchedNegative, landing.touchedWatched)
    } else {
        listOf(false, landing.touchedNegative, landing.touchedWatched)
    }

    private companion object {
        const val PIECES = 120
        const val GROUNDS = 80
        const val MAX_HOLD = 22
        const val RUN_AFTER = 30
        const val GIVE_UP = 4f
        const val NEAR = 20.0
        const val FAR = 80.0
        val PRESSES = listOf(0, 4, 9, 15)
    }
}
