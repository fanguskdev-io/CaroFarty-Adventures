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
 * The marker (UI-04): [points] on the left, in red when they are running out ([warn]), and on the right [distance], the
 * sound and the pause buttons of [layout] ([soundOn] says which icon).
 */
internal fun DrawScope.paintHud(
    points: String,
    distance: String,
    layout: MenuLayout,
    soundOn: Boolean,
    warn: Boolean = false,
) {
    val top = layout.corner.top + (layout.corner.height - PixelFont.HEIGHT) / 2
    drawText(points, MARGIN + layout.insets.left, top, if (warn) GreyPalette.bad else GreyPalette.text, GreyPalette.shadow)
    drawText(distance, layout.hudSound.left - MARGIN - PixelFont.width(distance), top, GreyPalette.text, GreyPalette.shadow)
    paintIconButton(layout.hudSound, if (soundOn) Icon.SoundOn else Icon.SoundOff)
    paintIconButton(layout.corner, Icon.Pause)
}

/** The hint to hold the finger in the first gaps of her first runs (GDD-17, UI-12), under the marker. */
internal fun DrawScope.paintHint(text: String, layout: MenuLayout) {
    val y = layout.corner.bottom + MARGIN
    drawText(text, (layout.width - PixelFont.width(text)) / 2, y, GreyPalette.record, GreyPalette.shadow)
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
private const val LINE_GAP = 3
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
