package com.fanguskdev.carofarty.engine

import kotlin.math.max
import kotlin.math.min

/**
 * How the game's pixels fit a screen (ADR 0003): every game pixel is [scale] × [scale] pixels of the phone, with [scale]
 * the largest integer that still shows the minimum area, and the visible area grows to fill the screen up to a maximum.
 * What does not fit evenly is split as a band on each side ([offsetX], [offsetY], in pixels of the phone).
 */
data class PixelViewport(
    val scale: Int,
    val width: Int,
    val height: Int,
    val offsetX: Int,
    val offsetY: Int,
) {
    companion object {
        const val MIN_WIDTH = 320
        const val MIN_HEIGHT = 180
        const val MAX_WIDTH = 448
        const val MAX_HEIGHT = 270

        /** The viewport for a screen of [screenWidth] × [screenHeight] pixels of the phone. */
        fun fit(screenWidth: Int, screenHeight: Int): PixelViewport {
            val scale = max(1, min(screenWidth / MIN_WIDTH, screenHeight / MIN_HEIGHT))
            val width = min(screenWidth / scale, MAX_WIDTH)
            val height = min(screenHeight / scale, MAX_HEIGHT)
            return PixelViewport(
                scale = scale,
                width = width,
                height = height,
                offsetX = (screenWidth - width * scale) / 2,
                offsetY = (screenHeight - height * scale) / 2,
            )
        }
    }
}
