package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import com.fanguskdev.carofarty.ui.render.PixelFont.drawOutlined
import com.fanguskdev.carofarty.ui.render.PixelFont.drawText

/** The veil over a paused run, under its menus (GameMaster's mockup: «ciruela oscura» at 59 %). */
internal fun DrawScope.paintVeil(width: Int, height: Int) {
    drawRect(VEIL, Offset.Zero, Size(width.toFloat(), height.toFloat()))
}

/**
 * The pause (UI-05, GameMaster's mockup): its panel with [title] on top, twice as big, and its buttons in a column,
 * [names] with their [icons] on the left, so the icons go in a column too; [pressed] is the box under the finger.
 */
internal fun DrawScope.paintColumnMenu(
    menu: ColumnMenu,
    title: String,
    names: List<String>,
    icons: List<Icon?>,
    ui: UiArt?,
    pressed: Box?,
) {
    paintPanelBox(menu.panel, ui)
    drawTextAround(title, menu.panel.centreX, menu.titleTop, ui.textColor, ui.shadowColor, MenuLayout.TITLE_SIZE)
    for (i in menu.buttons.indices) {
        val box = menu.buttons[i]
        paintButton(box, names[i], ui, icons.getOrNull(i), pressed = box == pressed, lit = i == 0, fromLeft = true)
    }
}

/** What the settings show (UI-11): each one on or off, and whether the button of the sound silences everything. */
internal class SettingsView(
    var music: Boolean,
    var effects: Boolean,
    var vibration: Boolean,
    var soundOn: Boolean,
)

/** The words of the settings: their title, the name of each, «SÍ» and «NO», and the button of the credits. */
internal class SettingsTexts(
    val title: String,
    val names: List<String>,
    val on: String,
    val off: String,
    val credits: String,
)

/**
 * The settings (UI-11, GameMaster's mockup A): the way back, the ribbon, a panel with a row for the music, the sounds and
 * the vibration, every other one lighter, each with its icon, its name and its «SÍ | NO», and the button of the credits
 * under it. With the sound off by its button, the music and the sounds show dimmed, keeping what they were (GameMaster).
 * Over the sky of the menus or, from the pause, over the run under its veil, which the caller has drawn.
 */
internal fun DrawScope.paintSettings(
    layout: MenuLayout,
    view: SettingsView,
    texts: SettingsTexts,
    ui: UiArt?,
    pressed: Box?,
) {
    paintIconButton(layout.back, Icon.Back, ui, pressed = layout.back == pressed)
    paintRibbon(layout.ribbon(texts.title), texts.title, ui)
    paintPanelBox(layout.settingsPanel, ui)
    val rows = layout.settingsRows
    for (i in rows.indices) {
        val row = rows[i]
        if (i % 2 == 0) fill(row, LIGHT_PLUM)
        val icon = SETTING_ICONS[i]
        val iconSet = ui?.icons
        if (iconSet == null || !drawUiSprite(iconSet, icon.key, row.left + ROW_PAD, row.top + (row.height - MenuLayout.ICON) / 2)) {
            drawText(
                "·",
                row.left + ROW_PAD + MenuLayout.ICON / 2,
                row.top + (row.height - PixelFont.HEIGHT) / 2,
                ui.textColor,
                ui.shadowColor,
            )
        }
        val labelLeft = row.left + ROW_PAD + MenuLayout.ICON + ROW_PAD
        drawText(texts.names[i], labelLeft, row.top + (row.height - PixelFont.HEIGHT) / 2 + 1, ui.textColor, ui.shadowColor)
        val on = when (i) {
            0 -> view.music
            1 -> view.effects
            else -> view.vibration
        }
        paintSwitch(layout, row, on, texts, ui, pressed)
        // The music and the sounds, silenced by the button of the sound: dimmed, but as they are.
        if (!view.soundOn && i < 2) fill(row, DIM)
    }
    paintButton(layout.creditsButton, texts.credits, ui, pressed = layout.creditsButton == pressed)
}

/**
 * The «SÍ | NO» of the setting in [row] (GameMaster's mockup A): the two tabs in a slot, the chosen one raised in raspberry
 * and the other in plum, 2 px lower; the slot covers their bottom, which the tabs bring open. While the finger is on a
 * half, that one shows raised.
 */
private fun DrawScope.paintSwitch(
    layout: MenuLayout,
    row: Box,
    on: Boolean,
    texts: SettingsTexts,
    ui: UiArt?,
    pressed: Box?,
) {
    val slot = layout.switchSlot(row)
    val halves = layout.switchHalves(row)
    val raised = when (pressed) {
        halves[0] -> 0
        halves[1] -> 1
        else -> if (on) 0 else 1
    }
    fill(slot, OUTLINE)
    fill(Box(slot.left + 1, slot.top + 1, slot.width - 2, slot.height - 2), DARK_PLUM)
    val active = ui?.piece("pestana")
    val inactive = ui?.piece("pestana-inactiva")
    clipRect(slot.left.toFloat(), slot.top.toFloat(), slot.right.toFloat(), (slot.bottom - 1).toFloat()) {
        val lower = halves[1 - raised]
        if (inactive != null) drawPiece(inactive, lower) else fill(Box(lower.left, lower.top + 2, lower.width, lower.height - 2), DARK_PLUM)
        val upper = halves[raised]
        if (active != null) drawPiece(active, upper) else fill(upper, GreyPalette.record)
    }
    for (k in halves.indices) {
        val word = if (k == 0) texts.on else texts.off
        val down = if (k == raised) 0 else SWITCH_LOWER
        val color = if (k == raised) ui.textColor else ui.secondaryColor
        drawTextAround(
            word,
            halves[k].left + halves[k].width / 2,
            slot.top + (slot.height - PixelFont.HEIGHT) / 2 + 1 + down,
            color,
            ui.shadowColor,
        )
    }
}

