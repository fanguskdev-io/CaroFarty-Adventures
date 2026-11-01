package com.fanguskdev.carofarty.game

import kotlin.math.max

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

/** A collectible centred at ([x], [y]) in world pixels, [y] above the bottom edge. */
class Item(val kind: ItemKind, val x: Int, val y: Int) {
    var taken = false

    companion object {
        /** The side of a collectible's collision box (CHR-08). */
        const val SIZE = 12
    }
}

/** What Caro collides with. */
interface Terrain {
    /** The highest ground top under the span from [left] to [right], or null if it is all gap. */
    fun groundTopUnder(left: Double, right: Double): Int?

    /** The start of a ground whose top is clearly above [feet] and that begins after [fromRight] and up to [toRight]. */
    fun wallAhead(fromRight: Double, toRight: Double, feet: Float): Int?
}

/**
 * The course as far as it has been generated: its grounds and its collectibles in order, from what is behind the camera
 * to [end], where the next piece will start. What the camera leaves behind is dropped (TEC-17); the queries loop by index
 * so a step allocates nothing.
 */
class Course : Terrain {
    val grounds = ArrayDeque<Ground>()
    val items = ArrayDeque<Item>()
    var end = 0
        private set

    fun append(piece: Piece) {
        grounds.addAll(piece.grounds)
        items.addAll(piece.items)
        end = piece.end
    }

    /** Forgets what ends before [x], keeping at least one ground. */
    fun dropBefore(x: Double) {
        while (grounds.size > 1 && grounds.first().end < x) grounds.removeFirst()
        while (items.isNotEmpty() && items.first().x + Item.SIZE < x) items.removeFirst()
    }

    fun clear() {
        grounds.clear()
        items.clear()
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

    override fun wallAhead(fromRight: Double, toRight: Double, feet: Float): Int? {
        for (i in 0 until grounds.size) {
            val ground = grounds[i]
            if (ground.start > fromRight && ground.start <= toRight && ground.top > feet + CORNER_TOLERANCE) return ground.start
        }
        return null
    }

    companion object {
        /** How far below a ground's top her feet can be when she reaches its edge and still get on it, instead of a wall. */
        const val CORNER_TOLERANCE = 4f
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
) {
    val exitTop: Int get() = grounds.last().top
    val entryTop: Int get() = grounds.first().top
}
