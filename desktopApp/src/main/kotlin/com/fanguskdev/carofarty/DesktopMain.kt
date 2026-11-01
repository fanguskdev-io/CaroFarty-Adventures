package com.fanguskdev.carofarty

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.fanguskdev.carofarty.storage.DesktopKeyValueStore

/** The desktop window, a development tool: the game at 1280 × 720, with the debugging tools; `--autopilot` lets the pilot play. */
fun main(args: Array<String>) = application {
    val store = DesktopKeyValueStore()
    Window(
        onCloseRequest = ::exitApplication,
        title = "CaroFarty",
        state = rememberWindowState(width = 1280.dp, height = 720.dp),
    ) {
        App(store, debug = true, autopilot = "--autopilot" in args)
    }
}
