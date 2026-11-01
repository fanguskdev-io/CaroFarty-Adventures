package com.fanguskdev.carofarty.storage

import com.fanguskdev.carofarty.game.Scenario

/** What she chose last time, so the game opens where she left it (UI-20, ADR 0006). */
class Choices(private val store: KeyValueStore) {
    var scenario: Scenario
        get() = Scenario.entries.getOrElse(store.getInt(SCENARIO, 0)) { Scenario.Bosque }
        set(value) = store.putInt(SCENARIO, value.ordinal)

    private companion object {
        const val SCENARIO = "chosen_scenario"
    }
}
