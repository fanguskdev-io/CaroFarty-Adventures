package com.fanguskdev.carofarty.game

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * The course generator (GEN-03, GEN-04, GEN-07; ADR 0004, docs/generacion.md): piece after piece, by rules, with the
 * difficulty growing with the distance, and every piece tried by [JumpLab] before it is handed over. A piece that is not
 * fair is generated again; after [CourseConfig.validatorAttempts] tries, an easy one goes instead.
 *
 * Two random sources with the seed (GEN-05): one lays out the terrain and the other the collectibles, so the same seed gives
 * the same course on any phone. Distances (and with them the speed and the difficulty) are counted from [runStart].
 */
class CourseGenerator(private val config: GameConfig, seed: Long, private val runStart: Double = 0.0) {
    private val course = config.course
    private val movement = config.movement
    private val terrainRandom = Random(seed)
    private val itemRandom = Random(seed xor ITEM_SALT)
    private val lab = JumpLab(movement)
    private val sandbox = Course()
    private val half = movement.hitboxWidth / 2
    private val maxHold = ceil(movement.holdWindow / JumpLab.STEP).toInt()
    private var last: Ground? = null
    private var nextRest = 0.0

    /** The next piece of course. The first one is the flat start. */
    fun next(): Piece {
        val previous = last ?: return start()
        val piece = if (meters(previous.end) >= nextRest) rest(previous) else gapPiece(previous)
        last = piece.grounds.last()
        return piece
    }

    /** The difficulty, from 0 at the start to 1 after [CourseConfig.difficultyRampMeters] (GDD-08). */
    fun difficultyAt(meters: Double): Float {
        val t = (meters / course.difficultyRampMeters).coerceIn(0.0, 1.0).toFloat()
        return t * t * (3 - 2 * t)
    }

    private fun meters(x: Int): Double = (x - runStart) / TILE

    private fun start(): Piece {
        val ground = Ground(START_BEHIND, (runStart + course.startFlatTiles * TILE).toInt(), course.minGroundTop * TILE)
        last = ground
        nextRest = meters(ground.end) + randomRestGap()
        return Piece(listOf(ground), emptyList(), ground.end, difficulty = 0f)
    }

    private fun randomRestGap(): Int = terrainRandom.nextInt(course.restEveryMinMeters, course.restEveryMaxMeters + 1)

    /** A breather (GDD-08): flat ground with nothing that takes points away. */
    private fun rest(previous: Ground): Piece {
        nextRest = meters(previous.end) + randomRestGap()
        val tiles = terrainRandom.nextInt(course.restTiles, course.restTiles + REST_EXTRA_TILES + 1)
        val ground = Ground(previous.end, previous.end + tiles * TILE, previous.top)
        val items = mutableListOf<Item>()
        if (itemRandom.nextFloat() < POSITIVE_ROW_ON_REST) row(ground, ground.start + TILE, items)
        return Piece(listOf(ground), items, ground.end, difficulty = difficultyAt(meters(previous.end)))
    }

    private fun gapPiece(previous: Ground): Piece {
        repeat(course.validatorAttempts) { attempt(previous, easy = false)?.let { return it } }
        return attempt(previous, easy = true)
            ?: error("an easy gap at ${previous.end} cannot be crossed: the settings make every gap impossible")
    }

