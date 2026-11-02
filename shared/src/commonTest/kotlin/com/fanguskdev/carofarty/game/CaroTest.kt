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

    /** A course of the given grounds and blocks in the air. */
    private fun terrain(blocks: List<Block>, vararg grounds: Ground) =
        Course().apply { append(Piece(grounds.toList(), emptyList(), grounds.last().end, blocks = blocks)) }

    /** How many pixels of her collision box are inside [block] up and down, if she is inside it across at all. */
    private fun Caro.insideOf(block: Block): Float {
        if (right <= block.start || left >= block.end) return 0f
        return max(0f, minOf(y + config.hitboxHeight, block.top.toFloat()) - maxOf(y, block.bottom.toFloat()))
    }

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
    fun aBlockOverHerHeadCutsTheJumpShort() {
        val block = Block(60, 300, bottom = 32 + 50, top = 32 + 66)
        val course = terrain(listOf(block), Ground(0, 100_000, 32))
        val caro = runningOn(course)
        caro.step(step, pressed = true, held = true, terrain = course)
        var highest = caro.y
        repeat(60) {
            caro.step(step, pressed = false, held = true, terrain = course)
            highest = max(highest, caro.y)
            assertEquals(0f, caro.insideOf(block), "her head never goes into the block")
        }
        assertEquals(block.bottom - config.hitboxHeight, highest, 0.01f)
        assertTrue(caro.state.grounded && caro.y == 32f, "she falls back to the ground")
    }

    @Test
    fun theSideOfABlockStopsHerInTheAirAndSheFallsIntoTheGap() {
        val block = Block(240, 272, bottom = 40, top = 120)
        val course = terrain(listOf(block), Ground(0, 200, 32), Ground(600, 2_000, 32))
        val caro = runningOn(course, x = 180.0)
        caro.step(step, pressed = true, held = true, terrain = course)
        var touched = false
        repeat(240) { frame ->
            caro.step(step, pressed = false, held = frame < 9, terrain = course)
            if (caro.right == block.start.toDouble()) touched = true
            assertTrue(caro.insideOf(block) <= Course.CORNER_TOLERANCE, "never through the block")
        }
        assertTrue(touched, "she reached the block's side")
        assertEquals(CaroState.Fallen, caro.state)
    }

    @Test
    fun sheCanLandOnABlockRunOnItAndDropOffItsEnd() {
        val block = Block(200, 400, bottom = 60, top = 76)
        val course = terrain(listOf(block), Ground(0, 100_000, 32))
        val caro = runningOn(course, x = 140.0)
        caro.step(step, pressed = true, held = true, terrain = course)
        var onTop = false
        repeat(240) {
            caro.step(step, pressed = false, held = true, terrain = course)
            if (caro.state.grounded && caro.y == block.top.toFloat()) onTop = true
            assertTrue(caro.insideOf(block) <= Course.CORNER_TOLERANCE, "never through the block")
        }
        assertTrue(onTop, "she ran on top of the block")
        assertTrue(caro.x > block.end && caro.state.grounded && caro.y == 32f, "and dropped back to the ground after it")
    }

    /** A course of the given grounds and platforms. */
    private fun withPlatforms(platforms: List<Platform>, vararg grounds: Ground) =
        Course().apply { append(Piece(grounds.toList(), emptyList(), grounds.last().end, platforms = platforms)) }

    @Test
    fun sheJumpsThroughAPlatformFromBelowLandsOnItAndRunsOffItsEnd() {
        // Low over the ground: a held jump from under it goes through it and comes down on top (ADR 0009).
        val platform = Platform(150, 350, top = 32 + 40)
        val course = withPlatforms(listOf(platform), Ground(0, 100_000, 32))
        val caro = runningOn(course, x = 170.0)
        caro.step(step, pressed = true, held = true, terrain = course)
        var onTop = false
        var highest = caro.y
        repeat(240) {
            caro.step(step, pressed = false, held = true, terrain = course)
            highest = max(highest, caro.y)
            if (caro.state.grounded && caro.y == platform.top.toFloat()) onTop = true
        }
        assertTrue(highest > platform.top, "through it from below")
        assertTrue(onTop, "and on top of it")
        assertTrue(caro.x > platform.end && caro.state.grounded && caro.y == 32f, "then off its end, back on the ground")
    }

    @Test
    fun aPlatformStopsNothingFromTheSide() {
        // At the height of her body, ahead of her: she runs on under it.
        val platform = Platform(200, 260, top = 32 + 12)
        val course = withPlatforms(listOf(platform), Ground(0, 100_000, 32))
        val caro = runningOn(course, x = 150.0)
        repeat(60) { caro.step(step, pressed = false, held = false, terrain = course) }
        assertTrue(caro.x > platform.end, "she ran past it: ${caro.x}")
        assertEquals(32f, caro.y)
    }

    @Test
    fun aPlatformThatGoesAcrossCarriesHer() {
        // Long, over a long gap, moving forward and back by 64 px every 2 s.
        val mover = Platform(100, 1_600, top = 32, motion = Motion(dx = 64, dy = 0, period = 2.0, phase = 0.0))
        val still = Platform(100, 1_600, top = 32)
        fun runOn(platform: Platform): Double {
            val course = withPlatforms(listOf(platform), Ground(0, 100, 32), Ground(2_000, 100_000, 32))
            val caro = Caro(config).apply {
                place(120.0, 32f)
                start()
            }
            repeat(30) {
                course.time += step
                caro.step(step, pressed = false, held = false, terrain = course)
            }
            assertTrue(caro.state.grounded, "on the platform all the time")
            return caro.x
        }
        // In half a second going forward, the platform took her farther than running alone.
        assertTrue(runOn(mover) > runOn(still) + 10.0, "${runOn(mover)} against ${runOn(still)}")
    }

    @Test
    fun aPlatformThatGoesDownKeepsHerOnItWithoutLandingAgain() {
        val lift = Platform(0, 100_000, top = 120, motion = Motion(dx = 0, dy = -48, period = 4.0, phase = 0.0))
        val course = withPlatforms(listOf(lift), Ground(0, 100_000, 16))
        val caro = Caro(config).apply {
            place(100.0, 120f)
            start()
        }
        repeat(120) {
            course.time += step
            caro.step(step, pressed = false, held = false, terrain = course)
            assertTrue(caro.state == CaroState.Running, "she keeps running on it: ${caro.state} at ${caro.y}")
            assertEquals(lift.topAt(course.time).toFloat(), caro.y, absoluteTolerance = 0.01f)
        }
    }

    @Test
    fun grazingTheBackCornerOfABlockDoesNotStopTheJump() {
        // Her left edge is two pixels short of the block's end when her head reaches its bottom: she is leaving it behind.
        val block = Block(0, 100, bottom = 32 + 22, top = 32 + 38)
        val course = terrain(listOf(block), Ground(0, 100_000, 32))
        val caro = runningOn(course)
        caro.step(step, pressed = true, held = true, terrain = course)
        var highest = caro.y
        repeat(60) {
            caro.step(step, pressed = false, held = true, terrain = course)
            highest = max(highest, caro.y)
        }
        assertTrue(highest - 32f > 60f, "the jump went on to ${highest - 32f} px")
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

    @Test
    fun anEggPushesHerBackALittleAndBrakesHerAndTheScreenShowsIt() {
        val course = terrain(Ground(-512, 1_000_000, 32))
        val caro = runningOn(course)
        repeat(30) { caro.step(step, pressed = false, held = false, terrain = course) }
        val before = caro.x
        caro.knockBack()
        repeat(3) { caro.step(step, pressed = false, held = false, terrain = course) }
        assertTrue(caro.x < before, "pushed back: ${caro.x} < $before")
        assertTrue(caro.cameraLag > 0f, "the camera stays a little ahead, so she is seen going back")
        repeat(60) { caro.step(step, pressed = false, held = false, terrain = course) }
        assertTrue(!caro.knockedBack && caro.cameraLag < 1f, "and soon she runs again where she was on the screen")
    }

    @Test
    fun theWallBehindHerStopsThePushBack() {
        // She has just come down a step: the higher ground ends right behind her.
        val course = terrain(Ground(-512, 100, 64), Ground(100, 1_000_000, 32))
        val caro = runningOn(course, x = 100.0 + config.hitboxWidth / 2 + 0.5)
        caro.knockBack()
        repeat(20) {
            caro.step(step, pressed = false, held = false, terrain = course)
            assertTrue(caro.left >= 100.0, "she went into the wall: ${caro.left}")
        }
    }

    @Test
    fun inTheAirThePushBackShortensTheJumpAndCanLeaveHerShortOfTheGround() {
        fun landing(knocked: Boolean): Double {
            val course = terrain(Ground(-512, 1_000_000, 32))
            val caro = runningOn(course)
            caro.step(step, pressed = true, held = true, terrain = course)
            var steps = 0
            while (!caro.state.grounded) {
                if (knocked && steps == 5) caro.knockBack()
                caro.step(step, pressed = false, held = steps < 20, terrain = course)
                steps++
            }
            return caro.x
        }
        val normal = landing(knocked = false)
        val pushed = landing(knocked = true)
        assertTrue(pushed < normal - 20, "a jump pushed back lands short: $pushed against $normal")
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
