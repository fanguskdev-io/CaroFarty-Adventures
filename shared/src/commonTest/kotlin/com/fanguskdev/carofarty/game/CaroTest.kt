package com.fanguskdev.carofarty.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CaroTest {
    private val config = MovementConfig()
    private val step = 1f / 60f

    /** A course of the given grounds. */
    private fun terrain(vararg grounds: Ground) = Course().apply { append(Piece(grounds.toList(), emptyList(), grounds.last().end)) }

    private fun runningOn(course: Course, x: Double = 100.0): Caro = Caro(config).apply {
        place(x, course.groundTopUnder(x - 1, x + 1)?.toFloat() ?: 0f)
        start()
    }

    /** Keeps the finger down for [frames] steps from the jump; returns the highest point above the takeoff and the airtime. */
    private fun jump(frames: Int): Pair<Float, Float> {
        val course = terrain(Ground(0, 1_000_000, 32))
        val caro = runningOn(course)
        caro.step(step, pressed = true, held = frames > 0, terrain = course)
        var steps = 1
        var highest = caro.y - 32f
        while (!caro.state.grounded) {
            caro.step(step, pressed = false, held = steps < frames, terrain = course)
            steps++
            highest = max(highest, caro.y - 32f)
        }
        return highest to steps * step
    }

    @Test
    fun theJumpMatchesTheTableOfDocsMovimiento() {
        // Steps with the finger down, height (px) and airtime (s), from docs/movimiento.md.
        val table = listOf(
            Triple(3, 23.7f, 0.350f),
            Triple(5, 29.4f, 0.400f),
            Triple(7, 34.8f, 0.450f),
            Triple(10, 42.4f, 0.517f),
            Triple(12, 47.2f, 0.550f),
            Triple(15, 53.8f, 0.617f),
            Triple(18, 59.8f, 0.683f),
            Triple(22, 66.8f, 0.750f),
        )
        for ((frames, height, airtime) in table) {
            val (measuredHeight, measuredTime) = jump(frames)
            assertTrue(abs(measuredHeight - height) < 0.1f, "$frames steps: $measuredHeight px, expected $height")
            assertTrue(abs(measuredTime - airtime) < step / 2, "$frames steps: $measuredTime s, expected $airtime")
        }
    }

    @Test
    fun aShorterPressNeverJumpsHigher() {
        // The dinosaur of Chrome ignores a release in the first 30 px and gives its highest jump: here letting go always
        // lowers it, from the first step on.
        val heights = (1..30).map { jump(it).first }
        heights.zipWithNext().forEach { (shorter, longer) -> assertTrue(shorter <= longer, "$shorter then $longer") }
        assertTrue(heights.first() < heights.last() / 2, "a quick tap jumps less than half as high as a long press")
    }

    @Test
    fun holdingLongerThanTheWindowJumpsNoHigher() {
        assertEquals(jump(22).first, jump(120).first, 0.01f)
    }

    @Test
    fun theFirstTapStartsTheRunWithoutJumping() {
        val course = terrain(Ground(0, 10_000, 32))
        val caro = Caro(config).apply { place(100.0, 32f) }
        assertEquals(CaroState.Waiting, caro.state)

        caro.start()
        caro.step(step, pressed = false, held = false, terrain = course)

        assertEquals(CaroState.Running, caro.state)
        assertEquals(32f, caro.y)
        assertTrue(caro.x > 100.0)
    }

    @Test
    fun theSpeedGrowsWithTheDistanceTowardsItsMaximum() {
        assertEquals(160f, config.speedAt(0.0), 0.01f)
        assertEquals(254.8f, config.speedAt(600.0), 0.1f)
        val speeds = (0..20_000 step 500).map { config.speedAt(it.toDouble()) }
        assertTrue(speeds.zipWithNext().all { (a, b) -> b > a }, "it keeps rising the whole run")
        assertTrue(config.speedAt(100_000.0) <= config.maxSpeed)
        assertTrue(config.speedAt(100_000.0) > 359.9f)
    }

    @Test
    fun runningOffAnEdgeSheFallsIntoTheGapAndTheRunEnds() {
        val course = terrain(Ground(0, 200, 32), Ground(400, 1_000, 32))
        val caro = runningOn(course, x = 150.0)

        repeat(240) { caro.step(step, pressed = false, held = false, terrain = course) }

        assertEquals(CaroState.Fallen, caro.state)
    }

    @Test
    fun aTapJustAfterTheEdgeStillJumps() {
        val course = terrain(Ground(0, 200, 32), Ground(400, 1_000, 32))
        val caro = runningOn(course, x = 150.0)
        while (caro.state.grounded) caro.step(step, pressed = false, held = false, terrain = course)
        assertEquals(CaroState.Falling, caro.state)

        caro.step(step, pressed = true, held = true, terrain = course)

        assertEquals(CaroState.Jumping, caro.state)
        assertEquals(1, caro.coyoteJumps, "the debug line counts it as a coyote jump")
        assertEquals(0, caro.bufferedJumps)
    }

    @Test
    fun aTapJustBeforeLandingJumpsOnLanding() {
        val course = terrain(Ground(0, 100_000, 32))
        val caro = runningOn(course)
        caro.step(step, pressed = true, held = false, terrain = course)
        while (caro.vy > 0f || caro.y > 32f + 3f) caro.step(step, pressed = false, held = false, terrain = course)

        caro.step(step, pressed = true, held = false, terrain = course)
        repeat(6) { caro.step(step, pressed = false, held = false, terrain = course) }

        assertEquals(CaroState.Jumping, caro.state, "the tap was remembered and she jumped again")
        assertEquals(2, caro.jumps)
        assertEquals(1, caro.bufferedJumps, "the debug line counts it as a buffered jump")
        assertEquals(0, caro.coyoteJumps)
    }

    @Test
    fun aShortJumpAgainstAHigherGroundSlidesDownItsWall() {
        val course = terrain(Ground(0, 200, 32), Ground(260, 2_000, 96))
        val caro = runningOn(course, x = 190.0)
        caro.step(step, pressed = true, held = false, terrain = course)

        repeat(240) { caro.step(step, pressed = false, held = false, terrain = course) }

        assertEquals(CaroState.Fallen, caro.state)
        assertTrue(caro.right <= 260.0, "she never went through the wall")
    }

    @Test
    fun aLongJumpCrossesAGapAndLandsOnTheOtherSide() {
        val course = terrain(Ground(0, 200, 32), Ground(260, 2_000, 48))
        val caro = runningOn(course, x = 180.0)
        caro.step(step, pressed = true, held = true, terrain = course)

        var time = step
        while (!caro.state.grounded && !caro.state.over) {
            caro.step(step, pressed = false, held = time < 0.3f, terrain = course)
            time += step
        }

        assertEquals(CaroState.Landing, caro.state)
        assertEquals(48f, caro.y)
        assertTrue(caro.left >= 200.0)
    }

    @Test
    fun aJumpUpToAHigherGroundLandsOnIt() {
        val course = terrain(Ground(0, 200, 32), Ground(230, 2_000, 64))
        val caro = runningOn(course, x = 190.0)
        caro.step(step, pressed = true, held = true, terrain = course)
        var time = step
        while (!caro.state.grounded && !caro.state.over) {
            caro.step(step, pressed = false, held = time < 0.2f, terrain = course)
            time += step
        }

        assertEquals(CaroState.Landing, caro.state)
        assertEquals(64f, caro.y)
    }

    @Test
    fun outOfPointsSheStopsAndNothingMovesHerAgain() {
        val course = terrain(Ground(0, 10_000, 32))
        val caro = runningOn(course)
        caro.stop()
        val x = caro.x

        caro.step(step, pressed = true, held = true, terrain = course)

        assertEquals(CaroState.Stopped, caro.state)
        assertEquals(x, caro.x)
    }
}

class FarFromTheOriginTest {
    /**
     * GEN-09: two hours at the top speed put her past a million pixels. Positions are Doubles and grounds Ints, so a jump
     * there is the same jump as at the start, to the hundredth of a pixel, and no origin has to be moved.
     */
    @Test
    fun aJumpTwoHoursIntoTheRunIsTheSameJump() {
        fun jumpAt(x0: Double): List<Float> {
            val config = MovementConfig()
            val course = Course().apply { append(Piece(listOf(Ground((x0 - 1_000).toInt(), (x0 + 10_000).toInt(), 32)), emptyList(), 0)) }
            val caro = Caro(config).apply {
                place(x0, 32f, runStart = x0 - 2 * 3600.0 * config.maxSpeed)
                start()
            }
            val heights = mutableListOf<Float>()
            caro.step(1f / 60f, pressed = true, held = true, terrain = course)
            repeat(40) {
                caro.step(1f / 60f, pressed = false, held = true, terrain = course)
                heights += caro.y
            }
            return heights
        }
        val near = jumpAt(2 * 3600.0 * 200)
        val far = jumpAt(2 * 3600.0 * 200 + 1_440_000)
        near.zip(far).forEach { (a, b) -> assertEquals(a, b, 0.01f) }
    }
}
