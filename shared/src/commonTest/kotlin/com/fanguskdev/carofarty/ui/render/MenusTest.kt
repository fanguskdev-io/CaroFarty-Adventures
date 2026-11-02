package com.fanguskdev.carofarty.ui.render

import com.fanguskdev.carofarty.engine.PixelViewport
import com.fanguskdev.carofarty.game.Scenario
import com.fanguskdev.carofarty.ui.Texts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The layout of the menus (UI-04, UI-07, UI-20) and of the title screen on every screen the game can have, with and without insets. */
class MenusTest {
    private val screens = listOf(
        PixelViewport.MIN_WIDTH to PixelViewport.MIN_HEIGHT,
        400 to 180,
        448 to 252,
        PixelViewport.MAX_WIDTH to PixelViewport.MAX_HEIGHT,
        360 to PixelViewport.MAX_HEIGHT,
    )
    private val insets = listOf(SafeInsets(), SafeInsets(left = 30), SafeInsets(right = 30), SafeInsets(top = 10))
    private val names = Scenario.entries.map { Texts.scenario(it) }

    private fun layouts(): List<MenuLayout> = screens.flatMap { (width, height) -> insets.map { MenuLayout(width, height, it) } }

    private fun MenuLayout.has(box: Box): Boolean = box.left >= 0 && box.top >= 0 && box.right <= width && box.bottom <= height

    private fun Box.overlaps(other: Box): Boolean = left < other.right && other.left < right && top < other.bottom && other.top < bottom

    @Test
    fun everythingOfTheMenusIsOnTheScreen() {
        for (layout in layouts()) {
            val boxes = listOf(
                layout.corner,
                layout.hudSound,
                layout.back,
                layout.exit,
                layout.title,
                layout.recordsButton,
                layout.howToButton,
                layout.titleSound,
                layout.recordsPanel,
                layout.howToPanel,
                layout.forward(Texts.UNDERSTOOD),
                layout.ribbon(Texts.HOW_TO_TITLE),
                layout.introScene,
                layout.prompt(Texts.TAP_TO_START),
            ) + layout.cards + layout.frames + layout.tabs(names)
            for (box in boxes) assertTrue(layout.has(box), "$box on ${layout.width} × ${layout.height}, ${layout.insets}")
            val dots = layout.howToPanel.bottom + MenuLayout.DOTS_GAP + 7
            assertTrue(dots <= layout.height, "the page dots on ${layout.width} × ${layout.height}")
        }
    }

    @Test
    fun nothingOfTheMenusIsOnTopOfSomethingElse() {
        for (layout in layouts()) {
            val start = listOf(layout.exit, layout.title, layout.recordsButton, layout.howToButton, layout.titleSound) + layout.cards
            for (i in start.indices) {
                for (j in i + 1 until start.size) assertFalse(start[i].overlaps(start[j]), "${start[i]} and ${start[j]}, ${layout.width}")
            }
            val header = listOf(layout.back, layout.ribbon(Texts.HOW_TO_TITLE), layout.forward(Texts.UNDERSTOOD))
            for (i in header.indices) {
                for (j in i + 1 until header.size) assertFalse(header[i].overlaps(header[j]), "${header[i]} and ${header[j]}")
            }
            assertFalse(layout.ribbon(Texts.RECORDS_TITLE).overlaps(layout.tabs(names).first()), "the ribbon and the tabs")
            assertFalse(layout.frames[0].overlaps(layout.frames[1]), "the two frames of how to play")
            assertFalse(layout.hudSound.overlaps(layout.corner), "the sound and the pause")
            val intro = listOf(layout.title, layout.prompt(Texts.TAP_TO_START), layout.introScene)
            for (i in intro.indices) {
                for (j in i + 1 until intro.size) assertFalse(intro[i].overlaps(intro[j]), "${intro[i]} and ${intro[j]}, ${layout.height}")
            }
        }
    }

    @Test
    fun caroFitsInTheScenarioOfTheTitleScreen() {
        for (layout in layouts()) {
            val scene = layout.introScene
            assertTrue(layout.introGround - CARO_HEIGHT > scene.top, "her head under the sky of the menus on ${layout.height}")
            assertTrue(layout.introGround < scene.bottom, "the ground on the screen")
        }
    }

    @Test
    fun theSmallButtonsKeepClearOfARoundedCorner() {
        val layout = MenuLayout(400, 180, SafeInsets())
        assertEquals(400 - 8 - 20, layout.corner.left)
        assertEquals(7, layout.corner.top)
        assertEquals(8, layout.back.left)
        assertEquals(layout.corner.left - 4 - 20, layout.hudSound.left)
    }

    @Test
    fun theNumbersOfTheRecordsHaveADotEveryThreeFigures() {
        assertEquals("0", thousands(0))
        assertEquals("999", thousands(999))
        assertEquals("1.284", thousands(1284))
        assertEquals("12.345", thousands(12345))
        assertEquals("1.000.000", thousands(1_000_000))
    }
}
