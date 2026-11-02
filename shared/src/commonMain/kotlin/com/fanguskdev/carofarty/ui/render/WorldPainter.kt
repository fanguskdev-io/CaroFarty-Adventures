package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.fanguskdev.carofarty.engine.PixelViewport
import com.fanguskdev.carofarty.game.GameRun
import com.fanguskdev.carofarty.game.Item
import com.fanguskdev.carofarty.game.TILE
import com.fanguskdev.carofarty.ui.render.PixelFont.drawText
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Draws the world into the low resolution buffer, in game pixels: the ground, the blocks and the collectibles, with the
 * scenario's art (ArtPainter) or, where there is none in [art], as the grey prototype: the ground in tiles with its edges
 * marked and the collectibles as squares with their letter. [cameraLeft] is the integer world x of the buffer's left
 * column and [cameraBottom] the world y of its bottom row; the buffer's y grows downwards from its top, the world's upwards
 * from the bottom. [millis] moves the animations.
 */
internal fun DrawScope.paintWorld(
    run: GameRun,
    cameraLeft: Long,
    cameraBottom: Int,
    width: Int,
    height: Int,
    palette: GreyPalette,
    art: Art?,
    millis: Long,
    debugBoxes: Boolean,
    caroX: Double,
    caroY: Float,
) {
    // The buffer row of the world's y = 0, which goes down out of the buffer when the camera goes up (PLY-13).
    val base = height + cameraBottom
    // Clear: the sky and the background are drawn behind it, each with its own offset (paintBackground).
    drawRect(Color.Transparent, Offset.Zero, Size(width.toFloat(), height.toFloat()), blendMode = BlendMode.Clear)
    val scenery = art?.scenario(run.scenario)
    if (scenery != null) {
        paintGroundTiles(run, scenery, cameraLeft, cameraBottom, width, base, millis)
        paintBlockTiles(run, scenery, cameraLeft, width, base)
    } else {
        paintGrounds(run, cameraLeft, width, base, palette)
        paintBlocks(run, cameraLeft, width, base, palette)
    }
    val items = art?.items
    if (items != null) paintItemSprites(run, items, cameraLeft, width, base, millis) else paintItems(run, cameraLeft, width, base)
    if (debugBoxes) paintBoxes(run, cameraLeft, base, caroX, caroY)
}

/**
 * The sky and the far band, drawn straight on the screen behind the world (TEC-04): the scenario's art if there is
 * ([scenery], paintScenery), or else the grey ones. The band moves at [FAR_SPEED] of the [camera] with its own offset
 * rounded to a pixel of the phone, so it glides at its own pace instead of stepping a whole game pixel at a time and
 * taking the world's sub-pixel shift on top, which made it shake. At the start of a run, with the camera at
 * [restCamera], it sits on the game's pixel grid.
 */
internal fun DrawScope.paintBackground(
    camera: Double,
    restCamera: Double,
    cameraBottom: Int,
    viewport: PixelViewport,
    palette: GreyPalette,
    scenery: ScenarioArt?,
    groundLine: Int,
    millis: Long,
    plan: BackgroundPlan?,
) {
    val scale = viewport.scale
    val left = viewport.offsetX.toFloat()
    val top = viewport.offsetY.toFloat()
    drawRect(palette.sky, Offset(left, top), Size((viewport.width * scale).toFloat(), (viewport.height * scale).toFloat()))
    if (scenery != null) {
        paintScenery(scenery, camera, restCamera, cameraBottom, viewport, groundLine, millis, plan)
        return
    }
    val far = (camera - restCamera) * FAR_SPEED + floor(restCamera * FAR_SPEED)
    val farPixels = (far * scale).roundToLong()
    val base = viewport.height + floor(cameraBottom * FAR_SPEED).toInt()
    var column = floor(far / FAR_COLUMN).toLong() - 1
    while (column * FAR_COLUMN * scale - farPixels < viewport.width * scale) {
        val x = left + (column * FAR_COLUMN * scale - farPixels)
        val tall = FAR_MIN + (hash(column) % FAR_RANGE).toInt()
        drawRect(
            palette.far,
            Offset(x, top + (base - tall) * scale),
            Size((FAR_COLUMN * scale).toFloat(), (tall * scale).toFloat()),
        )
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

/** The solid blocks in the air (ADR 0007), outlined and split in tiles so a row of them reads as blocks. */
private fun DrawScope.paintBlocks(
    run: GameRun,
    cameraLeft: Long,
    width: Int,
    base: Int,
    palette: GreyPalette,
) {
    val blocks = run.course.blocks
    for (i in 0 until blocks.size) {
        val block = blocks[i]
        val left = block.start - cameraLeft
        if (left > width || block.end - cameraLeft < 0) continue
        val top = (base - block.top).toFloat()
        val across = (block.end - block.start).toFloat()
        val tall = (block.top - block.bottom).toFloat()
        drawRect(GreyPalette.ink, Offset(left.toFloat(), top), Size(across, tall))
        drawRect(palette.groundTop, Offset(left + 1f, top + 1f), Size(across - 2f, tall - 2f))
        var x = block.start + TILE
        while (x < block.end) {
            drawRect(GreyPalette.ink, Offset((x - cameraLeft).toFloat(), top), Size(1f, tall))
            x += TILE
        }
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
        // Where it is now: the perritos walk (ADR 0008).
        val x = item.currentX.roundToLong() - cameraLeft
        if (x - half > width || x + half < 0) continue
        paintGreyItem(item, x.toInt(), base - item.y)
    }
}

/** A collectible of the grey prototype: a square with its letter, centred on ([x], [y]) of the buffer. */
internal fun DrawScope.paintGreyItem(item: Item, x: Int, y: Int) {
    val left = (x - Item.SIZE / 2).toFloat()
    val top = (y - Item.SIZE / 2).toFloat()
    val outline = if (item.kind.adds) GreyPalette.good else GreyPalette.bad
    drawRect(outline, Offset(left, top), Size(Item.SIZE.toFloat(), Item.SIZE.toFloat()))
    drawRect(GreyPalette.itemFill(item), Offset(left + 1f, top + 1), Size(Item.SIZE - 2f, Item.SIZE - 2f))
    drawText(GreyPalette.itemLetter(item.kind), (left + 4).toInt(), (top + 3).toInt(), GreyPalette.itemInk(item))
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
    // The collision boxes, not the drawings: generous for what adds points, strict for what takes them away (CHR-08).
    val items = run.course.items
    for (i in 0 until items.size) {
        val item = items[i]
        if (item.taken) continue
        val box = run.config.items.box(item.kind)
        val x = (item.currentX.roundToLong() - box / 2 - cameraLeft).toFloat()
        drawRect(GreyPalette.debug, Offset(x, (base - item.y - box / 2).toFloat()), Size(box.toFloat(), box.toFloat()), style = BOX)
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
