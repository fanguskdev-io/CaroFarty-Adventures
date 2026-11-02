package com.fanguskdev.carofarty.storage

import com.fanguskdev.carofarty.game.Scenario

/**
 * What she chose and what she has seen, so the game opens as she left it: the last scenario (UI-20, ADR 0006), whether the
 * sound is on (on the first time, and as she leaves it after), whether she has seen how to play, and how many runs she has
 * played, for the hint to hold the finger in the first ones.
 */
class Choices(private val store: KeyValueStore) {
    var scenario: Scenario
        get() = Scenario.entries.getOrElse(store.getInt(SCENARIO, 0)) { Scenario.Bosque }
        set(value) = store.putInt(SCENARIO, value.ordinal)

    var soundOn: Boolean
        get() = store.getBoolean(SOUND, true)
        set(value) = store.putBoolean(SOUND, value)

    var tutorialSeen: Boolean
        get() = store.getBoolean(TUTORIAL, false)
        set(value) = store.putBoolean(TUTORIAL, value)

    var runsPlayed: Int
        get() = store.getInt(RUNS, 0)
        set(value) = store.putInt(RUNS, value)

    private companion object {
        const val SCENARIO = "chosen_scenario"
        const val SOUND = "sound_on"
        const val TUTORIAL = "tutorial_seen"
        const val RUNS = "runs_played"
    }
}
