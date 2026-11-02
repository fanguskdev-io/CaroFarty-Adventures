package com.fanguskdev.carofarty.game

import kotlin.math.roundToInt

/**
 * The jump validator (GEN-10, ADR 0004): it tries the jumps a player can make with the very code that runs the game, a Caro
 * of its own on a terrain it is given, so what it says can be done can be done. One instance is reused for every try:
 * trying allocates nothing.
 */
class JumpLab(
    private val movement: MovementConfig,
    private val negativeBox: Int = ItemConfig().negativeBox,
    private val positiveBox: Int = ItemConfig().positiveBox,
) {
    private val caro = Caro(movement)

    /** How a try ended: where she landed (or that she fell) and whether she touched something that takes points away. */
    class Landing {
        var landed = false
            internal set
        var top = 0
            internal set
        var x = 0.0
            internal set
        var touchedNegative = false
            internal set

        /** Whether she touched the item the try was told to watch, an animal, at any moment of it, the run-up included. */
        var touchedWatched = false
            internal set

        /** The seconds of the run when she landed: where the moving platforms were. */
        var time = 0.0
            internal set
    }

    private val landing = Landing()

    /**
     * Runs her from ([x], [y]) on [terrain], presses after [pressAfter] steps, keeps the finger down [hold] steps and
     * follows her until she lands, falls or [maxSteps] go by; and, with [runAfter], that many steps more of running after
     * the landing, for what she runs into then. [path], if given, receives the centre of her body at every step from the
     * press on; [watch], an animal to know whether she touches it. The terrain goes on from [time], as in a run, and is
     * left at the time it had. The result is reused by the next try.
     */
    fun tryJump(
        terrain: Terrain,
        items: List<Item>,
        x: Double,
        y: Float,
        runStart: Double,
        pressAfter: Int,
        hold: Int,
        maxSteps: Int = 240,
        path: MutableList<Pair<Double, Float>>? = null,
        time: Double = terrain.time,
        watch: Item? = null,
        runAfter: Int = 0,
    ): Landing {
        val before = terrain.time
        terrain.time = time
        caro.place(x, y, runStart)
        caro.start()
        landing.landed = false
        landing.touchedNegative = false
        landing.touchedWatched = false
        var airborne = false
        var after = -1
        for (step in 0 until maxSteps) {
            val pressed = step == pressAfter
            val held = step >= pressAfter && step < pressAfter + hold
            terrain.time += STEP
            caro.step(STEP, pressed, held, terrain)
            if (touchesNegative(items)) landing.touchedNegative = true
            if (watch != null && overlaps(caro, watch, positiveBox, movement)) landing.touchedWatched = true
            if (after >= 0) {
                if (++after >= runAfter || caro.state.over) break
                continue
            }
            if (step >= pressAfter) path?.add(caro.x to caro.y + movement.hitboxHeight / 2)
            if (caro.state == CaroState.Jumping || caro.state == CaroState.Falling) airborne = true
            if (airborne && caro.state.grounded) {
                landing.landed = true
                landing.top = caro.y.roundToInt()
                landing.x = caro.x
                landing.time = terrain.time
                if (runAfter <= 0) break
                after = 0
                continue
            }
            if (caro.state.over) break
        }
        terrain.time = before
        return landing
    }

    /** How long a jump with the finger down the whole window stays in the air, in seconds, landing at its own height. */
    val longestAirtime: Float by lazy {
        val flat = Course().apply { append(Piece(listOf(Ground(0, 1_000_000, TILE * 4)), emptyList(), 1_000_000)) }
        var steps = 0
        caro.place(100.0, TILE * 4f)
        caro.start()
        caro.step(STEP, pressed = true, held = true, terrain = flat)
        while (!caro.state.grounded && steps < 1_000) {
            caro.step(STEP, pressed = false, held = true, terrain = flat)
            steps++
        }
        (steps + 1) * STEP
    }

    private fun touchesNegative(items: List<Item>): Boolean {
        for (i in items.indices) {
            val item = items[i]
            if (!item.taken && !item.kind.adds && overlaps(caro, item, negativeBox, movement)) return true
        }
        return false
    }

    companion object {
        /** The step of the simulation, the same as the game's (TEC-05). */
        const val STEP = 1f / 60f
    }
}

/** Whether Caro's collision box overlaps [item]'s, a square [box] pixels wide around its centre (ItemConfig). */
fun overlaps(
    caro: Caro,
    item: Item,
    box: Int,
    movement: MovementConfig,
): Boolean {
    val half = box / 2
    val x = item.currentX
    return caro.right > x - half &&
        caro.left < x + half &&
        caro.y + movement.hitboxHeight > item.y - half &&
        caro.y < item.y + half
}
