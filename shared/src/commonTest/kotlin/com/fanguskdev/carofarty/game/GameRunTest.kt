package com.fanguskdev.carofarty.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameRunTest {
    private val config = GameConfig()
    private val step = JumpLab.STEP

    private fun newRun(seed: Long = 3) = GameRun(config, Scenario.Bosque, seed)

    @Test
    fun beforeTheFirstTapNothingMovesAndTheTapOnlyStartsTheRun() {
        val run = newRun()
        repeat(30) { run.step(step, pressed = false, held = false) }
        assertEquals(RunPhase.Ready, run.phase)
        assertEquals(0.0, run.meters)

        run.step(step, pressed = true, held = true)
        run.step(step, pressed = false, held = true)

        assertEquals(RunPhase.Running, run.phase)
        assertEquals(CaroState.Running, run.caro.state, "the first tap does not jump")
        assertTrue(run.meters > 0.0)
    }

    @Test
    fun theRunStartsWithTenPointsAndTheCourseAhead() {
        val run = newRun()

        assertEquals(10, run.points)
        assertEquals(10, run.maxPoints)
        assertTrue(run.course.end >= run.caro.x + GameRun.AHEAD)
    }

    @Test
    fun perritosAndGatitosAddAndEggsAndFishTakeAway() {
        val run = newRun()
        run.step(step, pressed = true, held = false)
        val y = run.caro.y.toInt() + 8
        val x = run.caro.x.toInt()
        run.course.items.addFirst(Item(ItemKind.Pescado, x + 6, y))
        run.course.items.addFirst(Item(ItemKind.Perrito, x + 4, y))
        run.course.items.addFirst(Item(ItemKind.Gatito, x + 2, y))

        run.step(step, pressed = false, held = false)

        assertEquals(10 + 1 + 1 - 3, run.points)
        assertEquals(12, run.maxPoints)
        assertTrue(run.events.filterIsInstance<RunEvent.Picked>().size == 3)
    }

    @Test
    fun withNoPointsLeftTheRunEndsAndSheStops() {
        val run = newRun()
        run.step(step, pressed = true, held = false)
        val y = run.caro.y.toInt() + 8
        val x = run.caro.x.toInt()
        repeat(4) { run.course.items.addFirst(Item(ItemKind.Pescado, x + 2, y)) }

        run.step(step, pressed = false, held = false)

        assertEquals(0, run.points)
        assertEquals(RunPhase.Over, run.phase)
        assertEquals(EndCause.NoPoints, run.endCause)
        assertEquals(CaroState.Stopped, run.caro.state)
        assertEquals(RunEvent.Ended(EndCause.NoPoints), run.events.last())
    }

    @Test
    fun withoutJumpingSheFallsIntoTheFirstGapAndTheRunEnds() {
        val run = newRun()
        run.step(step, pressed = true, held = false)

        repeat(60 * 20) { run.step(step, pressed = false, held = false) }

        assertEquals(RunPhase.Over, run.phase)
        assertEquals(EndCause.Fall, run.endCause)
        assertTrue(run.meters >= config.course.startFlatTiles - 1, "she ran the whole start first")
    }

    @Test
    fun whatTheCameraLeftBehindIsForgotten() {
        val run = newRun()
        Pilot(run).play(seconds = 60f)

        assertTrue(run.course.grounds.first().end >= run.caro.x - GameRun.BEHIND - config.course.maxGroundTiles * TILE)
        assertTrue(run.course.grounds.size < 40, "${run.course.grounds.size} grounds kept")
        assertNull(run.course.items.firstOrNull { it.x < run.caro.x - GameRun.BEHIND - Item.SIZE })
    }
}
