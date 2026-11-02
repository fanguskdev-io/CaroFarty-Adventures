package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.fanguskdev.carofarty.game.Reaction
import com.fanguskdev.carofarty.game.Scenario
import com.fanguskdev.carofarty.game.TILE
import com.fanguskdev.carofarty.storage.RecordRun
import com.fanguskdev.carofarty.ui.render.PixelFont.drawText
import kotlin.math.floor

/**
 * The start screen (UI-07, UI-20), as GameMaster's mockup that Fangusk chose: the title, a card for each scenario with its
 * name on its plate and a view of it going by with Caro running dressed for it, and the buttons of the records, of how to
 * play and of the sound. [pressed] is the box under the finger, drawn pressed. Without the interface's art, the grey
 * prototype's, with the [chosen] scenario outlined.
 */
internal fun DrawScope.paintTitle(
    layout: MenuLayout,
    art: Art?,
    names: List<String>,
    chosen: Scenario,
    soundOn: Boolean,
    millis: Long,
    pressed: Box?,
) {
    val ui = art?.ui
    paintMenuBackground(ui, layout.width, layout.height)
    val title = ui?.title
    if (title != null) {
        drawImage(
            title,
            IntOffset.Zero,
            IntSize(title.width, title.height),
            IntOffset(layout.title.left + (layout.title.width - title.width) / 2, layout.title.top),
            IntSize(title.width, title.height),
            filterQuality = FilterQuality.None,
        )
    } else {
        drawCentred(TITLE, layout.title, GreyPalette.record, GreyPalette.shadow, size = TITLE_SIZE)
    }
    for (scenario in Scenario.entries) {
        val box = layout.cards[scenario.ordinal]
        paintCard(box, art, scenario, names[scenario.ordinal], millis, lit = ui == null && scenario == chosen, pressed = box == pressed)
    }
    paintButton(layout.recordsButton, MenuLayout.RECORDS_TEXT, ui, Icon.Records, pressed = layout.recordsButton == pressed)
    paintButton(layout.howToButton, MenuLayout.HOW_TO_TEXT, ui, Icon.Help, pressed = layout.howToButton == pressed)
    paintIconButton(layout.titleSound, if (soundOn) Icon.SoundOn else Icon.SoundOff, ui, pressed = layout.titleSound == pressed)
}

/**
 * The records of a scenario (GDD-13, META-01), as GameMaster's mockup: the tabs of the scenarios over the panel, the one
 * of [chosen] in front; and in the panel its five best [runs], the longest first, with their metres, the most points of
 * each and the day, the first three with their medals and the rows that are still empty with a hyphen; with no run at
 * all, [RecordTexts.empty].
 */
internal fun DrawScope.paintRecords(
    layout: MenuLayout,
    names: List<String>,
    chosen: Scenario,
    runs: List<RecordRun>,
    texts: RecordTexts,
    ui: UiArt?,
    pressed: Box?,
) {
    paintMenuBackground(ui, layout.width, layout.height)
    paintIconButton(layout.back, Icon.Back, ui, pressed = layout.back == pressed)
    paintRibbon(layout.ribbon(texts.title), texts.title, ui)
    val text = ui?.colors?.text ?: GreyPalette.text
    val shadow = ui?.colors?.shadow ?: GreyPalette.shadow
    val secondary = ui?.colors?.secondary ?: GreyPalette.text
    val tabs = layout.tabs(names)
    val panel = layout.recordsPanel
    // The tabs behind, the panel, and the chosen tab in front of it, joined to its frame.
    for (i in tabs.indices) if (i != chosen.ordinal) paintTab(tabs[i], ui, active = false)
    paintPanelBox(panel, ui)
    paintTab(tabs[chosen.ordinal], ui, active = true)
    for (i in tabs.indices) {
        val color = if (ui == null && i == chosen.ordinal) GreyPalette.record else text
        drawTextAround(names[i], tabs[i].centreX, tabs[i].top + TAB_TEXT, color, shadow)
    }
    // The table: the place, the metres and the points on the right of their columns, the day on the left of its own.
    val placeX = panel.left + TABLE_LEFT
    val metersEnd = placeX + PLACE_WIDTH + COLUMN_GAP + NUMBER_WIDTH
    val pointsEnd = metersEnd + COLUMN_GAP + NUMBER_WIDTH
    val dayX = pointsEnd + COLUMN_GAP
    val headings = panel.top + TABLE_TOP
    drawText(texts.metersHeading, metersEnd - PixelFont.width(texts.metersHeading), headings, secondary, shadow)
    drawText(texts.pointsHeading, pointsEnd - PixelFont.width(texts.pointsHeading), headings, secondary, shadow)
    drawText(texts.dayHeading, dayX, headings, secondary, shadow)
    val firstRow = headings + HEADINGS_HEIGHT
    for (i in 0 until ROWS) {
        val y = firstRow + i * ROW_HEIGHT
        if (i % 2 == 0) {
            val stripe = Box(panel.left + MenuLayout.PANEL_BORDER, y - ROW_TEXT, panel.width - 2 * MenuLayout.PANEL_BORDER, ROW_HEIGHT)
            drawRect(STRIPE, Offset(stripe.left.toFloat(), stripe.top.toFloat()), Size(stripe.width.toFloat(), stripe.height.toFloat()))
        }
        val run = runs.getOrNull(i)
        if (run == null && runs.isEmpty()) continue
        val medal = MEDALS.getOrNull(i)
        val medals = ui?.medals
        if (run != null && medal != null && medals != null) {
            drawUiSprite(medals, medal, placeX + 1, y - MEDAL_UP)
        } else {
            drawText("${i + 1}", placeX + PLACE_TEXT, y, secondary, shadow)
        }
        val meters = run?.let { thousands(it.meters) } ?: EMPTY
        val points = run?.points?.toString() ?: EMPTY
        val day = run?.let { texts.day(it.day) } ?: EMPTY
        val color = if (run != null) text else secondary
        drawText(meters, metersEnd - PixelFont.width(meters), y, color, shadow)
        drawText(points, pointsEnd - PixelFont.width(points), y, color, shadow)
        drawText(day, dayX, y, color, shadow)
    }
    if (runs.isEmpty()) drawTextAround(texts.empty, panel.centreX, firstRow + 2 * ROW_HEIGHT, text, shadow)
}

