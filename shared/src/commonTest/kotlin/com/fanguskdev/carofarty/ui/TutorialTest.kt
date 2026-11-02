package com.fanguskdev.carofarty.ui

import com.fanguskdev.carofarty.game.CaroState
import com.fanguskdev.carofarty.game.GameConfig
import com.fanguskdev.carofarty.game.RunPhase
import com.fanguskdev.carofarty.game.Scenario
import com.fanguskdev.carofarty.ui.render.CaroLook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TutorialTest {
    private val config = GameConfig()

    private fun scene(page: Int) = TutorialScene(page, config, Scenario.Bosque) { CaroLook() }

    /** Steps [scene] for [seconds], telling what [watch] saw on each step. */
    private fun play(scene: TutorialScene, seconds: Float, watch: (TutorialScene) -> Unit = {}) {
        repeat((seconds * 60).toInt()) {
            scene.step()
            watch(scene)
        }
    }

    @Test
    fun theJumpingPageClearsItsTwoGapsOverAndOver() {
        val scene = scene(TutorialScene.JUMPING)
        var longest = 0
        var holding = 0
        play(scene, seconds = 30f) {
            assertTrue(it.run.caro.state != CaroState.Fallen, "she fell on the page of jumping")
            holding = if (it.held) holding + 1 else 0
            longest = maxOf(longest, holding)
        }
        assertTrue(longest > 6, "a jump held long enough to see it: $longest steps")
    }

    @Test
    fun theRescuingPagePicksUpTheFourAnimals() {
        val scene = scene(TutorialScene.RESCUING)
        var seen = 0
        play(scene, seconds = 3.2f) { seen = maxOf(seen, it.run.points) }
        assertEquals(config.score.startPoints + 4, seen)
        assertTrue(scene.floating.isNotEmpty() || seen > 0)
    }

    @Test
    fun theAvoidingPageNeverTouchesTheEggNorTheFish() {
        val scene = scene(TutorialScene.AVOIDING)
        play(scene, seconds = 20f) { assertEquals(config.score.startPoints, it.run.points, "she touched something") }
    }

    @Test
    fun theEndingPageFallsAndStartsAgain() {
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
