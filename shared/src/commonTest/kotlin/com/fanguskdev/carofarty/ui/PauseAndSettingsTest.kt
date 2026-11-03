package com.fanguskdev.carofarty.ui

import com.fanguskdev.carofarty.audio.AudioFocus
import com.fanguskdev.carofarty.engine.PixelViewport
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
        frames(DIP)
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
    fun thePauseFollowsTheScreenIfItChangesWhileItWaits() {
        // The lock screen is upright: the game is measured that way while it waits, and comes back to its own (QA-07).
        val session = session().running()
        session.back()
        val before = assertNotNull(session.pauseMenu).panel
        session.viewport = PixelViewport.fit(1600, 720)
        val after = assertNotNull(session.pauseMenu)
        assertTrue(after.panel != before && after.panel.right <= session.layout.width, "laid out again: $after.panel")
        session.tap(after.buttons[0])
        assertEquals(3, session.countdown, "its buttons where they are drawn")
    }

    /** The phone's sound as the test sees it: whether the game holds it, and a way to take it from the game. */
    private class FakeFocus : AudioFocus {
        var held = false
        override var listener: AudioFocus.Listener? = null

        override fun hold(holding: Boolean) {
            held = holding
        }

        fun takeAway() {
            held = false
            listener?.lost()
        }
    }

    @Test
    fun aCallThatRingsPausesTheRunAndTheMusicOffLetsTheSoundGo() {
        // QA-07, Fangusk: «Okey adelante». The audio is not played here, so the session is given the focus as if it was.
        val focus = FakeFocus()
        val session =
            GameSession(GameConfig(), null, null, scope, Records(store), choices, debugBuild = false, focus = focus).running()
        assertTrue(focus.held, "with its music on, the game holds the phone's sound")
        focus.takeAway()
        assertTrue(session.paused)
        assertNotNull(session.pauseMenu, "paused with its menu, as when the app goes away")
        session.background()
        assertFalse(focus.held, "away, it lets it go")
        session.foreground()
        assertTrue(focus.held)
        session.tap(assertNotNull(session.pauseMenu).buttons[2])
        session.tap(session.layout.switchTouch(session.layout.settingsRows[0])[1])
        assertFalse(focus.held, "with its music off, the music of other apps plays along")
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
        session.frames(DIP)
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
        session.frames(DIP)
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
        session.frames(DIP)
        session.tap(session.layout.titleSettings)
        session.tap(session.layout.creditsButton)
        session.tap(session.layout.forward(Texts.FULL_TEXTS))
        assertEquals(Sheet.Licences, session.sheet)
        assertEquals(0, session.licencePage)
        session.back()
        assertEquals(Sheet.Credits, session.sheet)
    }

    @Test
    fun aScreenGoesToPlumAndTheNextComesOutOfIt() {
        // VFX-05: the change happens under the plum, and the screen that goes takes no taps meanwhile.
        val session = session()
        session.frames(0.1)
        session.tapAt(10f, 100f)
        assertEquals(Screen.Intro, session.screen, "still the title screen while it goes to plum")
        assertTrue(session.dip.closing && session.dip.shade > 0f)
        session.tapAt(10f, 100f)
        session.frames(0.1)
        assertEquals(Screen.Title, session.screen)
        assertTrue(session.dip.shade in 0f..1f)
        session.frames(0.2)
        assertEquals(0f, session.dip.shade)
    }

    @Test
    fun aJumpLeavesItsDustWhereSheJumpedFrom() {
        val session = session().running()
        val x = session.run.caro.x
        session.tapAt(200f, 100f)
        session.frames(0.1)
        assertEquals(1, session.effects.size, "the dust of the jump")
        assertTrue(session.run.caro.x > x)
    }

    @Test
    fun slowFramesTrimTheLeavesFirstAndThenTheRest() {
        val budget = EffectsBudget()
        repeat(2 * FPS * 2) { budget.add(1.0 / FPS) }
        assertEquals(0, budget.level, "on time")
        // Late frames, 25 a second: two windows of 2 s in a row and the leaves go; two more and the rest of the decoration.
        var trimmed = false
        repeat(6 * SLOW_FPS) { trimmed = budget.add(1.0 / SLOW_FPS) || trimmed }
        assertTrue(trimmed)
        assertEquals(1, budget.level)
        assertFalse(budget.leaves)
        assertTrue(budget.trail)
        repeat(8 * SLOW_FPS) { budget.add(1.0 / SLOW_FPS) }
        assertEquals(2, budget.level)
        assertFalse(budget.trail || budget.haze)
        repeat(16 * SLOW_FPS) { budget.add(1.0 / SLOW_FPS) }
        assertEquals(2, budget.level, "never the game")
    }

    private companion object {
        const val FPS = 60
        const val SLOW_FPS = 25

        /** Long enough for the dip between two screens (VFX-05). */
        const val DIP = 0.3
        const val NANOS = 1_000_000_000L
    }
}
