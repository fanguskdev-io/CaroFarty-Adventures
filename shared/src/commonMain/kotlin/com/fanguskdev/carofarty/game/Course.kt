package com.fanguskdev.carofarty.game

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

/** A collectible centred at ([x], [y]) in world pixels, [y] above the bottom edge. */
class Item(val kind: ItemKind, val x: Int, val y: Int) {
    var taken = false

    companion object {
        /** The side of a collectible's collision box (CHR-08). */
        const val SIZE = 12
    }
}

/** What Caro collides with: the grounds and the blocks in the air. */
interface Terrain {
    /** The highest ground top under the span from [left] to [right], or null if it is all gap. Grounds only. */
    fun groundTopUnder(left: Double, right: Double): Int?

    /**
     * What she can stand on under the span from [left] to [right]: the highest top of a ground there, or of a block whose
     * top is not above her [feet], or null if there is nothing.
     */
    fun supportUnder(left: Double, right: Double, feet: Float): Int?

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
    var end = 0
        private set

    fun append(piece: Piece) {
        grounds.addAll(piece.grounds)
        items.addAll(piece.items)
        blocks.addAll(piece.blocks)
        end = piece.end
    }

    /** Forgets what ends before [x], keeping at least one ground. */
    fun dropBefore(x: Double) {
        while (grounds.size > 1 && grounds.first().end < x) grounds.removeFirst()
        while (items.isNotEmpty() && items.first().x + Item.SIZE < x) items.removeFirst()
        while (blocks.isNotEmpty() && blocks.first().end < x) blocks.removeFirst()
    }

    fun clear() {
        grounds.clear()
        items.clear()
        blocks.clear()
        end = 0
    }

    override fun groundTopUnder(left: Double, right: Double): Int? {
        var best = Int.MIN_VALUE
        for (i in 0 until grounds.size) {
            val ground = grounds[i]
            if (ground.end > left && ground.start < right) best = max(best, ground.top)
        }
        return if (best == Int.MIN_VALUE) null else best
    }

    override fun supportUnder(left: Double, right: Double, feet: Float): Int? {
        var best = groundTopUnder(left, right) ?: Int.MIN_VALUE
        for (i in 0 until blocks.size) {
            val block = blocks[i]
            if (block.end > left && block.start < right && block.top <= feet + CORNER_TOLERANCE) best = max(best, block.top)
        }
        return if (best == Int.MIN_VALUE) null else best
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
) {
    val exitTop: Int get() = grounds.last().top
    val entryTop: Int get() = grounds.first().top
}
