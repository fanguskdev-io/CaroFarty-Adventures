package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * The confetti of a new record (UI-15), as GameMaster designed it (diseno/ui/ui_diseno.py, CONFETI_LANZAMIENTO): from
 * the moment the counter of the record arrives, once, a burst of [BURST] pieces and then [DRIZZLE] a second for
 * [DRIZZLE_SECONDS] s, for every [PER_WIDTH] px of the screen's width. Each is born just above the top of the screen,
 * going down fast, and the air brakes it to a top speed of its own, `v += (g − r·v)·dt` with `r = g / top`, so in half a
 * second it floats; it drifts sideways and sways, turns with its four frames at a pace of its own, and goes when it leaves
 * through the bottom: some 5.5 s in all. Drawn in front of everything, on whole pixels; nothing is made while it falls.
 */
internal class Confetti(private val random: Random = Random.Default) {
    private val x = FloatArray(CAPACITY)
    private val y = FloatArray(CAPACITY)
    private val speed = FloatArray(CAPACITY)
    private val drag = FloatArray(CAPACITY)
    private val drift = FloatArray(CAPACITY)
    private val sway = FloatArray(CAPACITY)
    private val hertz = FloatArray(CAPACITY)
    private val phase = FloatArray(CAPACITY)
    private val spin = FloatArray(CAPACITY)
    private val turnedAtBirth = FloatArray(CAPACITY)
    private val age = FloatArray(CAPACITY)
    private val kind = IntArray(CAPACITY)
    private var count = 0
    private var born = 0
    private var width = 0
    private var height = 0
    private var drizzle = 0f
    private var drizzleTotal = 0
    private var drizzled = 0
    private var drizzleTime = 0f

    /** Whether there is confetti in the air or still to come. */
    val falling: Boolean get() = count > 0 || drizzled < drizzleTotal

    /** How many pieces are in the air. */
    val size: Int get() = count

    /** The confetti starts over a screen [width] × [height] game pixels. */
    fun start(width: Int, height: Int) {
        this.width = width
        this.height = height
        count = 0
        born = 0
        val share = width / PER_WIDTH
        repeat((BURST * share).roundToInt()) { launch() }
        drizzle = DRIZZLE * share
        drizzleTotal = (drizzle * DRIZZLE_SECONDS).roundToInt()
        drizzled = 0
        drizzleTime = 0f
    }

    /** No more confetti: a new run, or the start screen. */
    fun stop() {
        count = 0
        drizzled = drizzleTotal
    }

    /** [seconds] later: the drizzle goes on and each piece falls, and those that left the screen go. */
    fun step(seconds: Float) {
        if (!falling) return
        if (drizzled < drizzleTotal) {
            drizzleTime += seconds
            val due = min(drizzleTotal, (drizzleTime * drizzle + ROUNDING).toInt())
            while (drizzled < due) {
                launch()
                drizzled++
            }
        }
        var kept = 0
        for (i in 0 until count) {
            speed[i] += (GRAVITY - drag[i] * speed[i]) * seconds
            y[i] += speed[i] * seconds
            x[i] += drift[i] * seconds
            age[i] += seconds
            if (y[i] < height) {
                if (kept != i) move(i, kept)
                kept++
            }
        }
        count = kept
    }

    private fun launch() {
        if (count == CAPACITY) return
        val i = count++
        x[i] = random.nextFloat() * (width - SMALL_SIDE)
        y[i] = between(BIRTH_TOP, BIRTH_BOTTOM)
        speed[i] = between(BIRTH_SPEED_LOW, BIRTH_SPEED_HIGH)
        drag[i] = GRAVITY / between(TOP_SPEED_LOW, TOP_SPEED_HIGH)
        drift[i] = between(-MAX_DRIFT, MAX_DRIFT)
        sway[i] = between(SWAY_LOW, SWAY_HIGH)
        hertz[i] = between(HERTZ_LOW, HERTZ_HIGH)
        phase[i] = between(0f, TURN)
        spin[i] = between(SPIN_LOW, SPIN_HIGH)
        turnedAtBirth[i] = between(0f, TURN_MILLIS)
        age[i] = 0f
        // The five colours by turns, as many of each; two in five square and the rest long.
        val long = random.nextFloat() >= SQUARE_SHARE
        kind[i] = (born % COLOURS) * 2 + if (long) 1 else 0
        born++
    }

