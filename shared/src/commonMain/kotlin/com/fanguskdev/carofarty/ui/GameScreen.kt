package com.fanguskdev.carofarty.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import carofarty.shared.generated.resources.Res
import com.fanguskdev.carofarty.audio.GameAudio
import com.fanguskdev.carofarty.audio.musicPlayer
import com.fanguskdev.carofarty.audio.soundPlayer
import com.fanguskdev.carofarty.engine.PixelViewport
import com.fanguskdev.carofarty.game.GameConfig
import com.fanguskdev.carofarty.game.GameRun
import com.fanguskdev.carofarty.game.MovementConfig
import com.fanguskdev.carofarty.game.RunPhase
import com.fanguskdev.carofarty.game.TILE
import com.fanguskdev.carofarty.storage.Choices
import com.fanguskdev.carofarty.storage.KeyValueStore
import com.fanguskdev.carofarty.storage.Records
import com.fanguskdev.carofarty.storage.dayText
import com.fanguskdev.carofarty.ui.render.Art
import com.fanguskdev.carofarty.ui.render.Box
import com.fanguskdev.carofarty.ui.render.CARO_HEIGHT
import com.fanguskdev.carofarty.ui.render.CreditsTexts
import com.fanguskdev.carofarty.ui.render.GreyPalette
import com.fanguskdev.carofarty.ui.render.PixelBuffer
import com.fanguskdev.carofarty.ui.render.RecordTexts
import com.fanguskdev.carofarty.ui.render.SafeInsets
import com.fanguskdev.carofarty.ui.render.SettingsTexts
import com.fanguskdev.carofarty.ui.render.UiArt
import com.fanguskdev.carofarty.ui.render.drawFrame
import com.fanguskdev.carofarty.ui.render.paintBackground
import com.fanguskdev.carofarty.ui.render.paintButton
import com.fanguskdev.carofarty.ui.render.paintCaro
import com.fanguskdev.carofarty.ui.render.paintCaroSprite
import com.fanguskdev.carofarty.ui.render.paintColumnMenu
import com.fanguskdev.carofarty.ui.render.paintCountdown
import com.fanguskdev.carofarty.ui.render.paintCredits
import com.fanguskdev.carofarty.ui.render.paintDebug
import com.fanguskdev.carofarty.ui.render.paintHint
import com.fanguskdev.carofarty.ui.render.paintHud
import com.fanguskdev.carofarty.ui.render.paintIntro
import com.fanguskdev.carofarty.ui.render.paintLicences
import com.fanguskdev.carofarty.ui.render.paintMenuBackground
import com.fanguskdev.carofarty.ui.render.paintMenuGround
import com.fanguskdev.carofarty.ui.render.paintPanel
import com.fanguskdev.carofarty.ui.render.paintRecordCelebration
import com.fanguskdev.carofarty.ui.render.paintRecords
import com.fanguskdev.carofarty.ui.render.paintSettings
import com.fanguskdev.carofarty.ui.render.paintShade
import com.fanguskdev.carofarty.ui.render.paintTitle
import com.fanguskdev.carofarty.ui.render.paintVeil
import com.fanguskdev.carofarty.ui.render.paintWorld
import com.fanguskdev.carofarty.ui.render.panelBox
import kotlinx.coroutines.isActive
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.TimeSource

/**
 * The game: the settings of `files/ajustes.json` (TEC-07) and the art of `files/arte/` (TEC-18), a run, the loop of a frame
 * (TEC-05), the finger (PLY-10) and the drawing (TEC-04): the world in a low resolution buffer scaled by an integer,
 * shifted by the fraction of the camera so it scrolls smoothly, and Caro, the marker and the panels on top, on the same
 * pixel grid but without the shift. The camera
 * (PLY-13) keeps Caro [CARO_SCREEN_X] pixels from the left edge, and goes up only when her head would come closer than
 * [HEAD_ROOM] to the top: a dead zone as tall as the screen, so it never moves while she runs on the ground.
 */
