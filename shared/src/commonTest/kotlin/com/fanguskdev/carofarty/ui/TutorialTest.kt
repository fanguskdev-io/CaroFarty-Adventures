package com.fanguskdev.carofarty.ui

import com.fanguskdev.carofarty.game.CaroState
import com.fanguskdev.carofarty.game.GameConfig
import com.fanguskdev.carofarty.game.RunPhase
import com.fanguskdev.carofarty.game.Scenario
import com.fanguskdev.carofarty.ui.render.CaroLook
import com.fanguskdev.carofarty.ui.render.CaroPose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TutorialTest {
    private val config = GameConfig()

    private fun scene(index: Int) = TutorialScene(index, config, Scenario.Bosque) { CaroLook() }

    /** Steps [scene] for [seconds], telling what [watch] saw on each step. */
    private fun play(scene: TutorialScene, seconds: Float, watch: (TutorialScene) -> Unit = {}) {
        repeat((seconds * 60).toInt()) {
            scene.step()
            watch(scene)
        }
    }

    @Test
    fun theJumpingSceneClearsItsTwoGapsOverAndOver() {
        val scene = scene(TutorialScene.JUMPING)
        var longest = 0
        var holding = 0
        play(scene, seconds = 30f) {
            assertTrue(it.run.caro.state != CaroState.Fallen, "she fell in the scene of jumping")
            holding = if (it.held) holding + 1 else 0
            longest = maxOf(longest, holding)
        }
        assertTrue(longest > 3, "a jump held long enough to see it: $longest steps")
    }

    @Test
    fun aTapForTheNarrowGapAHoldForTheWideOneAndBothFitInTheScene() {
        val scene = scene(TutorialScene.JUMPING)
        val peaks = mutableListOf<Float>()
        var peak = 0f
        play(scene, seconds = 6f) {
            val height = it.run.caro.y - TutorialScene.TOP
            if (height > 0f) {
                peak = maxOf(peak, height)
            } else if (peak > 0f) {
                peaks += peak
                peak = 0f
            }
        }
        assertTrue(peaks.size >= 2, "two jumps: $peaks")
        assertTrue(peaks[0] < 20f, "the tap is short: $peaks")
        assertTrue(peaks[1] > peaks[0] + 5f, "the hold goes higher: $peaks")
        // Low enough for the scene, whose camera goes up with her: some of the ground is still seen at the top of it.
        assertTrue(peaks.all { it <= 30f }, "it fits: $peaks")
    }

    @Test
    fun theRescuingScenePicksUpTheFourAnimals() {
        val scene = scene(TutorialScene.RESCUING)
        var seen = 0
        play(scene, seconds = 3.2f) { seen = maxOf(seen, it.run.points) }
        assertEquals(TutorialScene.START_POINTS + 4, seen)
        assertTrue(scene.floating.isNotEmpty() || seen > 0)
    }

    @Test
    fun theAvoidingSceneShowsWhatTheEggAndTheFishTakeAway() {
        val scene = scene(TutorialScene.AVOIDING)
        var fewest = TutorialScene.START_POINTS
        val texts = mutableSetOf<String>()
        var again = false
        play(scene, seconds = 12f) {
            fewest = minOf(fewest, it.run.points)
            it.floating.forEach { floating -> texts += floating.text }
            if (fewest < TutorialScene.START_POINTS && it.run.points == TutorialScene.START_POINTS) again = true
        }
        assertEquals(TutorialScene.START_POINTS - 8, fewest, "the egg takes 3 and the fish 5")
        assertEquals(setOf("-3", "-5"), texts)
        assertTrue(again, "it started again")
    }

    @Test
    fun theFingerTellsATapFromAHold() {
        val scene = scene(TutorialScene.JUMPING)
        var tapped = false
        var held = false
        play(scene, seconds = 10f) {
            if (it.touching > 0f && it.holding <= 0f) tapped = true
            if (it.holding > 0f) held = true
        }
        assertTrue(tapped && held, "a tap and a hold: $tapped, $held")
    }

    @Test
    fun noSceneShowsHerWaitingNotEvenWhenItStartsAgain() {
        for (index in 0 until TutorialScene.SCENES) {
            val scene = scene(index)
            assertTrue(scene.look.pose != CaroPose.Espera, "scene $index as it opens")
            play(scene, seconds = 8f) { assertTrue(it.look.pose != CaroPose.Espera, "scene $index") }
        }
    }

    @Test
    fun theEndingSceneFallsAndStartsAgain() {
        val scene = scene(TutorialScene.ENDING)
        var fell = false
        var again = false
        play(scene, seconds = 8f) {
            if (it.run.phase == RunPhase.Over) fell = true
            if (fell && it.run.phase == RunPhase.Running) again = true
        }
        assertTrue(fell && again, "it showed the fall and started again")
    }
}