    private fun between(low: Float, high: Float): Float = low + random.nextFloat() * (high - low)

    private fun move(from: Int, to: Int) {
        x[to] = x[from]
        y[to] = y[from]
        speed[to] = speed[from]
        drag[to] = drag[from]
        drift[to] = drift[from]
        sway[to] = sway[from]
        hertz[to] = hertz[from]
        phase[to] = phase[from]
        spin[to] = spin[from]
        turnedAtBirth[to] = turnedAtBirth[from]
        age[to] = age[from]
        kind[to] = kind[from]
    }

    /** Where piece [i] is drawn across, on a whole pixel: its drift and its sway. */
    private fun left(i: Int): Int = (x[i] + sway[i] * sin(TURN * hertz[i] * age[i] + phase[i])).roundToInt()

    /**
     * Every piece in the air: GameMaster's sprites of the interface (`confeti-<colour>-<2x2|2x3>`, at the top left of
     * their frames), each at its own frame of its turn; without them, small rectangles of their colours.
     */
    fun paint(scope: DrawScope, ui: UiArt?) {
        val icons = ui?.icons
        for (i in 0 until count) {
            val left = left(i)
            val top = y[i].roundToInt()
            val millis = (turnedAtBirth[i] + age[i] * spin[i] * MILLIS).toLong()
            if (icons != null && scope.drawUiSprite(icons, NAMES[kind[i]], left, top, millis)) continue
            val tall = if (kind[i] % 2 == 1) LONG_SIDE else SMALL_SIDE
            scope.drawRect(
                GREY_COLOURS[kind[i] / 2],
                Offset(left.toFloat(), top.toFloat()),
                Size(SMALL_SIDE.toFloat(), tall.toFloat()),
            )
        }
    }

    internal companion object {
        /** For every this many pixels of width: a burst of so many, and then so many a second for so long. */
        const val PER_WIDTH = 400f
        const val BURST = 60
        const val DRIZZLE = 20f
        const val DRIZZLE_SECONDS = 2f

        /** So that a sum of steps that should make a whole piece does not fall short of it. */
        const val ROUNDING = 1e-3f

        /** As many as the widest screen can have in the air at once (ADR 0003: 448 px). */
        const val CAPACITY = 128

        /** Where they are born, above the top of the screen, and how fast they go down. */
        const val BIRTH_TOP = -24f
        const val BIRTH_BOTTOM = -4f
        const val BIRTH_SPEED_LOW = 150f
        const val BIRTH_SPEED_HIGH = 300f

        /** The fall: the gravity, in px/s², and the top speed of each, at random. */
        const val GRAVITY = 150f
        const val TOP_SPEED_LOW = 38f
        const val TOP_SPEED_HIGH = 58f

        /** Sideways: the drift, in px/s, and the sway, in pixels and turns a second. */
        const val MAX_DRIFT = 8f
        const val SWAY_LOW = 3f
        const val SWAY_HIGH = 8f
        const val HERTZ_LOW = 0.5f
        const val HERTZ_HIGH = 1.1f

        /** How fast each turns, over the 300 ms of a whole turn of its frames. */
        const val SPIN_LOW = 0.7f
        const val SPIN_HIGH = 1.4f
        const val TURN_MILLIS = 300f
        const val MILLIS = 1000f
        const val TURN = (2 * PI).toFloat()

        /** Their sizes: two pixels by two or by three, two in five square. */
        const val SMALL_SIDE = 2
        const val LONG_SIDE = 3
        const val SQUARE_SHARE = 0.4f
        const val COLOURS = 5

        /** The names of GameMaster's sprites, by colour and size, and the colours of the prototype's rectangles. */
        val NAMES = listOf("frambuesa", "oro", "azul", "violeta", "crema").flatMap { listOf("confeti-$it-2x2", "confeti-$it-2x3") }
        val GREY_COLOURS = listOf(Color(0xFFE5548A), Color(0xFFFFCC47), Color(0xFF5A8FF0), Color(0xFF8E6BD8), Color(0xFFFFF6E5))
    }
}