@Composable
internal fun GameScreen(
    store: KeyValueStore,
    debugBuild: Boolean,
    debugAtStart: Boolean = false,
    autopilot: Boolean = false,
    sound: Boolean = false,
    quit: (() -> Unit)? = null,
) {
    val openedAt = remember { TimeSource.Monotonic.markNow() }
    val resources by produceState<Loaded?>(null) {
        value = Loaded(GameConfig.parse(Res.readBytes(SETTINGS).decodeToString()), Art.load())
    }
    val loaded = resources
    if (loaded == null) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }
    val scope = rememberCoroutineScope()
    val records = remember(store) { Records(store) }
    val choices = remember(store) { Choices(store) }
    val audio = remember(sound) { if (sound) GameAudio(musicPlayer(), soundPlayer(), scope) else null }
    DisposableEffect(audio) { onDispose { audio?.release() } }
    val session =
        remember(loaded) {
            GameSession(loaded.config, loaded.art, audio, scope, records, choices, debugBuild, debugAtStart, autopilot, openedAt, quit)
        }
    val world = remember { PixelBuffer() }
    // The soft vibration of what goes wrong (VFX-07): the phone's own «reject», which also follows its settings of touch.
    val haptics = LocalHapticFeedback.current
    SideEffect { session.vibrate = { haptics.performHapticFeedback(HapticFeedbackType.Reject) } }

    // A buffer for each scene of how to play: the phone draws the frame later, so two scenes in one buffer would both
    // show the last one drawn (Fangusk saw it on his Nothing).
    val scenes = remember { List(TutorialScene.PER_PAGE) { PixelBuffer() } }
    val safe = WindowInsets.safeDrawing

    LaunchedEffect(session) {
        while (isActive) withFrameNanos { session.onFrame(it) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { session.background() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { session.foreground() }
    PlatformBackHandler(enabled = session.backEnabled) { session.back() }

    Canvas(
        Modifier.fillMaxSize().pointerInput(session) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val down = event.changes.count { it.pressed }
                    for (change in event.changes) {
                        if (change.changedToDown()) session.fingerDown(change.position.x, change.position.y, first = down == 1)
                    }
                    if (down == 0) session.fingersUp()
                }
            }
        },
    ) {
        session.frame.longValue
        val viewport = PixelViewport.fit(size.width.toInt(), size.height.toInt())
        session.viewport = viewport
        session.insets =
            SafeInsets(
                left = ceil(safe.getLeft(this, layoutDirection).toFloat() / viewport.scale).toInt(),
                top = ceil(safe.getTop(this).toFloat() / viewport.scale).toInt(),
                right = ceil(safe.getRight(this, layoutDirection).toFloat() / viewport.scale).toInt(),
            )
        val drawing = TimeSource.Monotonic.markNow()
        if (session.screen == Screen.Game) drawGame(session, world, viewport) else drawMenu(session, scenes, viewport)
        session.drawn(drawing.elapsedNow())
    }
}

private fun DrawScope.drawGame(session: GameSession, world: PixelBuffer, viewport: PixelViewport) {
    val run = session.run
    val caro = run.caro
    val x = caro.previousX + (caro.x - caro.previousX) * session.alpha
    val y = caro.previousY + (caro.y - caro.previousY) * session.alpha
    // The push back of an egg or a fish shows as her going back on the screen, until the camera catches her up.
    val lag = caro.previousLag + (caro.cameraLag - caro.previousLag) * session.alpha
    val screenAt = caroScreenX(caro.speed, run.config.movement)
    val camera = x - screenAt + lag
    val cameraLeft = floor(camera).toLong()
    val fraction = camera - cameraLeft
    // Both rounded the same way, so while the camera goes up with her she stays still on the screen and nothing shakes.
    val feetY = y.roundToInt()
    val cameraBottom = cameraBottom(feetY, viewport.height)
    val width = viewport.width + 1
    val art = session.art
    val palette = GreyPalette.of(run.scenario)
    val millis = session.millis
    val image =
        world.paint(width, viewport.height) {
            paintWorld(
                run,
                cameraLeft,
                cameraBottom,
                width,
                viewport.height,
                palette,
                art,
                millis,
                session.showDebug,
                x,
                y,
                session.alpha,
                session.effects,
            )
        }

    drawRect(Color.Black)
    val left = viewport.offsetX.toFloat()
    val top = viewport.offsetY.toFloat()
    val scale = viewport.scale.toFloat()
    clipRect(left, top, left + viewport.width * scale, top + viewport.height * scale) {
        val groundLine = run.config.course.minGroundTop * TILE
        val plan = session.backgroundPlan
        plan?.reach(camera)
        paintBackground(
            camera,
            REST_CAMERA,
            cameraBottom,
            viewport,
            palette,
            art?.scenario(run.scenario),
            groundLine,
            millis,
            plan,
            session.leaves,
            session.haze,
        )
        val shift = (fraction * viewport.scale).roundToInt()
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(width, viewport.height),
            dstOffset = IntOffset(viewport.offsetX - shift, viewport.offsetY),
            dstSize = IntSize(width * viewport.scale, viewport.height * viewport.scale),
            filterQuality = FilterQuality.None,
        )
        withTransform({
            translate(left, top)
            scale(scale, scale, Offset.Zero)
        }) {
            // Sunk a little in the quicksand (PLY-18).
            val sunk = if (caro.sinking && caro.state.grounded) run.config.movement.quicksandSink else 0
            val feet = viewport.height + cameraBottom - feetY + sunk
            val caroArt = art?.caro
            val screenX = screenAt - lag.roundToInt()
            // Blinking after an egg or a fish: shown and hidden by turns.
            val shown = run.blinking <= 0f || (run.blinking * BLINKS_PER_SECOND).toInt() % 2 == 0
            // Her trail, behind her and glued to her feet (VFX-01, GameMaster).
            val effects = art?.effects
            val trail = effects?.trail
            if (shown && trail != null && session.trail) {
                drawFrame(
                    effects.sheet(run.scenario),
                    trail.frameAt(millis, loops = true),
                    screenX - effects.anchorX,
                    feet - effects.anchorY,
                )
            }
            if (shown && caroArt != null) {
                paintCaroSprite(caroArt, run.scenario, session.look, run.reaction, screenX, feet)
            } else if (shown) {
                paintCaro(screenX, feet, caro.state, stride = (caro.meters * STRIDES_PER_METER).toInt() % 2 == 0)
            }
            val ui = art?.ui
            paintFloating(session.floating, screenX, viewport.height + cameraBottom, ui, head = feet - CARO_HEIGHT, behind = session.hint)
            val pressed = session.pressed
            if (run.phase != RunPhase.Ready) {
                paintHud(session.pointsText, session.metersText, session.layout, ui, session.soundOn, session.lowPoints, pressed)
            }
            if (session.hint) paintHint(Texts.HOLD_HINT, screenX, feet, session.hintFill, ui, millis)
            if (session.paused) paintPause(session, viewport, ui, pressed)
            if (session.panel.isNotEmpty()) paintPanel(session.panel, viewport.width, viewport.height, ui)
            for (i in session.panelButtons.indices) {
                val box = session.panelButtons[i]
                paintButton(
                    box,
                    session.panelButtonTexts[i],
                    ui,
                    session.panelButtonIcons.getOrNull(i),
                    pressed = box == pressed,
                    lit =
                    i == 0,
                )
            }
            if (caroArt != null && run.phase == RunPhase.Over && session.result?.newRecord == true) {
                val panel = panelBox(session.panel, viewport.width, viewport.height, ui)
                paintRecordCelebration(caroArt, run.scenario, session.millis, panel, viewport.width)
            }
            // The confetti of a new record, in front of everything (GameMaster).
            session.confetti.paint(this, ui)
            if (session.showDebug) paintDebug(session.debugLines, viewport.height, session.insets)
        }
    }
}

