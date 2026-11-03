package com.fanguskdev.carofarty.ui.render

import com.fanguskdev.carofarty.engine.PixelViewport
import com.fanguskdev.carofarty.ui.Texts
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * ACC-03: every button, with the room around it that still takes the finger, at least 48 dp on Android and 44 pt on
 * iOS, on screens from Fangusk's Nothing to a small iPhone; and the row of the pause fits on the narrowest of them.
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
            val pause = layout.row(listOf(Texts.RESUME, Texts.RESTART, Texts.HOME), top = 100, listOf(Icon.Resume, Icon.Again, null))
            val boxes = listOf(layout.corner, layout.hudSound, layout.back, layout.titleSound, layout.recordsButton, layout.howToButton) +
                pause
            for (box in boxes) {
                val side = (minOf(box.width, box.height) + 2 * Box.SLACK) * units
                assertTrue(side >= screen.least, "${screen.name}: a button of $side, at least ${screen.least}")
            }
            assertTrue(pause.first().left >= 0 && pause.last().right <= viewport.width, "${screen.name}: the pause fits")
        }
    }

    private companion object {
        const val ANDROID = 48f
        const val IOS = 44f
    }
}
