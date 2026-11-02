package com.fanguskdev.carofarty.game

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** The size of a tile in game pixels: also a metre of the run. */
const val TILE = 16

/** The four collectibles (GDD-10): perritos and gatitos add points, fried eggs and fish take them away. */
enum class ItemKind {
    Perrito,
    Gatito,
    Huevo,
    Pescado,
    ;

    val adds: Boolean get() = this == Perrito || this == Gatito

    fun points(score: ScoreConfig): Int = when (this) {
        Perrito -> score.perrito
        Gatito -> score.gatito
        Huevo -> score.huevo
        Pescado -> score.pescado
    }
}

/** A run of ground from [start] to [end] (world pixels, [end] excluded), its top [top] pixels above the bottom edge. */
class Ground(val start: Int, val end: Int, val top: Int)

/**
 * A solid block in the air (ADR 0007), from [start] to [end] across and from [bottom] to [top] up, in world pixels. From
 * below it cuts a jump short, from the side it stops her in the air, and on top she can run.
 */
class Block(
    val start: Int,
    val end: Int,
    val bottom: Int,
    val top: Int,
)

/**
 * A platform (ADR 0009, Fangusk after the beta 4): semisolid, as in Mario. She jumps through it from below and lands on
 * its top; its sides and its underside stop nothing, and on top she runs as on the ground. From [start] to [end] across
 * and its top at [top], in world pixels. One with a [motion] goes back and forth by itself and carries her along.
 */
class Platform(
    val start: Int,
    val end: Int,
    val top: Int,
    val motion: Motion? = null,
) {
    /** How far it has gone across from its place at [time] seconds of the run. */
    fun shiftAt(time: Double): Double = motion?.let { it.dx * it.share(time) } ?: 0.0

    /** How far it has gone up from its place at [time]. */
    fun riseAt(time: Double): Double = motion?.let { it.dy * it.share(time) } ?: 0.0

    fun leftAt(time: Double): Double = start + shiftAt(time)

    fun rightAt(time: Double): Double = end + shiftAt(time)

    fun topAt(time: Double): Double = top + riseAt(time)

    /** How fast it goes across at [time], in pixels a second: what it carries her by. */
    fun speedAt(time: Double): Double = motion?.let { it.dx * it.rate(time) } ?: 0.0
}

/**
 * How a moving platform moves (ADR 0009): [dx] pixels across and [dy] up, there and back every [period] seconds, starting
 * [phase] of the way round, slowing down at both ends. Only sums and products, so it moves the same on every phone.
 */
class Motion(
    val dx: Int,
    val dy: Int,
    val period: Double,
    val phase: Double,
) {
    /** How far along its way it is at [time]: 0 at its place, 1 at the other end. */
    fun share(time: Double): Double {
        val s = there(time)
        return s * s * (3 - 2 * s)
    }

    /** How fast [share] changes at [time], per second. */
    fun rate(time: Double): Double {
        val u = time / period + phase
        val going = u - floor(u) < HALF
        val s = there(time)
        return 6 * s * (1 - s) * (if (going) 2 else -2) / period
    }

    /** Where it is on its way, from 0 to 1 and back, evenly. */
    private fun there(time: Double): Double {
        val u = time / period + phase
        val f = u - floor(u)
        return if (f < HALF) 2 * f else 2 - 2 * f
    }

    private companion object {
        const val HALF = 0.5
    }
}

/** A collectible placed at ([x], [y]) in world pixels, [y] above the bottom edge. */
class Item(val kind: ItemKind, val x: Int, val y: Int) {
    var taken = false

    /** For a gatito, which of its [COATS] coats it wears, drawn with the seed (ADR 0008). */
    var coat = 0

    /** For a perrito, which of the [BREEDS] breeds it is, drawn with the seed: they differ in shape, not only in colour. */
    var breed = 0

    /** For a perrito that walks along its ground (ADR 0008); null if it stays still. */
    var walk: Walk? = null

    /** Where its centre is now across: its place, plus what it has walked. */
    val currentX: Double get() = x + (walk?.offset ?: 0f).toDouble()

    /** Where its centre is drawn, [alpha] of the way from the step before to this one. */
    fun drawnX(alpha: Float): Double {
        val walk = walk ?: return x.toDouble()
        return x + (walk.previousOffset + (walk.offset - walk.previousOffset) * alpha).toDouble()
    }

    companion object {
        /** The side of a collectible's drawing in the prototype (CHR-08). */
        const val SIZE = 12

        /** How high over what it stands on a collectible's centre is: its box of 16 px rests on it. */
        const val ABOVE_GROUND = 8

        /** The coats of the gatitos, in this order: naranja, gris azulado, calicó, crema, negro, blanco, chocolate, siamés. */
        const val COATS = 8

        /**
         * The breeds of the perritos, in this order (Fangusk): shiba, pastor alemán, carlino, dálmata, golden, beagle,
         * yorkshire, bulldog francés y corgi.
         */
        const val BREEDS = 9
    }
}