/** A tab of the records: GameMaster's, in front ([active]) or behind; a button in the prototype. */
private fun DrawScope.paintTab(box: Box, ui: UiArt?, active: Boolean) {
    val piece = ui?.piece(if (active) "pestana" else "pestana-inactiva")
    if (piece != null) {
        drawPiece(piece, box)
    } else {
        drawRect(GreyPalette.panel, Offset(box.left.toFloat(), box.top.toFloat()), Size(box.width.toFloat(), box.height.toFloat()))
    }
}

/** [number] with a dot every three figures, as Spanish writes it: 1.284. */
internal fun thousands(number: Int): String {
    val figures = number.toString()
    if (figures.length <= THOUSAND_FIGURES || number < 0) return figures
    val text = StringBuilder()
    for ((i, figure) in figures.withIndex()) {
        if (i > 0 && (figures.length - i) % THOUSAND_FIGURES == 0) text.append('.')
        text.append(figure)
    }
    return text.toString()
}

/** The words of the records screen, which live with the other texts of the game. */
internal class RecordTexts(
    val title: String,
    val empty: String,
    val metersHeading: String,
    val pointsHeading: String,
    val dayHeading: String,
    val day: (Int) -> String,
)

/**
 * A scenario in a card of the start screen, as GameMaster's mockup: in GameMaster's card, its view going by slowly, the
 * sky, the two layers and the ground, with Caro running in its clothes a quarter of the way in; and its name on the
 * plate. Pressed, the card sinks. Without the interface's art, a frame with the name over it, [lit] if it is the
 * scenario she chose; without the scenario's art, its grey colours.
 */
private fun DrawScope.paintCard(
    box: Box,
    art: Art?,
    scenario: Scenario,
    name: String,
    millis: Long,
    lit: Boolean,
    pressed: Boolean,
) {
    val ui = art?.ui
    val piece = ui?.piece(if (pressed) "tarjeta-pulsada" else "tarjeta")
    val view = piece?.inside(box) ?: Box(box.left, box.top + CARD_PLATE, box.width, box.height - CARD_PLATE)
    if (piece == null) {
        val frame = if (lit) GreyPalette.record else GreyPalette.ink
        drawRect(frame, Offset(box.left - 1f, box.top - 1f), Size(box.width + 2f, box.height + 2f))
        drawRect(GreyPalette.panel, Offset(box.left.toFloat(), box.top.toFloat()), Size(box.width.toFloat(), CARD_PLATE.toFloat()))
    }
    // Each card a little ahead of the one before, so the three do not go in step.
    val travelled = millis * CARD_SPEED / SECOND + scenario.ordinal * CARD_OUT_OF_STEP
    val ground = view.bottom - CARD_GROUND
    clipRect(view.left.toFloat(), view.top.toFloat(), view.right.toFloat(), view.bottom.toFloat()) {
        paintMiniScenery(view, art?.scenario(scenario), GreyPalette.of(scenario), ground, travelled.toDouble(), withGround = true)
        art?.caro?.let {
            val animation = it.animation(CaroPose.Corre, Reaction.None)
            val cycle = animation.cycleTiles
            val frame = if (cycle != null && cycle > 0f) {
                animation.frame((travelled * animation.size / (cycle * TILE)).toInt())
            } else {
                animation.frameAt(millis, loops = true)
            }
            drawFrame(it.sheet(scenario), frame, view.left + view.width / 4 - it.anchorX, ground - it.anchorY)
        }
    }
    if (piece != null) {
        drawPiece(piece, box)
        drawTextAround(name, box.centreX, box.top + piece.content[1] - PLATE_TEXT_UP, ui.colors.text, ui.colors.shadow)
    } else {
        drawCentred(
            name,
            Box(box.left, box.top, box.width, CARD_PLATE),
            if (lit) GreyPalette.record else GreyPalette.text,
            GreyPalette.shadow,
        )
    }
}