    private fun attempt(previous: Ground, easy: Boolean): Piece? {
        val meters = meters(previous.end)
        val speed = movement.speedAt(meters)
        val difficulty = difficultyAt(meters)
        val longest = speed * lab.longestAirtime
        val fraction = course.gapFractionStart + (course.gapFractionEnd - course.gapFractionStart) * difficulty
        val widest = max(course.minGapTiles, floor(fraction * longest / TILE).toInt())
        val gapTiles = if (easy) course.minGapTiles else terrainRandom.nextInt(course.minGapTiles, widest + 1)
        val stepTiles = if (easy) 0 else terrainRandom.nextInt(-course.maxStepDown, course.maxStepUp + 1)
        val topTiles = (previous.top / TILE + stepTiles).coerceIn(course.minGroundTop, course.maxGroundTop)
        val minRun = max(course.minGroundTiles, ceil((course.landingMarginTiles * TILE + speed * RUNWAY_SECONDS * 2) / TILE).toInt())
        val runTiles = terrainRandom.nextInt(minRun, max(minRun, course.maxGroundTiles) + 1)
        val start = previous.end + gapTiles * TILE
        val ground = Ground(start, start + runTiles * TILE, topTiles * TILE)

        val items = mutableListOf<Item>()
        val pieceMeters = (ground.end - previous.end).toFloat() / TILE
        val per100m = course.negativesPer100mStart + (course.negativesPer100mEnd - course.negativesPer100mStart) * difficulty
        if (!easy) negatives(previous, ground, items, count = draw(pieceMeters * per100m / 100f))
        if (!fairGap(previous, ground, items, path = null)) return null
        positives(previous, ground, items, pieceMeters)
        items.sortBy { it.x }
        return Piece(listOf(ground), items, ground.end, difficulty)
    }

    /** A whole number of things around [expected]: its whole part, and one more as often as its fraction says. */
    private fun draw(expected: Float): Int {
        val whole = expected.toInt()
        return min(MAX_NEGATIVES_PER_PIECE, whole + if (itemRandom.nextFloat() < expected - whole) 1 else 0)
    }

    /**
     * [count] collectibles that take points away, up to two: on the ground where there is room to jump it, high over the
     * gap, or both. One that cannot be made fair is left out.
     */
    private fun negatives(
        previous: Ground,
        ground: Ground,
        items: MutableList<Item>,
        count: Int,
    ) {
        if (count == 0) return
        val onTheGround = (count >= 2 || itemRandom.nextBoolean()) && obstacle(ground, items)
        if (count >= 2 || !onTheGround) ceiling(previous, ground, items)
    }

    /**
     * A huevo frito or a pescado on [ground], as far in as a landing and a run-up to jump it take (fairGap makes her land
     * before that run-up), and half the time with the choice over it. Whether it went in.
     */
    private fun obstacle(ground: Ground, items: MutableList<Item>): Boolean {
        val speed = movement.speedAt(meters(ground.start))
        val fromTile = ceil(((course.landingMarginTiles + LANDING_ROOM_TILES) * TILE + speed * OBSTACLE_WINDOW_SECONDS) / TILE).toInt()
        val toTile = (ground.end - ground.start) / TILE - OBSTACLE_TO_END_TILES
        if (toTile < fromTile) return false
        val x = ground.start + itemRandom.nextInt(fromTile, toTile + 1) * TILE
        val obstacle = Item(negativeKind(), x, ground.top + GROUND_ITEM_HEIGHT)
        if (!fairObstacle(ground, obstacle)) return false
        items += obstacle
        if (itemRandom.nextFloat() < CHOICE_SHARE) choice(ground, obstacle, items)
        return true
    }

    /** A huevo frito or a pescado high over the gap, where a long jump goes: it asks for a lower one. */
    private fun ceiling(previous: Ground, ground: Ground, items: MutableList<Item>) {
        val ceiling = Item(negativeKind(), (previous.end + ground.start) / 2, previous.top + CEILING_HEIGHT)
        items += ceiling
        if (!fairGap(previous, ground, items, path = null)) items -= ceiling
    }

    private fun negativeKind(): ItemKind = if (itemRandom.nextFloat() < FISH_SHARE) ItemKind.Pescado else ItemKind.Huevo

    /** Perritos and gatitos: an arc along a good jump over the gap, or a row on the ground. */
    private fun positives(
        previous: Ground,
        ground: Ground,
        items: MutableList<Item>,
        pieceMeters: Float,
    ) {
        val expected = pieceMeters * course.positivesPer100m / 100f
        if (itemRandom.nextFloat() >= expected) return
        if (itemRandom.nextBoolean()) {
            val path = mutableListOf<Pair<Double, Float>>()
            if (fairGap(previous, ground, items, path) && path.size >= ARC_ITEMS + 1) {
                for (k in 1..ARC_ITEMS) {
                    val (x, y) = path[path.size * k / (ARC_ITEMS + 1)]
                    items += Item(positiveKind(), x.toInt(), y.toInt())
                }
                return
            }
        }
        row(ground, ground.start + ROW_FROM_TILE * TILE, items)
    }

