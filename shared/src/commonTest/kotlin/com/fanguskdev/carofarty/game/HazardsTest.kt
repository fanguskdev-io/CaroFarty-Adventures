package com.fanguskdev.carofarty.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The dangers Fangusk chose after the beta 7 (ADR 0011): how they move, how they move her and what they take. */
class HazardsTest {
    private val config = GameConfig()
    private val movement = config.movement
    private val step = JumpLab.STEP

    private fun course(grounds: List<Ground>, platforms: List<Platform> = emptyList(), items: List<Item> = emptyList()) =
        Course().apply { append(Piece(grounds, items, grounds.last().end, platforms = platforms)) }

    @Test
    fun aFloatingEggGoesUpAndDownWithHerAndNotWithTheClock() {
        // ENM-17: the same height whenever she gets there, a whole travel up and down every period.
        val bob = Bob(fromX = 1_000.0, pace = 300f, amplitude = 6, period = 1.5, phase = 0.0)
        assertEquals(-6.0, bob.dy(1_000.0), 1e-9)
        assertEquals(6.0, bob.dy(1_000.0 + 300 * 0.75), 1e-9)
        assertEquals(-6.0, bob.dy(1_000.0 + 300 * 1.5), 1e-9)
        assertEquals(0.0, bob.dx(1_234.0))
        assertEquals(bob.dy(1_100.0), bob.dy(1_100.0))
    }

    @Test
    fun aBirdThatFliesAtHerMeetsHerAtItsPlace() {
        // ENM-18: from the right, faster than the ground, at its place when her feet get there.
        val charge = Charge(fromX = 2_000.0, pace = 400f, speed = 100f, wave = 0, wavePeriod = 1.0, phase = 0.0)
        assertEquals(0.0, charge.dx(2_000.0), 1e-9)
        assertEquals(100.0, charge.dx(2_000.0 - 400), 1e-9)
        assertTrue(charge.dx(2_100.0) < 0.0)
        assertTrue(charge.towardsLeft(1_000.0))
        val bird = Item(ItemKind.Pajaro, 2_000, 40).also { it.flight = charge }
        val caro = Caro(movement).apply { place(2_000.0, 32f) }
        assertTrue(overlaps(caro, bird, config.items.negativeBox, movement))
        caro.place(1_900.0, 32f)
        assertFalse(overlaps(caro, bird, config.items.negativeBox, movement))
    }

    @Test
    fun aStrollingBirdGoesBackAndForthOverItsStretchAndTurns() {
        val stroll = Stroll(fromX = 0.0, pace = 300f, span = 48, speed = 40f, phase = 0.0, bob = 2)
        var left = false
        var right = false
        var reach = 0.0
        while (reach < 300.0 * stroll.period * 2) {
            assertTrue(abs(stroll.dx(reach)) <= 24.0 + 1e-9)
            if (stroll.towardsLeft(reach)) left = true else right = true
            reach += 5.0
        }
        assertTrue(left && right, "it turns at the ends")
    }

    @Test
    fun aFishLeapsOutOfTheWaterWhenSheComesAndFallsBackIntoIt() {
        // ENM-15: under the water before and after, at the top halfway, where the generator puts the top.
        val leap = Leap(fromX = 500.0, pace = 300f, across = -30f, rise = 350f, gravity = 700f)
        assertFalse(leap.out(499.0))
        assertTrue(leap.out(500.0 + 300 * leap.airtime / 2))
        assertFalse(leap.out(500.0 + 300 * (leap.airtime + 0.01)))
        assertEquals(350.0 * 350.0 / (2 * 700), leap.dy(500.0 + 300 * leap.airtime / 2), 1e-6)
        assertTrue(leap.towardsLeft(600.0))
        assertTrue(leap.climb(500.0) > 0 && leap.climb(500.0 + 300 * leap.airtime) < 0)
    }

