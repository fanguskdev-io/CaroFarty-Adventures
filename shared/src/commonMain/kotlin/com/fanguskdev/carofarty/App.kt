package com.fanguskdev.carofarty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The whole game, as each platform shows it. Until the grey prototype arrives (M1) it is only the sky the game will be
 * drawn on.
 */
@Composable
fun App() {
    Box(Modifier.fillMaxSize().background(Color(0xFF78B8F8)))
}
