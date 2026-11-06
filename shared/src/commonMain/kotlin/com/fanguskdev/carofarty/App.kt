package com.fanguskdev.carofarty

import androidx.compose.runtime.Composable
import com.fanguskdev.carofarty.audio.AudioFocus
import com.fanguskdev.carofarty.storage.KeyValueStore
import com.fanguskdev.carofarty.storage.MemoryStore
import com.fanguskdev.carofarty.ui.GameScreen

/**
 * The whole game, as each platform shows it. [store] is where each phone keeps the records; [debug] opens the debugging
 * tools (TEC-10), which only debug builds have: a tap with a second finger shows them, and [debugAtStart] shows them from
 * the start (a launch extra of the debug builds, for the emulator and the simulator, which cannot tap with two fingers).
 * [autopilot], another launch extra of the debug builds, lets the pilot of the tests play by itself: to watch runs where
 * nobody can tap and to measure long ones on a phone (MOB-02). [sound] plays the music and the sounds: the phones and the
 * desktop do, the tests do not. [quit] closes the game, from the button of the start screen (Fangusk): Android and the
 * desktop can, iOS cannot (Apple does not let an app close itself), so there it is null and the button does not show.
 * [focus] shares the sound with the rest of the phone, where there is a way (Android's audio focus). The app that
 * measures has its [measure] log, and debug builds and it may give the seed of the first run ([firstSeed]), for the
 * benchmark against CaroFarty G (GOD-27).
 */
@Composable
fun App(
    store: KeyValueStore = MemoryStore(),
    debug: Boolean = false,
    debugAtStart: Boolean = false,
    autopilot: Boolean = false,
    sound: Boolean = false,
    quit: (() -> Unit)? = null,
    focus: AudioFocus? = null,
    measure: MeasureLog? = null,
    firstSeed: Long? = null,
) {
    GameScreen(store, debug, debugAtStart, autopilot, sound, quit, focus, measure, firstSeed)
}
