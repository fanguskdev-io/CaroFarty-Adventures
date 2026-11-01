package com.fanguskdev.carofarty

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * The app for iOS: hosts the Compose app. Its name is the one the Swift side of the template calls
 * (`MainViewControllerKt.MainViewController()`), hence the capital letter.
 */
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
