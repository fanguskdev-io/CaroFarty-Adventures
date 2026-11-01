package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * The low resolution buffer the world is drawn into (ADR 0003, TEC-04): one pixel of it is one game pixel, and it is
 * scaled to the screen by an integer without smoothing. It is made again only when the size changes, so a frame
 * allocates nothing.
 */
internal class PixelBuffer {
    var image: ImageBitmap? = null
        private set
    private var canvas: Canvas? = null
    private val drawScope = CanvasDrawScope()
    private val density = Density(1f)

    fun paint(width: Int, height: Int, block: DrawScope.() -> Unit): ImageBitmap {
        val current = image
        val bitmap =
            if (current != null && current.width == width && current.height == height) {
                current
            } else {
                ImageBitmap(width, height).also {
                    image = it
                    canvas = Canvas(it)
                }
            }
        val target = canvas ?: Canvas(bitmap).also { canvas = it }
        drawScope.draw(density, LayoutDirection.Ltr, target, Size(width.toFloat(), height.toFloat()), block)
        return bitmap
    }
}
