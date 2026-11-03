package com.fanguskdev.carofarty.ui.render

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
 * The marker (UI-04), as GameMaster's mockup: [points] on the left, in the warning colour when they are running out
 * ([warn]); on the right [distance], and the buttons of the sound ([soundOn] says which icon) and of the pause, [pressed]
 * if the finger is on one of them.
 */
internal fun DrawScope.paintHud(
    points: String,
    distance: String,
    layout: MenuLayout,
    ui: UiArt?,
    soundOn: Boolean,
    warn: Boolean,
    pressed: Box?,
) {
    val text = ui?.colors?.text ?: GreyPalette.text
    val shadow = ui?.colors?.shadow ?: GreyPalette.shadow
    val warning = ui?.colors?.warning ?: GreyPalette.bad
    drawText(points, layout.pointsLeft, layout.hudTextTop, if (warn) warning else text, shadow)
    drawText(distance, layout.metersRight - PixelFont.width(distance), layout.hudTextTop, text, shadow)
    paintIconButton(layout.hudSound, if (soundOn) Icon.SoundOn else Icon.SoundOff, ui, pressed = layout.hudSound == pressed)
    paintIconButton(layout.corner, Icon.Pause, ui, pressed = layout.corner == pressed)
}

/**
 * The hint to hold the finger at the first gaps of her first runs (GDD-17, UI-12), as GameMaster's storyboard: over Caro,
 * a little ahead, whose feet are at ([x], [feet]), and going with her: the finger holding, the meter, [fill] full from 0
 * to 1, and the word under them. Without the interface's art, the word alone.
 */
internal fun DrawScope.paintHint(
    text: String,
    x: Int,
    feet: Int,
    fill: Float,
    ui: UiArt?,
    millis: Long,
) {
    val left = x + HINT_AHEAD
    val top = feet - HINT_OVER_FEET
    val icons = ui?.icons
    val meter = ui?.meter
    if (icons != null) drawUiSprite(icons, "dedo-mantiene", left, top + FINGER_DOWN, millis)
    if (meter != null) {
        drawUiSprite(meter, "vacio", left + METER_RIGHT, top)
        drawUiSpriteBottom(meter, "lleno", left + METER_RIGHT, top, fill)
    }
    drawTextAround(
        text,
        left + WORD_CENTRE,
        top + WORD_DOWN,
        ui?.colors?.text ?: GreyPalette.record,
        ui?.colors?.shadow ?: GreyPalette.shadow,
    )
}

/**
 * A panel in the middle with [lines] centred, each a text, its tone and its size: GameMaster's panel as wide as its
 * longest line needs, or a dark band across the screen in the prototype.
 */
internal fun DrawScope.paintPanel(
    lines: List<PanelLine>,
    width: Int,
    height: Int,
    ui: UiArt?,
) {
    val box = panelBox(lines, width, height, ui)
    paintPanelBox(box, ui)
    var y = box.top + PANEL_TOP
    for (line in lines) {
        y += (PixelFont.LINE - PixelFont.HEIGHT) * line.size
        val color = when (line.tone) {
            Tone.Plain -> ui?.colors?.text ?: GreyPalette.text
            Tone.Warning -> ui?.colors?.warning ?: GreyPalette.bad
            Tone.Record -> ui?.colors?.record ?: GreyPalette.record
        }
        drawTextAround(line.text, width / 2, y - line.lift, color, ui?.colors?.shadow ?: GreyPalette.shadow, line.size)
        y += PixelFont.HEIGHT * line.size + 2
    }
}

/** Where the panel of [lines] goes: as wide as its longest line needs with the interface's art, else across the screen. */
internal fun panelBox(
    lines: List<PanelLine>,
    width: Int,
    height: Int,
    ui: UiArt?,
): Box {
    val tall = panelHeight(lines)
    val across = if (ui?.piece("panel") != null) lines.maxOf { PixelFont.width(it.text, it.size) } + 2 * PANEL_SIDE else width
    return Box((width - across) / 2, (height - tall) / 2, across, tall)
}

/** What a line of a panel says, by its colour: something plain, a warning or a record. */
internal enum class Tone { Plain, Warning, Record }

/**
 * A line of a panel: what it says, its tone and its size. The counter of a new record (UI-15) changes its text and its
 * tone as it counts, and lifts it a little ([lift], in pixels) when it arrives.
 */
internal class PanelLine(text: String, tone: Tone = Tone.Plain, val size: Int = 1) {
    var text = text
    var tone = tone
    var lift = 0
}

private fun panelHeight(lines: List<PanelLine>): Int = lines.sumOf { PixelFont.LINE * it.size + 2 } + 2 * PANEL_TOP

/** The row of game pixels under the panel of [lines] that [paintPanel] draws on a screen [height] tall. */
internal fun panelBottom(lines: List<PanelLine>, height: Int): Int = panelBox(lines, 0, height, null).bottom

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

/** The panels: from their edge to their text, above and at the sides. */
private const val PANEL_TOP = 8
private const val PANEL_SIDE = 16

/** The hint (diseno/ui/maquetas.py): from Caro's feet to its top, how far ahead of her, and where its parts go. */
private const val HINT_AHEAD = 2
private const val HINT_OVER_FEET = 66
private const val FINGER_DOWN = 4
private const val METER_RIGHT = 18
private const val WORD_CENTRE = 13
private const val WORD_DOWN = 26
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