/**
 * The screens around the runs (UI-01, UI-07, UI-20): the title screen, the start screen, the records and how to play, on
 * the same pixel grid as the game; over the day sky of the interface's art or, in the grey prototype, over the scenario
 * she chose, still.
 */
private fun DrawScope.drawMenu(session: GameSession, scenes: List<PixelBuffer>, viewport: PixelViewport) {
    val art = session.art
    val scenario = if (session.screen == Screen.Records) session.recordsScenario else session.scenario
    val palette = GreyPalette.of(scenario)
    val scenery = art?.scenario(scenario)
    val millis = session.millis
    drawRect(Color.Black)
    val left = viewport.offsetX.toFloat()
    val top = viewport.offsetY.toFloat()
    val scale = viewport.scale.toFloat()
    clipRect(left, top, left + viewport.width * scale, top + viewport.height * scale) {
        val ui = art?.ui
        if (ui == null) {
            val groundLine = session.run.config.course.minGroundTop * TILE
            paintBackground(REST_CAMERA, REST_CAMERA, 0, viewport, palette, scenery, groundLine, millis, null)
        }
        withTransform({
            translate(left, top)
            scale(scale, scale, Offset.Zero)
        }) {
            if (ui == null) paintMenuGround(scenery, palette, viewport.width, viewport.height)
            val pressed = session.pressed
            when (session.screen) {
                Screen.Intro -> paintIntro(session.layout, art, session.scenario, Texts.SUBTITLE, Texts.TAP_TO_START, millis)
                Screen.Title -> if (session.sheet != null) {
                    paintMenuBackground(ui, viewport.width, viewport.height)
                    paintSheet(session, ui, pressed)
                } else {
                    paintTitle(
                        session.layout,
                        art,
                        session.scenarioNames,
                        session.scenario,
                        session.soundOn,
                        millis,
                        pressed,
                        session.canQuit,
                    )
                    if (session.askingToQuit) paintQuestion(session, viewport, ui, pressed)
                }
                Screen.Records -> paintRecords(
                    session.layout,
                    session.scenarioNames,
                    session.recordsScenario,
                    session.recordRuns,
                    RECORD_TEXTS,
                    ui,
                    pressed,
                )
                Screen.HowTo -> paintHowTo(session, scenes)
                Screen.Game -> Unit
            }
            if (session.showDebug) paintDebug(session.debugLines, viewport.height, session.insets)
        }
    }
}