/**
 * A perrito's walk along its ground (ADR 0008): back and forth between [from] and [to], offsets from its place, now walking
 * and now standing a while. It has its own small random source, seeded by the generator, so the same run walks the same on
 * every phone.
 */
class Walk(
    val from: Float,
    val to: Float,
    private val config: ItemConfig,
    seed: Int,
) {
    var offset = 0f
        private set

    /** The offset of the step before, to draw it between the two on screens faster than the steps (TEC-05). */
    var previousOffset = 0f
        private set
    var facingLeft = false
        private set
    var walking = true
        private set
    private var state = seed
    private var timer = 0f

    init {
        facingLeft = next() % 2 == 0
        timer = between(config.perritoWalkMin, config.perritoWalkMax)
    }

    fun step(dt: Float) {
        previousOffset = offset
        timer -= dt
        if (timer <= 0f) {
            walking = !walking
            timer =
                if (walking) {
                    between(
                        config.perritoWalkMin,
                        config.perritoWalkMax,
                    )
                } else {
                    between(config.perritoStopMin, config.perritoStopMax)
                }
        }
        if (!walking) return
        offset += (if (facingLeft) -config.perritoWalkSpeed else config.perritoWalkSpeed) * dt
        // It turns at the ends of its stretch, never off its ground.
        if (offset <= from) {
            offset = from
            facingLeft = false
        } else if (offset >= to) {
            offset = to
            facingLeft = true
        }
    }

    private fun between(low: Float, high: Float): Float = low + (high - low) * next() / RANDOM_MAX

    /** A number from 0 to [RANDOM_MAX], from a linear congruential generator: small and the same on every platform. */
    private fun next(): Int {
        state = state * 1_103_515_245 + 12_345
        return (state ushr 16) and RANDOM_MAX
    }

    private companion object {
        const val RANDOM_MAX = 0x7FFF
    }
}

/** What Caro collides with: the grounds, the blocks in the air and the platforms, as they are at [time]. */
interface Terrain {
    /** The seconds of the run the terrain is at: where its moving platforms are. */
    var time: Double

    /** How fast the moving platform she stands on, under the span from [left] to [right] at her [feet], goes across; 0 if none. */
    fun carryUnder(left: Double, right: Double, feet: Float): Double

    /** The highest ground top under the span from [left] to [right], or null if it is all gap. Grounds only. */
    fun groundTopUnder(left: Double, right: Double): Int?

    /**
     * What she can stand on under the span from [left] to [right]: the highest top of a ground there, or of a block or a
     * platform whose top is not above her [feet], or null if there is nothing. A moving platform's top may fall between
     * two pixels.
     */
    fun supportUnder(left: Double, right: Double, feet: Float): Float?

    /**
     * The start of the nearest ground or block in her way: it begins after [fromRight] and up to [toRight], and its side
     * reaches clearly into her body, from her [feet] to her [head].
     */
    fun wallAhead(
        fromRight: Double,
        toRight: Double,
        feet: Float,
        head: Float,
    ): Int?

    /**
     * The end of the nearest ground or block behind her that she would back into: it ends before [fromLeft] and down to
     * [toLeft], and its side reaches clearly into her body, from her [feet] to her [head]. The push back of an egg or a
     * fish (Fangusk) is the only thing that moves her backwards.
     */
    fun wallBehind(
        fromLeft: Double,
        toLeft: Double,
        feet: Float,
        head: Float,
    ): Int?

    /** The lowest bottom of a block over the span from [left] to [right] that her head reaches going up from [headBefore] to [head]. */
    fun ceilingAbove(
        left: Double,
        right: Double,
        headBefore: Float,
        head: Float,
    ): Int?
}

/**
 * The course as far as it has been generated: its grounds and its collectibles in order, from what is behind the camera
 * to [end], where the next piece will start. What the camera leaves behind is dropped (TEC-17); the queries loop by index
 * so a step allocates nothing.
 */
class Course : Terrain {
    val grounds = ArrayDeque<Ground>()
    val items = ArrayDeque<Item>()
    val blocks = ArrayDeque<Block>()
    val platforms = ArrayDeque<Platform>()
    var end = 0
        private set

    override var time = 0.0

    fun append(piece: Piece) {
        grounds.addAll(piece.grounds)
        items.addAll(piece.items)
        blocks.addAll(piece.blocks)
        platforms.addAll(piece.platforms)
        end = piece.end
    }

    /** Forgets what ends before [x], keeping at least one ground. */
    fun dropBefore(x: Double) {
        while (grounds.size > 1 && grounds.first().end < x) grounds.removeFirst()
        while (items.isNotEmpty() && items.first().x + Item.SIZE < x) items.removeFirst()
        while (blocks.isNotEmpty() && blocks.first().end < x) blocks.removeFirst()
        while (platforms.isNotEmpty() && platforms.first().end + abs(platforms.first().motion?.dx ?: 0) < x) platforms.removeFirst()
    }

