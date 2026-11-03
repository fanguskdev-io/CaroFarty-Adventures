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

    /** The try that goes on from [caro] with the finger up, in [tryHolds]. */
    private val branch = Caro(movement)

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
     * When she had stood on the platforms that fall (ENM-16) before a try, and before a branch of [tryHolds]: put back
     * after each, so a try leaves the terrain as it found it.
     */
    @PublishedApi
    internal val falls = DoubleArray(MAX_FALLS)
    private val branchFalls = DoubleArray(MAX_FALLS)

    /**
     * Runs her from ([x], [y]) on [terrain], presses after [pressAfter] steps, keeps the finger down [hold] steps and
     * follows her until she lands, falls or [maxSteps] go by; and, with [runAfter], that many steps more of running after
     * the landing, for what she runs into then. [path], if given, receives the centre of her body at every step from the
     * press on; [watch], an animal to know whether she touches it. With [giveUpBelow], a try that is falling below that
     * height has no landing to look for any more, and ends there. The terrain goes on from [time], as in a run, and is
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
        giveUpBelow: Float = Float.NEGATIVE_INFINITY,
    ): Landing {
        val before = terrain.time
        terrain.saveFalls(falls)
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
            // Only after the press: running off an edge, she dips a little before a jump in the coyote time.
            if (step >= pressAfter && airborne && caro.vy < 0f && caro.y < giveUpBelow) break
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
        terrain.restoreFalls(falls)
        return landing
    }

    /**
     * Every hold length from 0 to [maxHold] of a jump pressed after [pressAfter] steps, as [tryJump] tries them one by one,
     * but at a fraction of the cost: the run-up and the steps with the finger down are simulated once, and each hold goes
     * on from there with the finger up. [each] gets each hold and its landing, in order, and says whether to go on. With
     * [runAfter] the press may come after a landing, so each hold is tried on its own, as [tryJump] does.
     */
    inline fun tryHolds(
        terrain: Terrain,
        items: List<Item>,
        x: Double,
        y: Float,
        runStart: Double,
        pressAfter: Int,
        maxHold: Int,
        time: Double = terrain.time,
        watch: Item? = null,
        runAfter: Int = 0,
        giveUpBelow: Float = Float.NEGATIVE_INFINITY,
        each: (hold: Int, landing: Landing) -> Boolean,
    ) {
        if (runAfter > 0) {
            for (hold in 0..maxHold) {
                val landing = tryJump(
                    terrain,
                    items,
                    x,
                    y,
                    runStart,
                    pressAfter,
                    hold,
                    time = time,
                    watch = watch,
                    runAfter = runAfter,
                    giveUpBelow = giveUpBelow,
                )
                if (!each(hold, landing)) break
            }
            return
        }
        val before = terrain.time
        terrain.saveFalls(falls)
        terrain.time = time
        startHolds(x, y, runStart)
        for (step in 0 until pressAfter) {
            if (!holdStep(terrain, items, watch, pressed = false, held = false, giveUpBelow = Float.NEGATIVE_INFINITY)) break
        }
        for (hold in 0..maxHold) {
            // The finger has been down [hold] steps: from here on, up.
            if (hold > 0) holdStep(terrain, items, watch, pressed = hold == 1, held = true, giveUpBelow = giveUpBelow)
            val result = goOn(terrain, items, watch, pressed = hold == 0, runAfter, giveUpBelow, MAX_STEPS - pressAfter - hold)
            if (!each(hold, result)) break
        }
        terrain.time = before
        terrain.restoreFalls(falls)
    }

    /** [tryHolds]: places her for a new try. */
    @PublishedApi
    internal fun startHolds(x: Double, y: Float, runStart: Double) {
        caro.place(x, y, runStart)
        caro.start()
        mainNegative = false
        mainWatched = false
        mainLanded = false
        mainGaveUp = false
        airborneMain = false
    }

    /** What the try with the finger down has touched so far, and whether it has already landed or fallen. */
    private var mainNegative = false
    private var mainWatched = false
    private var mainLanded = false
    private var mainGaveUp = false
    private var airborneMain = false

    /** [tryHolds]: one more step of the try with the finger down. Whether it can go on. */
    @PublishedApi
    internal fun holdStep(
        terrain: Terrain,
        items: List<Item>,
        watch: Item?,
        pressed: Boolean,
        held: Boolean,
        giveUpBelow: Float,
    ): Boolean {
        if (mainLanded || mainGaveUp || caro.state.over) return false
        terrain.time += STEP
        caro.step(STEP, pressed, held, terrain)
        if (touchesNegative(caro, items)) mainNegative = true
        if (watch != null && overlaps(caro, watch, positiveBox, movement)) mainWatched = true
        if (caro.state == CaroState.Jumping || caro.state == CaroState.Falling) airborneMain = true
        // Falling past where it could land, after the press: every longer hold gives up too, as [tryJump] does.
        if (airborneMain && caro.vy < 0f && caro.y < giveUpBelow) {
            mainGaveUp = true
            return false
        }
        // Landed with the finger still down: every longer hold lands the same, as it does not jump again.
        if (airborneMain && caro.state.grounded) mainLanded = true
        return true
    }

    /**
     * [tryHolds]: from the try with the finger down, on with it up until she lands, falls, goes below [giveUpBelow] or
     * [maxSteps] go by, and [runAfter] steps more after landing; [pressed] for the press itself, the hold of 0.
     */
    @PublishedApi
    internal fun goOn(
        terrain: Terrain,
        items: List<Item>,
        watch: Item?,
        pressed: Boolean,
        runAfter: Int,
        giveUpBelow: Float,
        maxSteps: Int,
    ): Landing {
        val time = terrain.time
        terrain.saveFalls(branchFalls)
        branch.copyFrom(caro)
        landing.landed = false
        landing.touchedNegative = mainNegative
        landing.touchedWatched = mainWatched
        if (mainGaveUp) {
            terrain.restoreFalls(branchFalls)
            return landing
        }
        var airborne = airborneMain && !mainLanded
        if (mainLanded) {
            landing.landed = true
            landing.top = branch.y.roundToInt()
            landing.x = branch.x
            landing.time = terrain.time
        }
        var after = if (mainLanded) 0 else -1
        var press = pressed
        for (step in 0 until maxSteps.coerceAtLeast(1)) {
            if (after >= runAfter && after >= 0) break
            terrain.time += STEP
            branch.step(STEP, press, held = false, terrain)
            press = false
            if (touchesNegative(branch, items)) landing.touchedNegative = true
            if (watch != null && overlaps(branch, watch, positiveBox, movement)) landing.touchedWatched = true
            if (after >= 0) {
                if (++after >= runAfter || branch.state.over) break
                continue
            }
            if (branch.state == CaroState.Jumping || branch.state == CaroState.Falling) airborne = true
            if (airborne && branch.vy < 0f && branch.y < giveUpBelow) break
            if (airborne && branch.state.grounded) {
                landing.landed = true
                landing.top = branch.y.roundToInt()
                landing.x = branch.x
                landing.time = terrain.time
                if (runAfter <= 0) break
                after = 0
                continue
            }
            if (branch.state.over) break
        }
        terrain.time = time
        terrain.restoreFalls(branchFalls)
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

    private fun touchesNegative(items: List<Item>): Boolean = touchesNegative(caro, items)

    private fun touchesNegative(who: Caro, items: List<Item>): Boolean {
        for (i in items.indices) {
            val item = items[i]
            if (!item.taken && !item.kind.adds && overlaps(who, item, negativeBox, movement)) return true
        }
        return false
    }

    companion object {
        /** The step of the simulation, the same as the game's (TEC-05). */
        const val STEP = 1f / 60f

        /** How many steps a try lasts at most, as [tryJump]'s default. */
        const val MAX_STEPS = 240

        /** More platforms that fall than a course ever has at once. */
        const val MAX_FALLS = 64
    }
}

/**
 * Whether Caro's collision box overlaps [item]'s, a square [box] pixels wide around its centre (ItemConfig); one that flies,
 * where it is when she has got to [reach] (Flight): in the validator's tries, where she is.
 */
fun overlaps(
    caro: Caro,
    item: Item,
    box: Int,
    movement: MovementConfig,
    reach: Double = caro.x,
): Boolean {
    val half = box / 2
    if (item.flight == null) {
        val x = item.currentX
        return caro.right > x - half &&
            caro.left < x + half &&
            caro.y + movement.hitboxHeight > item.y - half &&
            caro.y < item.y + half
    }
    if (!item.outAt(reach)) return false
    val x = item.xAt(reach)
    val y = item.yAt(reach)
    return caro.right > x - half && caro.left < x + half && caro.y + movement.hitboxHeight > y - half && caro.y < y + half
}