    @Test
    fun aBirdTakesEightPoints() {
        val bird = Item(ItemKind.Pajaro, (GameRun.START_X + 60).toInt(), 32 + 10).also {
            it.flight = Charge(it.x.toDouble(), 160f, 80f, 0, 1.0, 0.0)
        }
        val start = Piece(listOf(Ground(-512, 4_096, 32)), listOf(bird), 4_096)
        val pieces = object : PieceSource {
            var given = false

            override fun nextOrNull(): Piece? = if (given) null else start.also { given = true }
        }
        val run = GameRun(config.copy(score = config.score.copy(startPoints = 20)), Scenario.Bosque, seed = 1, pieces = pieces)
        run.step(step, pressed = true, held = false)
        repeat(60) { run.step(step, pressed = false, held = false) }
        assertEquals(20 + config.score.pajaro, run.points)
        assertEquals(-8, config.score.pajaro)
    }

    @Test
    fun onTheIceSheGoesFasterAndJumpsFurther() {
        // PLY-18: +12 % running and in the jump that takes off from it.
        val plain = course(listOf(Ground(0, 20_000, 32)))
        val icy = course(listOf(Ground(0, 20_000, 32, Patch(0, 20_000, Footing.Ice))))
        fun landing(on: Course): Double {
            val caro = Caro(movement).apply {
                place(1_000.0, 32f)
                start()
            }
            repeat(10) { caro.step(step, pressed = false, held = false, terrain = on) }
            val from = caro.x
            caro.step(step, pressed = true, held = true, terrain = on)
            var steps = 0
            while (!caro.state.grounded && steps++ < 200) caro.step(step, pressed = false, held = steps < 10, terrain = on)
            return caro.x - from
        }
        val normal = landing(plain)
        val longer = landing(icy)
        assertEquals(movement.iceSpeed.toDouble(), longer / normal, 0.02)
    }

    @Test
    fun onTheQuicksandSheJumpsAboutThirtyPerCentLower() {
        fun highest(on: Course): Float {
            val caro = Caro(movement).apply {
                place(1_000.0, 32f)
                start()
            }
            repeat(5) { caro.step(step, pressed = false, held = false, terrain = on) }
            caro.step(step, pressed = true, held = true, terrain = on)
            var top = caro.y
            var steps = 0
            while (!caro.state.grounded && steps++ < 200) {
                caro.step(step, pressed = false, held = true, terrain = on)
                top = max(top, caro.y)
            }
            return top - 32f
        }
        val normal = highest(course(listOf(Ground(0, 20_000, 32))))
        val sunk = highest(course(listOf(Ground(0, 20_000, 32, Patch(0, 20_000, Footing.Quicksand)))))
        assertEquals(0.7f, sunk / normal, 0.03f)
    }

    @Test
    fun aPlatformThatFallsTremblesWhenSheStandsOnItAndThenFallsWithHer() {
        // ENM-16: it trembles the seconds of the settings and falls; on it, she falls too.
        val tremble = config.hazards.trembleSeconds.toDouble()
        val platform = Platform(900, 2_000, 64, fall = Fall(tremble))
        val terrain = course(listOf(Ground(0, 896, 32)), listOf(platform))
        val caro = Caro(movement).apply {
            place(1_000.0, 64f)
            start()
        }
        terrain.time += step
        caro.step(step, pressed = false, held = false, terrain = terrain)
        assertFalse(platform.touchedAt.isNaN(), "she stood on it")
        assertTrue(platform.tremblingAt(terrain.time))
        var fell = false
        repeat(60) {
            terrain.time += step
            caro.step(step, pressed = false, held = false, terrain = terrain)
            if (caro.y < 60f) fell = true
        }
        assertTrue(platform.topAt(terrain.time) < 64.0, "it fell")
        assertTrue(fell, "and she with it")
    }

    @Test
    fun theValidatorLeavesThePlatformsThatFallAsItFoundThem() {
        val platform = Platform(900, 2_000, 64, fall = Fall(0.3))
        val terrain = course(listOf(Ground(0, 896, 32)), listOf(platform))
        val lab = JumpLab(movement)
        lab.tryJump(terrain, emptyList(), 1_000.0, 64f, 0.0, pressAfter = 30, hold = 5)
        assertTrue(platform.touchedAt.isNaN())
        lab.tryHolds(terrain, emptyList(), 1_000.0, 64f, 0.0, pressAfter = 30, maxHold = 10) { _, _ -> true }
        assertTrue(platform.touchedAt.isNaN())
    }
}
