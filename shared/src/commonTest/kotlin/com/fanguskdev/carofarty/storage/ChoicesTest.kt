package com.fanguskdev.carofarty.storage

import com.fanguskdev.carofarty.game.Scenario
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
