package com.fanguskdev.carofarty.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import carofarty.shared.generated.resources.Res
import com.fanguskdev.carofarty.audio.GameAudio
import com.fanguskdev.carofarty.audio.RescueStreak
import com.fanguskdev.carofarty.audio.Sound
import com.fanguskdev.carofarty.engine.FixedStep
import com.fanguskdev.carofarty.engine.PixelViewport
import com.fanguskdev.carofarty.engine.TouchInput
import com.fanguskdev.carofarty.game.BufferedPieces
import com.fanguskdev.carofarty.game.CaroState
import com.fanguskdev.carofarty.game.CourseGenerator
import com.fanguskdev.carofarty.game.EndCause
import com.fanguskdev.carofarty.game.GameConfig
import com.fanguskdev.carofarty.game.GameRun
import com.fanguskdev.carofarty.game.ItemKind
import com.fanguskdev.carofarty.game.JumpLab
import com.fanguskdev.carofarty.game.Pilot
import com.fanguskdev.carofarty.game.Reaction
import com.fanguskdev.carofarty.game.RunEvent
import com.fanguskdev.carofarty.game.RunPhase
import com.fanguskdev.carofarty.game.Scenario
import com.fanguskdev.carofarty.game.TILE
import com.fanguskdev.carofarty.storage.Choices
import com.fanguskdev.carofarty.storage.RecordRun
import com.fanguskdev.carofarty.storage.Records
import com.fanguskdev.carofarty.storage.RunResult
import com.fanguskdev.carofarty.storage.today
import com.fanguskdev.carofarty.ui.render.Art
import com.fanguskdev.carofarty.ui.render.BackgroundPlan
import com.fanguskdev.carofarty.ui.render.Box
import com.fanguskdev.carofarty.ui.render.CaroLook
import com.fanguskdev.carofarty.ui.render.CaroPose
import com.fanguskdev.carofarty.ui.render.ColumnMenu
import com.fanguskdev.carofarty.ui.render.Confetti
import com.fanguskdev.carofarty.ui.render.CreditsLayout
import com.fanguskdev.carofarty.ui.render.Icon
import com.fanguskdev.carofarty.ui.render.MenuLayout
import com.fanguskdev.carofarty.ui.render.PanelLine
import com.fanguskdev.carofarty.ui.render.SafeInsets
import com.fanguskdev.carofarty.ui.render.SettingsView
import com.fanguskdev.carofarty.ui.render.Tone
import com.fanguskdev.carofarty.ui.render.backgroundPlan
import com.fanguskdev.carofarty.ui.render.licencePages
import com.fanguskdev.carofarty.ui.render.panelBottom
import com.fanguskdev.carofarty.ui.render.thousands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * The screens of the game (UI-01): the title screen it opens on ([Intro], UI-07), the start screen with the scenarios
 * ([Title]), the records, how to play and the run.
 */
internal enum class Screen {
    Intro,
    Title,
    Records,
    HowTo,
    Game,
}

/**
 * What can open over the start screen or over the pause (UI-11, UI-16, LEG-05): the settings, the credits from them and
 * the whole texts of the licences from the credits.
 */
internal enum class Sheet {
    Settings,
    Credits,
    Licences,
}

/**
 * What the game keeps between frames: the screen it is on, the current run, the finger, the fixed step, the pause, the end
 * of the run against the records, Caro's look, the sound and the debugging numbers. The screen calls [onFrame] once per
 * frame and [frame] makes it redraw; nothing here allocates per frame except the debugging line, which only debug builds
 * show (TEC-17). In debug builds started with the autopilot, the [Pilot] plays instead of the finger, from the first
 * frame and again on its own when a run ends. [art] is null when there is none: then everything is drawn as the grey
 * prototype; and [audio] when the game plays no sound, as in the tests.
 *
 * The way through the screens (Fangusk, after playing the beta): the game opens on the title screen, and a tap there
 * leads to how to play the first time and to the start screen after; a card there starts a run in its scenario; the end
 * of a run offers another one or the start screen, never the title screen again.
 * A button shows pressed while the finger is on it and does its thing when the finger goes up, as GameMaster's pressed
 * pieces ask; only the jump goes with the finger down.
 */