/** The words of the credits: the title, the line of the game, the heading of the licences, the libraries and the button. */
internal class CreditsTexts(
    val title: String,
    val game: String,
    val heading: String,
    val libraries: List<Pair<String, String>>,
    val fullTexts: String,
)

/**
 * The credits (UI-16, GameMaster's mockup; Fangusk: «A FANGUSK.DEV GAME»): the way back, the ribbon, the button of the
 * whole texts of the licences at the top right and a panel with the line of the game, big, a thin line, and the libraries
 * with their licences (LEG-05), the libraries in cream and the licences in light raspberry.
 */
internal fun DrawScope.paintCredits(
    layout: MenuLayout,
    credits: CreditsLayout,
    texts: CreditsTexts,
    ui: UiArt?,
    pressed: Box?,
) {
    paintIconButton(layout.back, Icon.Back, ui, pressed = layout.back == pressed)
    paintRibbon(layout.ribbon(texts.title), texts.title, ui)
    val forward = layout.forward(texts.fullTexts)
    paintButton(forward, texts.fullTexts, ui, pressed = forward == pressed)
    val panel = credits.panel
    paintPanelBox(panel, ui)
    drawTextAround(texts.game, panel.centreX, credits.titleTop, ui.textColor, ui.shadowColor, MenuLayout.TITLE_SIZE)
    fill(credits.rule, LIGHT_PLUM)
    drawTextAround(texts.heading, panel.centreX, credits.headingTop, ui.secondaryColor, ui.shadowColor)
    var y = credits.firstLine
    for ((name, licence) in texts.libraries) {
        drawText(name, credits.namesLeft, y, ui.textColor, ui.shadowColor)
        drawText(licence, credits.licencesRight - PixelFont.width(licence), y, ui.secondaryColor, ui.shadowColor)
        y += PixelFont.LINE
    }
}

/**
 * A page of the whole texts of the licences (LEG-05): the way back, the ribbon, the button to the next page at the top
 * right if there is one ([next] not null), and the [lines] of the page in a panel, with where it is ([where]) at its foot.
 */
internal fun DrawScope.paintLicences(
    layout: MenuLayout,
    title: String,
    next: String?,
    lines: List<String>,
    where: String,
    ui: UiArt?,
    pressed: Box?,
) {
    paintIconButton(layout.back, Icon.Back, ui, pressed = layout.back == pressed)
    paintRibbon(layout.ribbon(title), title, ui)
    if (next != null) {
        val forward = layout.forward(next)
        paintButton(forward, next, ui, pressed = forward == pressed)
    }
    paintPanelBox(layout.licencesPanel, ui)
    val text = layout.licencesText
    var y = text.top
    for (line in lines) {
        drawText(line, text.left, y, ui.textColor, ui.shadowColor)
        y += PixelFont.LINE
    }
    drawTextAround(where, layout.licencesPanel.centreX, layout.licencesFoot, ui.secondaryColor, ui.shadowColor)
}

/**
 * The count before a paused run goes on (GameMaster: «un respiro corto al continuar»): [digit] big in the middle, in gold
 * with its outline, over the run without its veil, so she sees where Caro is.
 */
internal fun DrawScope.paintCountdown(
    digit: Int,
    width: Int,
    height: Int,
    ui: UiArt?,
) {
    val text = digit.toString()
    val x = width / 2 - PixelFont.width(text, COUNTDOWN_SIZE) / 2
    val y = height / 2 - PixelFont.HEIGHT * COUNTDOWN_SIZE / 2
    drawOutlined(text, x, y, ui?.colors?.record ?: GreyPalette.record, ui.shadowColor, COUNTDOWN_SIZE)
}

private fun DrawScope.fill(box: Box, color: Color) {
    drawRect(color, Offset(box.left.toFloat(), box.top.toFloat()), Size(box.width.toFloat(), box.height.toFloat()))
}

/** The colours of the text of the interface, or the prototype's. */
private val UiArt?.textColor: Color get() = this?.colors?.text ?: GreyPalette.text
private val UiArt?.shadowColor: Color get() = this?.colors?.shadow ?: GreyPalette.shadow
private val UiArt?.secondaryColor: Color get() = this?.colors?.secondary ?: GreyPalette.text

/** The icons of the settings, by their rows: the music, the sounds with the loudspeaker of always, and the vibration. */
private val SETTING_ICONS = listOf(Icon.Music, Icon.SoundOn, Icon.Vibration)

/** The palette of the interface (diseno/ui/ui_diseno.py): its outline, «ciruela oscura» and «ciruela clara». */
private val OUTLINE = Color(0xFF24161C)
private val DARK_PLUM = Color(0xFF3A1A34)
private val LIGHT_PLUM = Color(0xFF6E345C)

/** The veil of the pause, «ciruela oscura» at 59 %, and a setting that does not count now, under the same plum. */
private val VEIL = Color(0x963A1A34)
private val DIM = Color(0xA63A1A34)

/** From the side of a row of the settings to its icon, and from the icon to its name. */
private const val ROW_PAD = 6

/** How much lower the word of the tab behind is. */
private const val SWITCH_LOWER = 2
private const val COUNTDOWN_SIZE = 4
