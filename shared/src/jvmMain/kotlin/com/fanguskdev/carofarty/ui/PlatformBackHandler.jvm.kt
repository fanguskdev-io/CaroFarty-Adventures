package com.fanguskdev.carofarty.ui

import androidx.compose.runtime.Composable

/** The desktop window is a development tool: it has no back button. */
@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
