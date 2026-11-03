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
    fun takingPointsAwayEndsTheRunAtZeroAsTouchingSomethingWould() {
        val run = newRun()
        run.takeAway(4)
        assertEquals(config.score.startPoints, run.points, "nothing is taken before the run starts")

        run.step(step, pressed = true, held = false)
        run.takeAway(4)
        assertEquals(config.score.startPoints - 4, run.points)
        run.takeAway(100)
        assertEquals(0, run.points)
        assertEquals(RunPhase.Over, run.phase)
        assertEquals(EndCause.NoPoints, run.endCause)
        assertEquals(config.score.startPoints, run.maxPoints)
    }

    @Test
    fun aWalkingPerritoIsPickedUpJustByRunningPastIt() {
        val perrito = Item(ItemKind.Perrito, 400, 32 + 8).apply { walk = Walk(-32f, 32f, config.items, seed = 3) }
        val pieces = object : PieceSource {
            private var given = false

            override fun nextOrNull(): Piece? {
                if (given) return null
                given = true
                return Piece(listOf(Ground(-512, 100_000, 32)), listOf(perrito), 100_000)
            }
        }
        val run = GameRun(config, Scenario.Bosque, seed = 1, pieces = pieces)
        run.step(step, pressed = true, held = false)
        repeat(240) { run.step(step, pressed = false, held = false) }
        assertTrue(perrito.taken, "she picked it up without jumping")
        assertEquals(config.score.startPoints + config.score.perrito, run.points)
    }

    @Test
    fun theRunStartsWithFivePointsAndTheCourseAhead() {
        val run = newRun()

        assertEquals(5, run.points)
        assertEquals(5, run.maxPoints)
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

        assertEquals(5 + 1 + 1 - 5, run.points)
        assertEquals(7, run.maxPoints)
        assertTrue(run.events.filterIsInstance<RunEvent.Picked>().size == 3)
    }

    @Test
    fun herFaceShowsWhatSheJustPickedUpForAMoment() {
        val run = newRun()
        run.step(step, pressed = true, held = false)
        assertEquals(Reaction.None, run.reaction)
        val y = run.caro.y.toInt() + 8
        run.course.items.addFirst(Item(ItemKind.Gatito, run.caro.x.toInt() + 2, y))
        run.step(step, pressed = false, held = false)
        assertEquals(Reaction.Happy, run.reaction, "a rescued gatito makes her happy")

        run.course.items.addFirst(Item(ItemKind.Huevo, run.caro.x.toInt() + 2, y))
        run.step(step, pressed = false, held = false)
        assertEquals(Reaction.Disgusted, run.reaction, "the last thing picked up is the one that shows")

        val steps = (config.items.reactionSeconds / step).toInt()
        repeat(steps - 1) { run.step(step, pressed = false, held = false) }
        assertEquals(Reaction.Disgusted, run.reaction)
        repeat(3) { run.step(step, pressed = false, held = false) }
        assertEquals(Reaction.None, run.reaction, "and after ${config.items.reactionSeconds} s her face is back")
    }

    /** A run on flat ground with nothing on it, where the test puts what it wants. */
    private fun flatRun(): GameRun {
        val pieces = object : PieceSource {
            private var given = false

            override fun nextOrNull(): Piece? {
                if (given) return null
                given = true
                return Piece(listOf(Ground(-512, 100_000, 32)), emptyList(), 100_000)
            }
        }
        return GameRun(config, Scenario.Bosque, seed = 1, pieces = pieces)
    }

    @Test
    fun afterAnEggSheBlinksThroughTheNextFoodButStillRescuesAnimals() {
        val run = flatRun()
        run.step(step, pressed = true, held = false)
        val y = run.caro.y.toInt() + 8
        run.course.items.addFirst(Item(ItemKind.Huevo, run.caro.x.toInt() + 2, y))
        run.step(step, pressed = false, held = false)
        assertEquals(config.score.startPoints + config.score.huevo, run.points)
        assertTrue(run.caro.knockedBack, "the egg pushes her back")
        assertTrue(run.blinking > 0f, "and she blinks")

        val fish = Item(ItemKind.Pescado, run.caro.x.toInt() + 2, y)
        run.course.items.addFirst(fish)
        run.step(step, pressed = false, held = false)
        assertEquals(config.score.startPoints + config.score.huevo, run.points, "blinking, the fish takes nothing away")
        assertTrue(!fish.taken, "and stays where it was")

        run.course.items.addFirst(Item(ItemKind.Gatito, run.caro.x.toInt() + 2, y))
        run.step(step, pressed = false, held = false)
        assertEquals(config.score.startPoints + config.score.huevo + config.score.gatito, run.points, "the gatito she rescues")

        repeat((config.items.blinkSeconds / step).toInt() + 2) { run.step(step, pressed = false, held = false) }
        assertEquals(0f, run.blinking)
        run.course.items.addFirst(Item(ItemKind.Huevo, run.caro.x.toInt() + 2, y))
        run.step(step, pressed = false, held = false)
        assertEquals(config.score.startPoints + 2 * config.score.huevo + config.score.gatito, run.points, "after the blinking, again")
    }

    @Test
    fun withNoPointsLeftTheRunEndsAndSheStops() {
        val run = newRun()
        run.step(step, pressed = true, held = false)
        val y = run.caro.y.toInt() + 8
        val x = run.caro.x.toInt()
        run.takeAway(config.score.startPoints + config.score.pescado)
        run.course.items.addFirst(Item(ItemKind.Pescado, x + 2, y))

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
