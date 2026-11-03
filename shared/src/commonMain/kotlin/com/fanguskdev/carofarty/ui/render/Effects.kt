package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.fanguskdev.carofarty.engine.PixelViewport
import com.fanguskdev.carofarty.game.Scenario
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.random.Random

/** What plays where something happened (VFX-01, VFX-03), by the names of GameMaster's animations in efectos.json. */
internal enum class Effect(val key: String) {
    /** Where she jumps from and where she lands. */
    DustUp("polvo-salto"),
    DustDown("polvo-aterriza"),

    /** A perrito or a gatito rescued. */
    Rescued("recogido"),

    /** An egg or a fish she touched, and a bird, which leaves feathers instead. */
    Burst("olor-revienta"),
    Feathers("plumas"),
}

/**
 * The effects that play where something happened and stay there in the world while they play (VFX-01, VFX-03): the dust of
 * a jump and of a landing, the sparkle of a rescue, the burst of an egg or a fish and the feathers of a bird. At most
 * [limit] at once, [MAX] at most (VFX-09, GameMaster: «como mucho 6 efectos sueltos»): a new one takes the place of one
 * that is free or, if none is, of the oldest. They go by the time of the animations, so they stop in the pause.
 */
internal class WorldEffects {
    private val effects = arrayOfNulls<Effect>(MAX)
    private val xs = DoubleArray(MAX)
    private val ys = IntArray(MAX)
    private val starts = LongArray(MAX)

    /** How many may play at once: fewer when the frames do not keep up (VFX-09). */
    var limit = MAX
        set(value) {
            field = value.coerceIn(0, MAX)
            for (i in field until MAX) effects[i] = null
        }

    /** How many are playing or waiting to be drawn. */
    val size: Int get() = effects.count { it != null }

    /** [effect] starts at [millis] of the animations at ([x], [y]) in the world, [y] up from the bottom. */
    fun add(
        effect: Effect,
        x: Double,
        y: Int,
        millis: Long,
    ) {
        if (limit == 0) return
        var slot = 0
        for (i in 0 until limit) {
            if (effects[i] == null) {
                slot = i
                break
            }
            if (starts[i] < starts[slot]) slot = i
        }
        effects[slot] = effect
        xs[slot] = x
        ys[slot] = y
        starts[slot] = millis
    }

    fun clear() {
        effects.fill(null)
    }

    /**
     * Draws those that are playing in [scenario]'s colours, in the buffer of the world whose left column is the world's
     * [cameraLeft] and whose row [base] is the world's y = 0; those that have ended are let go.
     */
    fun paint(
        scope: DrawScope,
        art: EffectsArt,
        scenario: Scenario,
        cameraLeft: Long,
        width: Int,
        base: Int,
        millis: Long,
    ) {
        val sheet = art.sheet(scenario)
        for (i in 0 until limit) {
            val effect = effects[i] ?: continue
            val animation = art.animation(effect)
            val time = millis - starts[i]
            if (animation == null || time >= animation.duration) {
                effects[i] = null
                continue
            }
            val left = (xs[i].roundToLong() - cameraLeft).toInt() - art.anchorX
            if (left >= width || left + sheet.width <= 0) continue
            scope.drawFrame(sheet, animation.frameAt(time.coerceAtLeast(0L), loops = false), left, base - ys[i] - art.anchorY)
        }
    }

    companion object {
        const val MAX = 6
    }
}

/**
 * The leaves that fall in the air of the Bosque (ART-16), as GameMaster designed them: 4 to 6 at a time, each falling at
 * 16 to 26 px/s and swaying ±5 to 9 px every 1.6 to 2.4 s, turning with its animation. They are in front of the background
 * and behind the world and go by with the camera, like the copos of the Nieve; one that leaves the screen comes back where
 * the others come in from: over the top, or at the right while she runs, as many as come in from each side.
 */
internal class Leaves(private val random: Random = Random.Default) {
    private val x = FloatArray(MAX)
    private val y = FloatArray(MAX)
    private val fall = FloatArray(MAX)
    private val sway = FloatArray(MAX)
    private val period = FloatArray(MAX)
    private val phase = FloatArray(MAX)
    private val turn = FloatArray(MAX)
    private val kind = IntArray(MAX)
    private val age = FloatArray(MAX)
    private var count = 0
    private var width = 0
    private var height = 0
    private var lastMillis = -1L
    private var lastCamera = 0.0

    /** How many may fall at once: none when the frames do not keep up, the first thing to go (VFX-09, GameMaster). */
    var limit = MAX
        set(value) {
            field = value.coerceIn(0, MAX)
        }

    /** How many are falling. */
    val size: Int get() = min(count, limit)

    /** Where leaf [i] is drawn, on whole pixels of the screen: its top left, with its sway. */
    fun left(i: Int): Int = (x[i] + sway[i] * sin(TURN * age[i] / period[i] + phase[i])).roundToInt()

    fun top(i: Int): Int = y[i].roundToInt()

