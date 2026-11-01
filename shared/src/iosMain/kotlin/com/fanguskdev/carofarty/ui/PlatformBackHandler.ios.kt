package com.fanguskdev.carofarty.ui

import androidx.compose.runtime.Composable

/** An iPhone has no back button: the pause comes from the button on screen or from leaving the app. */
@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
