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
    private val lab = JumpLab(movement, config.items.negativeBox)
    private val sandbox = Course()

    /** The blocks of the piece being made, which the validator's sandbox carries too. */
    private val pieceBlocks = mutableListOf<Block>()
    private val half = movement.hitboxWidth / 2
    private val maxHold = ceil(movement.holdWindow / JumpLab.STEP).toInt()
    private var last: Ground? = null
    private var nextCalm = 0.0
    private var calmLeft = 0

    /** The shapes of the last two pieces (see [shapeOf]), so no difficult piece comes three times alike in a row. */
    private val shapes = intArrayOf(NO_SHAPE, NO_SHAPE)

    /**
     * The next piece of course. The first one is the flat start; then a stretch of tension and a calm one take turns (GDD-08;
     * guía 1 de GameMaster, «calma, tensión, calma»).
     */
    fun next(): Piece {
        val previous = last ?: return start()
        if (calmLeft == 0 && meters(previous.end) >= nextCalm) {
            calmLeft = terrainRandom.nextInt(course.calmMinPieces, course.calmMaxPieces + 1)
        }
        val piece = if (calmLeft > 0) calm(previous) else gapPiece(previous)
        last = piece.grounds.last()
        return piece
    }

    /** The difficulty, from 0 at the start to 1 after [CourseConfig.difficultyRampMeters] (GDD-08). */
    fun difficultyAt(meters: Double): Float {
        val t = (meters / course.difficultyRampMeters).coerceIn(0.0, 1.0).toFloat()
        return t * t * (3 - 2 * t)
    }

    private fun meters(x: Int): Double = (x - runStart) / TILE

    /** The flat start: perritos and gatitos within reach from the first seconds and nothing else (guía 1, principio 1). */
    private fun start(): Piece {
        val ground = Ground(START_BEHIND, (runStart + course.startFlatTiles * TILE).toInt(), course.minGroundTop * TILE)
        last = ground
        nextCalm = meters(ground.end) + randomTension()
        val items = mutableListOf<Item>()
        row(ground, (runStart + START_ROW_TILE * TILE).toInt(), items)
        return Piece(listOf(ground), items, ground.end, difficulty = 0f)
    }

    private fun randomTension(): Int = terrainRandom.nextInt(course.tensionMinMeters, course.tensionMaxMeters + 1)

    /**
     * A piece of a calm stretch: flat ground or the narrowest gap at the same height, with perritos and gatitos and nothing
     * that takes points away. After the last one, the next stretch of tension.
     */
    private fun calm(previous: Ground): Piece {
        calmLeft--
        val piece = (if (terrainRandom.nextBoolean()) attempt(previous, easy = true) else null) ?: flat(previous)
        if (calmLeft == 0) nextCalm = meters(piece.end) + randomTension()
        return piece
    }

    private fun flat(previous: Ground): Piece {
        val tiles = terrainRandom.nextInt(CALM_FLAT_MIN_TILES, CALM_FLAT_MAX_TILES + 1)
        val ground = Ground(previous.end, previous.end + tiles * TILE, previous.top)
        val items = mutableListOf<Item>()
        row(ground, ground.start + TILE, items)
        remember(NO_SHAPE)
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
        pieceBlocks.clear()
        val pieceMeters = (ground.end - previous.end).toFloat() / TILE
        val per100m = course.negativesPer100mStart + (course.negativesPer100mEnd - course.negativesPer100mStart) * difficulty
        if (!easy) negatives(previous, ground, items, count = draw(pieceMeters * per100m / 100f))
        if (!easy) airObstacles(previous, ground, items, difficulty)
        if (!fairGap(previous, ground, items, path = null)) return null
        // The step as built, which the limits of height may have cut short.
        val shape = shapeOf(gapTiles, (ground.top - previous.top) / TILE, ground, items)
        // Never three difficult pieces alike in a row (guía 1): a third one is made again.
        if (!easy && difficult(gapTiles, items) && shape == shapes[0] && shape == shapes[1]) return null
        positives(previous, ground, items, pieceMeters)
        items.sortBy { it.x }
        remember(if (easy) NO_SHAPE else shape)
        return Piece(listOf(ground), items, ground.end, difficulty, pieceBlocks.toList())
    }

    /**
     * The obstacles in the air that Fangusk asked for (ADR 0007), more often with the difficulty: a huevo frito or a
     * pescado, or a solid block, on the path of the jump anyone would make first over the gap, so it has to be timed or
     * measured another way. One that would leave the piece unfair is taken away again.
     */
    private fun airObstacles(
        previous: Ground,
        ground: Ground,
        items: MutableList<Item>,
        difficulty: Float,
    ) {
        if (difficulty < course.airFromDifficulty) return
        val chance = course.airChanceStart + (course.airChanceEnd - course.airChanceStart) * difficulty
        if (itemRandom.nextFloat() >= chance) return
        val path = mutableListOf<Pair<Double, Float>>()
        if (!fairGap(previous, ground, items, path) || path.size < MIN_AIR_PATH) return
        // Her body's centre around the top of that jump, where shorter jumps pass under and higher ones over, and over the
        // gap itself: over the run-up she would run into it before any jump the validator tries.
        val half = Item.SIZE / 2
        val from = path.size * AIR_FROM_TENTHS / 10
        val to = path.size * AIR_TO_TENTHS / 10
        val overTheGap = (from until to).filter { path[it].first - half > previous.end && path[it].first + half < ground.start }
        if (overTheGap.isEmpty()) return
        val wantsBlock = itemRandom.nextFloat() < course.airBlockShare
        repeat(AIR_TRIES) {
            val (x, centre) = path[overTheGap[itemRandom.nextInt(overTheGap.size)]]
            val placed = if (wantsBlock) block(previous, ground, items, x, centre) else onThePath(previous, ground, items, x, centre)
            if (placed) return
        }
    }

    /** A huevo frito or a pescado where her body would be at ([x], [centre]) on the obvious jump. Whether it stayed. */
    private fun onThePath(
        previous: Ground,
        ground: Ground,
        items: MutableList<Item>,
        x: Double,
        centre: Float,
    ): Boolean {
        // A little above or below her body on that jump: it still catches the obvious jump and leaves room around it.
        val item = Item(negativeKind(), x.toInt(), centre.toInt() + itemRandom.nextInt(-AIR_SPREAD, AIR_SPREAD + 1))
        items += item
        if (fairGap(previous, ground, items, path = null)) return true
        items -= item
        return false
    }

    /** A block whose bottom her head reaches at ([x], [centre]) of the obvious jump, which it cuts short. Whether it stayed. */
    private fun block(
        previous: Ground,
        ground: Ground,
        items: List<Item>,
        x: Double,
        centre: Float,
    ): Boolean {
        val width = TILE * itemRandom.nextInt(1, MAX_BLOCK_TILES + 1)
        val left = (x - width / 2).toInt()
        val bottom = (centre + movement.hitboxHeight / 2 - BLOCK_BITE).toInt()
        val block = Block(left, left + width, bottom, bottom + TILE)
        // Never so low over a ground that she cannot run under it, and never over a collectible.
        for (under in listOf(previous, ground)) {
            if (block.end > under.start && block.start < under.end && block.bottom < under.top + BLOCK_CLEARANCE) return false
        }
        if (items.any { overlaps(it, block) }) return false
        pieceBlocks += block
        if (fairGap(previous, ground, items, path = null) && obstaclesStillFair(ground, items)) return true
        pieceBlocks -= block
        return false
    }

    private fun overlaps(item: Item, block: Block): Boolean {
        val half = Item.SIZE / 2
        return item.x + half > block.start && item.x - half < block.end && item.y + half > block.bottom && item.y - half < block.top
    }

    /** Whether every huevo frito and pescado on [ground] can still be jumped with the piece's blocks in place. */
    private fun obstaclesStillFair(ground: Ground, items: List<Item>): Boolean = items.all { item ->
        val onTheGround = !item.kind.adds && item.x >= ground.start && item.x < ground.end && item.y - ground.top < Item.SIZE
        !onTheGround || fairObstacle(ground, item)
    }

    /** What makes a piece look like another: its gap, its step and whether it has something to jump over the gap or on the ground. */
    private fun shapeOf(
        gapTiles: Int,
        stepTiles: Int,
        ground: Ground,
        items: List<Item>,
    ): Int {
        var negatives = if (pieceBlocks.isEmpty()) 0 else WITH_A_BLOCK
        for (item in items) {
            if (!item.kind.adds) negatives = negatives or if (item.x < ground.start) OVER_THE_GAP else ON_THE_GROUND
        }
        return ((gapTiles * SHAPE_STEPS) + stepTiles + course.maxStepDown) * SHAPE_NEGATIVES + negatives
    }

    private fun difficult(gapTiles: Int, items: List<Item>): Boolean =
        gapTiles >= DIFFICULT_GAP_TILES || pieceBlocks.isNotEmpty() || items.any { !it.kind.adds }

    private fun remember(shape: Int) {
        shapes[1] = shapes[0]
        shapes[0] = shape
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
                    // Not where it would touch a ground (by the far edge, a landing passes close to its corner) or a block.
                    val under = sandbox.groundTopUnder(x - Item.SIZE / 2, x + Item.SIZE / 2)
                    val item = Item(positiveKind(), x.toInt(), y.toInt())
                    if ((under == null || y - Item.SIZE / 2 >= under) && pieceBlocks.none { overlaps(item, it) }) items += item
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
        sandbox.append(Piece(listOf(from, to), emptyList(), to.end, blocks = pieceBlocks))
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
            sandbox.append(Piece(listOf(ground), emptyList(), ground.end, blocks = pieceBlocks))
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
            // Not enough moments left to make it fair: the rest need not be tried.
            if (fairPresses + press + 1 < FAIR_PRESSES) return false
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

        const val START_ROW_TILE = 8
        const val CALM_FLAT_MIN_TILES = 8
        const val CALM_FLAT_MAX_TILES = 14
        const val FISH_SHARE = 0.4f
        const val CHOICE_SHARE = 0.5f
        const val MAX_NEGATIVES_PER_PIECE = 2
        const val DIFFICULT_GAP_TILES = 4

        /** How a piece's shape is counted: room for every step, and a bit for each place of the negatives. */
        const val NO_SHAPE = -1
        const val SHAPE_STEPS = 16
        const val SHAPE_NEGATIVES = 8
        const val OVER_THE_GAP = 1
        const val ON_THE_GROUND = 2
        const val WITH_A_BLOCK = 4

        /**
         * The obstacles in the air: how long the obvious jump must be to carry one, the middle of it where it goes (in
         * tenths), how wide a block is at most, how far into her head its bottom reaches on that jump and how much room it
         * leaves over a ground, her whole sprite and a little more, so she always runs under it.
         */
        const val MIN_AIR_PATH = 10
        const val AIR_FROM_TENTHS = 3
        const val AIR_TO_TENTHS = 7
        const val AIR_TRIES = 4
        const val AIR_SPREAD = 8
        const val MAX_BLOCK_TILES = 2
        const val BLOCK_BITE = 4
        const val BLOCK_CLEARANCE = 28
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