internal class GameSession(
    private val config: GameConfig,
    val art: Art?,
    private val audio: GameAudio?,
    private val scope: CoroutineScope,
    private val records: Records,
    private val choices: Choices,
    private val debugBuild: Boolean,
    debugAtStart: Boolean = false,
    autopilot: Boolean = false,
    private val openedAt: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow(),
    private val quit: (() -> Unit)? = null,
) {
    var scenario = choices.scenario
        private set
    var run: GameRun = createRun()
        private set

    /** When the background shows its variants in this run, if the scenario has any (the cabin of the snow). */
    var backgroundPlan: BackgroundPlan? = planFor(run)
        private set
    private val autopilot = debugBuild && autopilot
    private var pilot: Pilot? = if (this.autopilot) Pilot(run) else null
    val input = TouchInput()
    private val clock = FixedStep()

    /** The screen it is on. */
    var screen = if (this.autopilot) Screen.Title else Screen.Intro
        private set

    /** Read by the drawing so it redraws every frame. */
    val frame = mutableLongStateOf(0L)

    /**
     * Whether the back button has something to do, for the platform: Compose state, so it can enable and disable it. On the
     * title screen and on the start screen it has nothing to do, and the phone leaves the game.
     */
    var backEnabled by mutableStateOf(false)
        private set
    var paused = false
        private set
    var showDebug = debugBuild && debugAtStart
        private set
    var alpha = 0f
        private set
    var result: RunResult? = null
        private set

    var viewport = PixelViewport.fit(MIN_SCREEN_WIDTH, MIN_SCREEN_HEIGHT)
    var insets = SafeInsets()

    /** Where everything of the menus goes on this screen, made again when the screen changes. */
    val layout: MenuLayout
        get() {
            val current = laidOut
            if (current != null && current.width == viewport.width && current.height == viewport.height && current.insets == insets) {
                return current
            }
            return MenuLayout(viewport.width, viewport.height, insets).also { laidOut = it }
        }
    private var laidOut: MenuLayout? = null

    var pointsText = ""
        private set

    /** Which of Caro's animations shows, stepped with the run (CHR-02 to CHR-07). */
    val look = lookOf(art)

    /** The names of the scenarios, for the cards and the tabs (UI-20). */
    val scenarioNames = Scenario.entries.map { Texts.scenario(it) }

    /** Whether the sound is on: the button by the pause and on the start screen, remembered (Fangusk). */
    var soundOn = choices.soundOn
        private set

    /** The sheet open over the start screen or the pause, if any. */
    var sheet: Sheet? = null
        private set

    /** What the settings show: the music, the sounds and the vibration (UI-11, TEC-09), and the button of the sound. */
    val settings = SettingsView(choices.musicOn, choices.effectsOn, choices.vibrationOn, soundOn)

    /** The texts of the licences, read when the credits open, and the page of them that shows, from 0. */
    private var licenceTexts: List<String>? = null
    private var pagesMade: List<List<String>> = emptyList()
    private var pagesFor: MenuLayout? = null
    var licencePage = 0
        private set

    /** Where the page of the licences is («3 DE 26»), made when it changes. */
    var licenceWhere = ""
        private set

    /** The credits as they go on this screen, made again when the screen changes. */
    val creditsLayout: CreditsLayout
        get() {
            val layout = layout
            val made = credits
            if (made != null && creditsFor === layout) return made
            creditsFor = layout
            return layout.credits(Texts.CREDITS_LINE, Texts.libraries).also { credits = it }
        }
    private var credits: CreditsLayout? = null
    private var creditsFor: MenuLayout? = null

    /** The menu of the pause in a column (UI-05), while it shows, and its buttons in GameMaster's order with their icons. */
    var pauseMenu: ColumnMenu? = null
        private set
    val pauseTexts = listOf(Texts.RESUME, Texts.RESTART, Texts.SETTINGS, Texts.HOME)
    val pauseIcons = listOf(Icon.Resume, Icon.Again, Icon.Settings, Icon.Home)

    /** The seconds left of the count before a paused run goes on, or 0; and the digit it shows, 3, 2 and 1. */
    private var resumeIn = 0.0
    val countdown: Int get() = if (resumeIn > 0.0) ceil(resumeIn / RESUME_STEP).toInt() else 0

    /** The counter of a new record in the panel of the end (UI-15), what it shows, and the confetti when it arrives. */
    private var recordCounter: PanelLine? = null
    private var counterShown = -1
    val confetti = Confetti()
    private var confettiStarted = false

    /** The scenario whose records show, and its best runs. */
    var recordsScenario = scenario
        private set
    var recordRuns: List<RecordRun> = emptyList()
        private set

    /** The page of how to play that shows, from 0, and its two small runs. */
    var howToPage = 0
        private set
    var tutorial: List<TutorialScene> = emptyList()
        private set

    /** The buttons under the panel of the pause or of the end of a run: another run at the end, and the start screen. */
    var panelButtons: List<Box> = emptyList()
        private set
    var panelButtonTexts: List<String> = emptyList()
        private set
    var panelButtonIcons: List<Icon?> = emptyList()
        private set

    /** Whether the start screen has the button to leave the game: on Android and the desktop, not on iOS (UI-22). */
    val canQuit: Boolean get() = quit != null

    /** Whether the start screen is asking whether to leave the game (Fangusk chose to ask), with its panel and buttons. */
    var askingToQuit = false
        private set
    val quitPanel = listOf(PanelLine(Texts.QUIT_QUESTION, size = 2))
    val quitButtonTexts = listOf(Texts.YES, Texts.NO)
    val quitButtons: List<Box>
        get() = layout.row(quitButtonTexts, panelBottom(quitPanel, viewport.height) + MenuLayout.GAP, atLeast = MenuLayout.ANSWER_WIDTH)

    /** The button under the finger, drawn pressed, and what it does when the finger goes up. */
    var pressed: Box? = null
        private set
    private var onRelease: (() -> Unit)? = null

    /** The time of the animations, in milliseconds: it stops in the pause. */
    val millis: Long get() = (animationTime * MILLIS).toLong()

    /** Whether the points are running out, for the marker in red (guía 1, principio 8). */
    val lowPoints: Boolean get() = run.phase != RunPhase.Ready && run.points <= config.score.warnAtPoints
    var metersText = ""
        private set

    /** Whether the hint to hold the finger shows: at the first gaps of her first runs (GDD-17, UI-12). */
    var hint = false
        private set

    /** How full the meter of the hint is, from 0 to 1: how much of the longest hold her jump has had. */
    var hintFill = 0f
        private set

    /**
     * The debugging lines: the frames, what they cost and how long the game took to open (MOB-02), the jumps that came from
     * the coyote time and the buffer (PLY-04), and the run.
     */
    val debugLines = Array(DEBUG_LINES) { "" }
    var panel: List<PanelLine> = emptyList()
        private set

    private var shownPoints = -1
    private var shownMeters = -1
    private var time = 0.0
    private var animationTime = 0.0
    private var overAt = 0.0
    private var lastNanos = -1L
    private val fps = FpsMeter()
    private val logic = Smoothed()
    private val drawing = Smoothed()
    private var startupMillis = -1L
    private var debugFrames = 0

    /** The best distance in the scenario when the run began, to hear the moment she beats it. */
    private var bestBefore = records.meters(scenario)
    private var recordHeard = false

    /** Steps left to know whether the jump is a long one, or 0. */
    private var longJumpSteps = 0

    /** The rescues in a row, for the pitch of their sound, and the seconds the run has gone on, to time them. */
    private val streak = RescueStreak()
    private var runSeconds = 0.0

    /** The start of the gap ahead and how many gaps the run has come to, for the hint. */
    private var hintGap = Int.MIN_VALUE
    private var gapsMet = 0

    init {
        applySound()
        if (this.autopilot) startRun(scenario) else audio?.music(scenario)
    }

    fun onFrame(nanos: Long) {
        val elapsed = if (lastNanos < 0) 0.0 else (nanos - lastNanos) / NANOS
        lastNanos = nanos
        time += elapsed
        fps.add(elapsed)
        if (startupMillis < 0) startupMillis = openedAt.elapsedNow().inWholeMilliseconds
        val started = TimeSource.Monotonic.markNow()
        if (!paused) {
            when (screen) {
                Screen.Game -> alpha = clock.advance(elapsed) { step() }
                Screen.HowTo -> alpha = clock.advance(elapsed) { for (i in tutorial.indices) tutorial[i].step() }
                else -> input.consumePress()
            }
            animationTime += elapsed
        } else if (resumeIn > 0.0) {
            // The count before going on: a frame that takes long does not swallow it.
            resumeIn -= min(elapsed, LONGEST_FRAME)
            if (resumeIn <= 0.0) goOn()
        }
        countRecord()
        if (confetti.falling) confetti.step(min(elapsed, LONGEST_FRAME).toFloat())
        if (showDebug) logic.add(started.elapsedNow())
        refreshTexts()
        frame.longValue = nanos
    }

    /** How long drawing the last frame took, for the debugging line. */
    fun drawn(took: Duration) {
        if (showDebug) drawing.add(took)
    }

    /** A finger went down at ([x], [y]) in pixels of the phone; [first] says whether it is the only one. */
    fun fingerDown(x: Float, y: Float, first: Boolean) {
        val gameX = (x - viewport.offsetX) / viewport.scale
        val gameY = (y - viewport.offsetY) / viewport.scale
        if (!first) {
            if (debugBuild) showDebug = !showDebug
            return
        }
        pressed = null
        onRelease = null
        when (screen) {
            // A tap anywhere: the screen is its button.
            Screen.Intro -> press(null) { leaveIntro() }
            Screen.Title -> if (sheet != null) sheetTap(gameX, gameY) else titleTap(gameX, gameY)
            Screen.Records -> recordsTap(gameX, gameY)
            Screen.HowTo -> howToTap(gameX, gameY)
            Screen.Game -> gameTap(gameX, gameY)
        }
    }

    /** The fingers went up: the jump ends and the button that was pressed, if any, does its thing. */
    fun fingersUp() {
        input.release()
        val action = onRelease ?: return
        pressed = null
        onRelease = null
        action()
    }

    /** [box] shows pressed until the finger goes up, and then [action] happens. */
    private fun press(box: Box?, action: () -> Unit) {
        pressed = box
        onRelease = action
    }

    /**
     * The back button of the phone: a sheet back to where it came from; in a run, the pause, and from the pause the run goes
     * on; a page back in how to play, and else the start screen.
     */
    fun back() {
        val running = screen == Screen.Game && run.phase == RunPhase.Running
        when {
            askingToQuit -> stopAskingToQuit()
            sheet != null -> closeSheet()
            running && paused && resumeIn > 0.0 -> pause()
            running && paused -> resume()
            running -> pause()
            screen == Screen.HowTo -> goBackInHowTo()
            screen == Screen.Records || screen == Screen.Game -> goHome()
        }
    }

    /**
     * Pauses the run, if it is going, with its menu: the pause button, the app going to the background or the back button
     * (MOB-05); and back to the menu from the count before going on. Its settings, if open, stay.
     */
    fun pause() {
        if (screen != Screen.Game || run.phase != RunPhase.Running) return
        if (paused && resumeIn <= 0.0) return
        paused = true
        resumeIn = 0.0
        input.clear()
        // Its menu (UI-05, GameMaster's mockup): go on, the same scenario from the start, the settings or the start screen.
        pauseMenu = layout.pauseMenu(pauseTexts, pauseIcons)
        audio?.pause()
    }

    /** The app went to the background: the run pauses and the music stops. */
    fun background() {
        pause()
        audio?.pause()
    }

    /** The app came back: the music goes on, unless the run waits in its pause or has just ended. */
    fun foreground() {
        if (!paused && !(screen == Screen.Game && run.phase == RunPhase.Over)) audio?.resume()
    }

    private fun titleTap(x: Float, y: Float) {
        val layout = layout
        if (askingToQuit) {
            val buttons = quitButtons
            when {
                buttons[0].contains(x, y) -> press(buttons[0]) { quit?.invoke() }
                buttons[1].contains(x, y) -> press(buttons[1]) { stopAskingToQuit() }
            }
            return
        }
        when {
            canQuit && layout.exit.contains(x, y) -> press(layout.exit) { askToQuit() }
            layout.titleSound.contains(x, y) -> press(layout.titleSound) { toggleSound() }
            layout.titleSettings.contains(x, y) -> press(layout.titleSettings) { openSettings() }
            layout.recordsButton.contains(x, y) -> press(layout.recordsButton) { openRecords() }
            layout.howToButton.contains(x, y) -> press(layout.howToButton) { openHowTo() }
            else -> {
                val card = layout.cards.indexOfFirst { it.contains(x, y, slack = 0) }
                if (card >= 0) press(layout.cards[card]) { startRun(Scenario.entries[card]) }
            }
        }
    }

    private fun recordsTap(x: Float, y: Float) {
        if (layout.back.contains(x, y)) {
            press(layout.back) { goHome() }
            return
        }
        // The tabs side by side, with room for the finger above and below (GameMaster: 16 px are short).
        val tab = layout.tabs(scenarioNames).indexOfFirst { it.contains(x, y, across = 0, up = MenuLayout.TAB_SLACK) }
        if (tab >= 0 && tab != recordsScenario.ordinal) {
            press(null) {
                audio?.play(Sound.Tap)
                recordsScenario = Scenario.entries[tab]
                recordRuns = records.best(recordsScenario)
            }
        }
    }

    /** How to play: the way back, the button forward, or forward too from a tap anywhere else. */
    private fun howToTap(x: Float, y: Float) {
        val layout = layout
        val forward = layout.forward(forwardText())
        when {
            layout.back.contains(x, y) -> press(layout.back) { goBackInHowTo() }
            forward.contains(x, y) -> press(forward) { turnPage() }
            else -> press(null) { turnPage() }
        }
    }

    private fun gameTap(x: Float, y: Float) {
        if (paused) {
            pausedTap(x, y)
            return
        }
        if (run.phase == RunPhase.Over) {
            if (time - overAt < RETRY_DELAY) return
            // Another run in the same scenario, from its button or a tap anywhere else (UI-06), or the start screen.
            val again = panelButtons.getOrNull(0)
            val home = panelButtons.getOrNull(1)
            when {
                home != null && home.contains(x, y) -> press(home) { goHome() }
                again != null && again.contains(x, y) -> press(again) { restart() }
                else -> press(null) { restart() }
            }
            return
        }
        // With the debugging tools open, a tap on the points takes some away, to see the end of a run without points.
        if (showDebug && run.phase == RunPhase.Running && y < insets.top + DEBUG_CORNER && x < insets.left + DEBUG_CORNER * 3) {
            run.takeAway(DEBUG_TAKE_AWAY)
            handleEvents()
            return
        }
        val layout = layout
        if (run.phase == RunPhase.Running && layout.hudSound.contains(x, y)) {
            press(layout.hudSound) { toggleSound() }
            return
        }
        if (run.phase == RunPhase.Running && layout.corner.contains(x, y)) {
            press(layout.corner) {
                if (run.phase == RunPhase.Running) audio?.play(Sound.Tap)
                pause()
            }
            return
        }
        input.press()
    }

    /**
     * In the pause: its settings if open; the buttons of its menu, each with half the gap above and below it; and the
     * buttons of the marker under the veil, the sound and the pause, which goes on too (GameMaster). A tap anywhere else
     * does nothing: only «CONTINUAR» or the pause button go on. In the count before going on, the pause button stops it.
     */
    private fun pausedTap(x: Float, y: Float) {
        if (sheet != null) {
            sheetTap(x, y)
            return
        }
        val layout = layout
        if (layout.corner.contains(x, y)) {
            press(layout.corner) { if (resumeIn > 0.0) pause() else resume() }
            return
        }
        if (layout.hudSound.contains(x, y)) {
            press(layout.hudSound) { toggleSound() }
            return
        }
        val buttons = pauseMenu?.buttons ?: return
        when (buttons.indexOfFirst { it.contains(x, y, Box.SLACK, MenuLayout.COLUMN_GAP / 2) }) {
            0 -> press(buttons[0]) { resume() }
            1 -> press(buttons[1]) { restartFromPause() }
            2 -> press(buttons[2]) { openSettings() }
            3 -> press(buttons[3]) { goHome() }
        }
    }

    /** On a sheet: the settings, the credits or a page of the licences. */
    private fun sheetTap(x: Float, y: Float) {
        val layout = layout
        if (layout.back.contains(x, y)) {
            press(layout.back) { closeSheet() }
            return
        }
        when (sheet) {
            Sheet.Settings -> settingsTap(x, y)
            Sheet.Credits -> {
                val forward = layout.forward(Texts.FULL_TEXTS)
                if (forward.contains(x, y)) press(forward) { openLicences() }
            }
            Sheet.Licences -> {
                // The button forward, or a tap anywhere on the page, to the next one; from the last, nowhere.
                val forward = layout.forward(Texts.NEXT)
                if (licencePage + 1 < licencePages().size) press(forward.takeIf { it.contains(x, y) }) { turnLicencePage(1) }
            }
            null -> Unit
        }
    }

    /**
     * The settings (UI-11, GameMaster's mockup A): each half of a «SÍ | NO» chooses its own, the rest of the row changes
     * from one to the other; the music and the sounds, dimmed while the button of the sound silences everything, do
     * nothing then. And the credits.
     */
    private fun settingsTap(x: Float, y: Float) {
        val layout = layout
        if (layout.creditsButton.contains(x, y)) {
            press(layout.creditsButton) { openCredits() }
            return
        }
        val rows = layout.settingsRows
        val index = rows.indexOfFirst { it.contains(x, y, slack = 0) }
        if (index < 0 || (index < 2 && !soundOn)) return
        val row = rows[index]
        val halves = layout.switchHalves(row)
        val touch = layout.switchTouch(row)
        when {
            touch[0].contains(x, y, slack = 0) -> press(halves[0]) { setSetting(index, true) }
            touch[1].contains(x, y, slack = 0) -> press(halves[1]) { setSetting(index, false) }
            else -> press(null) { setSetting(index, !setting(index)) }
        }
    }

    private fun setting(index: Int): Boolean = when (index) {
        0 -> settings.music
        1 -> settings.effects
        else -> settings.vibration
    }

    /**
     * A setting changes, remembered: the music stops or goes on at once, the sounds sound with the tap that turns them on,
     * and the vibration, turned on, shakes once so she knows what it is.
     */
    private fun setSetting(index: Int, on: Boolean) {
        when (index) {
            0 -> {
                settings.music = on
                choices.musicOn = on
            }
            1 -> {
                settings.effects = on
                choices.effectsOn = on
            }
            else -> {
                settings.vibration = on
                choices.vibrationOn = on
            }
        }
        if (index < 2) applySound()
        audio?.play(Sound.Tap)
        if (index == 2 && on) shake()
    }

    /** The settings, over the start screen or over the pause: from the gear of the one or the button of the other. */
    private fun openSettings() {
        audio?.play(Sound.Tap)
        openSheet(Sheet.Settings)
    }

    /** The credits, from the settings; the texts of the licences are read now, for when she wants them. */
    private fun openCredits() {
        audio?.play(Sound.Tap)
        openSheet(Sheet.Credits)
        if (licenceTexts != null) return
        scope.launch {
            licenceTexts = LICENCE_FILES.map { file -> runCatching { Res.readBytes(file).decodeToString() }.getOrDefault("") }
            pagesFor = null
        }
    }

    private fun openLicences() {
        audio?.play(Sound.Tap)
        openSheet(Sheet.Licences)
        showLicencePage(0)
    }

    private fun openSheet(next: Sheet) {
        sheet = next
        backEnabled = true
    }

    /** A sheet back: from the licences, a page back and the credits from the first; then the settings; then out of them. */
    private fun closeSheet() {
        audio?.play(Sound.Back)
        when (sheet) {
            Sheet.Licences -> if (licencePage > 0) showLicencePage(licencePage - 1) else sheet = Sheet.Credits
            Sheet.Credits -> sheet = Sheet.Settings
            Sheet.Settings, null -> sheet = null
        }
        backEnabled = sheet != null || backFor(screen)
    }

    private fun turnLicencePage(by: Int) {
        audio?.play(Sound.Tap)
        showLicencePage(licencePage + by)
    }

    private fun showLicencePage(page: Int) {
        val pages = licencePages()
        licencePage = page.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        licenceWhere = if (pages.isEmpty()) "" else Texts.page(licencePage + 1, pages.size)
    }

    /**
     * The texts of the licences in pages of this screen, made again when the screen changes or the texts arrive; the page
     * that shows, kept among them.
     */
    private fun licencePages(): List<List<String>> {
        val layout = layout
        if (pagesFor !== layout) {
            val texts = licenceTexts ?: return emptyList()
            pagesMade = licencePages(texts, layout.licencesText.width, layout.licenceLines)
            pagesFor = layout
            licencePage = licencePage.coerceAtMost((pagesMade.size - 1).coerceAtLeast(0))
            licenceWhere = if (pagesMade.isEmpty()) "" else Texts.page(licencePage + 1, pagesMade.size)
        }
        return pagesMade
    }

    /** The lines of the page of the licences that shows, and the text of the button to the next one, if there is one. */
    val licenceLines: List<String> get() = licencePages().getOrNull(licencePage) ?: emptyList()
    val licenceNext: String? get() = if (licencePage + 1 < licencePages().size) Texts.NEXT else null

    private fun askToQuit() {
        audio?.play(Sound.Tap)
        askingToQuit = true
        backEnabled = true
    }

    private fun stopAskingToQuit() {
        audio?.play(Sound.Back)
        askingToQuit = false
        backEnabled = backFor(screen)
    }

    /**
     * From the title screen, with the sound of a tap and the music going on as it was: how to play the first time, and the
     * start screen after.
     */
    private fun leaveIntro() {
        audio?.play(Sound.Tap)
        if (choices.tutorialSeen) show(Screen.Title) else showHowTo()
    }

    private fun toggleSound() {
        soundOn = !soundOn
        choices.soundOn = soundOn
        settings.soundOn = soundOn
        applySound()
        audio?.play(Sound.Tap)
    }

    /** The button of the sound silences everything; with it on, the music and the sounds as the settings say (UI-11). */
    private fun applySound() {
        audio?.muted = !soundOn || !choices.musicOn
        audio?.effectsMuted = !soundOn || !choices.effectsOn
    }

    /**
     * What the phone does with a soft vibration (VFX-07), the screen's; null where it cannot. The run calls it with what
     * goes wrong: an egg, a fish, a bird or a fall (Fangusk), if the vibration of the settings is on.
     */
    var vibrate: (() -> Unit)? = null

    private fun shake() {
        if (choices.vibrationOn && !autopilot) vibrate?.invoke()
    }

    private fun openRecords() {
        audio?.play(Sound.Tap)
        recordsScenario = scenario
        recordRuns = records.best(recordsScenario)
        show(Screen.Records)
    }

    private fun openHowTo() {
        audio?.play(Sound.Tap)
        showHowTo()
    }

    private fun showHowTo() {
        showPage(0)
        clock.reset()
        show(Screen.HowTo)
    }

    /** How to play, a page forward; past the last one, it has been seen and the start screen comes. */
    private fun turnPage() {
        audio?.play(Sound.Tap)
        if (howToPage + 1 >= TutorialScene.PAGES) closeHowTo() else showPage(howToPage + 1)
    }

    /** How to play, a page back; from the first one, the start screen. */
    private fun goBackInHowTo() {
        if (howToPage == 0) {
            closeHowTo()
            return
        }
        audio?.play(Sound.Back)
        showPage(howToPage - 1)
    }

    private fun showPage(page: Int) {
        howToPage = page
        tutorial = scenesOf(page)
    }

    /** The two scenes of a [page] of how to play, each from its start. */
    private fun scenesOf(page: Int): List<TutorialScene> =
        List(TutorialScene.PER_PAGE) { TutorialScene(page * TutorialScene.PER_PAGE + it, config, scenario) { lookOf(art) } }

    private fun closeHowTo() {
        choices.tutorialSeen = true
        tutorial = emptyList()
        goHome()
    }

    /**
     * The text of the button forward of how to play (GameMaster's proposal): on, until the last page; there, «¡A JUGAR!»
     * the first time and «¡ENTENDIDO!» when she opened it from the start screen.
     */
    fun forwardText(): String = when {
        howToPage < TutorialScene.PAGES - 1 -> Texts.NEXT
        choices.tutorialSeen -> Texts.UNDERSTOOD
        else -> Texts.PLAY
    }

    private fun show(next: Screen) {
        screen = next
        backEnabled = backFor(next)
    }

    /** Whether the back button has something to do on [screen]: everywhere but on the title screen and the start screen. */
    private fun backFor(screen: Screen): Boolean = screen != Screen.Intro && screen != Screen.Title

    /** A run in [chosen], already going: a card of the start screen. The scenario is remembered for the next time. */
    private fun startRun(chosen: Scenario) {
        if (chosen != scenario || run.phase != RunPhase.Ready) {
            scenario = chosen
            replaceRun()
        }
        choices.scenario = chosen
        leavePause()
        clearEnd()
        show(Screen.Game)
        run.step(JumpLab.STEP, pressed = true, held = false)
        lastNanos = -1L
        clock.reset()
        audio?.start(scenario, run.seed)
    }

    /** Back to the start screen, with a new run of the scenario waiting there and its music. */
    private fun goHome() {
        audio?.play(Sound.Back)
        if (run.phase != RunPhase.Ready) replaceRun()
        leavePause()
        clearEnd()
        input.clear()
        show(Screen.Title)
        audio?.music(scenario, restart = true)
    }

    /**
     * «CONTINUAR»: the menu goes and, after a short breath for the finger, 3, 2, 1 (GameMaster: «un respiro corto al
     * continuar»), the run goes on.
     */
    private fun resume() {
        audio?.play(Sound.Back)
        pauseMenu = null
        sheet = null
        input.clear()
        resumeIn = RESUME_COUNT * RESUME_STEP
    }

    /** The count is over: the run goes on where it was, with its music. */
    private fun goOn() {
        resumeIn = 0.0
        paused = false
        lastNanos = -1L
        clock.reset()
        input.clear()
        audio?.resume()
    }

    /** Out of the pause, its menu, its sheets and its count. */
    private fun leavePause() {
        paused = false
        pauseMenu = null
        sheet = null
        resumeIn = 0.0
        backEnabled = backFor(screen)
    }

    /** The end of a run cleared: its panel, its buttons, its counter and its confetti. */
    private fun clearEnd() {
        result = null
        panel = emptyList()
        panelButtons = emptyList()
        recordCounter = null
        confettiStarted = false
        confetti.stop()
    }

    private fun step() {
        val touched = input.consumePress()
        val pilot = pilot
        if (run.phase == RunPhase.Over) {
            look.step(JumpLab.STEP, run, result?.newRecord == true)
            if (pilot != null && time - overAt >= PILOT_RETRY_DELAY) restart()
            return
        }
        pilot?.decide()
        val pressed = pilot?.pressed ?: touched
        val held = pilot?.held ?: input.isHeld
        val caro = run.caro
        val jumps = caro.jumps
        val landing = caro.state == CaroState.Landing
        run.step(JumpLab.STEP, pressed, held)
        runSeconds += JumpLab.STEP
        // The music follows the run while it goes on: its layers with the speed, its tense version with few points.
        if (run.phase == RunPhase.Running) {
            audio?.speed(caro.speed)
            audio?.lowPoints(lowPoints)
        }
        jumpSound(jumped = caro.jumps > jumps, held = held)
        if (!landing && caro.state == CaroState.Landing) audio?.play(Sound.Landing)
        if (!recordHeard && bestBefore > 0 && run.meters > bestBefore) {
            recordHeard = true
            audio?.play(Sound.Record)
        }
        handleEvents()
        val pose = look.pose
        look.step(JumpLab.STEP, run, result?.newRecord == true)
        if (pose != CaroPose.AlBarranco && look.pose == CaroPose.AlBarranco) audio?.play(Sound.IntoTheGap)
        hint = hintNow()
        hintFill = if (hint && !caro.state.grounded) caro.holdShare else 0f
    }

    /**
     * Whether to show the hint to hold the finger (GameMaster's storyboard): in her first [HINT_RUNS] runs, from a moment
     * before each of the first [HINT_GAPS] gaps of the run, over Caro, and on with her in the air until she lands.
     */
    private fun hintNow(): Boolean {
        if (choices.runsPlayed >= HINT_RUNS || run.phase != RunPhase.Running) return false
        val caro = run.caro
        if (!caro.state.grounded) return hint
        val grounds = run.course.grounds
        var gap = Int.MIN_VALUE
        for (i in 0 until grounds.size - 1) {
            val ground = grounds[i]
            if (ground.end > caro.right && grounds[i + 1].start > ground.end) {
                gap = ground.end
                break
            }
        }
        if (gap == Int.MIN_VALUE) return false
        if (gap != hintGap) {
            hintGap = gap
            gapsMet++
        }
        return gapsMet <= HINT_GAPS && caro.state.grounded && gap - caro.right < caro.speed * HINT_SECONDS
    }

    /**
     * The sound of a jump (Fangusk, option C): the short one at the take-off and, if after [LONG_JUMP_SECONDS] the finger is
     * still down, the long one on top, the tail that goes on rising. It never comes late.
     */
    private fun jumpSound(jumped: Boolean, held: Boolean) {
        if (jumped) {
            audio?.play(Sound.Jump)
            longJumpSteps = (LONG_JUMP_SECONDS / JumpLab.STEP).roundToInt()
            return
        }
        if (longJumpSteps <= 0) return
        if (!held) {
            longJumpSteps = 0
        } else if (--longJumpSteps == 0) {
            audio?.play(Sound.LongJump)
        }
    }

    private fun handleEvents() {
        if (run.events.isEmpty()) return
        for (event in run.events) {
            when (event) {
                is RunEvent.Picked -> {
                    if (event.kind.adds) {
                        audio?.play(soundOf(event.kind), streak = streak.rescued(runSeconds))
                    } else {
                        streak.broken()
                        audio?.play(soundOf(event.kind))
                        shake()
                    }
                }
                is RunEvent.Ended -> {
                    if (event.cause == EndCause.Fall) shake()
                    finish(event.cause)
                }
            }
        }
        run.events.clear()
    }

    private fun finish(cause: EndCause) {
        val ended = records.submit(scenario, run.meters.toInt(), run.maxPoints, today())
        result = ended
        overAt = time
        hint = false
        recordCounter = null
        counterShown = -1
        confettiStarted = false
        if (!autopilot) choices.runsPlayed = choices.runsPlayed + 1
        panel = overPanel(ended)
        panelButtonTexts = listOf(Texts.AGAIN, Texts.HOME)
        panelButtonIcons = listOf(Icon.Again, Icon.Home)
        panelButtons = layout.row(panelButtonTexts, panelBottom(panel, viewport.height) + MenuLayout.GAP, panelButtonIcons)
        // The music stops: the sound of the end, and a moment later its jingle, of a record if there is one.
        if (cause == EndCause.NoPoints) audio?.play(Sound.NoPoints)
        audio?.end(scenario, ended.newRecord, JINGLE_DELAY_MILLIS)
    }

    /** From the pause, the run is left and another starts in the same scenario (UI-05). */
    private fun restartFromPause() {
        leavePause()
        audio?.play(Sound.Tap)
        restart()
    }

    /** Another run at once, already running: from the end of a run to playing again takes one tap (UI-06). */
    private fun restart() {
        replaceRun()
        clearEnd()
        run.step(JumpLab.STEP, pressed = true, held = false)
        audio?.start(scenario, run.seed)
    }

    /**
     * The counter of a new record (UI-15, GameMaster's design): from 0 to the metres in [RECORD_COUNT] seconds, quickly
     * and braking at the end, `final·(1−(1−t)³)`, in cream; at the metres, in gold, it bounces up 2 px for 120 ms and the
     * confetti starts, once.
     */
    private fun countRecord() {
        val counter = recordCounter ?: return
        val meters = result?.meters ?: return
        val since = time - overAt
        val share = (since / RECORD_COUNT).coerceIn(0.0, 1.0)
        val shown = (meters * (1 - (1 - share).pow(3))).roundToInt()
        if (shown != counterShown) {
            counterShown = shown
            counter.text = "${thousands(shown)} M"
        }
        if (share < 1.0) return
        counter.tone = Tone.Record
        val bounce = since - RECORD_COUNT
        counter.lift = if (bounce < BOUNCE_SECONDS) (BOUNCE_PIXELS * sin(PI * bounce / BOUNCE_SECONDS)).roundToInt() else 0
        if (!confettiStarted) {
            confettiStarted = true
            confetti.start(viewport.width, viewport.height)
        }
    }

    private fun soundOf(kind: ItemKind): Sound = when (kind) {
        ItemKind.Perrito -> Sound.Perrito
        ItemKind.Gatito -> Sound.Gatito
        ItemKind.Huevo -> Sound.Huevo
        ItemKind.Pescado -> Sound.Pescado
        ItemKind.Pajaro -> Sound.Pajaro
    }

    /** Caro's look with the frames of her runs and, if the art says, how many tiles a cycle of each covers. */
    private fun lookOf(art: Art?): CaroLook {
        val run = art?.caro?.animation(CaroPose.Corre, Reaction.None)
        val fast = art?.caro?.animation(CaroPose.CorreRapido, Reaction.None)
        val sprint = art?.caro?.animation(CaroPose.CorreSprint, Reaction.None)
        return CaroLook(
            run?.size ?: DEFAULT_RUN_FRAMES,
            fast?.size ?: DEFAULT_RUN_FRAMES,
            run?.cycleTiles,
            fast?.cycleTiles,
            sprint?.size ?: DEFAULT_RUN_FRAMES,
            sprint?.cycleTiles,
        )
    }

    private fun replaceRun() {
        run.close()
        run = createRun()
        backgroundPlan = planFor(run)
        if (autopilot) pilot = Pilot(run)
        bestBefore = records.meters(scenario)
        recordHeard = false
        hintGap = Int.MIN_VALUE
        gapsMet = 0
        hint = false
        streak.broken()
        runSeconds = 0.0
    }

    private fun planFor(run: GameRun): BackgroundPlan? {
        val scenery = art?.scenario(run.scenario) ?: return null
        val movement = config.movement
        return backgroundPlan(scenery, run.seed, REST_CAMERA) { camera ->
            movement.speedAt((camera + CARO_SCREEN_X - GameRun.START_X) / TILE).toDouble()
        }
    }

    private fun createRun(): GameRun {
        val seed = Random.nextLong()
        return GameRun(config, scenario, seed, BufferedPieces(CourseGenerator(config, seed, GameRun.START_X, scenario), scope))
    }

    private fun refreshTexts() {
        if (run.points != shownPoints) {
            shownPoints = run.points
            pointsText = "$shownPoints"
        }
        val meters = run.meters.toInt()
        if (meters != shownMeters) {
            shownMeters = meters
            metersText = "$meters M"
        }
        // A few times a second, so the line itself does not weigh on what it measures.
        if (showDebug && (debugLines[0].isEmpty() || ++debugFrames % DEBUG_REFRESH_FRAMES == 0)) {
            debugLines[0] = "FPS ${fps.value} · PEOR ${fps.worstMillis} MS · LENTOS ${tenths(fps.slowPercent)} %"
            debugLines[1] =
                "LÓGICA ${tenths(logic.millis)} MS · DIBUJO ${tenths(drawing.millis)} MS · ARRANQUE ${tenths(startupMillis / 1000.0)} S"
            val caro = run.caro
            debugLines[2] = "SALTOS ${caro.jumps} · COYOTE ${caro.coyoteJumps} · BUFFER ${caro.bufferedJumps}"
            debugLines[3] = "${caro.speed.toInt()} PX/S · ${Texts.scenario(scenario)} · SEMILLA ${run.seed % SEED_DIGITS}"
        }
    }

    /**
     * The panel of the end: how she lost, the animals she saved, the metres and the most points. With a new record
     * (UI-15, GameMaster's mockup), «¡NUEVO RÉCORD!» and its metres big, counting, where the distance and the record went,
     * which would say the same twice.
     */
    private fun overPanel(result: RunResult): List<PanelLine> {
        val title = PanelLine(Texts.endTitle(run.endCause, scenario, run.lastTaken), Tone.Warning, size = 2)
        val saved = PanelLine(Texts.saved(run.rescued))
        val points = PanelLine("${Texts.MAX_POINTS} ${result.maxPoints}")
        if (result.newRecord) {
            val counter = PanelLine("0 M", size = 2)
            recordCounter = counter
            return listOf(title, saved, PanelLine(Texts.NEW_RECORD, Tone.Record, size = 2), counter, points)
        }
        // The metres as the records and the counter write them, with a dot every three figures.
        val distance = "${Texts.DISTANCE} ${thousands(result.meters)} M · ${Texts.RECORD} ${thousands(result.bestMeters)} M"
        val lines = mutableListOf(title, saved, PanelLine(distance), points)
        if (result.place > 1) lines += PanelLine(Texts.place(result.place, Records.SIZE), Tone.Record)
        return lines
    }

    private companion object {
        const val NANOS = 1_000_000_000.0
        const val RETRY_DELAY = 0.6
        const val PILOT_RETRY_DELAY = 3.0
        const val MIN_SCREEN_WIDTH = 1280
        const val MIN_SCREEN_HEIGHT = 720
        const val DEBUG_CORNER = 24
        const val MILLIS = 1000
        const val DEFAULT_RUN_FRAMES = 6

        /** The hint to hold the finger: in her first runs, before the first gaps, this many seconds before each. */
        const val HINT_RUNS = 3
        const val HINT_GAPS = 3
        const val HINT_SECONDS = 1.6f

        /** From the sound of the end of a run to its jingle. */
        const val JINGLE_DELAY_MILLIS = 600L

        /** How long the finger has to stay down for the long jump to sound. */
        const val LONG_JUMP_SECONDS = 0.1f

        /** The count before a paused run goes on: three digits, this long each. */
        const val RESUME_COUNT = 3
        const val RESUME_STEP = 0.35

        /** The longest a frame counts for the count and for the confetti. */
        const val LONGEST_FRAME = 0.1

        /** The counter of a new record: how long it counts, and its bounce when it arrives (GameMaster). */
        const val RECORD_COUNT = 1.0
        const val BOUNCE_SECONDS = 0.12
        const val BOUNCE_PIXELS = 2

        /** The texts of the licences, in the order they show: the list of the libraries, the Apache 2.0 and Skia's. */
        val LICENCE_FILES = listOf("files/licencias/librerias.txt", "files/licencias/apache-2.0.txt", "files/licencias/skia.txt")
        const val DEBUG_TAKE_AWAY = 5
        const val SEED_DIGITS = 100_000
        const val DEBUG_REFRESH_FRAMES = 15
        const val DEBUG_LINES = 4
    }
}

