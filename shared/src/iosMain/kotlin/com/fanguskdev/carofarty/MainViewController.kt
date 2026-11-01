package com.fanguskdev.carofarty

import androidx.compose.ui.window.ComposeUIViewController
import com.fanguskdev.carofarty.storage.IosKeyValueStore
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi

/**
 * The app for iOS (MOB-04): hosts the Compose app. Its name is the one the Swift side of the template calls
 * (`MainViewControllerKt.MainViewController()`), hence the capital letter.
 */
@OptIn(ExperimentalNativeApi::class)
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController {
    val store = IosKeyValueStore()
    val debug = Platform.isDebugBinary
    // `xcrun simctl launch <simulador> com.fanguskdev.carofarty.CaroFarty -debug` opens the debugging tools at once, and
    // `-autopilot` lets the pilot play.
    val arguments = NSProcessInfo.processInfo.arguments
    val debugAtStart = debug && arguments.contains("-debug")
    val autopilot = debug && arguments.contains("-autopilot")
    return ComposeUIViewController { App(store, debug, debugAtStart, autopilot) }
}
