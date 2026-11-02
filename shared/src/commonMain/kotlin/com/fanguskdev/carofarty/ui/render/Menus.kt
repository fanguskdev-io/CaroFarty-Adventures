package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.fanguskdev.carofarty.ui.render.PixelFont.drawText
import kotlin.math.min

/** A box of the screen in game pixels, from its top left corner. */
internal class Box(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
) {
    val right: Int get() = left + width
    val bottom: Int get() = top + height
    val centreX: Int get() = left + width / 2

    /** Whether ([x], [y]) is on it, with some [slack] around for the finger. */
    fun contains(x: Float, y: Float, slack: Int = SLACK): Boolean =
        x >= left - slack && x < right + slack && y >= top - slack && y < bottom + slack

    private companion object {
        const val SLACK = 4
    }
}

/** The icons of the buttons, drawn in grey until GameMaster's arrive (UI-02). */
internal enum class Icon {
    Pause,
    SoundOn,
    SoundOff,
    Back,
}

/**
 * Where everything of the menus goes on a screen [width] × [height] game pixels with its [insets] (UI-01, UI-07, UI-20):
 * the start screen with the title, a card for each scenario and its two buttons; the records with their tabs; how to play
 * with its pages; and the buttons of the marker. Made again only when the screen changes.
 */
internal class MenuLayout(val width: Int, val height: Int, val insets: SafeInsets) {
    /** The round button at the top right: the sound on the start screen, the pause in a run. */
    val corner = Box(width - insets.right - MARGIN - ICON_BUTTON, MARGIN + insets.top, ICON_BUTTON, ICON_BUTTON)

    /** In a run, the sound, left of the pause. */
    val hudSound = Box(corner.left - GAP - ICON_BUTTON, corner.top, ICON_BUTTON, ICON_BUTTON)

    /** The way back, at the top left of the records and of how to play. */
    val back = Box(MARGIN + insets.left, MARGIN + insets.top, ICON_BUTTON, ICON_BUTTON)

    /** The title, its top row. */
    val titleTop = MARGIN + insets.top + TITLE_DROP

    /** The cards of the scenarios on the start screen, with their name under each. */
    val cards: List<Box>

    /** The two buttons of the start screen, with their texts. */
    val recordsButton: Box
    val howToButton: Box

    init {
        val cardWidth = min(CARD_MAX_WIDTH, (width - insets.left - insets.right - 2 * SIDE - 2 * GAP) / 3)
        val cardHeight = cardWidth * CARD_HEIGHT_PERCENT / 100
        val cardsLeft = (width - 3 * cardWidth - 2 * GAP) / 2
        val cardsTop = titleTop + TITLE_HEIGHT + GAP + (height - MIN_HEIGHT) / 3
        cards = List(3) { Box(cardsLeft + it * (cardWidth + GAP), cardsTop, cardWidth, cardHeight) }
        val buttonsTop = height - MARGIN - BUTTON_HEIGHT - (height - MIN_HEIGHT) / 4
        val records = buttonWidth(RECORDS_TEXT)
        val howTo = buttonWidth(HOW_TO_TEXT)
        val buttonsLeft = (width - records - howTo - GAP) / 2
        recordsButton = Box(buttonsLeft, buttonsTop, records, BUTTON_HEIGHT)
        howToButton = Box(buttonsLeft + records + GAP, buttonsTop, howTo, BUTTON_HEIGHT)
    }

    /** The tabs of the records, a button for each of [names], under the title. */
    fun tabs(names: List<String>): List<Box> = row(names, top = back.bottom + GAP)

    /** The top row of the list of records. */
    val listTop: Int get() = back.bottom + GAP + BUTTON_HEIGHT + GAP

    /** Buttons in a row for [names], centred, from [top]. */
    fun row(names: List<String>, top: Int): List<Box> {
        val widths = names.map { buttonWidth(it) }
        var left = (width - widths.sum() - GAP * (names.size - 1)) / 2
        return widths.map { across -> Box(left, top, across, BUTTON_HEIGHT).also { left += across + GAP } }
    }

