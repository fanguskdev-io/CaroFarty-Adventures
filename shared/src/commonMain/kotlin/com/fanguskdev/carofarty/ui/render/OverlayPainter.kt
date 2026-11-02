package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.fanguskdev.carofarty.game.CaroState
import com.fanguskdev.carofarty.ui.render.PixelFont.drawText

/** The insets of the screen (notch, rounded corners), in game pixels, that the marker keeps clear of (UI-04). */
internal data class SafeInsets(val left: Int = 0, val top: Int = 0, val right: Int = 0)

/**
 * Caro as a placeholder of the grey prototype, 16 × 24 pixels facing right, with her feet's centre at ([x], [feet]) in
 * game pixels from the top left of the screen. Her real sprite comes with the references in M2.
 */
internal fun DrawScope.paintCaro(
    x: Int,
    feet: Int,
    state: CaroState,
    stride: Boolean,
) {
    val frame =
        when {
            state == CaroState.Jumping || state == CaroState.Falling || state == CaroState.Fallen -> CARO_JUMP
            state == CaroState.Waiting || state == CaroState.Stopped -> CARO_STAND
            stride -> CARO_RUN_A
            else -> CARO_RUN_B
        }
    frame.draw(this, x - CARO_WIDTH / 2, feet - PLACEHOLDER_HEIGHT, size = 1, ::caroColor)
}

/**
 * The marker (UI-04): [points] on the left, in red when they are running out ([warn]), and [distance] and the pause button on
 * the right, already as text.
 */
internal fun DrawScope.paintHud(
    points: String,
    distance: String,
    width: Int,
    insets: SafeInsets,
    warn: Boolean = false,
) {
    val top = MARGIN + insets.top
    drawText(points, MARGIN + insets.left, top, if (warn) GreyPalette.bad else GreyPalette.text, GreyPalette.shadow)
    val pauseLeft = width - insets.right - MARGIN - PAUSE_SIZE
    drawText(distance, pauseLeft - 6 - PixelFont.width(distance), top, GreyPalette.text, GreyPalette.shadow)
    drawRect(GreyPalette.panel, Offset(pauseLeft.toFloat(), (top - 3).toFloat()), Size(PAUSE_SIZE.toFloat(), PAUSE_SIZE.toFloat()))
    drawRect(GreyPalette.text, Offset(pauseLeft + 4f, top.toFloat()), Size(2f, 7f))
    drawRect(GreyPalette.text, Offset(pauseLeft + 8f, top.toFloat()), Size(2f, 7f))
}

/** Whether the point ([x], [y]) in game pixels is on the pause button. */
internal fun pauseButtonHit(
    x: Float,
    y: Float,
    width: Int,
    insets: SafeInsets,
): Boolean {
    val left = width - insets.right - MARGIN - PAUSE_SIZE - PAUSE_SLACK
    val bottom = MARGIN + insets.top + PAUSE_SIZE + PAUSE_SLACK
    return x >= left && y <= bottom
}

/** A dark panel across the middle with [lines] centred, each a text, its colour and its size. */
internal fun DrawScope.paintPanel(lines: List<PanelLine>, width: Int, height: Int) {
    val tall = panelHeight(lines)
    val top = (height - tall) / 2
    drawRect(GreyPalette.panel, Offset(0f, top.toFloat()), Size(width.toFloat(), tall.toFloat()))
    var y = top + 8
    for (line in lines) {
        y += (PixelFont.LINE - PixelFont.HEIGHT) * line.size
        val x = (width - PixelFont.width(line.text, line.size)) / 2
        drawText(line.text, x, y, line.color, GreyPalette.shadow, line.size)
        y += PixelFont.HEIGHT * line.size + 2
    }
}

internal class PanelLine(val text: String, val color: Color = GreyPalette.text, val size: Int = 1)

private fun panelHeight(lines: List<PanelLine>): Int = lines.sumOf { PixelFont.LINE * it.size + 2 } + 12

/** The row of game pixels under the panel of [lines] that [paintPanel] draws on a screen [height] tall. */
internal fun panelBottom(lines: List<PanelLine>, height: Int): Int = (height - panelHeight(lines)) / 2 + panelHeight(lines)

/**
 * The top row of the choice of scenario over the panel of [lines] that [paintPanel] draws on a screen [height] tall: in
 * the sky, clear of Caro and of what is on the ground.
 */
internal fun chooserTop(lines: List<PanelLine>, height: Int): Int =
    (height - panelHeight(lines)) / 2 - CHOOSER_GAP - PixelFont.HEIGHT - 2 * BUTTON_PAD_Y

/**
 * The choice of scenario (UI-20, ADR 0006): its [names] in a row of buttons from [top], centred, the [chosen] one lit. The
 * world behind already shows the chosen one.
 */
