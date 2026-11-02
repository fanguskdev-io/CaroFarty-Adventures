package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.fanguskdev.carofarty.ui.render.PixelFont.drawText
import kotlin.math.max

/** A box of the screen in game pixels, from its top left corner; two with the same place and size are equal. */
internal data class Box(
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

/** The icons of the buttons, by the names of GameMaster's sheet (iconos.json). */
internal enum class Icon(val key: String) {
    Pause("pausa"),
    SoundOn("sonido-si"),
    SoundOff("sonido-no"),
    Records("records"),
    Help("ayuda"),
    Back("volver"),
    Again("reintentar"),
    Exit("salir"),
}

/**
 * Where everything of the menus and of the marker goes on a screen [width] × [height] game pixels with its [insets]
 * (UI-01, UI-04, UI-07, UI-20), by the rules of GameMaster's mockups that Fangusk chose (diseno/ui/maquetas.py): the small
 * buttons 7 px from the top and 8 from the side, clear of a rounded corner; the start screen with the title, the cards of
 * the scenarios and the row of buttons, what is left shared out in four equal gaps; the records and how to play with
 * their ribbon on top and their panel in the middle of the rest. Made again only when the screen changes.
 */
internal class MenuLayout(val width: Int, val height: Int, val insets: SafeInsets) {
    /** The useful part of the screen, without its margins and its insets: from [left] to [right], down to [bottom]. */
    private val left = insets.left + MARGIN
    private val right = width - insets.right - MARGIN
    private val bottom = height - MARGIN

    /** The small button at the top right: the pause in a run. */
    val corner = Box(width - insets.right - SIDE_MARGIN - SMALL, insets.top + TOP_MARGIN, SMALL, SMALL)

    /** In a run, the sound, left of the pause. */
    val hudSound = Box(corner.left - SMALL_GAP - SMALL, corner.top, SMALL, SMALL)

    /** The way back, at the top left of the records and of how to play. */
    val back = Box(insets.left + SIDE_MARGIN, insets.top + TOP_MARGIN, SMALL, SMALL)

    /** The way out of the game, at the top left of the start screen (UI-22). */
    val exit = back

    /** The texts of the marker: their top, where the points begin and where the metres end, left of the sound. */
    val hudTextTop = corner.top + HUD_TEXT_DOWN
    val pointsLeft = left
    val metersRight = hudSound.left - METERS_GAP

    /** Under the small buttons and the ribbon, where the records and how to play begin. */
    private val headerBottom = insets.top + TOP_MARGIN + BUTTON_HEIGHT

    /** Where something [across] wide begins, centred on the useful part of the screen; on the screen, if it does not fit. */
    private fun centred(across: Int): Int = ((left + right - across) / 2).coerceIn(0, max(0, width - across))

    /** The ribbon with the title of the records and of how to play, centred; its text decides its width. */
    fun ribbon(text: String): Box {
        val across = PixelFont.width(text) + RIBBON_SIDES
        return Box((left + right) / 2 - across / 2, insets.top + TOP_MARGIN + 1, across, RIBBON_HEIGHT)
    }

    /** The title image of the start screen and the cards of the scenarios, with the row of buttons and the sound. */
    val title: Box
    val cards: List<Box>
    val recordsButton: Box
    val howToButton: Box
    val titleSound: Box

    init {
        val gap = max(MIN_START_GAP, (bottom - insets.top - MARGIN - TITLE_HEIGHT - CARD_HEIGHT - BUTTON_HEIGHT) / 4)
        var y = insets.top + MARGIN + gap
        title = Box(centred(TITLE_WIDTH), y, TITLE_WIDTH, TITLE_HEIGHT)
        y += TITLE_HEIGHT + gap
        val row = 3 * CARD_WIDTH + 2 * GAP
        val cardsLeft = centred(row)
        cards = List(3) { Box(cardsLeft + it * (CARD_WIDTH + GAP), y, CARD_WIDTH, CARD_HEIGHT) }
        y += CARD_HEIGHT + gap
        val records = buttonWidth(RECORDS_TEXT, icon = true)
        val howTo = buttonWidth(HOW_TO_TEXT, icon = true)
        val buttonsLeft = centred(records + howTo + GAP)
        recordsButton = Box(buttonsLeft, y, records, BUTTON_HEIGHT)
        howToButton = Box(buttonsLeft + records + GAP, y, howTo, BUTTON_HEIGHT)
        titleSound = Box(cardsLeft + row - SMALL, y + 1, SMALL, SMALL)
    }

    /**
     * The title screen (UI-07, the animated title Fangusk chose): the title where the start screen has it, so it stays in
     * its place from one to the other; the scenario along the bottom, from side to side, under the second band of the sky
     * of the menus, with its ground [introGround]; and Caro running in it at [introCaroX].
     */
    val introScene = minOf(menuSkySecondBandEnd(height), height - INTRO_SCENE_MIN).let { Box(0, it, width, height - it) }
    val introGround = height - INTRO_GROUND
    val introCaroX = width / INTRO_CARO_PART

    /** «TOCA PARA EMPEZAR», [text] in big letters, centred between the title and the scenario. */
    fun prompt(text: String): Box {
        val across = PixelFont.width(text, PROMPT_SIZE)
        val tall = PixelFont.HEIGHT * PROMPT_SIZE
        return Box(centred(across), (title.bottom + introScene.top - tall) / 2, across, tall)
    }

    /** The panel of the records, with its tabs over it, in the middle of what the header leaves. */
    val recordsPanel: Box
    val tabsTop: Int

    init {
        val block = TAB_HEIGHT - TAB_OVERLAP + RECORDS_HEIGHT
        tabsTop = headerBottom + max(RECORDS_MIN_GAP, (bottom - headerBottom - block) / 2)
        recordsPanel = Box(centred(RECORDS_WIDTH), tabsTop + TAB_HEIGHT - TAB_OVERLAP, RECORDS_WIDTH, RECORDS_HEIGHT)
    }

    /** The tabs of the records over its panel, a tab for each of [names], from its left. */
    fun tabs(names: List<String>): List<Box> {
        var x = recordsPanel.left + TAB_INDENT
        return names.map { name ->
            val across = PixelFont.width(name) + 2 * TAB_PAD
            Box(x, tabsTop, across, TAB_HEIGHT).also { x += across + TAB_GAP }
        }
    }

    /** The panel of how to play, in the middle of what the header leaves, with the page dots under it. */
    val howToPanel: Box

    /** The two frames of a page of how to play, its scenes in them and their two lines under each. */
    val frames: List<Box>

    init {
        val block = HOW_TO_HEIGHT + DOTS_SPACE
        val top = headerBottom + max(HOW_TO_MIN_GAP, (bottom - headerBottom - block) / 2)
        howToPanel = Box(centred(HOW_TO_WIDTH), top, HOW_TO_WIDTH, HOW_TO_HEIGHT)
        frames = List(2) {
            Box(
                howToPanel.left + PANEL_BORDER + HOW_TO_PAD + it * (FRAME_WIDTH + FRAMES_GAP),
                top + PANEL_BORDER + HOW_TO_PAD - 2,
                FRAME_WIDTH,
                FRAME_HEIGHT,
            )
        }
    }

    /** The button forward of how to play, at the top right, for its [text]. */
    fun forward(text: String): Box {
        val across = buttonWidth(text, icon = false)
        return Box(width - insets.right - SIDE_MARGIN - across, insets.top + TOP_MARGIN, across, BUTTON_HEIGHT)
    }

    /**
     * Buttons in a row for [names], centred, from [top]; with the icons of [icons] where they are not null, and [atLeast]
     * wide, so a short answer is as easy to tap as a long one.
     */
    fun row(
        names: List<String>,
        top: Int,
        icons: List<Icon?> = emptyList(),
        atLeast: Int = 0,
    ): List<Box> {
        val widths = names.mapIndexed { i, name -> max(atLeast, buttonWidth(name, icon = icons.getOrNull(i) != null)) }
        var x = centred(widths.sum() + GAP * (names.size - 1))
        return widths.map { across -> Box(x, top, across, BUTTON_HEIGHT).also { x += across + GAP } }
    }

    companion object {
        const val GAP = 8

        /** The narrowest a button with an answer is: «SÍ» and «NO». */
        const val ANSWER_WIDTH = 56
        const val SMALL = 20
        const val SMALL_GAP = 4
        const val MARGIN = 6
        const val TOP_MARGIN = 7
        const val SIDE_MARGIN = 8
        const val BUTTON_HEIGHT = 22
        const val BUTTON_PAD = 8
        const val ICON = 16
        const val ICON_GAP = 4
        const val TITLE_WIDTH = 189
        const val TITLE_HEIGHT = 42
        const val RIBBON_HEIGHT = 17
        const val TAB_HEIGHT = 16
        const val TAB_OVERLAP = 2

        /** A card: the view of 88 × 56 in the frame of GameMaster's card, with the plate of the name over it. */
        const val CARD_WIDTH = 96
        const val CARD_HEIGHT = 75

        /** A frame of how to play: the scene of 120 × 68 in GameMaster's thin frame. */
        const val FRAME_WIDTH = 128
        const val FRAME_HEIGHT = 76

        /** The border of GameMaster's panel, outside of what goes in it. */
        const val PANEL_BORDER = 6

        /** From the frames of how to play to their first line, and from the panel to the page dots. */
        const val CAPTION_GAP = 5
        const val DOTS_GAP = 3
        private const val HUD_TEXT_DOWN = 5
        private const val METERS_GAP = 6
        private const val RIBBON_SIDES = 28
        private const val TAB_PAD = 8
        private const val TAB_GAP = 2
        private const val TAB_INDENT = 8
        private const val MIN_START_GAP = 4
        private const val RECORDS_MIN_GAP = 6
        private const val RECORDS_WIDTH = 224
        private const val RECORDS_HEIGHT = 108
        private const val HOW_TO_MIN_GAP = 4
        private const val HOW_TO_PAD = 7
        private const val FRAMES_GAP = 10
        private const val HOW_TO_WIDTH = 2 * FRAME_WIDTH + FRAMES_GAP + 2 * HOW_TO_PAD + 2 * PANEL_BORDER
        private const val HOW_TO_HEIGHT = PANEL_BORDER + HOW_TO_PAD + FRAME_HEIGHT + 4 + 2 * PixelFont.LINE + PANEL_BORDER - 2
        private const val DOTS_SPACE = 4 + 7

        /**
         * The title screen: the scenario never less tall than enough for Caro over its ground, its ground the two tiles of
         * the start of a run, Caro a quarter of the way in, as in the cards, and the prompt in letters twice the size.
         */
        private const val INTRO_SCENE_MIN = 80
        private const val INTRO_GROUND = 32
        private const val INTRO_CARO_PART = 4
        const val PROMPT_SIZE = 2

        /** The texts of the buttons whose size the layout needs. */
        const val RECORDS_TEXT = "RÉCORDS"
        const val HOW_TO_TEXT = "CÓMO SE JUEGA"

        /** How wide a button is for [text], with an icon before it or not. */
        fun buttonWidth(text: String, icon: Boolean): Int = PixelFont.width(text) + 2 * BUTTON_PAD + if (icon) ICON + ICON_GAP else 0
    }
}

/**
 * A button with [text], and [icon] before it if given, in [box]: GameMaster's if there is [ui] art, in its pressed look
 * while the finger is on it ([pressed]), with what is in it centred on its face; or the grey prototype's, where [lit]
 * marks the main one.
 */
internal fun DrawScope.paintButton(
    box: Box,
    text: String,
    ui: UiArt? = null,
    icon: Icon? = null,
    pressed: Boolean = false,
    lit: Boolean = false,
) {
    val piece = ui?.piece(if (pressed) "boton-pulsado" else "boton")
    if (piece == null) {
        if (lit) drawRect(GreyPalette.record, Offset(box.left - 1f, box.top - 1f), Size(box.width + 2f, box.height + 2f))
        drawRect(GreyPalette.panel, Offset(box.left.toFloat(), box.top.toFloat()), Size(box.width.toFloat(), box.height.toFloat()))
        drawCentred(text, box, if (lit) GreyPalette.record else GreyPalette.text, GreyPalette.shadow)
        return
    }
    drawPiece(piece, box)
    val face = piece.inside(box)
    val icons = ui.icons?.takeIf { icon != null }
    val across = PixelFont.width(text) + if (icons != null) MenuLayout.ICON + MenuLayout.ICON_GAP else 0
    var x = (face.left + face.right - across) / 2
    if (icon != null && icons != null) {
        drawUiSprite(icons, icon.key, x, (face.top + face.bottom - MenuLayout.ICON) / 2)
        x += MenuLayout.ICON + MenuLayout.ICON_GAP
    }
    drawText(text, x, (face.top + face.bottom - PixelFont.HEIGHT) / 2 + 1, ui.colors.text, ui.colors.shadow)
}

/** A small square button with [icon] in [box]: GameMaster's if there is [ui] art, pressed or not, or the grey prototype's. */
internal fun DrawScope.paintIconButton(
    box: Box,
    icon: Icon,
    ui: UiArt? = null,
    pressed: Boolean = false,
) {
    val piece = ui?.piece(if (pressed) "boton-pequeno-pulsado" else "boton-pequeno")
    val icons = ui?.icons
    if (piece != null && icons != null) {
        drawPiece(piece, box)
        // An icon that has not arrived yet in GameMaster's sheet: the prototype's, in the colour of the text.
        if (!drawUiSprite(icons, icon.key, box.left + piece.icon[0], box.top + piece.icon[1])) {
            val face = piece.inside(box)
            drawGreyIcon(icon, Box(face.left, face.top - piece.content[1] + piece.icon[1], face.width, MenuLayout.ICON), ui.colors.text)
        }
        return
    }
    drawRect(GreyPalette.panel, Offset(box.left.toFloat(), box.top.toFloat()), Size(box.width.toFloat(), box.height.toFloat()))
    drawGreyIcon(icon, box, GreyPalette.text)
}

/** The prototype's drawing of [icon], centred in [box], in [color]. */
private fun DrawScope.drawGreyIcon(icon: Icon, box: Box, color: Color) {
    val pattern = when (icon) {
        Icon.SoundOn -> SOUND_ON
        Icon.SoundOff -> SOUND_OFF
        Icon.Back -> BACK
        Icon.Exit -> EXIT
        else -> PAUSE
    }
    pattern.draw(this, box.left + (box.width - GREY_ICON) / 2, box.top + (box.height - GREY_ICON) / 2, size = 1) { code ->
        when (code) {
            'x' -> color
            'r' -> GreyPalette.bad
            else -> null
        }
    }
}

/** The ribbon with [text] in [box]: GameMaster's, or the text alone, bigger, in the prototype. */
internal fun DrawScope.paintRibbon(box: Box, text: String, ui: UiArt?) {
    val piece = ui?.piece("cinta")
    if (piece != null) {
        drawPiece(piece, box)
        drawCentred(text, piece.inside(box), ui.colors.text, ui.colors.shadow)
    } else {
        drawCentred(text, box, GreyPalette.record, GreyPalette.shadow, size = 2)
    }
}

/** A panel over [box]: GameMaster's, or a dark rectangle in the prototype. */
internal fun DrawScope.paintPanelBox(box: Box, ui: UiArt?) {
    val piece = ui?.piece("panel")
    if (piece != null) {
        drawPiece(piece, box)
    } else {
        drawRect(GreyPalette.panel, Offset(box.left.toFloat(), box.top.toFloat()), Size(box.width.toFloat(), box.height.toFloat()))
    }
}

/** [text] with its middle at [centreX] and its top at [y], as GameMaster's mockups centre it. */
internal fun DrawScope.drawTextAround(
    text: String,
    centreX: Int,
    y: Int,
    color: Color,
    shadow: Color,
    size: Int = 1,
) {
    drawText(text, centreX - PixelFont.width(text, size) / 2, y, color, shadow, size)
}

/** The background of the menus: GameMaster's day sky with its clouds, or a dark veil over the scenery in the prototype. */
internal fun DrawScope.paintMenuBackground(ui: UiArt?, width: Int, height: Int) {
    if (ui == null) paintShade(width, height) else paintMenuSky(width, height, ui)
}

/** A dark veil over the whole screen, for the menus over the moving scenery of the prototype. */
internal fun DrawScope.paintShade(width: Int, height: Int, color: Color = GreyPalette.shade) {
    drawRect(color, Offset.Zero, Size(width.toFloat(), height.toFloat()))
}

private const val GREY_ICON = 12

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

/** A door, open, and the way out through it. */
private val EXIT = PixelRuns(
    listOf(
        "............",
        "..xxxxxx....",
        "..x....x....",
        "..x....x....",
        "..x....x.x..",
        "..x..x.xxxx.",
        "..x....x.x..",
        "..x....x....",
        "..x....x....",
        "..xxxxxx....",
        "............",
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
