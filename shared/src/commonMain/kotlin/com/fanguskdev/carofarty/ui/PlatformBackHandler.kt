package com.fanguskdev.carofarty.ui

import androidx.compose.runtime.Composable

/** The back button and gesture of the platform (MOB-05): while [enabled], they call [onBack] instead of leaving the app. */
@Composable
internal expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