    private fun row(ground: Ground, from: Int, items: MutableList<Item>) {
        val count = itemRandom.nextInt(ROW_MIN, ROW_MAX + 1)
        for (k in 0 until count) {
            val x = from + k * ROW_SPACING
            if (x > ground.end - TILE) return
            if (items.any { !it.kind.adds && abs(it.x - x) < ROW_CLEARANCE }) continue
            items += Item(positiveKind(), x, ground.top + GROUND_ITEM_HEIGHT)
        }
    }

    private fun positiveKind(): ItemKind = if (itemRandom.nextBoolean()) ItemKind.Perrito else ItemKind.Gatito

    /**
     * Whether a player can cross from [from] to [to] (GEN-10): among the press moments of the last part of the run-up (and
     * the coyote time), at least [FAIR_PRESSES] let [CourseConfig.fairHoldSteps] hold lengths or more land on [to] with
     * margin, leave room to run up to the next jump and touch nothing that takes points away. [path], if given, receives
     * the path of one of those jumps.
     */
    private fun fairGap(
        from: Ground,
        to: Ground,
        items: List<Item>,
        path: MutableList<Pair<Double, Float>>?,
    ): Boolean {
        sandbox.clear()
        sandbox.append(Piece(listOf(from, to), emptyList(), to.end))
        val speed = movement.speedAt(meters(from.end))
        val windowSteps = ceil(PRESS_WINDOW_SECONDS / JumpLab.STEP).toInt()
        val coyoteSteps = floor(movement.coyoteTime / JumpLab.STEP).toInt()
        val startX = max(from.start + half.toDouble(), from.end - half - speed.toDouble() * (windowSteps + 1) * JumpLab.STEP)
        val lastPress = windowSteps + coyoteSteps
        val minLand = to.start + course.landingMarginTiles * TILE
        var maxLand = to.end - speed.toDouble() * RUNWAY_SECONDS
        // Something to jump on the ground she lands on: she has to land with the whole run-up to jump it still ahead.
        for (item in items) {
            if (!item.kind.adds && item.x > to.start && item.x < to.end && item.y - to.top < Item.SIZE) {
                maxLand = min(maxLand, item.x - Item.SIZE / 2 - half - speed.toDouble() * OBSTACLE_WINDOW_SECONDS)
            }
        }
        return fair(lastPress, from.top.toFloat(), startX, items) { landing ->
            landing.landed && landing.top == to.top && landing.x >= minLand && landing.x <= maxLand
        }.also { if (it && path != null) recordGoodPath(path, from.top.toFloat(), startX, items, lastPress, minLand, maxLand, to.top) }
    }

    /** Whether the [obstacle] on [ground] can be jumped, with the same rule as a gap. */
    private fun fairObstacle(ground: Ground, obstacle: Item): Boolean {
        val jump = ObstacleJump(ground, obstacle)
        return fair(jump.windowSteps, ground.top.toFloat(), jump.startX, listOf(obstacle)) { jump.clears(it) }
    }

    /**
     * The choice of GEN-07: a perrito or a gatito at the top of a long jump over the [obstacle]. A quick tap clears the
     * obstacle and passes under it; keeping the finger down longer gets it.
     */
    private fun choice(ground: Ground, obstacle: Item, items: MutableList<Item>) {
        val jump = ObstacleJump(ground, obstacle)
        val avoid = listOf(obstacle)
        for (hold in maxHold downTo maxHold * 2 / 3) {
            for (press in jump.windowSteps downTo 0) {
                if (!jump.clears(lab.tryJump(sandbox, avoid, jump.startX, ground.top.toFloat(), runStart, press, hold))) continue
                val path = mutableListOf<Pair<Double, Float>>()
                lab.tryJump(sandbox, avoid, jump.startX, ground.top.toFloat(), runStart, press, hold, path = path)
                val (x, y) = path.maxByOrNull { it.second } ?: return
                items += Item(positiveKind(), x.toInt(), y.toInt())
                return
            }
        }
    }