    /** They start again over a screen [width] × [height] game pixels, anywhere on its upper part. */
    fun start(width: Int, height: Int) {
        this.width = width
        this.height = height
        count = MIN + random.nextInt(MAX - MIN + 1)
        for (i in 0 until count) {
            renew(i)
            x[i] = random.nextFloat() * width
            y[i] = random.nextFloat() * height * UPPER
        }
        lastMillis = -1L
    }

    /** They fall until [millis] of the animations, with the camera at the world's [camera], on a screen [width] × [height]. */
    fun advance(
        millis: Long,
        camera: Double,
        width: Int,
        height: Int,
    ) {
        if (count == 0 || width != this.width || height != this.height) start(width, height)
        if (lastMillis < 0) {
            lastMillis = millis
            lastCamera = camera
            return
        }
        val seconds = (millis - lastMillis).coerceIn(0L, LONGEST_STEP) / MILLIS
        val moved = (camera - lastCamera).toFloat()
        lastMillis = millis
        lastCamera = camera
        val across = if (seconds > 0f) abs(moved) / seconds else 0f
        for (i in 0 until count) {
            x[i] -= moved
            y[i] += fall[i] * seconds
            age[i] += seconds
            if (x[i] < -ROOM || x[i] > width + ROOM || y[i] > height) comeBack(i, across)
        }
    }

    /**
     * Leaf [i] comes back, with the camera going at [across] px/s: in at the right as often as the leaves of a whole field
     * would come in there, the rest over the top.
     */
    private fun comeBack(i: Int, across: Float) {
        renew(i)
        val fromSide = across * height
        val fromTop = fall[i] * width
        if (random.nextFloat() * (fromSide + fromTop) < fromSide) {
            x[i] = width + random.nextFloat() * ROOM
            y[i] = random.nextFloat() * height * UPPER
        } else {
            x[i] = random.nextFloat() * width
            y[i] = -ROOM - random.nextFloat() * ROOM
        }
    }

    private fun renew(i: Int) {
        fall[i] = between(FALL_LOW, FALL_HIGH)
        sway[i] = between(SWAY_LOW, SWAY_HIGH)
        period[i] = between(PERIOD_LOW, PERIOD_HIGH)
        phase[i] = between(0f, TURN)
        turn[i] = between(0f, TURN_MILLIS)
        kind[i] = random.nextInt(KINDS)
        age[i] = 0f
    }

    private fun between(low: Float, high: Float): Float = low + random.nextFloat() * (high - low)

    /** Draws them with the frames of [animations] of [sheet], in game pixels of [viewport], straight on the screen. */
    fun paint(
        scope: DrawScope,
        sheet: Sheet,
        animations: List<Animation>,
        viewport: PixelViewport,
    ) {
        val shown = size
        if (animations.isEmpty() || shown == 0) return
        val scale = viewport.scale.toFloat()
        scope.withTransform({
            translate(viewport.offsetX.toFloat(), viewport.offsetY.toFloat())
            scale(scale, scale, Offset.Zero)
        }) {
            for (i in 0 until shown) {
                val animation = animations[kind[i] % animations.size]
                val frame = animation.frameAt((turn[i] + age[i] * MILLIS).toLong(), loops = true)
                drawFrame(sheet, frame, left(i), top(i))
            }
        }
    }

    companion object {
        const val MIN = 4
        const val MAX = 6
        const val KINDS = 3

        /** GameMaster's fall, sway and its period, in px/s, px and seconds. */
        const val FALL_LOW = 16f
        const val FALL_HIGH = 26f
        const val SWAY_LOW = 5f
        const val SWAY_HIGH = 9f
        const val PERIOD_LOW = 1.6f
        const val PERIOD_HIGH = 2.4f

        /** Where they come in at the right: the upper part of the screen; and how far out of it a leaf of 16 px goes. */
        const val UPPER = 0.7f
        const val ROOM = 16f + SWAY_HIGH
        const val TURN = (2 * PI).toFloat()
        const val TURN_MILLIS = 560f
        const val MILLIS = 1000f

        /** The longest a step of theirs may be, so a long stop does not throw them out. */
        const val LONGEST_STEP = 100L
    }
}

/**
 * The calima of the Desierto (ART-16), as GameMaster designed it: rows [FIRST] to [LAST] of the strip of its far layer go
 * sideways by `round(sin(2π(t/1.2 + g/12)))` px, two rows at a time, `g` the pair: −1, 0 or +1. [phases] holds it for the
 * 12 moments of its 1.2 s, each pair's shift.
 */
internal object Haze {
    const val FIRST = 10
    const val LAST = 55
    const val ROWS = 2
    private const val PAIRS = (LAST - FIRST + 1) / ROWS
    private const val MOMENTS = 12
    private const val PERIOD_MILLIS = 1_200L

    private val phases: Array<IntArray> = Array(MOMENTS) { moment ->
        IntArray(PAIRS) { pair -> sin(2 * PI * (moment.toDouble() / MOMENTS + pair.toDouble() / MOMENTS)).roundToInt() }
    }

    /** The shift of each pair of rows at [millis] of the animations. */
    fun at(millis: Long): IntArray = phases[((millis.mod(PERIOD_MILLIS)) * MOMENTS / PERIOD_MILLIS).toInt()]
}
