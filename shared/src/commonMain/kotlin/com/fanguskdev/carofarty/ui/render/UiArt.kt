package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.fanguskdev.carofarty.ui.render.PixelFont.drawText
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The art of the interface (UI-02), made by GameMaster for Fangusk's menus: the pieces of the panels, buttons, tabs,
 * frames, cards and ribbon, most of them in 9-slice; the colours of the text; the icons, the digits, the medals, the
 * page dots and the meter; and the title.
 */
internal class UiArt(
    val pieces: Map<String, UiPiece>,
    val colors: UiColors,
    val icons: SpriteSet?,
    val digits: SpriteSet?,
    val medals: SpriteSet?,
    val dots: SpriteSet?,
    val meter: SpriteSet?,
    val title: ImageBitmap?,
) {
    fun piece(name: String): UiPiece? = pieces[name]
}

/**
 * A piece of the interface: its image and, for those that stretch, its 9-slice: how many columns and rows of each side
 * ([slices], left, top, right, bottom) stay as they are while the middle repeats; [content], what to leave on each side
 * for what goes inside; [icon], where the icon goes in a small button.
 */
internal class UiPiece(
    val image: ImageBitmap,
    slices: List<Int>?,
    content: List<Int>?,
    icon: List<Int>?,
) {
    val slices: IntArray? = slices?.takeIf { it.size == SIDES }?.toIntArray()
    val content: IntArray =
        content?.takeIf { it.size == SIDES }?.toIntArray() ?: slices?.takeIf { it.size == SIDES }?.toIntArray() ?: IntArray(SIDES)
    val icon: IntArray = icon?.takeIf { it.size == 2 }?.toIntArray() ?: IntArray(2)
    val width: Int get() = image.width
    val height: Int get() = image.height

    /** The box inside [box] for what goes in it: text, a scene. */
    fun inside(box: Box): Box = Box(
        box.left + content[0],
        box.top + content[1],
        box.width - content[0] - content[2],
        box.height - content[1] - content[3],
    )

    private companion object {
        const val SIDES = 4
    }
}

/** The colours of the text on the interface: the text, its shadow, the headings, the warning and the records. */
internal class UiColors(
    val text: Color,
    val shadow: Color,
    val secondary: Color,
    val warning: Color,
    val record: Color,
) {
    companion object {
        /** From the colours of ui.json, as #rrggbb; one that is missing or wrong keeps the grey prototype's. */
        fun of(text: Map<String, String>): UiColors = UiColors(
            text = color(text["color"]) ?: GreyPalette.text,
            shadow = color(text["sombra"]) ?: GreyPalette.shadow,
            secondary = color(text["secundario"]) ?: GreyPalette.text,
            warning = color(text["aviso"]) ?: GreyPalette.bad,
            record = color(text["record"]) ?: GreyPalette.record,
        )

        private fun color(hex: String?): Color? {
            val digits = hex?.removePrefix("#")?.takeIf { it.length == HEX_DIGITS } ?: return null
            val value = digits.toLongOrNull(HEX) ?: return null
            return Color(OPAQUE or value)
        }

        private const val HEX_DIGITS = 6
        private const val HEX = 16
        private const val OPAQUE = 0xFF000000
    }
}

/**
 * [piece] drawn over [box]: its corners as they are, its sides and its middle repeated to fill the rest, never scaled,
 * so the pixel art stays sharp. A piece without 9-slice is drawn as it is at the box's top left.
 */