    companion object {
        const val MARGIN = 6
        const val GAP = 8
        const val ICON_BUTTON = 18
        const val BUTTON_HEIGHT = PixelFont.HEIGHT + 2 * 5
        const val TITLE_SIZE = 3
        const val TITLE_HEIGHT = PixelFont.HEIGHT * TITLE_SIZE
        private const val TITLE_DROP = 6
        private const val SIDE = 16
        private const val CARD_MAX_WIDTH = 120
        private const val CARD_HEIGHT_PERCENT = 64
        private const val MIN_HEIGHT = 180
        private const val BUTTON_PAD = 8

        /** The texts of the two buttons of the start screen, which the layout needs to size them. */
        const val RECORDS_TEXT = "RÉCORDS"
        const val HOW_TO_TEXT = "CÓMO SE JUEGA"

        fun buttonWidth(text: String): Int = PixelFont.width(text) + 2 * BUTTON_PAD
    }
}

/** A button with [text] in [box]; [lit] draws it in the colour of the records, as the chosen one. */
internal fun DrawScope.paintButton(box: Box, text: String, lit: Boolean = false) {
    if (lit) drawRect(GreyPalette.record, Offset(box.left - 1f, box.top - 1f), Size(box.width + 2f, box.height + 2f))
    drawRect(GreyPalette.panel, Offset(box.left.toFloat(), box.top.toFloat()), Size(box.width.toFloat(), box.height.toFloat()))
    val x = box.left + (box.width - PixelFont.width(text)) / 2
    val y = box.top + (box.height - PixelFont.HEIGHT) / 2
    drawText(text, x, y, if (lit) GreyPalette.record else GreyPalette.text, GreyPalette.shadow)
}

/** A round button with [icon] in [box]. */
internal fun DrawScope.paintIconButton(box: Box, icon: Icon) {
    drawRect(GreyPalette.panel, Offset(box.left.toFloat(), box.top.toFloat()), Size(box.width.toFloat(), box.height.toFloat()))
    val pattern = when (icon) {
        Icon.Pause -> PAUSE
        Icon.SoundOn -> SOUND_ON
        Icon.SoundOff -> SOUND_OFF
        Icon.Back -> BACK
    }
    pattern.draw(this, box.left + (box.width - ICON) / 2, box.top + (box.height - ICON) / 2, size = 1) { code ->
        when (code) {
            'x' -> GreyPalette.text
            'r' -> GreyPalette.bad
            else -> null
        }
    }
}

/** A dark panel over the whole screen, for the menus over the moving scenery. */
internal fun DrawScope.paintShade(width: Int, height: Int, color: Color = GreyPalette.shade) {
    drawRect(color, Offset.Zero, Size(width.toFloat(), height.toFloat()))
}

private const val ICON = 12

private val PAUSE = PixelRuns(
    listOf(
        "............",
        "............",
        "...xx..xx...",
        "...xx..xx...",
        "...xx..xx...",
        "...xx..xx...",
        "...xx..xx...",
        "...xx..xx...",
        "...xx..xx...",
        "...xx..xx...",
        "............",
        "............",
    ),
)

private val SOUND_ON = PixelRuns(
    listOf(
        "............",
        ".....x....x.",
        "....xx..x..x",
        "...xxx...x.x",
        "xxxxxx.x.x.x",
        "xxxxxx.x.x.x",
        "xxxxxx.x.x.x",
        "xxxxxx.x.x.x",
        "...xxx...x.x",
        "....xx..x..x",
        ".....x....x.",
        "............",
    ),
)

private val SOUND_OFF = PixelRuns(
    listOf(
        "............",
        ".....x......",
        "....xx......",
        "...xxx......",
        "xxxxxx.r...r",
        "xxxxxx..r.r.",
        "xxxxxx...r..",
        "xxxxxx..r.r.",
        "...xxx.r...r",
        "....xx......",
        ".....x......",
        "............",
    ),
)

private val BACK = PixelRuns(
    listOf(
        "............",
        ".....x......",
        "....xx......",
        "...xxx......",
        "..xxxxxxxxx.",
        ".xxxxxxxxxx.",
        ".xxxxxxxxxx.",
        "..xxxxxxxxx.",
        "...xxx......",
        "....xx......",
        ".....x......",
        "............",
    ),
)