    fun clear() {
        grounds.clear()
        items.clear()
        blocks.clear()
        platforms.clear()
        end = 0
        time = 0.0
    }

    override fun groundTopUnder(left: Double, right: Double): Int? {
        var best = Int.MIN_VALUE
        for (i in 0 until grounds.size) {
            val ground = grounds[i]
            if (ground.end > left && ground.start < right) best = max(best, ground.top)
        }
        return if (best == Int.MIN_VALUE) null else best
    }

    override fun supportUnder(left: Double, right: Double, feet: Float): Float? {
        var best = groundTopUnder(left, right)?.toFloat() ?: Float.NEGATIVE_INFINITY
        for (i in 0 until blocks.size) {
            val block = blocks[i]
            if (block.end > left && block.start < right && block.top <= feet + CORNER_TOLERANCE) best = max(best, block.top.toFloat())
        }
        for (i in 0 until platforms.size) {
            val platform = platforms[i]
            val top = platform.topAt(time).toFloat()
            if (platform.rightAt(time) > left && platform.leftAt(time) < right && top <= feet + CORNER_TOLERANCE) best = max(best, top)
        }
        return if (best == Float.NEGATIVE_INFINITY) null else best
    }

    override fun carryUnder(left: Double, right: Double, feet: Float): Double {
        for (i in 0 until platforms.size) {
            val platform = platforms[i]
            if (platform.motion == null || platform.rightAt(time) <= left || platform.leftAt(time) >= right) continue
            if (abs(platform.topAt(time) - feet) <= CORNER_TOLERANCE) return platform.speedAt(time)
        }
        return 0.0
    }

    override fun wallAhead(
        fromRight: Double,
        toRight: Double,
        feet: Float,
        head: Float,
    ): Int? {
        var nearest = Int.MAX_VALUE
        for (i in 0 until grounds.size) {
            val ground = grounds[i]
            // A wall right at her front edge still stops her: she is pressed against it, not past it.
            if (ground.start >= fromRight && ground.start <= toRight && ground.top > feet + CORNER_TOLERANCE) {
                nearest = min(nearest, ground.start)
            }
        }
        for (i in 0 until blocks.size) {
            val block = blocks[i]
            val inTheWay = block.top > feet + CORNER_TOLERANCE && block.bottom < head - CORNER_TOLERANCE
            if (block.start >= fromRight && block.start <= toRight && inTheWay) nearest = min(nearest, block.start)
        }
        return if (nearest == Int.MAX_VALUE) null else nearest
    }

    override fun wallBehind(
        fromLeft: Double,
        toLeft: Double,
        feet: Float,
        head: Float,
    ): Int? {
        var nearest = Int.MIN_VALUE
        for (i in 0 until grounds.size) {
            val ground = grounds[i]
            if (ground.end <= fromLeft && ground.end >= toLeft && ground.top > feet + CORNER_TOLERANCE) nearest = max(nearest, ground.end)
        }
        for (i in 0 until blocks.size) {
            val block = blocks[i]
            val inTheWay = block.top > feet + CORNER_TOLERANCE && block.bottom < head - CORNER_TOLERANCE
            if (block.end <= fromLeft && block.end >= toLeft && inTheWay) nearest = max(nearest, block.end)
        }
        return if (nearest == Int.MIN_VALUE) null else nearest
    }

    override fun ceilingAbove(
        left: Double,
        right: Double,
        headBefore: Float,
        head: Float,
    ): Int? {
        var lowest = Int.MAX_VALUE
        for (i in 0 until blocks.size) {
            val block = blocks[i]
            if (block.end <= left || block.start >= right || block.bottom < headBefore - CORNER_TOLERANCE || block.bottom > head) continue
            // A corner she is already leaving behind, grazed by a few pixels, does not stop the jump (PLY-05).
            if (block.end - left <= CORNER_NUDGE) continue
            lowest = min(lowest, block.bottom)
        }
        return if (lowest == Int.MAX_VALUE) null else lowest
    }

    companion object {
        /** How far below a ground's top her feet can be when she reaches its edge and still get on it, instead of a wall. */
        const val CORNER_TOLERANCE = 4f

        /** How many pixels of a block's back corner she can graze from below without stopping. */
        const val CORNER_NUDGE = 3.0
    }
}

/**
 * A generated piece of course (GEN-01): its grounds and collectibles, where the next one starts and the difficulty it was
 * made with. Its entry and exit heights are those of its first and last grounds.
 */
class Piece(
    val grounds: List<Ground>,
    val items: List<Item>,
    val end: Int,
    val difficulty: Float = 0f,
    val blocks: List<Block> = emptyList(),
    val platforms: List<Platform> = emptyList(),
) {
    val exitTop: Int get() = grounds.last().top
    val entryTop: Int get() = grounds.first().top
}