internal fun DrawScope.drawPiece(piece: UiPiece, box: Box) {
    val slices = piece.slices
    if (slices == null) {
        drawPart(piece.image, 0, 0, piece.width, piece.height, box.left, box.top)
        return
    }
    val (left, top, right, bottom) = slices.let { listOf(it[0], it[1], it[2], it[3]) }
    val middleWidth = piece.width - left - right
    val middleHeight = piece.height - top - bottom
    val innerWidth = box.width - left - right
    val innerHeight = box.height - top - bottom
    // Corners.
    drawPart(piece.image, 0, 0, left, top, box.left, box.top)
    drawPart(piece.image, piece.width - right, 0, right, top, box.right - right, box.top)
    drawPart(piece.image, 0, piece.height - bottom, left, bottom, box.left, box.bottom - bottom)
    drawPart(piece.image, piece.width - right, piece.height - bottom, right, bottom, box.right - right, box.bottom - bottom)
    // Sides, repeated.
    tile(innerWidth, middleWidth) { offset, across ->
        drawPart(piece.image, left, 0, across, top, box.left + left + offset, box.top)
        drawPart(piece.image, left, piece.height - bottom, across, bottom, box.left + left + offset, box.bottom - bottom)
    }
    tile(innerHeight, middleHeight) { offset, tall ->
        drawPart(piece.image, 0, top, left, tall, box.left, box.top + top + offset)
        drawPart(piece.image, piece.width - right, top, right, tall, box.right - right, box.top + top + offset)
    }
    // The middle, repeated both ways.
    tile(innerHeight, middleHeight) { down, tall ->
        tile(innerWidth, middleWidth) { across, wide ->
            drawPart(piece.image, left, top, wide, tall, box.left + left + across, box.top + top + down)
        }
    }
}

/** Calls [part] for each repetition of a [unit] long over [length]: its offset and how long it is (the last, cut). */
private inline fun tile(length: Int, unit: Int, part: (offset: Int, size: Int) -> Unit) {
    if (unit <= 0) return
    var offset = 0
    while (offset < length) {
        part(offset, min(unit, length - offset))
        offset += unit
    }
}

/** A part of [image], from ([sourceX], [sourceY]) and [width] × [height], at ([x], [y]) as it is. */
private fun DrawScope.drawPart(
    image: ImageBitmap,
    sourceX: Int,
    sourceY: Int,
    width: Int,
    height: Int,
    x: Int,
    y: Int,
) {
    if (width <= 0 || height <= 0) return
    drawImage(
        image,
        IntOffset(sourceX, sourceY),
        IntSize(width, height),
        IntOffset(x, y),
        IntSize(width, height),
        filterQuality = FilterQuality.None,
    )
}

/** [text] centred in [box], in [color] with the shadow of the interface. */
internal fun DrawScope.drawCentred(
    text: String,
    box: Box,
    color: Color,
    shadow: Color,
    size: Int = 1,
) {
    val x = box.left + (box.width - PixelFont.width(text, size)) / 2
    val y = box.top + (box.height - PixelFont.HEIGHT * size) / 2
    drawText(text, x, y, color, shadow, size)
}

/** A sprite of a sheet of the interface, by the name of its animation, its first frame at ([x], [y]). */
internal fun DrawScope.drawUiSprite(
    set: SpriteSet,
    name: String,
    x: Int,
    y: Int,
    millis: Long = 0L,
): Boolean {
    val animation = set.animation(name) ?: return false
    drawFrame(set.sheet, animation.frameAt(millis, loops = true), x, y)
    return true
}

/**
 * [text] with the digits of the interface, gold to add and violet to take away ([adding]), its top left at ([x], [y]);
 * a character it does not have is skipped. Returns how wide it was.
 */
internal fun DrawScope.drawDigits(
    digits: SpriteSet,
    text: String,
    adding: Boolean,
    x: Int,
    y: Int,
): Int {
    val animation = digits.animation(if (adding) "suma" else "resta") ?: return 0
    var left = x
    for (char in text) {
        val index = digits.characters.indexOf(if (char == '-') '−' else char)
        if (index < 0) continue
        drawFrame(digits.sheet, animation.frame(index), left, y)
        left += digits.advance
    }
    return left - x
}

/** How wide [text] is with the digits of the interface. */
internal fun digitsWidth(digits: SpriteSet, text: String): Int = text.length * digits.advance

