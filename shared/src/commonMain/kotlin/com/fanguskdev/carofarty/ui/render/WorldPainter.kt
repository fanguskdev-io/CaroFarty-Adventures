package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.fanguskdev.carofarty.game.GameRun
import com.fanguskdev.carofarty.game.Item
import com.fanguskdev.carofarty.game.TILE
import com.fanguskdev.carofarty.ui.render.PixelFont.drawText
import kotlin.math.floor

/**
 * Draws the world of the grey prototype into the low resolution buffer, in game pixels: the sky, a far band that moves
 * slower to show the speed, the ground in tiles with its edges marked and the collectibles as squares with their letter.
 * [cameraLeft] is the integer world x of the buffer's left column and [cameraBottom] the world y of its bottom row; the
 * buffer's y grows downwards from its top, the world's upwards from the bottom.
 */
internal fun DrawScope.paintWorld(
    run: GameRun,
    cameraLeft: Long,
    cameraBottom: Int,
    width: Int,
    height: Int,
    palette: GreyPalette,
    debugBoxes: Boolean,
    caroX: Double,
    caroY: Float,
) {
    // The buffer row of the world's y = 0, which goes down out of the buffer when the camera goes up (PLY-13).
    val base = height + cameraBottom
    drawRect(palette.sky, Offset.Zero, Size(width.toFloat(), height.toFloat()))
    paintFarBand(cameraLeft, height + floor(cameraBottom * FAR_SPEED).toInt(), width, palette)
    paintGrounds(run, cameraLeft, width, base, palette)
    paintItems(run, cameraLeft, width, base)
    if (debugBoxes) paintBoxes(run, cameraLeft, base, caroX, caroY)
}

private fun DrawScope.paintFarBand(
    cameraLeft: Long,
    base: Int,
    width: Int,
    palette: GreyPalette,
) {
    val far = floor(cameraLeft * FAR_SPEED).toLong()
    var column = far / FAR_COLUMN - 1
    while ((column * FAR_COLUMN - far) < width) {
        val left = (column * FAR_COLUMN - far).toFloat()
        val tall = FAR_MIN + (hash(column) % FAR_RANGE).toInt()
        drawRect(palette.far, Offset(left, (base - tall).toFloat()), Size(FAR_COLUMN.toFloat(), tall.toFloat()))
        column++
    }
}

private fun DrawScope.paintGrounds(
    run: GameRun,
    cameraLeft: Long,
    width: Int,
    base: Int,
    palette: GreyPalette,
) {
    val grounds = run.course.grounds
    for (i in 0 until grounds.size) {
        val ground = grounds[i]
        val left = (ground.start - cameraLeft).coerceAtLeast(-1L)
        val right = (ground.end - cameraLeft).coerceAtMost(width + 1L)
        if (right <= 0 || left >= width + 1) continue
        val top = (base - ground.top).toFloat()
        drawRect(palette.groundBody, Offset(left.toFloat(), top), Size((right - left).toFloat(), ground.top.toFloat()))
        // The tiles, so the ground can be seen to move.
        var tile = ((ground.start + TILE - 1) / TILE) * TILE
        while (tile < ground.end) {
            val x = tile - cameraLeft
            if (x in left..right) drawRect(palette.groundLine, Offset(x.toFloat(), top + TOP_STRIPE), Size(1f, ground.top.toFloat()))
            tile += TILE
        }
        var row = TILE
        while (row < ground.top) {
            drawRect(palette.groundLine, Offset(left.toFloat(), top + row), Size((right - left).toFloat(), 1f))
            row += TILE
        }
        drawRect(palette.groundTop, Offset(left.toFloat(), top), Size((right - left).toFloat(), TOP_STRIPE))
        // The edges of a gap, dark, so they read at any speed (ENM-11).
        if (ground.start - cameraLeft == left) drawRect(GreyPalette.ink, Offset(left.toFloat(), top), Size(1f, ground.top.toFloat()))
        if (ground.end - cameraLeft == right) drawRect(GreyPalette.ink, Offset((right - 1).toFloat(), top), Size(1f, ground.top.toFloat()))
    }
}

private fun DrawScope.paintItems(
    run: GameRun,
    cameraLeft: Long,
    width: Int,
    base: Int,
) {
    val items = run.course.items
    val half = Item.SIZE / 2
    for (i in 0 until items.size) {
        val item = items[i]
        if (item.taken) continue
        val left = item.x - half - cameraLeft
        if (left > width || left + Item.SIZE < 0) continue
        val top = (base - item.y - half).toFloat()
        val outline = if (item.kind.adds) GreyPalette.good else GreyPalette.bad
        drawRect(outline, Offset(left.toFloat(), top), Size(Item.SIZE.toFloat(), Item.SIZE.toFloat()))
        drawRect(GreyPalette.itemFill(item.kind), Offset(left + 1f, top + 1), Size(Item.SIZE - 2f, Item.SIZE - 2f))
        drawText(GreyPalette.itemLetter(item.kind), (left + 4).toInt(), (top + 3).toInt(), GreyPalette.ink)
    }
}

private fun DrawScope.paintBoxes(
    run: GameRun,
    cameraLeft: Long,
    base: Int,
    caroX: Double,
    caroY: Float,
) {
    val movement = run.config.movement
    val left = (caroX - movement.hitboxWidth / 2 - cameraLeft).toFloat()
    val top = base - caroY - movement.hitboxHeight
    drawRect(GreyPalette.debug, Offset(left, top), Size(movement.hitboxWidth, movement.hitboxHeight), style = BOX)
    val items = run.course.items
    for (i in 0 until items.size) {
        val item = items[i]
        if (item.taken) continue
        val x = (item.x - Item.SIZE / 2 - cameraLeft).toFloat()
        drawRect(
            GreyPalette.debug,
            Offset(x, (base - item.y - Item.SIZE / 2).toFloat()),
            Size(Item.SIZE.toFloat(), Item.SIZE.toFloat()),
            style = BOX,
        )
    }
}

/** A fixed pseudo-random number for a column of the far band, so it looks the same every time it comes round. */
private fun hash(n: Long): Long {
    var x = n * 0x9E3779B97F4A7C15uL.toLong()
    x = x xor (x ushr 31)
    x *= 0xBF58476D1CE4E5B9uL.toLong()
    return (x xor (x ushr 29)) and Long.MAX_VALUE
}

private val BOX = Stroke(width = 1f)
private const val TOP_STRIPE = 3f
private const val FAR_SPEED = 0.3
private const val FAR_COLUMN = 24L
private const val FAR_MIN = 50
private const val FAR_RANGE = 50L
