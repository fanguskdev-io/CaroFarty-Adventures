package com.fanguskdev.carofarty.storage

import com.fanguskdev.carofarty.game.Scenario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChoicesTest {
    @Test
    fun theFirstTimeItIsTheForestAndThenWhatSheChoseLast() {
        val store = MemoryStore()
        assertEquals(Scenario.Bosque, Choices(store).scenario)

        Choices(store).scenario = Scenario.Desierto
        assertEquals(Scenario.Desierto, Choices(store).scenario, "it is remembered between launches")
    }

    @Test
    fun aValueItDoesNotKnowIsTheForest() {
        val store = MemoryStore()
        store.putInt("chosen_scenario", 7)
        assertEquals(Scenario.Bosque, Choices(store).scenario)
    }

    @Test
    fun theSoundIsOnTheFirstTimeAndStaysAsSheLeavesIt() {
        val store = MemoryStore()
        assertTrue(Choices(store).soundOn)
        Choices(store).soundOn = false
        assertFalse(Choices(store).soundOn, "off until she turns it on")
    }

    @Test
    fun howToPlayShowsUntilSheHasSeenIt() {
        val store = MemoryStore()
        assertFalse(Choices(store).tutorialSeen)
        Choices(store).tutorialSeen = true
        assertTrue(Choices(store).tutorialSeen)
        assertEquals(0, Choices(store).runsPlayed)
    }
}
