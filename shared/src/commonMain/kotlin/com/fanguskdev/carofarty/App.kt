package com.fanguskdev.carofarty

import androidx.compose.runtime.Composable
import com.fanguskdev.carofarty.storage.KeyValueStore
import com.fanguskdev.carofarty.storage.MemoryStore
import com.fanguskdev.carofarty.ui.GameScreen

/**
 * The whole game, as each platform shows it. [store] is where each phone keeps the records; [debug] opens the debugging
 * tools (TEC-10), which only debug builds have: a tap with a second finger shows them, and [debugAtStart] shows them from
 * the start (a launch extra of the debug builds, for the emulator and the simulator, which cannot tap with two fingers).
 * [autopilot], another launch extra of the debug builds, lets the pilot of the tests play by itself: to watch runs where
 * nobody can tap and to measure long ones on a phone (MOB-02).
 */
@Composable
fun App(
    store: KeyValueStore = MemoryStore(),
    debug: Boolean = false,
    debugAtStart: Boolean = false,
    autopilot: Boolean = false,
) {
    GameScreen(store, debug, debugAtStart, autopilot)
}
