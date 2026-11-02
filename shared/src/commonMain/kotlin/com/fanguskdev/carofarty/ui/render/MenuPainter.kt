package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

/**
 * The start screen (UI-07, UI-20): the title, a card for each scenario with a view of it and Caro dressed for it, its name
 * under each, the [chosen] one lit, and the buttons of the records, of how to play and of the sound.
 */
internal fun DrawScope.paintTitle(
    layout: MenuLayout,
    art: Art?,
    names: List<String>,
    chosen: Scenario,
    soundOn: Boolean,
    millis: Long,
) {
    paintShade(layout.width, layout.height)
    val title = TITLE
    val titleX = (layout.width - PixelFont.width(title, MenuLayout.TITLE_SIZE)) / 2
    drawText(title, titleX, layout.titleTop, GreyPalette.record, GreyPalette.shadow, MenuLayout.TITLE_SIZE)
    for (scenario in Scenario.entries) {
        val box = layout.cards[scenario.ordinal]
        paintCard(box, art, scenario, millis, lit = scenario == chosen)
        val name = names[scenario.ordinal]
        val color = if (scenario == chosen) GreyPalette.record else GreyPalette.text
        drawText(name, box.centreX - PixelFont.width(name) / 2, box.bottom + NAME_GAP, color, GreyPalette.shadow)
    }
    paintButton(layout.recordsButton, MenuLayout.RECORDS_TEXT)
    paintButton(layout.howToButton, MenuLayout.HOW_TO_TEXT)
    paintIconButton(layout.corner, if (soundOn) Icon.SoundOn else Icon.SoundOff)
}

/**
 * The records of a scenario (GDD-13, META-01): its [runs], the longest first, with their metres, the most points of each
 * and the day; the [chosen] scenario lit among the tabs.
 */
internal fun DrawScope.paintRecords(
    layout: MenuLayout,
    names: List<String>,
    chosen: Scenario,
    runs: List<RecordRun>,
    texts: RecordTexts,
) {
    paintShade(layout.width, layout.height)
    drawText(
        texts.title,
        (layout.width - PixelFont.width(texts.title, 2)) / 2,
        layout.back.top + 2,
        GreyPalette.record,
        GreyPalette.shadow,
        2,
    )
    paintIconButton(layout.back, Icon.Back)
    val tabs = layout.tabs(names)
    for (i in tabs.indices) paintButton(tabs[i], names[i], lit = i == chosen.ordinal)
    var y = layout.listTop
    if (runs.isEmpty()) {
        drawText(texts.empty, (layout.width - PixelFont.width(texts.empty)) / 2, y + ROW, GreyPalette.text, GreyPalette.shadow)
        return
    }
    val centre = layout.width / 2
    for ((i, run) in runs.withIndex()) {
        val color = if (i == 0) GreyPalette.record else GreyPalette.text
        val place = "${i + 1}."
        val meters = "${run.meters} M"
        val points = "${run.points} ${texts.points}"
        val day = texts.day(run.day)
        drawText(place, centre + PLACE_END - PixelFont.width(place), y, color, GreyPalette.shadow)
        drawText(meters, centre + METERS_END - PixelFont.width(meters), y, color, GreyPalette.shadow)
        drawText(points, centre + POINTS_END - PixelFont.width(points), y, color, GreyPalette.shadow)
        drawText(day, centre + DAY_START, y, color, GreyPalette.shadow)
        y += ROW
    }
}

/** The words of the records screen, which live with the other texts of the game. */
internal class RecordTexts(
    val title: String,
    val empty: String,
    val points: String,
    val day: (Int) -> String,
)

/**
 * A scenario in a card of the start screen: its sky, its two layers and its ground going by slowly, and Caro waiting in
 * its clothes. Without the scenario's art, its grey colours.
 */
private fun DrawScope.paintCard(
    box: Box,
    art: Art?,
    scenario: Scenario,
    millis: Long,
    lit: Boolean,
) {
    if (lit) drawRect(GreyPalette.record, Offset(box.left - 2f, box.top - 2f), Size(box.width + 4f, box.height + 4f))
    drawRect(GreyPalette.ink, Offset(box.left - 1f, box.top - 1f), Size(box.width + 2f, box.height + 2f))
    val ground = box.bottom - CARD_GROUND
    clipRect(box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat()) {
        paintMiniScenery(box, art?.scenario(scenario), GreyPalette.of(scenario), ground, millis, withGround = true)
        art?.caro?.let {
            val animation = it.animation(CaroPose.Espera, Reaction.None)
            drawFrame(it.sheet(scenario), animation.frameAt(millis, loops = true), box.left + CARD_CARO_X - it.anchorX, ground - it.anchorY)
        }
    }
}

/**
 * A scenario in a small [box]: its sky, its two layers going by slowly with their bottom on the [ground] row and, if
 * [withGround], its ground there; without its art, its grey colours.
 */
internal fun DrawScope.paintMiniScenery(
    box: Box,
    scenery: ScenarioArt?,
    palette: GreyPalette,
    ground: Int,
    millis: Long,
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
    scenery.sky?.let {
        drawImage(
            it,
            IntOffset.Zero,
            IntSize(it.width, it.height),
            IntOffset(box.left, box.top),
            IntSize(box.width, box.height),
            filterQuality = FilterQuality.None,
        )
    }
    scenery.far?.let { paintCardLayer(it, box, ground, millis * FAR_DRIFT / SECOND) }
    scenery.mid?.let { paintCardLayer(it, box, ground, millis * MID_DRIFT / SECOND) }
    if (!withGround) return
    val shift = (millis * GROUND_DRIFT / SECOND).toInt() % TILE
    var x = box.left - shift
    var column = 0
    while (x < box.right) {
        drawFrame(scenery.tiles, scenery.surface[column % scenery.surface.size], x, ground)
        drawFrame(scenery.tiles, scenery.fill[column % scenery.fill.size], x, ground + TILE)
        x += TILE
        column++
    }
}

/** The ground of [scenery] along the bottom of the menus, two tiles high, so their scenery stands on something. */
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

/** A layer of the background in a card, its bottom on the card's ground and going by at its own pace. */
private fun DrawScope.paintCardLayer(
    layer: Layer,
    box: Box,
    ground: Int,
    drift: Long,
) {
    val image = layer.image
    val top = ground + layer.belowGround - image.height
    var x = box.left - (drift % image.width).toInt()
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

private const val TITLE = "CAROFARTY"
private const val NAME_GAP = 5
private const val ROW = 11
private const val PLACE_END = -96
private const val METERS_END = -36
private const val POINTS_END = 40
private const val DAY_START = 52
private const val CARD_GROUND = 20
private const val MENU_GROUND = 2 * TILE
private const val CARD_CARO_X = 22
private const val SECOND = 1000L
private const val FAR_DRIFT = 4L
private const val MID_DRIFT = 8L
private const val GROUND_DRIFT = 14L
