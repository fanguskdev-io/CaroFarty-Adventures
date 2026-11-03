package com.fanguskdev.carofarty.ui.render

import com.fanguskdev.carofarty.game.CaroState
import com.fanguskdev.carofarty.game.GameConfig
import com.fanguskdev.carofarty.game.GameRun
import com.fanguskdev.carofarty.game.Ground
import com.fanguskdev.carofarty.game.JumpLab
import com.fanguskdev.carofarty.game.Piece
import com.fanguskdev.carofarty.game.PieceSource
import com.fanguskdev.carofarty.game.Scenario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CaroLookTest {
    private val step = JumpLab.STEP

    /** A run on [grounds] alone, with nothing to pick up, with [config]. */
    private fun runOn(vararg grounds: Ground, config: GameConfig = GameConfig()): GameRun {
        val pieces = object : PieceSource {
            private var given = false

            override fun nextOrNull(): Piece? {
                if (given) return null
                given = true
                return Piece(grounds.toList(), emptyList(), grounds.last().end)
            }
        }
        return GameRun(config, Scenario.Bosque, seed = 1, pieces = pieces)
    }

    @Test
    fun anAnimationLoopsOrStaysOnItsLastFrame() {
        val animation = Animation(intArrayOf(4, 5, 6), intArrayOf(100, 50, 100), emptyBelow = 0)
        assertEquals(4, animation.frameAt(0, loops = true))
        assertEquals(5, animation.frameAt(120, loops = true))
        assertEquals(6, animation.frameAt(249, loops = true))
        assertEquals(4, animation.frameAt(250, loops = true), "round again")
        assertEquals(6, animation.frameAt(10_000, loops = false), "or it stays on the last")
        assertEquals(5, animation.frame(4))
    }

    @Test
    fun sheWaitsRunsJumpsAndLands() {
        val run = runOn(Ground(-512, 100_000, 32))
        val look = CaroLook()
        look.step(step, run, newRecord = false)
        assertEquals(CaroPose.Espera, look.pose)

        run.step(step, pressed = true, held = false)
        look.step(step, run, newRecord = false)
        assertEquals(CaroPose.Corre, look.pose)

        run.step(step, pressed = true, held = true)
        look.step(step, run, newRecord = false)
        assertEquals(CaroPose.Salta, look.pose)
        val seen = mutableListOf(look.pose)
        repeat(120) {
            run.step(step, pressed = false, held = false)
            look.step(step, run, newRecord = false)
            if (look.pose != seen.last()) seen += look.pose
        }
        assertEquals(listOf(CaroPose.Salta, CaroPose.Apice, CaroPose.Cae, CaroPose.Aterriza, CaroPose.Corre), seen)
    }

    @Test
    fun herStrideFollowsTheGroundButNeverFasterThanFortyMillisecondsAFrame() {
        val run = runOn(Ground(-512, 1_000_000, 32))
        val look = CaroLook()
        run.step(step, pressed = true, held = false)
        // At the start, 160 px/s: a frame every 8 px of corre, every 50 ms.
        var changes = 0
        var last = -1
        val animation = Animation(IntArray(6) { it }, IntArray(6) { 1 }, emptyBelow = 0)
        repeat(60) {
            run.step(step, pressed = false, held = false)
            look.step(step, run, newRecord = false)
            val frame = look.frame(animation)
            if (frame != last) changes++
            last = frame
        }
        assertTrue(changes in 19..22, "$changes frames in a second at the start")
    }

    @Test
    fun pastFourHundredAndFiftyPixelsASecondSheSprints() {
        // GameMaster's third gait (ADR 0011), with its quickest frame of 30 ms: at 500 px/s and a cycle of 9 tiles, a frame
        // every 18 px, about 28 a second.
        val fast = GameConfig().let {
            it.copy(movement = it.movement.copy(startSpeed = 500f, quickSpeed = 500f, maxSpeed = 500f, lateSpeed = 500f))
        }
        val run = runOn(Ground(-512, 1_000_000, 32), config = fast)
        val look = CaroLook(sprintFrames = 8, sprintCycleTiles = 9f)
        run.step(step, pressed = true, held = false)
        var changes = 0
        var last = -1
        val animation = Animation(IntArray(8) { it }, IntArray(8) { 1 }, emptyBelow = 0)
        repeat(60) {
            run.step(step, pressed = false, held = false)
            look.step(step, run, newRecord = false)
            val frame = look.frame(animation)
            if (frame != last) changes++
            last = frame
        }
        assertEquals(CaroPose.CorreSprint, look.pose)
        assertTrue(changes in 26..30, "$changes frames in a second at 500 px/s")
    }

    @Test
    fun fallingIntoAGapSheGoesDownTheGap() {
        val run = runOn(Ground(-512, 600, 32), Ground(900, 100_000, 32))
        val look = CaroLook()
        run.step(step, pressed = true, held = false)
        val poses = mutableSetOf<CaroPose>()
        repeat(60 * 5) {
            run.step(step, pressed = false, held = false)
            look.step(step, run, newRecord = false)
            poses += look.pose
        }
        assertEquals(CaroState.Fallen, run.caro.state)
        assertEquals(CaroPose.AlBarranco, look.pose)
        assertTrue(CaroPose.Cae in poses, "she starts falling as in any fall")
    }

    @Test
    fun anEggOrAFishPushesHerBackOnTheGroundAndInTheAir() {
        for (inTheAir in listOf(false, true)) {
            val run = runOn(Ground(-512, 100_000, 32))
            val look = CaroLook()
            run.step(step, pressed = true, held = false)
            repeat(30) { run.step(step, pressed = false, held = false) }
            if (inTheAir) repeat(6) { run.step(step, pressed = it == 0, held = true) }
            assertEquals(inTheAir, !run.caro.state.grounded)
            run.caro.knockBack()
            run.step(step, pressed = false, held = false)
            look.step(step, run, newRecord = false)
            assertEquals(CaroPose.Retroceso, look.pose, if (inTheAir) "in the air" else "on the ground")
            repeat(60) {
                run.step(step, pressed = false, held = false)
                look.step(step, run, newRecord = false)
            }
            assertTrue(look.pose != CaroPose.Retroceso, "and then it is over")
        }
    }

    @Test
    fun outOfPointsSheIsSadAndWithARecordSheCelebratesItAfterwards() {
        val run = runOn(Ground(-512, 100_000, 32))
        run.step(step, pressed = true, held = false)
        run.takeAway(100)
        val sad = CaroLook()
        val proud = CaroLook()
        repeat(60) {
            sad.step(step, run, newRecord = false)
            proud.step(step, run, newRecord = true)
        }
        assertEquals(CaroPose.SinPuntos, sad.pose)
        assertEquals(CaroPose.Record, proud.pose)
    }
}