    /** The jumps over an [obstacle] on [ground]: where the run-up starts, and whether a landing clears it with room to run. */
    private inner class ObstacleJump(private val ground: Ground, obstacle: Item) {
        val windowSteps = ceil(OBSTACLE_WINDOW_SECONDS / JumpLab.STEP).toInt()
        private val speed = movement.speedAt(meters(obstacle.x))
        val startX =
            max(ground.start + half.toDouble(), obstacle.x - Item.SIZE - half - speed.toDouble() * (windowSteps + 1) * JumpLab.STEP)
        private val past = obstacle.x + Item.SIZE / 2 + half
        private val maxLand = ground.end - speed.toDouble() * RUNWAY_SECONDS

        init {
            sandbox.clear()
            sandbox.append(Piece(listOf(ground), emptyList(), ground.end))
        }

        fun clears(landing: JumpLab.Landing): Boolean =
            landing.landed && !landing.touchedNegative && landing.top == ground.top && landing.x >= past && landing.x <= maxLand
    }

    private inline fun fair(
        lastPress: Int,
        y: Float,
        startX: Double,
        items: List<Item>,
        good: (JumpLab.Landing) -> Boolean,
    ): Boolean {
        var fairPresses = 0
        for (press in lastPress downTo 0) {
            var holds = 0
            for (hold in 0..maxHold) {
                // Enough good holds for this press, or no longer possible: the rest need not be tried.
                if (holds >= course.fairHoldSteps || holds + (maxHold - hold + 1) < course.fairHoldSteps) break
                val landing = lab.tryJump(sandbox, items, startX, y, runStart, press, hold)
                if (!landing.touchedNegative && good(landing)) holds++
            }
            if (holds >= course.fairHoldSteps && ++fairPresses >= FAIR_PRESSES) return true
        }
        return false
    }

    private fun recordGoodPath(
        path: MutableList<Pair<Double, Float>>,
        y: Float,
        startX: Double,
        items: List<Item>,
        lastPress: Int,
        minLand: Int,
        maxLand: Double,
        top: Int,
    ) {
        for (press in lastPress downTo 0) {
            for (hold in maxHold / 2 downTo 0) {
                val landing = lab.tryJump(sandbox, items, startX, y, runStart, press, hold)
                if (landing.landed && !landing.touchedNegative && landing.top == top && landing.x >= minLand && landing.x <= maxLand) {
                    path.clear()
                    lab.tryJump(sandbox, items, startX, y, runStart, press, hold, path = path)
                    return
                }
            }
        }
    }

    internal companion object {
        const val ITEM_SALT = 0x5EEDCAFE0FL
        const val START_BEHIND = -32 * TILE

        /** The run-up a player uses to time a jump, and how much of it is left after a landing. */
        const val PRESS_WINDOW_SECONDS = 0.2f
        const val RUNWAY_SECONDS = 0.3f
        const val OBSTACLE_WINDOW_SECONDS = 0.4f
        const val FAIR_PRESSES = 3

        const val REST_EXTRA_TILES = 8
        const val POSITIVE_ROW_ON_REST = 0.6f
        const val FISH_SHARE = 0.4f
        const val CHOICE_SHARE = 0.5f
        const val MAX_NEGATIVES_PER_PIECE = 2
        const val LANDING_ROOM_TILES = 2
        const val OBSTACLE_TO_END_TILES = 4

        /** Where the collectibles go, above the ground's top: on the ground at her chest, high where a long jump goes. */
        const val GROUND_ITEM_HEIGHT = 8
        const val CEILING_HEIGHT = 74
        const val ARC_ITEMS = 3
        const val ROW_FROM_TILE = 3
        const val ROW_MIN = 2
        const val ROW_MAX = 4
        const val ROW_SPACING = 2 * TILE
        const val ROW_CLEARANCE = 3 * TILE
    }
}
