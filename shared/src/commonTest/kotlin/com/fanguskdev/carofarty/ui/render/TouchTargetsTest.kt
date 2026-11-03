package com.fanguskdev.carofarty.ui.render

import com.fanguskdev.carofarty.engine.PixelViewport
import com.fanguskdev.carofarty.game.Scenario
import com.fanguskdev.carofarty.ui.Texts
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * ACC-03: every button, with the room around it that still takes the finger, at least 48 dp on Android and 44 pt on
 * iOS, on screens from Fangusk's Nothing to a small iPhone: the buttons, the pause in its column, the rows and the
 * «SÍ | NO» of the settings and the tabs of the records; and the pause fits on the smallest of them.
 */
class TouchTargetsTest {
    /** A screen: its pixels across and down, how many of them make a dp (or a pt) and the least a button may be. */
    private class Screen(
        val name: String,
        val width: Int,
        val height: Int,
        val density: Float,
        val least: Float,
    )

    private val screens = listOf(
        Screen("Nothing Phone (3)", 2800, 1260, 2.875f, ANDROID),
        Screen("Android 2400 × 1080", 2400, 1080, 2.75f, ANDROID),
        Screen("Android 1920 × 1080", 1920, 1080, 2.625f, ANDROID),
        Screen("Android 1600 × 720", 1600, 720, 2f, ANDROID),
        Screen("iPhone 14", 2532, 1170, 3f, IOS),
        Screen("iPhone SE", 1334, 750, 2f, IOS),
    )

    @Test
    fun everyButtonIsBigEnoughForAFinger() {
        for (screen in screens) {
            val viewport = PixelViewport.fit(screen.width, screen.height)
            val layout = MenuLayout(viewport.width, viewport.height, SafeInsets())
            val units = viewport.scale / screen.density
            fun check(what: String, across: Int, down: Int) {
                val side = minOf(across, down) * units
                assertTrue(side >= screen.least, "${screen.name}: $what of $side, at least ${screen.least}")
            }
            // The buttons with the room around them that the game counts.
            val buttons = listOf(
                layout.corner,
                layout.hudSound,
                layout.back,
                layout.titleSound,
                layout.titleSettings,
                layout.recordsButton,
                layout.howToButton,
                layout.creditsButton,
                layout.forward(Texts.FULL_TEXTS),
                layout.forward(Texts.NEXT),
            )
            for (box in buttons) check("a button", box.width + 2 * Box.SLACK, box.height + 2 * Box.SLACK)
            // The pause in a column: each button with half the gap above and below it (GameMaster).
            val pause = layout.pauseMenu(
                listOf(Texts.RESUME, Texts.RESTART, Texts.SETTINGS, Texts.HOME),
                listOf(Icon.Resume, Icon.Again, Icon.Settings, Icon.Home),
            )
            for (box in pause.buttons) check("a button of the pause", box.width + 2 * Box.SLACK, box.height + MenuLayout.COLUMN_GAP)
            // The settings: each half of a «SÍ | NO» and each row, side by side and one over another, without room around.
            for (row in layout.settingsRows) {
                check("a row of the settings", row.width, row.height)
                for (half in layout.switchTouch(row)) check("a half of a «SÍ | NO»", half.width, half.height)
            }
            // The tabs of the records, side by side, with room above and below.
            for (tab in layout.tabs(Scenario.entries.map { Texts.scenario(it) })) {
                check("a tab of the records", tab.width, tab.height + 2 * MenuLayout.TAB_SLACK)
            }
            assertTrue(pause.panel.left >= 0 && pause.panel.bottom <= viewport.height, "${screen.name}: the pause fits")
        }
    }

    private companion object {
        const val ANDROID = 48f
        const val IOS = 44f
    }
}