/**
 * A scenario in a small [box]: its sky, its two layers with their bottom on the [ground] row and, if [withGround], its
 * ground there; all of it [travelled] pixels along, the layers at their own share of it, as in a run. Without its art,
 * its grey colours.
 */
internal fun DrawScope.paintMiniScenery(
    box: Box,
    scenery: ScenarioArt?,
    palette: GreyPalette,
    ground: Int,
    travelled: Double,
    withGround: Boolean,
) {
    if (scenery == null) {
        drawRect(palette.sky, Offset(box.left.toFloat(), box.top.toFloat()), Size(box.width.toFloat(), box.height.toFloat()))
        if (withGround) {
            drawRect(
                palette.groundTop,
                Offset(box.left.toFloat(), ground.toFloat()),
                Size(box.width.toFloat(), (box.bottom - ground).toFloat()),
            )
        }
        return
    }
    // The sky as in a run: its bottom as far under the ground as the screen's bottom is under the lowest ground.
    scenery.sky?.let {
        drawImage(
            it,
            IntOffset.Zero,
            IntSize(it.width, it.height),
            IntOffset(box.left, ground + SKY_BELOW_GROUND - it.height),
            IntSize(box.width, it.height),
            filterQuality = FilterQuality.None,
        )
    }
    scenery.far?.let { paintSmallLayer(it, box, ground, floor(travelled * LAYER_SPEEDS[0]).toLong()) }
    scenery.mid?.let { paintSmallLayer(it, box, ground, floor(travelled * LAYER_SPEEDS[1]).toLong()) }
    if (!withGround) return
    val along = floor(travelled).toLong()
    var column = along.floorDiv(TILE.toLong())
    var x = box.left - along.mod(TILE.toLong()).toInt()
    while (x < box.right) {
        val index = column.mod(scenery.surface.size.toLong()).toInt()
        drawFrame(scenery.tiles, scenery.surface[index], x, ground)
        drawFrame(scenery.tiles, scenery.fill[column.mod(scenery.fill.size.toLong()).toInt()], x, ground + TILE)
        x += TILE
        column++
    }
}

/** A layer of the background in a small box, its bottom on the box's ground and [shift] pixels along. */
private fun DrawScope.paintSmallLayer(
    layer: Layer,
    box: Box,
    ground: Int,
    shift: Long,
) {
    val image = layer.image
    val top = ground + layer.belowGround - image.height
    var x = box.left - shift.mod(image.width.toLong()).toInt()
    while (x < box.right) {
        drawImage(
            image,
            IntOffset.Zero,
            IntSize(image.width, image.height),
            IntOffset(x, top),
            IntSize(image.width, image.height),
            filterQuality = FilterQuality.None,
        )
        x += image.width
    }
}

/** The ground of [scenery] along the bottom of the menus of the prototype, two tiles high, so its scenery stands on it. */
internal fun DrawScope.paintMenuGround(
    scenery: ScenarioArt?,
    palette: GreyPalette,
    width: Int,
    height: Int,
) {
    val top = height - MENU_GROUND
    if (scenery == null) {
        drawRect(palette.groundTop, Offset(0f, top.toFloat()), Size(width.toFloat(), MENU_GROUND.toFloat()))
        return
    }
    var x = 0
    var column = 0
    while (x < width) {
        drawFrame(scenery.tiles, scenery.surface[column % scenery.surface.size], x, top)
        drawFrame(scenery.tiles, scenery.fill[column % scenery.fill.size], x, top + TILE)
        x += TILE
        column++
    }
}

private const val TITLE = "CAROFARTY"
private const val TITLE_SIZE = 3
private const val CARD_PLATE = 14
private const val PLATE_TEXT_UP = 10
private const val CARD_GROUND = 12
private const val TAB_TEXT = 6

/** The table of the records (diseno/ui/maquetas.py): where it begins in the panel, its columns, rows and their texts. */
private const val TABLE_LEFT = 16
private const val TABLE_TOP = 14
private const val PLACE_WIDTH = 14
private const val NUMBER_WIDTH = 40
private const val COLUMN_GAP = 12
private const val HEADINGS_HEIGHT = 12
private const val ROWS = 5
private const val ROW_HEIGHT = 14
private const val ROW_TEXT = 4
private const val PLACE_TEXT = 3
private const val MEDAL_UP = 3
private const val THOUSAND_FIGURES = 3
private const val EMPTY = "-"

/** The medals of the first three runs, by the names of GameMaster's sheet. */
private val MEDALS = listOf("oro", "plata", "bronce")

/** Every other row of the records, a little lighter: «ciruela clara» of the interface's palette. */
private val STRIPE = Color(0xFF6E345C)

/** How far under the lowest ground the screen ends, in a run: where the sky's bottom goes. */
private const val SKY_BELOW_GROUND = 2 * TILE
private const val MENU_GROUND = 2 * TILE
private const val SECOND = 1000L

/** The pace of the views of the cards, in pixels a second (GameMaster's mockup), and how far each is from the last. */
private const val CARD_SPEED = 60L
private const val CARD_OUT_OF_STEP = 37L
