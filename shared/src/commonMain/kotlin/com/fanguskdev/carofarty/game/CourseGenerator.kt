package com.fanguskdev.carofarty.game

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
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

    /** The blocks and the platforms of the piece being made, which the validator's sandbox carries too. */
    private val pieceBlocks = mutableListOf<Block>()
    private val piecePlatforms = mutableListOf<Platform>()

    /** How many challenges are left before the next one with platforms (ADR 0009: one of every 2 or 3). */
    private var untilPlatforms = 0
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
        val piece = if (calmLeft > 0) calm(previous) else challenge(previous)
        last = piece.grounds.last()
        return piece
    }

    /**
     * A piece of a stretch of tension: one of every [CourseConfig.platformEveryMin] to [CourseConfig.platformEveryMax] is a
     * gap that only platforms cross (ADR 0009), and the rest the gaps of always. A gap with platforms that does not come
     * out fair goes as a gap of always.
     */
    private fun challenge(previous: Ground): Piece {
        if (untilPlatforms > 0) {
            untilPlatforms--
            return gapPiece(previous)
        }
        untilPlatforms = terrainRandom.nextInt(course.platformEveryMin, course.platformEveryMax + 1) - 1
        repeat(course.validatorAttempts) { platformPiece(previous)?.let { return it } }
        return gapPiece(previous)
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
        untilPlatforms = terrainRandom.nextInt(course.platformEveryMin, course.platformEveryMax + 1) - 1
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
        val tiles = terrainRandom.nextInt(CALM_FLAT_MIN_TILES, max(CALM_FLAT_MIN_TILES, course.maxGroundTiles) + 1)
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
        val minRun = shortestGround(speed)
        // A piece with a platform and its reward (ADR 0008) gets a ground long enough for it at the speed of the moment.
        val withPlatform = !easy && itemRandom.nextFloat() < course.rewardPlatformShare
        val platformRun = ceil((course.landingMarginTiles * TILE + speed * PLATFORM_SECONDS) / TILE).toInt() + PLATFORM_MAX_TILES
        val shortest = if (withPlatform) max(minRun, platformRun) else minRun
        val runTiles = terrainRandom.nextInt(shortest, max(shortest, course.maxGroundTiles) + 1)
        val start = previous.end + gapTiles * TILE
        val ground = Ground(start, start + runTiles * TILE, topTiles * TILE)

        val items = mutableListOf<Item>()
        pieceBlocks.clear()
        piecePlatforms.clear()
        val pieceMeters = (ground.end - previous.end).toFloat() / TILE
        val per100m = course.negativesPer100mStart + (course.negativesPer100mEnd - course.negativesPer100mStart) * difficulty
        if (!easy) negatives(previous, ground, items, count = draw(pieceMeters * per100m / 100f))
        if (!easy) airObstacles(previous, ground, items, difficulty)
        if (!fairGap(previous, ground, items, path = null)) return null
        // The step as built, which the limits of height may have cut short.
        val shape = shapeOf(gapTiles, (ground.top - previous.top) / TILE, ground, items)
        // Never three difficult pieces alike in a row (guía 1): a third one is made again.
        if (!easy && difficult(gapTiles, items) && shape == shapes[0] && shape == shapes[1]) return null
        val rewarded = positives(previous, ground, items, pieceMeters, withPlatform)
        // A long ground made for a platform with a reward that did not fit is made again: no long flat grounds (ADR 0009).
        if (withPlatform && !rewarded) return null
        items.sortBy { it.x }
        remember(if (easy) NO_SHAPE else shape)
        return Piece(listOf(ground), items, ground.end, difficulty, pieceBlocks.toList(), piecePlatforms.toList())
    }

    /**
     * The shortest ground at [speed] (ADR 0009, Fangusk: «suelos más cortos»): room to land with margin and to take off
     * again without hurrying, which at the start is 4 tiles.
     */
    private fun shortestGround(speed: Float): Int = max(
        course.minGroundTiles,
        ceil((course.landingMarginTiles * TILE + speed * (RUNWAY_SECONDS + LANDING_SPREAD_SECONDS)) / TILE).toInt(),
    )

    /**
     * A gap too wide for any jump, that only platforms cross (ADR 0009, Fangusk: «plataformas sobre las que saltar de
     * forma obligatoria para salvar distancias imposibles en salto, como se hace en Super Mario»): 1 to 3 platforms, more
     * of them with the difficulty, each hop between them fair with the same rule as a gap, and the whole gap wider than
     * the longest jump of the moment. Perritos and gatitos sometimes wait on them. Null if this one did not come out fair.
     */
    private fun platformPiece(previous: Ground): Piece? {
        val meters = meters(previous.end)
        val speed = movement.speedAt(meters)
        val difficulty = difficultyAt(meters)
        val count = 1 + (terrainRandom.nextFloat() * (1 + difficulty * (course.platformMaxCount - 1))).toInt()
            .coerceAtMost(course.platformMaxCount - 1)
        // The heights first: each platform a little higher or lower than what comes before it, and the ground after them.
        val tops = IntArray(count + 1)
        var top = previous.top / TILE
        for (k in 0..count) {
            val step = when (val roll = terrainRandom.nextFloat()) {
                in 0f..PLATFORM_DOWN_SHARE -> -1
                in PLATFORM_DOWN_SHARE..PLATFORM_LEVEL_SHARE -> 0
                else -> if (roll > 1f - PLATFORM_UP_TWO_SHARE * difficulty) 2 else 1
            }
            val highest = if (k == count) course.maxGroundTop else course.maxGroundTop + PLATFORM_ABOVE_GROUNDS
            top = (top + step).coerceIn(course.minGroundTop, highest)
            tops[k] = top * TILE
        }
        // Each platform wide enough to land on and take off again, and wider when it is lower than what came before.
        val width = shortestGround(speed)
        val widths = IntArray(count) { k ->
            val drop = max(0, (if (k == 0) previous.top else tops[k - 1]) - tops[k]) / TILE
            width + drop + terrainRandom.nextInt(0, PLATFORM_EXTRA_WIDTH + 1)
        }
        // The hops, as wide as it takes for the whole gap to be beyond the longest jump, falling to the ground after it
        // too, and none beyond a comfortable one.
        val fall = max(0, previous.top - tops[count]).toFloat()
        val longest = speed * (lab.longestAirtime + sqrt(2 * fall / movement.gravity))
        val maxHop = max(MIN_HOP_TILES, floor(HOP_SHARE * speed * lab.longestAirtime / TILE).toInt())
        val needed = ceil((longest + TILE) / TILE).toInt() - widths.sum()
        val hops = IntArray(count + 1) { MIN_HOP_TILES }
        var missing = needed - hops.sum()
        var tries = 0
        while (missing > 0 && tries++ < HOP_TRIES) {
            val k = terrainRandom.nextInt(hops.size)
            if (hops[k] < maxHop) {
                hops[k]++
                missing--
            }
        }
        if (missing > 0) return null
        // Some more width here and there, so they do not all look alike.
        for (k in hops.indices) if (hops[k] < maxHop && terrainRandom.nextFloat() < WIDER_HOP_SHARE) hops[k]++

        pieceBlocks.clear()
        piecePlatforms.clear()
        val surfaces = mutableListOf(Surface(previous.start, previous.end, previous.top))
        var x = previous.end
        for (k in 0 until count) {
            val platform = Platform(x + hops[k] * TILE, x + (hops[k] + widths[k]) * TILE, tops[k])
            piecePlatforms += platform
            surfaces += Surface(platform.start, platform.end, platform.top)
            x = platform.end
        }
        val start = x + hops[count] * TILE
        val runTiles = terrainRandom.nextInt(width, max(width, course.maxGroundTiles) + 1)
        val next = Ground(start, start + runTiles * TILE, tops[count])
        surfaces += Surface(next.start, next.end, next.top)
        val grounds = listOf(previous, next)
        if (reachesDirectly(previous, next)) return null
        for (k in 0 until surfaces.size - 1) {
            if (!fairHop(surfaces[k], surfaces[k + 1], grounds, emptyList(), path = null)) return null
        }
        // When the run goes fast, one of them may move (Fangusk: «también móviles desde ya»); if it is not fair
        // wherever it is, it stays still.
        if (speed >= course.moverFromSpeed &&
            terrainRandom.nextFloat() < course.moverShare
        ) {
            move(terrainRandom.nextInt(count), surfaces, grounds)
        }
        val items = mutableListOf<Item>()
        for (platform in piecePlatforms) {
            // Never on one that moves: it would be left in the air.
            if (platform.motion != null || itemRandom.nextFloat() >= ANIMAL_ON_PLATFORM_SHARE) continue
            val animal = positive((platform.start + platform.end) / 2, platform.top + Item.ABOVE_GROUND)
            if (animal.kind == ItemKind.Perrito) animal.walk = walkOn(animal, platform.start + PLATFORM_EDGE, platform.end - PLATFORM_EDGE)
            items += animal
        }
        if (itemRandom.nextFloat() < course.positivesPer100m * runTiles / 100f) row(next, next.start + ROW_FROM_TILE * TILE, items)
        remember(NO_SHAPE)
        return Piece(listOf(next), items, next.end, difficulty, platforms = piecePlatforms.toList())
    }

    /**
     * The platform [k] of the piece set moving, across or up and down (ADR 0009), if the hops onto it and off it are fair
     * wherever it is; else it stays still. [surfaces] are the ground before, the platforms and the ground after.
     */
    private fun move(k: Int, surfaces: List<Surface>, grounds: List<Ground>) {
        val still = piecePlatforms[k]
        val reach = TILE * terrainRandom.nextInt(MOVER_MIN_TILES, MOVER_MAX_TILES + 1)
        // Up and down within the heights of the grounds and the platforms, never lower.
        val canGoDown = still.top - reach >= course.minGroundTop * TILE
        val canGoUp = still.top + reach <= (course.maxGroundTop + PLATFORM_ABOVE_GROUNDS) * TILE
        val across = terrainRandom.nextBoolean() || (!canGoDown && !canGoUp)
        val down = canGoDown && (!canGoUp || terrainRandom.nextBoolean())
        val motion = Motion(
            dx = if (across) reach else 0,
            dy = if (across) {
                0
            } else if (down) {
                -reach
            } else {
                reach
            },
            period = MOVER_MIN_PERIOD + terrainRandom.nextDouble() * (MOVER_MAX_PERIOD - MOVER_MIN_PERIOD),
            phase = terrainRandom.nextDouble(),
        )
        val mover = Platform(still.start, still.end, still.top, motion)
        piecePlatforms[k] = mover
        if (!fairAround(mover, surfaces[k], surfaces[k + 2], grounds)) piecePlatforms[k] = still
    }

    /**
     * Whether some jump from [from] lands on [to] without the platforms: then the gap does not need them. The farthest
     * jumps are tried: the longest holds from the last moments before the edge.
     */
    private fun reachesDirectly(from: Ground, to: Ground): Boolean {
        sandbox.clear()
        sandbox.append(Piece(listOf(from, to), emptyList(), to.end))
        val speed = movement.speedAt(meters(from.end))
        val windowSteps = ceil(PRESS_WINDOW_SECONDS / JumpLab.STEP).toInt()
        val lastPress = windowSteps + floor(movement.coyoteTime / JumpLab.STEP).toInt()
        val startX = max(from.start + half.toDouble(), from.end - half - speed.toDouble() * (windowSteps + 1) * JumpLab.STEP)
        for (press in lastPress downTo max(0, lastPress - DIRECT_PRESSES)) {
            for (hold in maxHold downTo 0 step DIRECT_HOLD_STEP) {
                val landing = lab.tryJump(sandbox, emptyList(), startX, from.top.toFloat(), runStart, press, hold)
                if (landing.landed && landing.top == to.top) return true
            }
        }
        return false
    }

    /**
     * Something she runs on, a ground or a platform, as the validator sees it: from [start] to [end], at [top], where a
     * moving [platform] has its place; at any time, where it is then.
     */
    private class Surface(
        val start: Int,
        val end: Int,
        val top: Int,
        val platform: Platform? = null,
    ) {
        fun startAt(time: Double): Double = platform?.leftAt(time) ?: start.toDouble()

        fun endAt(time: Double): Double = platform?.rightAt(time) ?: end.toDouble()

        fun topAt(time: Double): Double = platform?.topAt(time) ?: top.toDouble()
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
     * before that run-up). Whether it went in.
     */
    private fun obstacle(ground: Ground, items: MutableList<Item>): Boolean {
        val speed = movement.speedAt(meters(ground.start))
        val fromTile = ceil(((course.landingMarginTiles + LANDING_ROOM_TILES) * TILE + speed * OBSTACLE_WINDOW_SECONDS) / TILE).toInt()
        val toTile = (ground.end - ground.start) / TILE - OBSTACLE_TO_END_TILES
        if (toTile < fromTile) return false
        val x = ground.start + itemRandom.nextInt(fromTile, toTile + 1) * TILE
        val obstacle = Item(negativeKind(), x, ground.top + Item.ABOVE_GROUND)
        if (!fairObstacle(ground, obstacle)) return false
        items += obstacle
        return true
    }

    /** A huevo frito or a pescado high over the gap, where a long jump goes: it asks for a lower one. */
    private fun ceiling(previous: Ground, ground: Ground, items: MutableList<Item>) {
        val ceiling = Item(negativeKind(), (previous.end + ground.start) / 2, previous.top + CEILING_HEIGHT)
        items += ceiling
        if (!fairGap(previous, ground, items, path = null)) items -= ceiling
    }

    private fun negativeKind(): ItemKind = if (itemRandom.nextFloat() < FISH_SHARE) ItemKind.Pescado else ItemKind.Huevo

    /** Perritos and gatitos: on a platform with a reward if [withPlatform] and it fits (then true), or a row on the ground. */
    private fun positives(
        previous: Ground,
        ground: Ground,
        items: MutableList<Item>,
        pieceMeters: Float,
        withPlatform: Boolean,
    ): Boolean {
        // Never in the air (Fangusk: «En el aire no puede aparecer ningún animal, salvo que esté en una plataforma»).
        if (withPlatform && rewardPlatform(previous, ground, items)) return true
        val expected = pieceMeters * course.positivesPer100m / 100f
        if (itemRandom.nextFloat() < expected) row(ground, ground.start + ROW_FROM_TILE * TILE, items)
        return false
    }

    /**
     * A platform with its reward (ADR 0008): a block over [ground], high enough to run under, with a gatito sitting or a
     * perrito walking on top, which a jump with the finger held reaches. Without jumping she runs under it; on it, she picks
     * the reward up and drops back to the same ground. Whether it went in. (Whether it becomes a semisolid platform, as
     * the others of ADR 0009, Fangusk has not said yet: it stays the block he chose.)
     */
    private fun rewardPlatform(previous: Ground, ground: Ground, items: MutableList<Item>): Boolean {
        val speed = movement.speedAt(meters(ground.start))
        val tiles = itemRandom.nextInt(PLATFORM_MIN_TILES, PLATFORM_MAX_TILES + 1)
        // Time to land from the gap and take off again before it, and room to drop back to the ground after it.
        val from = ground.start + course.landingMarginTiles * TILE + (speed * PLATFORM_RUN_UP_SECONDS).toInt()
        val to = ground.end - (speed * PLATFORM_DROP_SECONDS).toInt() - tiles * TILE
        if (to < from) return false
        val start = from + itemRandom.nextInt(0, (to - from) / TILE + 1) * TILE
        val bottom = ground.top + BLOCK_CLEARANCE + itemRandom.nextInt(0, PLATFORM_EXTRA_HEIGHT + 1)
        val platform = Block(start, start + tiles * TILE, bottom, bottom + TILE)
        // Nothing that takes points away under it or near it, and no other block by it.
        if (items.any { !it.kind.adds && it.x > platform.start - ROW_CLEARANCE && it.x < platform.end + ROW_CLEARANCE }) return false
        if (pieceBlocks.any { it.end > platform.start - TILE && it.start < platform.end + TILE }) return false
        pieceBlocks += platform
        if (!reachable(ground, platform) || !fairGap(previous, ground, items, path = null)) {
            pieceBlocks -= platform
            return false
        }
        val count = if (tiles >= PLATFORM_MAX_TILES && itemRandom.nextBoolean()) 2 else 1
        for (k in 1..count) {
            val animal = positive(platform.start + k * (platform.end - platform.start) / (count + 1), platform.top + Item.ABOVE_GROUND)
            // A perrito walks the platform and turns at its edges, like on the ground.
            if (animal.kind == ItemKind.Perrito) animal.walk = walkOn(animal, platform.start + PLATFORM_EDGE, platform.end - PLATFORM_EDGE)
            items += animal
        }
        return true
    }

    /** Whether some jumps from [ground] before it land on top of the [platform]: getting its reward has to be fair too. */
    private fun reachable(ground: Ground, platform: Block): Boolean {
        sandbox.clear()
        sandbox.append(Piece(listOf(ground), emptyList(), ground.end, blocks = pieceBlocks, platforms = piecePlatforms))
        val speed = movement.speedAt(meters(platform.start))
        val windowSteps = ceil(PLATFORM_WINDOW_SECONDS / JumpLab.STEP).toInt()
        val startX = max(ground.start + half.toDouble(), platform.start - half - speed.toDouble() * (windowSteps + 1) * JumpLab.STEP)
        var good = 0
        for (press in windowSteps downTo 0) {
            for (hold in maxHold downTo 0) {
                val landing = lab.tryJump(sandbox, emptyList(), startX, ground.top.toFloat(), runStart, press, hold)
                if (landing.landed && landing.top == platform.top && ++good >= PLATFORM_GOOD_JUMPS) return true
            }
        }
        return false
    }

    private fun row(ground: Ground, from: Int, items: MutableList<Item>) {
        val count = itemRandom.nextInt(ROW_MIN, ROW_MAX + 1)
        for (k in 0 until count) {
            val x = from + k * ROW_SPACING
            if (x > ground.end - TILE) return
            if (items.any { !it.kind.adds && abs(it.x - x) < ROW_CLEARANCE }) continue
            val item = positive(x, ground.top + Item.ABOVE_GROUND)
            if (item.kind == ItemKind.Perrito) item.walk = walkFor(item, ground, items)
            items += item
        }
    }

    /** A perrito of one of its breeds or a gatito with one of its coats (ADR 0008). */
    private fun positive(x: Int, y: Int): Item {
        val item = Item(if (itemRandom.nextBoolean()) ItemKind.Perrito else ItemKind.Gatito, x, y)
        when (item.kind) {
            ItemKind.Gatito -> item.coat = itemRandom.nextInt(Item.COATS)
            else -> item.breed = itemRandom.nextInt(Item.BREEDS)
        }
        return item
    }

    /**
     * How a perrito on [ground] walks (ADR 0008), or null if it stays still: within its ground, away from the edges and from
     * what takes points away on it, so running along the ground always picks it up without a jump.
     */
    private fun walkFor(perrito: Item, ground: Ground, items: List<Item>): Walk? {
        var low = ground.start + WALK_EDGE
        var high = ground.end - WALK_EDGE
        for (item in items) {
            val onThisGround = item.x >= ground.start && item.x < ground.end && item.y - ground.top < Item.SIZE
            if (item.kind.adds || !onThisGround) continue
            if (item.x < perrito.x) low = max(low, item.x + ROW_CLEARANCE) else high = min(high, item.x - ROW_CLEARANCE)
        }
        return walkOn(perrito, low, high)
    }

    /** A walk for a perrito between [low] and [high] (its centre, in world pixels), or null if it stays still. */
    private fun walkOn(perrito: Item, low: Int, high: Int): Walk? {
        val settings = config.items
        if (itemRandom.nextFloat() < settings.perritoStillShare) return null
        val from = max(low, perrito.x - settings.perritoRangeTiles * TILE)
        val to = min(high, perrito.x + settings.perritoRangeTiles * TILE)
        if (perrito.x < from || perrito.x > to || to - from < MIN_WALK) return null
        return Walk((from - perrito.x).toFloat(), (to - perrito.x).toFloat(), settings, itemRandom.nextInt())
    }

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
    ): Boolean = fairHop(Surface(from.start, from.end, from.top), Surface(to.start, to.end, to.top), listOf(from, to), items, path)

    /**
     * Whether a player can go from the surface [from] to [to], a ground or a platform, with the [grounds] and the piece's
     * blocks and platforms around, by the rule of [fairGap].
     */
    private fun fairHop(
        from: Surface,
        to: Surface,
        grounds: List<Ground>,
        items: List<Item>,
        path: MutableList<Pair<Double, Float>>?,
        time: Double = 0.0,
    ): Boolean {
        sandbox.clear()
        sandbox.append(Piece(grounds, emptyList(), grounds.last().end, blocks = pieceBlocks, platforms = piecePlatforms))
        val speed = movement.speedAt(meters(from.end))
        val windowSteps = ceil(PRESS_WINDOW_SECONDS / JumpLab.STEP).toInt()
        val coyoteSteps = floor(movement.coyoteTime / JumpLab.STEP).toInt()
        val startX = max(from.startAt(time) + half, from.endAt(time) - half - speed.toDouble() * (windowSteps + 1) * JumpLab.STEP)
        val lastPress = windowSteps + coyoteSteps
        val margin = course.landingMarginTiles * TILE
        var maxLand = to.end - speed.toDouble() * RUNWAY_SECONDS
        // Something to jump on the ground she lands on: she has to land with the whole run-up to jump it still ahead.
        for (item in items) {
            if (!item.kind.adds && item.x > to.start && item.x < to.end && item.y - to.top < Item.SIZE) {
                maxLand = min(maxLand, item.x - Item.SIZE / 2 - half - speed.toDouble() * OBSTACLE_WINDOW_SECONDS)
            }
        }
        // On a moving platform, where it is when she lands.
        val shift = maxLand - to.end
        val y = from.topAt(time).toFloat()
        return fair(lastPress, y, startX, items, time) { landing ->
            val at = landing.time
            landing.landed &&
                abs(landing.top - to.topAt(at)) < 1.0 &&
                landing.x >= to.startAt(at) + margin &&
                landing.x <= to.endAt(at) + shift
        }.also {
            if (it && path != null) {
                recordGoodPath(path, from.top.toFloat(), startX, items, lastPress, to.start + margin, maxLand, to.top)
            }
        }
    }

    /**
     * Whether the hops onto a moving platform and off it are fair wherever it is when she comes (ADR 0009): she cannot
     * stop to wait for it, so each hop is tried with the platform at [MOVER_SAMPLES] moments of its way.
     */
    private fun fairAround(
        mover: Platform,
        before: Surface,
        after: Surface,
        grounds: List<Ground>,
    ): Boolean {
        val motion = mover.motion ?: return true
        val on = Surface(mover.start, mover.end, mover.top, mover)
        for (k in 0 until MOVER_SAMPLES) {
            val time = motion.period * k / MOVER_SAMPLES
            if (!fairHop(before, on, grounds, emptyList(), path = null, time = time)) return false
            if (!fairHop(on, after, grounds, emptyList(), path = null, time = time)) return false
        }
        return true
    }

    /** Whether the [obstacle] on [ground] can be jumped, with the same rule as a gap. */
    private fun fairObstacle(ground: Ground, obstacle: Item): Boolean {
        val jump = ObstacleJump(ground, obstacle)
        return fair(jump.windowSteps, ground.top.toFloat(), jump.startX, listOf(obstacle)) { jump.clears(it) }
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
            sandbox.append(Piece(listOf(ground), emptyList(), ground.end, blocks = pieceBlocks, platforms = piecePlatforms))
        }

        fun clears(landing: JumpLab.Landing): Boolean =
            landing.landed && !landing.touchedNegative && landing.top == ground.top && landing.x >= past && landing.x <= maxLand
    }

    private inline fun fair(
        lastPress: Int,
        y: Float,
        startX: Double,
        items: List<Item>,
        time: Double = 0.0,
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
                val landing = lab.tryJump(sandbox, items, startX, y, runStart, press, hold, time = time)
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
        const val RUNWAY_SECONDS = 0.2f

        /** On top of the run-up, how much room a landing has to fall in, in seconds of running: on the shortest grounds too. */
        const val LANDING_SPREAD_SECONDS = 0.1f
        const val OBSTACLE_WINDOW_SECONDS = 0.4f
        const val FAIR_PRESSES = 3

        const val START_ROW_TILE = 8
        const val CALM_FLAT_MIN_TILES = 6
        const val FISH_SHARE = 0.4f
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
        const val CEILING_HEIGHT = 74
        const val ROW_FROM_TILE = 3
        const val ROW_MIN = 2
        const val ROW_MAX = 4
        const val ROW_SPACING = 2 * TILE
        const val ROW_CLEARANCE = 3 * TILE

        /** How far from the edges of its ground a perrito walks at least, and the shortest stretch worth walking. */
        const val WALK_EDGE = 2 * TILE
        const val MIN_WALK = 8

        /**
         * The platforms with a reward: how many tiles wide, how much higher than the least to run under them they can be,
         * the time to take off for one after landing and to drop back after it, how long a run-up the jumps onto it are
         * tried from and how many have to land on it, and how far from its edges an animal stays.
         */
        const val PLATFORM_MIN_TILES = 2
        const val PLATFORM_MAX_TILES = 3
        const val PLATFORM_EXTRA_HEIGHT = 8
        const val PLATFORM_RUN_UP_SECONDS = 0.45f
        const val PLATFORM_DROP_SECONDS = 0.5f
        const val PLATFORM_SECONDS = PLATFORM_RUN_UP_SECONDS + PLATFORM_DROP_SECONDS
        const val PLATFORM_WINDOW_SECONDS = 0.5f
        const val PLATFORM_GOOD_JUMPS = 4
        const val PLATFORM_EDGE = 6

        /**
         * The gaps with platforms (ADR 0009): a hop of 1 tile at least and of at most this share of the longest jump, some
         * a tile wider; the platforms up to this many tiles wider than the shortest ground, and a tile wider for each one
         * they are lower than what comes before them; each a tile lower than what comes before it in this share, at the
         * same height up to the next and higher in the rest (two tiles in this share of those at full difficulty), and at
         * most this many tiles over the highest ground; a perrito or a gatito on this share of them; and the farthest
         * jumps tried to see that the gap needs them.
         */
        const val MIN_HOP_TILES = 1
        const val HOP_SHARE = 0.55f
        const val HOP_TRIES = 64
        const val WIDER_HOP_SHARE = 0.3f
        const val PLATFORM_EXTRA_WIDTH = 1
        const val PLATFORM_DOWN_SHARE = 0.25f
        const val PLATFORM_LEVEL_SHARE = 0.55f
        const val PLATFORM_UP_TWO_SHARE = 0.15f
        const val PLATFORM_ABOVE_GROUNDS = 2
        const val ANIMAL_ON_PLATFORM_SHARE = 0.4f
        const val DIRECT_PRESSES = 4
        const val DIRECT_HOLD_STEP = 3

        /**
         * The moving platforms: how far they go, across or up and down, in tiles; how long their way there and back
         * takes, in seconds; and at how many moments of it the hops onto them and off them are tried.
         */
        const val MOVER_MIN_TILES = 1
        const val MOVER_MAX_TILES = 3
        const val MOVER_MIN_PERIOD = 2.4
        const val MOVER_MAX_PERIOD = 3.6
        const val MOVER_SAMPLES = 6
    }
}
