package com.fanguskdev.carofarty.ui

import com.fanguskdev.carofarty.game.GameConfig
import com.fanguskdev.carofarty.game.RunPhase
import com.fanguskdev.carofarty.storage.Choices
import com.fanguskdev.carofarty.storage.MemoryStore
import com.fanguskdev.carofarty.storage.Records
import com.fanguskdev.carofarty.ui.render.Box
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The pause in a column (UI-05), its count before going on, and the settings, credits and licences (UI-11, UI-16). */
class PauseAndSettingsTest {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val store = MemoryStore().also { Choices(it).tutorialSeen = true }
    private val choices = Choices(store)
    private var nanos = 0L

    @AfterTest
    fun stop() = scope.cancel()

    private fun session(): GameSession =
        GameSession(GameConfig(), art = null, audio = null, scope = scope, records = Records(store), choices = choices, debugBuild = false)

    private fun GameSession.frames(seconds: Double) {
        repeat((seconds * FPS).toInt()) {
            nanos += NANOS / FPS
            onFrame(nanos)
        }
    }

    /** A finger down and up at ([x], [y]) in game pixels. */
    private fun GameSession.tapAt(x: Float, y: Float) {
        fingerDown(viewport.offsetX + x * viewport.scale, viewport.offsetY + y * viewport.scale, first = true)
        fingersUp()
        frames(1.0 / FPS)
    }

    private fun GameSession.tap(box: Box) = tapAt(box.left + box.width / 2f, box.top + box.height / 2f)

    /** The start screen, from the title screen, and a run in the Bosque, going. */
    private fun GameSession.running(): GameSession {
        frames(0.1)
        tapAt(10f, 100f)
        tap(layout.cards[0])
        frames(0.5)
        assertEquals(RunPhase.Running, run.phase)
        return this
    }

    @Test
    fun aTapOutsideTheMenuNoLongerGoesOnAndContinueCountsThreeTwoOne() {
        val session = session().running()
        session.tap(session.layout.corner)
        assertTrue(session.paused)
        val menu = assertNotNull(session.pauseMenu)
        // Outside the menu, nothing.
        session.tapAt(4f, session.layout.height - 4f)
        assertTrue(session.paused)
        assertNotNull(session.pauseMenu)
        // «CONTINUAR»: 3, 2, 1, and on.
        session.tap(menu.buttons[0])
        assertNull(session.pauseMenu)
        assertEquals(3, session.countdown)
        val meters = session.run.meters
        session.frames(0.4)
        assertEquals(2, session.countdown)
        assertEquals(meters, session.run.meters, "still while it counts")
        session.frames(0.7)
        assertFalse(session.paused)
        assertEquals(0, session.countdown)
        session.frames(0.3)
        assertTrue(session.run.meters > meters, "and on")
    }

    @Test
    fun theBackButtonGoesOnFromThePauseAndBackToItFromTheCount() {
        val session = session().running()
        session.back()
        assertNotNull(session.pauseMenu)
        session.back()
        assertEquals(3, session.countdown)
        session.back()
        assertNotNull(session.pauseMenu, "the count stopped, the menu again")
        assertEquals(0, session.countdown)
        // The app going away in the middle of the count leaves it in the menu.
        session.tap(assertNotNull(session.pauseMenu).buttons[0])
        session.background()
        assertNotNull(session.pauseMenu)
    }

    @Test
    fun theSettingsOfThePauseGoBackToIt() {
        val session = session().running()
        session.back()
        session.tap(assertNotNull(session.pauseMenu).buttons[2])
        assertEquals(Sheet.Settings, session.sheet)
        session.tap(session.layout.creditsButton)
        assertEquals(Sheet.Credits, session.sheet)
        session.back()
        assertEquals(Sheet.Settings, session.sheet)
        session.tap(session.layout.back)
        assertNull(session.sheet)
        assertTrue(session.paused)
        assertNotNull(session.pauseMenu)
    }

    @Test
    fun eachHalfChoosesItsOwnAndTheRestOfTheRowChanges() {
        val session = session()
        session.frames(0.1)
        session.tapAt(10f, 100f)
        var vibrations = 0
        session.vibrate = { vibrations++ }
        session.tap(session.layout.titleSettings)
        assertEquals(Sheet.Settings, session.sheet)
        assertTrue(session.backEnabled)
        val rows = session.layout.settingsRows
        session.tap(session.layout.switchTouch(rows[2])[1])
        assertFalse(choices.vibrationOn, "«NO» turns the vibration off")
        session.tap(session.layout.switchTouch(rows[2])[1])
        assertFalse(choices.vibrationOn, "and «NO» again leaves it off")
        session.tapAt(rows[2].left + 20f, rows[2].top + 13f)
        assertTrue(choices.vibrationOn, "the rest of the row turns it on")
        assertEquals(1, vibrations, "turned on, it shakes once")
        session.tap(session.layout.switchTouch(rows[0])[1])
        assertFalse(choices.musicOn)
        session.tap(session.layout.switchTouch(rows[1])[1])
        assertFalse(choices.effectsOn)
        assertTrue(choices.soundOn, "the button of the sound stays as it was")
        session.back()
        assertNull(session.sheet)
        assertFalse(session.backEnabled, "on the start screen the phone leaves the game")
    }

    @Test
    fun withTheSoundOffTheMusicAndTheSoundsDoNothing() {
        choices.soundOn = false
        val session = session()
        session.frames(0.1)
        session.tapAt(10f, 100f)
        session.tap(session.layout.titleSettings)
        val rows = session.layout.settingsRows
        session.tap(session.layout.switchTouch(rows[0])[1])
        session.tap(session.layout.switchTouch(rows[1])[1])
        assertTrue(choices.musicOn && choices.effectsOn, "dimmed, they keep their values")
        assertFalse(session.settings.soundOn)
    }

    @Test
    fun theTextsOfTheLicencesComeFromTheCredits() {
        val session = session()
        session.frames(0.1)
        session.tapAt(10f, 100f)
        session.tap(session.layout.titleSettings)
        session.tap(session.layout.creditsButton)
        session.tap(session.layout.forward(Texts.FULL_TEXTS))
        assertEquals(Sheet.Licences, session.sheet)
        assertEquals(0, session.licencePage)
        session.back()
        assertEquals(Sheet.Credits, session.sheet)
    }

    private companion object {
        const val FPS = 60
        const val NANOS = 1_000_000_000L
    }
}