internal fun DrawScope.paintChooser(
    names: List<String>,
    chosen: Int,
    width: Int,
    top: Int,
) {
    var left = chooserLeft(names, width)
    val tall = PixelFont.HEIGHT + 2 * BUTTON_PAD_Y
    for (i in names.indices) {
        val across = PixelFont.width(names[i]) + 2 * BUTTON_PAD_X
        val lit = i == chosen
        if (lit) drawRect(GreyPalette.record, Offset(left - 1f, top - 1f), Size(across + 2f, tall + 2f))
        drawRect(GreyPalette.panel, Offset(left.toFloat(), top.toFloat()), Size(across.toFloat(), tall.toFloat()))
        drawText(names[i], left + BUTTON_PAD_X, top + BUTTON_PAD_Y, if (lit) GreyPalette.record else GreyPalette.text, GreyPalette.shadow)
        left += across + BUTTON_GAP
    }
}

/** Which of the buttons of [paintChooser] is at ([x], [y]), with some slack around them, or -1. */
internal fun chooserHit(
    names: List<String>,
    x: Float,
    y: Float,
    width: Int,
    top: Int,
): Int {
    if (y < top - BUTTON_SLACK || y > top + PixelFont.HEIGHT + 2 * BUTTON_PAD_Y + BUTTON_SLACK) return -1
    var left = chooserLeft(names, width)
    for (i in names.indices) {
        val across = PixelFont.width(names[i]) + 2 * BUTTON_PAD_X
        if (x >= left - BUTTON_GAP / 2f && x < left + across + BUTTON_GAP / 2f) return i
        left += across + BUTTON_GAP
    }
    return -1
}

private fun chooserLeft(names: List<String>, width: Int): Int {
    val across = names.sumOf { PixelFont.width(it) + 2 * BUTTON_PAD_X } + BUTTON_GAP * (names.size - 1)
    return (width - across) / 2
}

/** One line of numbers for those who debug (TEC-10), at the bottom left. */
internal fun DrawScope.paintDebug(lines: Array<String>, height: Int, insets: SafeInsets) {
    var top = height - MARGIN - lines.size * (PixelFont.HEIGHT + LINE_GAP) + LINE_GAP
    for (line in lines) {
        drawText(line, MARGIN + insets.left, top, GreyPalette.debug, GreyPalette.ink)
        top += PixelFont.HEIGHT + LINE_GAP
    }
}

private fun caroColor(code: Char): Color? = when (code) {
    'h' -> HAIR
    's' -> SKIN
    'e' -> GreyPalette.ink
    'd' -> DRESS
    'b' -> SHOES
    else -> null
}

private const val MARGIN = 6
private const val BUTTON_PAD_X = 8
private const val BUTTON_PAD_Y = 5
private const val BUTTON_GAP = 8
private const val BUTTON_SLACK = 6
private const val CHOOSER_GAP = 6
private const val LINE_GAP = 3
private const val PAUSE_SIZE = 14
private const val PAUSE_SLACK = 8
private const val CARO_WIDTH = 16
private const val PLACEHOLDER_HEIGHT = 24

/** How tall Caro's drawing is (docs/arte.md): the camera keeps it on the screen (PLY-13). */
internal const val CARO_HEIGHT = 32
private val HAIR = Color(0xFF5A3825)
private val SKIN = Color(0xFFF2C9A0)
private val DRESS = Color(0xFFE8829B)
private val SHOES = Color(0xFF7A2E3B)

private val CARO_HEAD = listOf(
    "....hhhhhhh.....",
    "...hhhhhhhhh....",
    "..hhhhhhhhhhh...",
    "..hhhsssssshh...",
    "..hhsssssssss...",
    ".hhhssssesses...",
    ".hhhssssesses...",
    ".hh.sssssssss...",
    ".h...sssssss....",
    ".....dddddd.....",
    "....dddddddd....",
    "...dddddddddd...",
    "..sdddddddddds..",
    "..sdddddddddds..",
    "...dddddddddd...",
    "...dddddddddd...",
    "..dddddddddddd..",
    "..dddddddddddd..",
)
private val CARO_STAND = PixelRuns(
    CARO_HEAD + listOf(
        ".....ss..ss.....",
        ".....ss..ss.....",
        ".....ss..ss.....",
        ".....ss..ss.....",
        "....bbb..bbb....",
        "....bbb..bbb....",
    ),
)
private val CARO_RUN_A = PixelRuns(
    CARO_HEAD + listOf(
        "....ss....ss....",
        "....ss....ss....",
        "...ss......ss...",
        "...ss......ss...",
        "..bbb......bbb..",
        "..bbb.......bbb.",
    ),
)
private val CARO_RUN_B = PixelRuns(
    CARO_HEAD + listOf(
        ".....ss.ss......",
        ".....ss.ss......",
        ".....ss.ss......",
        ".....ss.ss......",
        "....bbbbbbb.....",
        "....bbbbbbb.....",
    ),
)
private val CARO_JUMP = PixelRuns(
    CARO_HEAD + listOf(
        ".....ssss.......",
        "....ss..ss......",
        "....bb..bb......",
        "................",
        "................",
        "................",
    ),
)