/** [value] with one decimal and a comma, as Spanish writes it. */
private fun tenths(value: Double): String {
    val tenths = (value * 10).roundToInt()
    return "${tenths / 10},${tenths % 10}"
}

/** A number of milliseconds that follows what it is given smoothly, so the debugging line can be read. */
internal class Smoothed {
    var millis = 0.0
        private set

    fun add(took: Duration) {
        millis += (took.inWholeMicroseconds / 1000.0 - millis) * SMOOTHING
    }

    private companion object {
        const val SMOOTHING = 0.1
    }
}

/**
 * Frames per second over the last [SIZE] frames, the longest frame of the last [WORST_SIZE] (two seconds) and the share of
 * slow frames since the game opened, those that missed a refresh of a 60 Hz screen: where a stutter or a pause of the
 * garbage collector shows up (MOB-02).
 */
internal class FpsMeter {
    private val frames = DoubleArray(WORST_SIZE)
    private var next = 0
    private var total = 0.0
    private var counted = 0L
    private var slow = 0L

    var value = 0
        private set
    var worstMillis = 0
        private set
    val slowPercent: Double get() = if (counted > 0) slow * 100.0 / counted else 0.0

    fun add(seconds: Double) {
        if (seconds > 0.0) {
            counted++
            if (seconds > SLOW_SECONDS) slow++
        }
        total += seconds - frames[(next + WORST_SIZE - SIZE) % WORST_SIZE]
        frames[next] = seconds
        next = (next + 1) % WORST_SIZE
        value = if (total > 0.0) (SIZE / total).roundToInt() else 0
        var worst = 0.0
        for (frame in frames) if (frame > worst) worst = frame
        worstMillis = (worst * 1000).roundToInt()
    }

    private companion object {
        const val SIZE = 30
        const val WORST_SIZE = 120
        const val SLOW_SECONDS = 0.025
    }
}
