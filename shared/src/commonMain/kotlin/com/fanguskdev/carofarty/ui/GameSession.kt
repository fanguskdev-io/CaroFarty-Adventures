package com.fanguskdev.carofarty.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fanguskdev.carofarty.engine.FixedStep
import com.fanguskdev.carofarty.engine.PixelViewport
import com.fanguskdev.carofarty.engine.TouchInput
import com.fanguskdev.carofarty.game.BufferedPieces
import com.fanguskdev.carofarty.game.CourseGenerator
import com.fanguskdev.carofarty.game.GameConfig
import com.fanguskdev.carofarty.game.GameRun
import com.fanguskdev.carofarty.game.JumpLab
import com.fanguskdev.carofarty.game.Pilot
import com.fanguskdev.carofarty.game.RunEvent
import com.fanguskdev.carofarty.game.RunPhase
import com.fanguskdev.carofarty.game.Scenario
import com.fanguskdev.carofarty.storage.Records
import com.fanguskdev.carofarty.storage.RunResult
import com.fanguskdev.carofarty.ui.render.GreyPalette
import com.fanguskdev.carofarty.ui.render.PanelLine
import com.fanguskdev.carofarty.ui.render.SafeInsets
import com.fanguskdev.carofarty.ui.render.pauseButtonHit
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * What the game screen keeps between frames: the current run, the finger, the fixed step, the pause, the end of the run
 * against the records and the debugging numbers. The screen calls [onFrame] once per frame and [frame] makes it redraw;
 * nothing here allocates per frame except the debugging line, which only debug builds show (TEC-17). In debug builds
 * started with the autopilot, the [Pilot] plays instead of the finger and starts again on its own when a run ends.
 */
