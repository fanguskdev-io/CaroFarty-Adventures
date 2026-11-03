package com.fanguskdev.carofarty.game

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

/**
 * How a collectible that moves by itself moves (ADR 0011): the eggs and the fish that float, the birds and the fish that
 * leap out of the water. Where it is depends on how far Caro has got, her reach, the farthest her feet have been, through
 * a clock of its own: the seconds she takes from [fromX] on, running at [pace], which the generator gives it. Tied to her
 * and not to the clock of the run, the validator, which runs her through each try, sees it where the game will show it,
 * and she, who never stops, meets it where the validator said, however the run went before. Only sums and products, so it
 * moves the same on every phone.
 */
abstract class Flight(val fromX: Double, val pace: Float) {
    /** Its clock when she has got to [reach]: negative before [fromX]. */
    fun clock(reach: Double): Double = (reach - fromX) / pace

    /** How far it is across from its place when she has got to [reach]. */
    abstract fun dx(reach: Double): Double

    /** And how far up. */
    abstract fun dy(reach: Double): Double

    /** Whether it is out, where she can touch it and it is drawn: a fish under the water is not. */
    open fun out(reach: Double): Boolean = true

    /** Whether it goes towards the left then: the birds are drawn flying that way, the leaping fish the other. */
    abstract fun towardsLeft(reach: Double): Boolean

    companion object {
        /**
         * Where something that goes there and back is at [u] rounds from one end: 0 there, 1 at the other end half a round
         * later, slowing down at both, like the moving platforms (Motion).
         */
        fun ease(u: Double): Double {
            val f = u - floor(u)
            val s = if (f < HALF) 2 * f else 2 - 2 * f
            return s * s * (3 - 2 * s)
        }

        /** Whether at [u] rounds it is on its way back, from the other end to the first. */
        fun back(u: Double): Boolean = u - floor(u) >= HALF

        private const val HALF = 0.5
    }
}

/**
 * An egg or a fish that floats up and down (ENM-17, Fangusk: «huevos y pescados que flotan»): from [amplitude] pixels
 * below its place to as many above, a round every [period] seconds, starting [phase] of the way round.
 */
class Bob(
    fromX: Double,
    pace: Float,
    val amplitude: Int,
    val period: Double,
    val phase: Double,
) : Flight(fromX, pace) {
    override fun dx(reach: Double): Double = 0.0

    override fun dy(reach: Double): Double = amplitude * (2 * ease(clock(reach) / period + phase) - 1)

    override fun towardsLeft(reach: Double): Boolean = false
}

/**
 * A bird that flies at her from the right (ENM-18), [speed] pixels a second faster than the ground, at its place when her
 * feet get there: the generator chooses where they meet and how high. It flies straight, or waves up and down from [wave]
 * pixels below that height to as many above, a wave every [wavePeriod] seconds, starting [phase] of the way round.
 */
class Charge(
    fromX: Double,
    pace: Float,
    val speed: Float,
    val wave: Int,
    val wavePeriod: Double,
    val phase: Double,
) : Flight(fromX, pace) {
    override fun dx(reach: Double): Double = -speed * clock(reach)

    override fun dy(reach: Double): Double = if (wave == 0) 0.0 else wave * (2 * ease(clock(reach) / wavePeriod + phase) - 1)

    override fun towardsLeft(reach: Double): Boolean = true
}

/**
 * A bird that strolls through the air (ENM-18, Fangusk: «otros que simplemente estén paseando por el aire»): there and back
 * across [span] pixels around its place at about [speed] pixels a second, slowing down to turn at the ends, starting
 * [phase] of the way round; and a little up and down, [bob] pixels, as it glides.
 */
class Stroll(
    fromX: Double,
    pace: Float,
    val span: Int,
    val speed: Float,
    val phase: Double,
    val bob: Int,
) : Flight(fromX, pace) {
    /** A whole round, there and back, in seconds. */
    val period: Double = 2.0 * span / speed

    override fun dx(reach: Double): Double = span * (ease(clock(reach) / period + phase) - HALF)

    override fun dy(reach: Double): Double = bob * (2 * ease(clock(reach) / (period / 2) + phase) - 1)

    override fun towardsLeft(reach: Double): Boolean = back(clock(reach) / period + phase)

    /** Whether it is near one end, where it beats its wings to turn instead of gliding. */
    fun turning(reach: Double): Boolean {
        val u = clock(reach) / period + phase
        val f = u - floor(u)
        val fromEnd = minOf(f, abs(f - HALF), 1 - f)
        return fromEnd < TURN_SHARE
    }

    private companion object {
        const val HALF = 0.5

        /** How much of a round around each end it flaps. */
        const val TURN_SHARE = 0.08
    }
}

/**
 * How far under the line of the lowest ground the water of the gaps is, in world pixels, in the Bosque and the Nieve:
 * where GameMaster drew the river and the icy water in the middle layer (AGUA_DESDE). The fish leap from it (ENM-15).
 */
const val WATER_BELOW = 18

/**
 * A fish that leaps out of the water of a gap (ENM-15, Fangusk: «como los peces de Mario»): it waits under the water at
 * its place, leaps when her feet get to [fromX], up at [rise] pixels a second and across at [across], pulled down by
 * [gravity], and falls back into the water. Its place is on the water, where it leaps from.
 */
class Leap(
    fromX: Double,
    pace: Float,
    val across: Float,
    val rise: Float,
    val gravity: Float,
) : Flight(fromX, pace) {
    /** How long it is out of the water, in seconds of its clock. */
    val airtime: Double = 2.0 * rise / gravity

    override fun dx(reach: Double): Double = across * clock(reach).coerceIn(0.0, airtime)

    override fun dy(reach: Double): Double {
        val t = clock(reach)
        if (t <= 0.0 || t >= airtime) return 0.0
        return t * (rise - gravity * t / 2)
    }

    override fun out(reach: Double): Boolean {
        val t = clock(reach)
        return t > 0.0 && t < airtime
    }

    override fun towardsLeft(reach: Double): Boolean = across < 0f

    /** How fast it goes up at [reach], in pixels a second, down if negative: its pose goes with it. */
    fun climb(reach: Double): Double = rise - gravity * max(0.0, clock(reach))
}