/**
 * The pause (UI-05): the run stopped under the veil with the menu in a column, or a sheet of it, the settings and what
 * opens from them; and, once she goes on, the count over the run without its veil.
 */
private fun DrawScope.paintPause(
    session: GameSession,
    viewport: PixelViewport,
    ui: UiArt?,
    pressed: Box?,
) {
    val countdown = session.countdown
    if (countdown > 0) {
        paintCountdown(countdown, viewport.width, viewport.height, ui)
        return
    }
    paintVeil(viewport.width, viewport.height)
    val menu = session.pauseMenu
    when {
        session.sheet != null -> paintSheet(session, ui, pressed)
        menu != null -> paintColumnMenu(menu, Texts.PAUSE, session.pauseTexts, session.pauseIcons, ui, pressed)
    }
}

/** The sheet open over the start screen or over the pause: the settings, the credits or a page of the licences. */
private fun DrawScope.paintSheet(session: GameSession, ui: UiArt?, pressed: Box?) {
    val layout = session.layout
    when (session.sheet) {
        Sheet.Settings -> paintSettings(layout, session.settings, SETTINGS_TEXTS, ui, pressed)
        Sheet.Credits -> paintCredits(layout, session.creditsLayout, CREDITS_TEXTS, ui, pressed)
        Sheet.Licences ->
            paintLicences(layout, Texts.LICENCES, session.licenceNext, session.licenceLines, session.licenceWhere, ui, pressed)
        null -> Unit
    }
}

/** Whether to leave the game, over the start screen dimmed: the panel of the kit and its two buttons. */
private fun DrawScope.paintQuestion(
    session: GameSession,
    viewport: PixelViewport,
    ui: UiArt?,
    pressed: Box?,
) {
    paintShade(viewport.width, viewport.height)
    paintPanel(session.quitPanel, viewport.width, viewport.height, ui)
    val buttons = session.quitButtons
    for (i in buttons.indices) paintButton(buttons[i], session.quitButtonTexts[i], ui, pressed = buttons[i] == pressed, lit = i == 1)
}

private val RECORD_TEXTS = RecordTexts(Texts.RECORDS_TITLE, Texts.NO_RECORDS, Texts.METERS, Texts.POINTS, Texts.DAY) { dayText(it) }

private val SETTINGS_TEXTS =
    SettingsTexts(Texts.SETTINGS, listOf(Texts.MUSIC, Texts.EFFECTS, Texts.VIBRATION), Texts.ON, Texts.OFF, Texts.CREDITS)

private val CREDITS_TEXTS = CreditsTexts(Texts.CREDITS, Texts.CREDITS_LINE, Texts.LICENCES, Texts.libraries, Texts.FULL_TEXTS)

/** What the game reads before its first frame: the settings and the art, which may be missing. */
private class Loaded(val config: GameConfig, val art: Art?)

/** The world y at the bottom of a screen [height] pixels tall with Caro's feet at [feetY] (PLY-13). */
internal fun cameraBottom(feetY: Int, height: Int): Int = max(0, feetY + CARO_HEIGHT + HEAD_ROOM - height)

/** Where Caro runs, in game pixels from the left edge (ADR 0003). */
internal const val CARO_SCREEN_X = 96

/** Where she runs at the ceiling of the speed, nearer the left edge to see further ahead (ADR 0011, GameMaster). */
private const val CARO_SCREEN_X_FAST = 64

/**
 * Where Caro runs on the screen at [speed]: [CARO_SCREEN_X] up to the speed of the first minutes, and nearer the left edge
 * past it, down to [CARO_SCREEN_X_FAST] at the ceiling of the speed, so she sees further ahead (ADR 0011). In whole
 * pixels: it moves one every so many seconds, too slowly to notice.
 */
internal fun caroScreenX(speed: Float, movement: MovementConfig): Int {
    val share = ((speed - movement.maxSpeed) / (movement.lateSpeed - movement.maxSpeed)).coerceIn(0f, 1f)
    return (CARO_SCREEN_X - (CARO_SCREEN_X - CARO_SCREEN_X_FAST) * share).roundToInt()
}

/** The camera before the first tap, with Caro at the start: where the far band sits on the game's pixel grid. */
internal const val REST_CAMERA = GameRun.START_X - CARO_SCREEN_X

/** The game pixels the camera leaves above Caro's head before going up. */
internal const val HEAD_ROOM = 8
private const val STRIDES_PER_METER = 4

/** How many times a second she goes on and off while blinking. */
private const val BLINKS_PER_SECOND = 12
private const val SETTINGS = "files/ajustes.json"