internal class GameSession(
    private val config: GameConfig,
    private val scope: CoroutineScope,
    private val records: Records,
    private val debugBuild: Boolean,
    debugAtStart: Boolean = false,
    autopilot: Boolean = false,
    private val openedAt: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow(),
) {
    var scenario = Scenario.Bosque
        private set
    var run: GameRun = createRun()
        private set
    private val autopilot = debugBuild && autopilot
    private var pilot: Pilot? = if (this.autopilot) Pilot(run) else null
    val input = TouchInput()
    private val clock = FixedStep()

    /** Read by the drawing so it redraws every frame. */
    val frame = mutableLongStateOf(0L)

    /** Whether the run is going, for the back button: Compose state, so it can enable and disable it. */
    var running by mutableStateOf(false)
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

    var pointsText = ""
        private set

    /** Whether the points are running out, for the marker in red (guía 1, principio 8). */
    val lowPoints: Boolean get() = run.phase != RunPhase.Ready && run.points <= config.score.warnAtPoints
    var metersText = ""
        private set

    /**
     * The debugging lines: the frames, what they cost and how long the game took to open (MOB-02), the jumps that came from
     * the coyote time and the buffer (PLY-04), and the run.
     */
    val debugLines = Array(DEBUG_LINES) { "" }
    var panel: List<PanelLine> = readyPanel()
        private set

    private var shownPoints = -1
    private var shownMeters = -1
    private var time = 0.0
    private var overAt = 0.0
    private var lastNanos = -1L
    private val fps = FpsMeter()
    private val logic = Smoothed()
    private val drawing = Smoothed()
    private var startupMillis = -1L
    private var debugFrames = 0

    fun onFrame(nanos: Long) {
        val elapsed = if (lastNanos < 0) 0.0 else (nanos - lastNanos) / NANOS
        lastNanos = nanos
        time += elapsed
        fps.add(elapsed)
        if (startupMillis < 0) startupMillis = openedAt.elapsedNow().inWholeMilliseconds
        val started = TimeSource.Monotonic.markNow()
        if (!paused) alpha = clock.advance(elapsed) { step() }
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
        if (paused) {
            resume()
            return
        }
        if (showDebug && run.phase == RunPhase.Ready && gameY > viewport.height - DEBUG_CORNER && gameX < DEBUG_CORNER * 3) {
            changeScenario()
            return
        }
        // With the debugging tools open, a tap on the points takes some away, to see the end of a run without points.
        if (showDebug && run.phase == RunPhase.Running && gameY < insets.top + DEBUG_CORNER && gameX < insets.left + DEBUG_CORNER * 3) {
            run.takeAway(DEBUG_TAKE_AWAY)
            handleEvents()
            return
        }
        if (run.phase == RunPhase.Running && pauseButtonHit(gameX, gameY, viewport.width, insets)) {
            pause()
            return
        }
        input.press()
    }

    fun fingersUp() = input.release()

    /** Pauses the run, if it is going: the pause button, the app going to the background or the back button (MOB-05). */
    fun pause() {
        if (run.phase != RunPhase.Running || paused) return
        paused = true
        input.clear()
        panel = pausePanel()
    }

    private fun resume() {
        paused = false
        lastNanos = -1L
        clock.reset()
        panel = emptyList()
    }

    private fun step() {
        val touched = input.consumePress()
        val pilot = pilot
        if (run.phase == RunPhase.Over) {
            if ((pilot != null && time - overAt >= PILOT_RETRY_DELAY) || (touched && time - overAt >= RETRY_DELAY)) restart()
            return
        }
        pilot?.decide()
        val pressed = pilot?.pressed ?: touched
        val held = pilot?.held ?: input.isHeld
        val wasReady = run.phase == RunPhase.Ready
        run.step(JumpLab.STEP, pressed, held)
        if (wasReady && run.phase == RunPhase.Running) {
            panel = emptyList()
            running = true
        }
        handleEvents()
    }

    private fun handleEvents() {
        if (run.events.isEmpty()) return
        for (event in run.events) {
            if (event is RunEvent.Ended) finish()
        }
        run.events.clear()
    }

    private fun finish() {
        val ended = records.submit(scenario, run.meters.toInt(), run.maxPoints)
        result = ended
        overAt = time
        running = false
        panel = overPanel(ended)
    }

    /** Another run at once, already running: from the end of a run to playing again takes one tap (UI-06). */
    private fun restart() {
        replaceRun()
        result = null
        run.step(JumpLab.STEP, pressed = true, held = false)
        running = true
        panel = emptyList()
    }

    private fun changeScenario() {
        scenario = Scenario.entries[(scenario.ordinal + 1) % Scenario.entries.size]
        replaceRun()
        panel = readyPanel()
    }

    private fun replaceRun() {
        run.close()
        run = createRun()
        if (autopilot) pilot = Pilot(run)
    }

    private fun createRun(): GameRun {
        val seed = Random.nextLong()
        return GameRun(config, scenario, seed, BufferedPieces(CourseGenerator(config, seed, GameRun.START_X), scope))
    }

    private fun refreshTexts() {
        if (run.points != shownPoints) {
            shownPoints = run.points
            pointsText = "${Texts.POINTS} $shownPoints"
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

    private fun readyPanel(): List<PanelLine> {
        val lines = mutableListOf(
            PanelLine(Texts.scenario(scenario), GreyPalette.record),
            PanelLine(Texts.TAP_TO_START, size = 2),
            PanelLine(Texts.HOLD_HINT),
        )
        val bestMeters = records.meters(scenario)
        if (bestMeters > 0) {
            lines += PanelLine("${Texts.RECORD} $bestMeters M · ${records.points(scenario)} ${Texts.POINTS}", GreyPalette.record)
        }
        return lines
    }

    private fun pausePanel(): List<PanelLine> = listOf(PanelLine(Texts.PAUSE, size = 2), PanelLine(Texts.TAP_TO_RESUME))

    private fun overPanel(result: RunResult): List<PanelLine> {
        val lines = mutableListOf(
            PanelLine(Texts.GAME_OVER, size = 2),
            PanelLine(Texts.cause(run.endCause), GreyPalette.bad),
            PanelLine("${Texts.DISTANCE} ${result.meters} M · ${Texts.RECORD} ${result.bestMeters} M"),
            PanelLine("${Texts.MAX_POINTS} ${result.maxPoints} · ${Texts.RECORD} ${result.bestPoints}"),
        )
        if (result.newRecord) lines += PanelLine(Texts.NEW_RECORD, GreyPalette.record, size = 2)
        lines += PanelLine(Texts.TAP_TO_RETRY)
        return lines
    }

    private companion object {
        const val NANOS = 1_000_000_000.0
        const val RETRY_DELAY = 0.6
        const val PILOT_RETRY_DELAY = 3.0
        const val MIN_SCREEN_WIDTH = 1280
        const val MIN_SCREEN_HEIGHT = 720
        const val DEBUG_CORNER = 24
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