/** The bottom [share] of a sprite of a sheet of the interface, from 0 to 1, at ([x], [y]): the meter of the hint, filling. */
internal fun DrawScope.drawUiSpriteBottom(
    set: SpriteSet,
    name: String,
    x: Int,
    y: Int,
    share: Float,
) {
    val animation = set.animation(name) ?: return
    val sheet = set.sheet
    val rows = (sheet.height * share.coerceIn(0f, 1f)).roundToInt()
    if (rows <= 0) return
    val frame = animation.frame(0)
    val sourceY = (frame / sheet.columns) * sheet.height + sheet.height - rows
    drawPart(sheet.image, (frame % sheet.columns) * sheet.width, sourceY, sheet.width, rows, x, y + sheet.height - rows)
}

/**
 * The day sky of the menus with its clouds, as Fangusk chose: three bands of blue, lighter downwards, and the clouds of
 * the interface here and there, in the same places of the screen whatever its size (diseno/ui/maquetas.py). With
 * [driftMillis], the clouds have drifted that long to the left, slowly, the small ones slower, as far away; one that goes
 * out on the left comes back on the right.
 */
internal fun DrawScope.paintMenuSky(
    width: Int,
    height: Int,
    ui: UiArt,
    driftMillis: Long = 0L,
) {
    val first = skyBandEnd(height, FIRST_BAND)
    val second = skyBandEnd(height, SECOND_BAND)
    drawRect(LIGHT_BLUE, Offset.Zero, Size(width.toFloat(), first.toFloat()))
    drawRect(CLOUD_SHADOW, Offset(0f, first.toFloat()), Size(width.toFloat(), (second - first).toFloat()))
    drawRect(CLEAR_SKY, Offset(0f, second.toFloat()), Size(width.toFloat(), (height - second).toFloat()))
    val big = ui.piece("nube-grande")
    val small = ui.piece("nube-pequena")
    for (i in CLOUDS.indices step 3) {
        val isBig = CLOUDS[i] == 1
        val piece = (if (isBig) big else small) ?: continue
        val drift = driftMillis * (if (isBig) BIG_CLOUD_SPEED else SMALL_CLOUD_SPEED) / MILLIS
        val x = (width * CLOUDS[i + 1] / PERCENT - drift + piece.width).mod(width + piece.width) - piece.width
        drawPiece(piece, Box(x, height * CLOUDS[i + 2] / PERCENT, piece.width, piece.height))
    }
}

/** Where a band of the sky of the menus that goes down to [percent] of a screen [height] pixels tall ends. */
private fun skyBandEnd(height: Int, percent: Int): Int = (height * percent + PERCENT - 1) / PERCENT

/** Where the second band of the sky of the menus ends: the title screen puts its scenario under it, with no third band. */
internal fun menuSkySecondBandEnd(height: Int): Int = skyBandEnd(height, SECOND_BAND)

/** The colours of the sky of the menus: «azul luz», «nube sombra» and «cielo claro» of the interface's palette. */
private val LIGHT_BLUE = Color(0xFFA6CDFF)
private val CLOUD_SHADOW = Color(0xFFBDD3F2)
private val CLEAR_SKY = Color(0xFFD6EBFF)

private const val PERCENT = 100
private const val FIRST_BAND = 34
private const val SECOND_BAND = 52

/** How fast the clouds drift on the title screen, in pixels a second: much slower than anything of the scenario. */
private const val BIG_CLOUD_SPEED = 6L
private const val SMALL_CLOUD_SPEED = 4L
private const val MILLIS = 1000L

/** The clouds of the menus: big (1) or small (0), and their top left across and down, in hundredths of the screen. */
private val CLOUDS = intArrayOf(
    1, 3, 5,
    0, 30, 17,
    1, 80, 10,
    0, 6, 70,
    1, 86, 80,
    0, 56, 90,
)
