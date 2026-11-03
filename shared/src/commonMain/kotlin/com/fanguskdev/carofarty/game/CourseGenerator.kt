package com.fanguskdev.carofarty.game

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
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
    private val lab = JumpLab(movement, config.items.negativeBox, config.items.positiveBox)
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
     * The animals of the stretches of tension it has made, on the ground and on platforms with a reward; how many of them
     * it watched over with a huevo frito or a pescado (GEN-23); and how many of those are a dilemma: for the tests.
     */
    internal var animals = 0
        private set
    internal var guarded = 0
        private set
    internal var dilemmas = 0
        private set

    /** Whether the next animal watched over on the ground has to be a dilemma, owed by one that did not fit. */
    private var dilemmaDue = false

    /**
     * The next piece of course. The first one is the flat start; then a stretch of tension and a calm one take turns (GDD-08;
     * guía 1 de GameMaster, «calma, tensión, calma»).
     */
    fun next(): Piece {
        val previous = last ?: return start()
        if (calmLeft == 0 && meters(previous.end) >= nextCalm) {
            // Shorter as the run goes on towards the minute 30 (ADR 0011): only the fewest pieces in the second half.
            val most = if (lateDifficultyAt(meters(previous.end)) >= SHORT_CALMS_FROM) course.calmMinPieces else course.calmMaxPieces
            calmLeft = terrainRandom.nextInt(course.calmMinPieces, most + 1)
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
        // Towards the minute 30, now and then one more gap with platforms right after (ADR 0011).
        if (terrainRandom.nextFloat() < MORE_PLATFORMS_LATE * lateDifficultyAt(meters(previous.end))) untilPlatforms = 0
        repeat(course.validatorAttempts) { platformPiece(previous)?.let { return it } }
        return gapPiece(previous)
    }

    /** The difficulty, from 0 at the start to 1 after [CourseConfig.difficultyRampMeters] (GDD-08). */
    fun difficultyAt(meters: Double): Float {
        val t = (meters / course.difficultyRampMeters).coerceIn(0.0, 1.0).toFloat()
        return t * t * (3 - 2 * t)
    }

    /**
     * What keeps growing past [difficultyAt] (GDD-23, ADR 0011, Fangusk: «una partida continua de 30 minutos debería de ser
     * muy difícil»): from 0 when that one is full to 1 [CourseConfig.lateDifficultyMeters] later, about the minute 30.
     */
    fun lateDifficultyAt(meters: Double): Float =
        ((meters - course.difficultyRampMeters) / course.lateDifficultyMeters).coerceIn(0.0, 1.0).toFloat()

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

    /** How long the next stretch of tension is, in metres: longer towards the minute 30 (ADR 0011). */
    private fun randomTension(): Int {
        val metres = terrainRandom.nextInt(course.tensionMinMeters, course.tensionMaxMeters + 1)
        return (metres * (1 + lateDifficultyAt(nextCalm))).toInt()
    }

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
        // Never shorter than the shortest ground of the moment: the jump after it takes off from it alone.
        val least = max(CALM_FLAT_MIN_TILES, shortestGround(movement.speedAt(meters(previous.end))))
        val tiles = terrainRandom.nextInt(least, max(least, course.maxGroundTiles) + 1)
        val ground = Ground(previous.end, previous.end + tiles * TILE, previous.top)
        val items = mutableListOf<Item>()
        val difficulty = difficultyAt(meters(previous.end))
        // Fewer as the run goes on, like the rest (ADR 0011).
        if (itemRandom.nextFloat() < calmPositivesAt(difficulty)) row(ground, ground.start + TILE, items)
        remember(NO_SHAPE)
        return Piece(listOf(ground), items, ground.end, difficulty = difficulty)
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
        val fraction = gapFraction(meters, difficulty)
        val widest = max(course.minGapTiles, floor(fraction * longest / TILE).toInt())
        val chosenGap = if (easy) course.minGapTiles else terrainRandom.nextInt(course.minGapTiles, widest + 1)
        val stepTiles = if (easy) 0 else terrainRandom.nextInt(-course.maxStepDown, course.maxStepUp + 1)
        val topTiles = (previous.top / TILE + stepTiles).coerceIn(course.minGroundTop, course.maxGroundTop)
        val minRun = shortestGround(speed)
        // Whether it gets a group of animals, fewer as the run goes on (ADR 0011), on a platform with a reward or in a row on
        // its ground: decided before its length, as many as a piece of the usual length asks for, since both need a longer one.
        val usualMeters = chosenGap + (minRun + max(minRun, course.maxGroundTiles)) / 2f
        val withAnimals = itemRandom.nextFloat() < usualMeters * positivesAt(difficulty) / 100f
        // A piece with a platform and its reward (ADR 0008) gets a ground long enough for it at the speed of the moment.
        val rewardShare = course.rewardPlatformShare + (course.rewardPlatformShareEnd - course.rewardPlatformShare) * difficulty
        val withPlatform = !easy && withAnimals && itemRandom.nextFloat() < rewardShare
        val platformRun = ceil((course.landingMarginTiles * TILE + speed * PLATFORM_SECONDS) / TILE).toInt() + PLATFORM_MAX_TILES
        val withRow = withAnimals && !withPlatform
        // And one whose animals are watched over on the ground (GEN-23), room to land past the huevo or the pescado: only
        // with animals, so that it never asks for them.
        val guarding = !easy &&
            (withPlatform || withRow) &&
            itemRandom.nextFloat() < guardShare(difficulty) * if (withPlatform) SINGLE_ANIMAL_SHARE else 1f
        val guardRun = ceil((GUARD_ROOM_TILES * TILE + speed * (RUNWAY_SECONDS + landingSpread(speed))) / TILE).toInt()
        val shortest = when {
            withPlatform -> max(minRun, platformRun)
            guarding -> max(minRun, guardRun)
            else -> minRun
        }
        // Over a gap a little narrower than the widest: the jump that leaves the egg or the fish behind is a longer one.
        val gapTiles = if (guarding && !withPlatform) min(chosenGap, max(course.minGapTiles, widest - GUARD_GAP_SLACK)) else chosenGap
        val runTiles = terrainRandom.nextInt(shortest, max(shortest, course.maxGroundTiles) + 1)
        val start = previous.end + gapTiles * TILE
        val ground = Ground(start, start + runTiles * TILE, topTiles * TILE)

        val items = mutableListOf<Item>()
        pieceBlocks.clear()
        piecePlatforms.clear()
        val pieceMeters = (ground.end - previous.end).toFloat() / TILE
        val per100m = course.negativesPer100mStart + (course.negativesPer100mEnd - course.negativesPer100mStart) * difficulty
        // Where the animals are watched over on the ground, that is what there is to jump on it.
        if (!easy) {
            negatives(previous, ground, items, draw(pieceMeters * per100m / 100f), onTheGround = !guarding || withPlatform, difficulty)
        }
        if (!easy) airObstacles(previous, ground, items, difficulty)
        // Where the jump over the gap lands, now and then, unless the animals are watched over there (GEN-25).
        val landing = !easy && !guarding && difficulty >= course.airFromDifficulty
        if (landing && itemRandom.nextFloat() < LANDING_NEGATIVE_SHARE * difficulty) {
            val from = Surface(previous.start, previous.end, previous.top)
            whereSheLands(from, Surface(ground.start, ground.end, ground.top), null, listOf(previous, ground), items)
        }
        if (!fairGap(previous, ground, items, path = null)) return null
        // The step as built, which the limits of height may have cut short.
        val shape = shapeOf(gapTiles, (ground.top - previous.top) / TILE, ground, items)
        // Never three difficult pieces alike in a row (guía 1): a third one is made again.
        if (!easy && difficult(gapTiles, items) && shape == shapes[0] && shape == shapes[1]) return null
        val placed = items.size
        val rewarded = positives(previous, ground, items, withRow, withPlatform, difficulty)
        // A long ground made for a platform with a reward that did not fit is made again: no long flat grounds (ADR 0009).
        if (withPlatform && !rewarded) return null
        if (!easy) animals += items.size - placed
        // A ground made long for animals watched over without them is made again, as one made for a platform.
        if (guarding && !guard(previous, ground, items, placed, rewarded) && !rewarded) return null
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
        ceil((course.landingMarginTiles * TILE + speed * (RUNWAY_SECONDS + landingSpread(speed))) / TILE).toInt(),
    )

    /**
     * How long the landings spread over, in seconds of running: past the speed of the first minutes each step more of
     * holding carries the jump further, so the room grows with it (ADR 0011).
     */
    internal fun landingSpread(speed: Float): Float = LANDING_SPREAD_SECONDS * max(1f, speed / movement.maxSpeed)

    /**
     * A gap too wide for any jump, that only platforms cross (ADR 0009, Fangusk: «plataformas sobre las que saltar de
     * forma obligatoria para salvar distancias imposibles en salto, como se hace en Super Mario»): 1 or 2 platforms at the
     * start and 2 or 3 at full difficulty (GEN-24), in some of them a staircase that goes up or down; each hop between them
     * fair with the same rule as a gap, and the whole gap wider than the longest jump of the moment. Perritos and gatitos
     * sometimes wait on them. Null if this one did not come out fair.
     */
    private fun platformPiece(previous: Ground): Piece? {
        val meters = meters(previous.end)
        val speed = movement.speedAt(meters)
        val difficulty = difficultyAt(meters)
        val many = terrainRandom.nextFloat()
        val count = when {
            many < ONE_PLATFORM_START + (ONE_PLATFORM_END - ONE_PLATFORM_START) * difficulty -> 1
            many < 1f - THREE_PLATFORMS_END * difficulty -> 2
            else -> 3
        }.coerceAtMost(course.platformMaxCount)
        // The heights first: a staircase, a tile up or down each time, or each platform a little higher or lower than what
        // comes before it; and the ground after them.
        val tops = IntArray(count + 1)
        var top = previous.top / TILE
        val stairs = stairs(top, count)
        for (k in 0..count) {
            val step = if (stairs != 0) {
                stairs
            } else {
                when (val roll = terrainRandom.nextFloat()) {
                    in 0f..PLATFORM_DOWN_SHARE -> -1
                    in PLATFORM_DOWN_SHARE..PLATFORM_LEVEL_SHARE -> 0
                    else -> if (roll > 1f - PLATFORM_UP_TWO_SHARE * difficulty) 2 else 1
                }
            }
            val highest = if (k == count) course.maxGroundTop else course.maxGroundTop + PLATFORM_ABOVE_GROUNDS
            top = (top + step).coerceIn(course.minGroundTop, highest)
            tops[k] = top * TILE
        }
        // Each platform wide enough to land on and take off again, and wider when it is lower than what came before; from
        // half the difficulty on, small (GEN-26, Fangusk: «plataformas más pequeñas para que exija coordinación en el
        // salto»): a tile or two, where she lands and jumps again at once.
        val width = shortestGround(speed)
        val small = difficulty >= SMALL_FROM_DIFFICULTY
        val widths = IntArray(count) { k ->
            if (small) {
                // A tile in half of them at first, and in nearly all towards the minute 30 (ADR 0011).
                if (terrainRandom.nextFloat() < ONE_TILE_START + (ONE_TILE_LATE - ONE_TILE_START) * lateDifficultyAt(meters)) {
                    SMALL_MIN_TILES
                } else {
                    SMALL_MAX_TILES
                }
            } else {
                val drop = max(0, (if (k == 0) previous.top else tops[k - 1]) - tops[k]) / TILE
                width + drop + terrainRandom.nextInt(0, PLATFORM_EXTRA_WIDTH + 1)
            }
        }
        // The hops, each as wide as a gap of the moment (GEN-24: the platforms ask as much as the gaps they stand for),
        // and wider where it takes for the whole gap to be beyond the longest jump, falling to the ground after it too;
        // none beyond the widest gap of the moment.
        val fall = max(0, previous.top - tops[count]).toFloat()
        val longest = speed * (lab.longestAirtime + sqrt(2 * fall / movement.gravity))
        val fraction = gapFraction(meters, difficulty)
        val maxHop = max(MIN_HOP_TILES, floor(fraction * speed * lab.longestAirtime / TILE).toInt())
        val needed = ceil((longest + TILE) / TILE).toInt() - widths.sum()
        val hops = IntArray(count + 1) { terrainRandom.nextInt(max(MIN_HOP_TILES, maxHop / 2), maxHop + 1) }
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

        pieceBlocks.clear()
        piecePlatforms.clear()
        val surfaces = mutableListOf(Surface(previous.start, previous.end, previous.top))
        var x = previous.end
        for (k in 0 until count) {
            val platform = Platform(x + hops[k] * TILE, x + (hops[k] + widths[k]) * TILE, tops[k])
            piecePlatforms += platform
            surfaces += Surface(platform.start, platform.end, platform.top, ledge = true)
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
            terrainRandom.nextFloat() < course.moverShare + (MOVER_SHARE_LATE - course.moverShare) * lateDifficultyAt(meters)
        ) {
            move(terrainRandom.nextInt(count), surfaces, grounds)
        }
        val items = mutableListOf<Item>()
        negativesOnTheWay(surfaces, grounds, items, difficulty)
        val share = guardShare(difficulty) * SINGLE_ANIMAL_SHARE
        val onPlatform = ANIMAL_ON_PLATFORM_SHARE * calmPositivesAt(difficulty)
        for ((k, platform) in piecePlatforms.withIndex()) {
            // Never on one that moves: it would be left in the air.
            if (platform.motion != null || itemRandom.nextFloat() >= onPlatform) continue
            val animal = positive((platform.start + platform.end) / 2, platform.top + Item.ABOVE_GROUND)
            if (animal.kind == ItemKind.Perrito) animal.walk = walkOn(animal, platform.start + PLATFORM_EDGE, platform.end - PLATFORM_EDGE)
            items += animal
            animals++
            // Watched over now and then, like the animals on the ground (GEN-23).
            if (itemRandom.nextFloat() < share) guardOnTheWay(surfaces[k], surfaces[k + 1], surfaces[k + 2], grounds, items, animal)
        }
        if (itemRandom.nextFloat() < calmPositivesAt(difficulty) * course.positivesPer100m * runTiles / 100f) {
            row(next, next.start + ROW_FROM_TILE * TILE, items, rowSize(difficulty))
        }
        items.sortBy { it.x }
        remember(NO_SHAPE)
        return Piece(listOf(next), items, next.end, difficulty, platforms = piecePlatforms.toList())
    }

    /**
     * What takes points away on the way over a gap with platforms (GEN-25, Fangusk: «más huevos fritos y pescado»): in the
     * air on the obvious hop from one to the next, alone or in a pair, and on a platform or the ground after them where she
     * lands, so that she has to land past it; more often with the difficulty. Never by one that moves, whose hops the
     * validator tries elsewhere, nor where she lands on a small one, which has room for nothing else.
     */
    private fun negativesOnTheWay(
        surfaces: List<Surface>,
        grounds: List<Ground>,
        items: MutableList<Item>,
        difficulty: Float,
    ) {
        if (difficulty < course.airFromDifficulty) return
        val air = (course.airChanceStart + (course.airChanceEnd - course.airChanceStart) * difficulty) * HOP_AIR_SHARE
        val landing = LANDING_NEGATIVE_SHARE * difficulty
        for (k in 0 until surfaces.size - 1) {
            val moving = piecePlatforms.getOrNull(k - 1)?.motion != null || piecePlatforms.getOrNull(k)?.motion != null
            if (moving) continue
            if (itemRandom.nextFloat() < air) airOnTheWay(surfaces[k], surfaces[k + 1], grounds, items, difficulty, blocks = false)
            if (itemRandom.nextFloat() < landing) whereSheLands(surfaces[k], surfaces[k + 1], surfaces.getOrNull(k + 2), grounds, items)
        }
    }

    /**
     * A huevo frito or a pescado on [on] where the hop from [before] lands, at its start, so that she lands past it; and
     * then the hop to [after], if there is one, still fair. Not on a small platform. Whether it stayed.
     */
    private fun whereSheLands(
        before: Surface,
        on: Surface,
        after: Surface?,
        grounds: List<Ground>,
        items: MutableList<Item>,
    ): Boolean {
        val speed = movement.speedAt(meters(on.start))
        if (small(on, speed)) return false
        val negative = Item(negativeKind(), on.start + WHERE_SHE_LANDS, on.top + Item.ABOVE_GROUND)
        if (items.any { abs(it.x - negative.x) < ROW_CLEARANCE }) return false
        items += negative
        val fair = fairHop(before, on, grounds, items, path = null) && (after == null || fairHop(on, after, grounds, items, path = null))
        if (fair) return true
        items -= negative
        return false
    }

    /**
     * Whether the [count] platforms of a gap that starts at [top] tiles go up (1) or down (-1) a tile each, like a
     * staircase (GEN-24, Fangusk: «más plataformeo»), or each its own way (0): stairs in [STAIRS_SHARE] of the gaps with
     * two or three, up or down where there is room for every step.
     */
    private fun stairs(top: Int, count: Int): Int {
        if (count < 2 || terrainRandom.nextFloat() >= STAIRS_SHARE) return 0
        val up = top + count <= course.maxGroundTop + PLATFORM_ABOVE_GROUNDS
        val down = top - count >= course.minGroundTop
        return when {
            up && down -> if (terrainRandom.nextBoolean()) 1 else -1
            up -> 1
            down -> -1
            else -> 0
        }
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
        /** A platform, not a ground: a small one she leaves again at once (GEN-26). */
        val ledge: Boolean = false,
    ) {
        fun startAt(time: Double): Double = platform?.leftAt(time) ?: start.toDouble()

        fun endAt(time: Double): Double = platform?.rightAt(time) ?: end.toDouble()

        fun topAt(time: Double): Double = platform?.topAt(time) ?: top.toDouble()

        /** The lowest it goes, moving or not. */
        fun lowest(): Float = top + min(0, platform?.motion?.dy ?: 0).toFloat()
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
        val from = Surface(previous.start, previous.end, previous.top)
        val to = Surface(ground.start, ground.end, ground.top)
        airOnTheWay(from, to, listOf(previous, ground), items, difficulty, blocks = true)
    }

    /**
     * Something in the air on the obvious jump from [from] to [to], with the [grounds] around: a block, if [blocks] and
     * drawn so (only over a gap between two grounds); a huevo frito and a pescado, one a little above the path and the
     * other a little below, more often with the difficulty (GEN-25: «parejas alto/bajo», the jump has to go between them);
     * or one alone. Whatever leaves the hop unfair is taken away again.
     */
    private fun airOnTheWay(
        from: Surface,
        to: Surface,
        grounds: List<Ground>,
        items: MutableList<Item>,
        difficulty: Float,
        blocks: Boolean,
    ) {
        val path = mutableListOf<Pair<Double, Float>>()
        // The hop is fair already: only the path of one of its good jumps is needed.
        goodPath(from, to, grounds, items, path)
        if (path.size < MIN_AIR_PATH) return
        // Her body's centre around the top of that jump, where shorter jumps pass under and higher ones over, and over the
        // gap itself: over the run-up she would run into it before any jump the validator tries.
        val half = Item.SIZE / 2
        val first = path.size * AIR_FROM_TENTHS / 10
        val last = path.size * AIR_TO_TENTHS / 10
        val overTheGap = (first until last).filter { path[it].first - half > from.end && path[it].first + half < to.start }
        if (overTheGap.isEmpty()) return
        val wantsBlock = blocks && itemRandom.nextFloat() < course.airBlockShare
        val pairs = PAIR_SHARE * difficulty + (1f - PAIR_SHARE) * lateDifficultyAt(meters(from.end))
        val wantsPair = !wantsBlock && itemRandom.nextFloat() < pairs
        repeat(AIR_TRIES) {
            val (x, centre) = path[overTheGap[itemRandom.nextInt(overTheGap.size)]]
            val placed = when {
                wantsBlock -> block(grounds[0], grounds[1], items, x, centre)
                wantsPair -> pairOnThePath(from, to, grounds, items, x, centre)
                else -> onThePath(from, to, grounds, items, x, centre)
            }
            if (placed) return
        }
    }

    /** A huevo frito or a pescado where her body would be at ([x], [centre]) on the obvious jump. Whether it stayed. */
    private fun onThePath(
        from: Surface,
        to: Surface,
        grounds: List<Ground>,
        items: MutableList<Item>,
        x: Double,
        centre: Float,
    ): Boolean {
        // A little above or below her body on that jump: it still catches the obvious jump and leaves room around it.
        val item = Item(negativeKind(), x.toInt(), centre.toInt() + itemRandom.nextInt(-AIR_SPREAD, AIR_SPREAD + 1))
        items += item
        if (fairHop(from, to, grounds, items, path = null)) return true
        items -= item
        return false
    }

    /**
     * A huevo frito or a pescado a little above where her body would be at ([x], [centre]) on the obvious jump and another a
     * little below and a step further: the jump has to go between them, measuring its height (GEN-25). Whether they stayed.
     */
    private fun pairOnThePath(
        from: Surface,
        to: Surface,
        grounds: List<Ground>,
        items: MutableList<Item>,
        x: Double,
        centre: Float,
    ): Boolean {
        val apart = (movement.hitboxHeight / 2 + config.items.negativeBox / 2).toInt() + PAIR_MARGIN
        val above = Item(negativeKind(), x.toInt(), centre.toInt() + apart)
        val below = Item(negativeKind(), x.toInt() + PAIR_STAGGER, centre.toInt() - apart)
        items += above
        items += below
        if (fairHop(from, to, grounds, items, path = null)) return true
        items -= above
        items -= below
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
     * [count] collectibles that take points away, up to three: on the ground where there is room to jump it (if
     * [onTheGround]), high over the gap, or both, and the third on the path of the jump. One that cannot be made fair is
     * left out.
     */
    private fun negatives(
        previous: Ground,
        ground: Ground,
        items: MutableList<Item>,
        count: Int,
        onTheGround: Boolean,
        difficulty: Float,
    ) {
        if (count == 0) return
        val placed = onTheGround && (count >= 2 || itemRandom.nextBoolean()) && obstacle(ground, items)
        if (count >= 2 || !placed) ceiling(previous, ground, items)
        // A third one, on the path of the jump (ADR 0011: «más huevos fritos y pescado»).
        if (count >= MAX_NEGATIVES_PER_PIECE) {
            val from = Surface(previous.start, previous.end, previous.top)
            airOnTheWay(from, Surface(ground.start, ground.end, ground.top), listOf(previous, ground), items, difficulty, blocks = false)
        }
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

    /** The share of the groups of animals watched over at [difficulty] (GEN-23): a row, or one alone on a platform. */
    private fun guardShare(difficulty: Float): Float =
        course.guardedShareStart + (course.guardedShareEnd - course.guardedShareStart) * difficulty

    /**
     * A huevo frito or a pescado by the animals just placed, those from [from] in [items] (GEN-23, ADR 0010; Fangusk:
     * «para que el jugador tenga que a veces elegir no rescatar a un animalito con tal de no coger un pescado o un
     * huevo»): on their platform with a reward ([onPlatform]), or by the first of their row, where the jump over the gap
     * lands. In [CourseConfig.guardedDilemmaShare] of those on the ground nothing rescues the animal without touching it,
     * a dilemma; in the rest some jumps do, by the rule of any jump. Leaving both behind is always fair: if it does not
     * come out like that, it is taken away. Whether it went in.
     */
    private fun guard(
        previous: Ground,
        ground: Ground,
        items: MutableList<Item>,
        from: Int,
        onPlatform: Boolean,
    ): Boolean {
        if (items.size == from) return false
        if (onPlatform) {
            // Always skill: a jump up through it from below rescues the animal whatever is there.
            if (!guardPlatform(previous, ground, items, from)) return false
        } else {
            val dilemma = dilemmaDue || itemRandom.nextFloat() < course.guardedDilemmaShare
            // A dilemma that did not fit is owed to the next one, so they come in their share.
            dilemmaDue = dilemma
            if (!guardRow(previous, ground, items, from, dilemma)) return false
            dilemmaDue = false
            if (dilemma) dilemmas++
        }
        guarded++
        return true
    }

    /**
     * The row from [from] in [items] laid out again around a huevo frito or a pescado where the jump over the gap from
     * [previous] lands on [ground]: in a [dilemma], the first animal where the natural jump lands and the other right
     * behind it, so that rescuing it means touching both; and else the other where the natural jump lands and the first
     * animal a little further, so that a longer jump rescues it clean. Landing past them is always fair. The animal watched
     * over stays still, and the others of the row walk clear of it. If none comes out so, the row stays as it was.
     * Whether it went in.
     */
    private fun guardRow(
        previous: Ground,
        ground: Ground,
        items: MutableList<Item>,
        from: Int,
        dilemma: Boolean,
    ): Boolean {
        val row = items.subList(from, items.size).sortedBy { it.x }
        val wanted = if (dilemma) DILEMMA else SKILL
        val kind = negativeKind()
        val distances = if (dilemma) DILEMMA_DISTANCES else SKILL_DISTANCES
        val first = itemRandom.nextInt(distances.size)
        val touching = (config.items.positiveBox + config.items.negativeBox) / 2
        for (k in distances.indices) {
            val distance = distances[(first + k) % distances.size]
            val animalX = ground.start + if (dilemma) DILEMMA_ANIMAL_AT else SKILL_NEGATIVE_AT + distance
            val negativeX = if (dilemma) animalX + distance else ground.start + SKILL_NEGATIVE_AT
            if (max(animalX, negativeX) > ground.end - OBSTACLE_TO_END_TILES * TILE) continue
            // Nothing else to jump on this ground near it.
            if (items.any { !it.kind.adds && onGround(it, ground) && abs(it.x - negativeX) < ROW_CLEARANCE }) continue
            val laid = ArrayList<Item>(row.size + 1)
            var x = animalX
            for (animal in row) {
                // The rest of the row after both, clear of the negative.
                if (laid.isNotEmpty() && abs(x - negativeX) <= touching) x = max(x, negativeX + touching + ROW_SPACING / 2)
                if (x > ground.end - TILE) break
                laid += Item(animal.kind, x, animal.y).also {
                    it.coat = animal.coat
                    it.breed = animal.breed
                }
                x += ROW_SPACING
            }
            val negative = Item(kind, negativeX, ground.top + Item.ABOVE_GROUND)
            items.subList(from, items.size).clear()
            items += laid
            items += negative
            if (fairGap(previous, ground, items, path = null) && rescueAcross(previous, ground, laid[0], items) == wanted) {
                for (animal in laid) if (animal !== laid[0] && animal.kind == ItemKind.Perrito) animal.walk = walkFor(animal, ground, items)
                return true
            }
        }
        items.subList(from, items.size).clear()
        items += row
        return false
    }

    /**
     * [animal], on the platform [on] of a gap with platforms, with a huevo frito or a pescado where the hop from [before]
     * lands, as on the ground ([guardRow]): a dilemma in [CourseConfig.guardedDilemmaShare] of them, else skill. The hops
     * onto the platform and off it to [after] stay fair, landing past it; if not, it is taken away and the animal goes
     * back to its place.
     */
    private fun guardOnTheWay(
        before: Surface,
        on: Surface,
        after: Surface,
        grounds: List<Ground>,
        items: MutableList<Item>,
        animal: Item,
    ) {
        val dilemma = dilemmaDue || itemRandom.nextFloat() < course.guardedDilemmaShare
        val distances = if (dilemma) DILEMMA_DISTANCES else SKILL_DISTANCES
        val first = itemRandom.nextInt(distances.size)
        val kind = negativeKind()
        val index = items.indexOf(animal)
        for (k in distances.indices) {
            val distance = distances[(first + k) % distances.size]
            val animalX = on.start + if (dilemma) DILEMMA_ANIMAL_AT else SKILL_NEGATIVE_AT + distance
            val negativeX = if (dilemma) animalX + distance else on.start + SKILL_NEGATIVE_AT
            if (max(animalX, negativeX) > on.end - PLATFORM_EDGE) continue
            val moved = Item(animal.kind, animalX, animal.y).also {
                it.coat = animal.coat
                it.breed = animal.breed
            }
            val negative = Item(kind, negativeX, on.top + Item.ABOVE_GROUND)
            items[index] = moved
            items += negative
            val fair = fairHop(before, on, grounds, items, path = null) && fairHop(on, after, grounds, items, path = null)
            if (fair && rescueHop(before, on, grounds, moved, items) == if (dilemma) DILEMMA else SKILL) {
                dilemmaDue = false
                guarded++
                if (dilemma) dilemmas++
                return
            }
            items -= negative
            items[index] = animal
        }
        if (dilemma) dilemmaDue = true
    }

    /** Whether [item] sits on [ground], not in the air over it or over the gap before it. */
    private fun onGround(item: Item, ground: Ground): Boolean =
        item.x >= ground.start && item.x < ground.end && item.y - ground.top in 0 until Item.SIZE

    /**
     * The animal on the platform with a reward just made, the only one there, with a huevo frito or a pescado where the
     * jump onto it lands, before it, so that a longer jump rescues it clean. Running under the platform leaves both
     * behind. Whether it went in.
     */
    private fun guardPlatform(
        previous: Ground,
        ground: Ground,
        items: MutableList<Item>,
        from: Int,
    ): Boolean {
        val platform = piecePlatforms.lastOrNull() ?: return false
        if (items.size - from != 1) return false
        val animal = items[from]
        val near = platform.start + PLATFORM_EDGE
        val far = platform.end - PLATFORM_EDGE
        // Moved to the far end of the platform, and still: a perrito that walked would take the room the other needs.
        val moved = Item(animal.kind, far - GUARD_ON_PLATFORM, animal.y)
        moved.coat = animal.coat
        moved.breed = animal.breed
        val negative = Item(negativeKind(), near, platform.top + Item.ABOVE_GROUND)
        if (rescueOnPlatform(ground, platform, moved, negative) != SKILL) return false
        items[from] = moved
        items += negative
        if (fairGap(previous, ground, items, path = null)) return true
        items -= negative
        items[from] = animal
        return false
    }

    /**
     * Whether a jump over the gap from [previous] lands well on [ground] and rescues [animal] there without touching what
     * takes points away in [items], running on after it lands: by the rule of any jump ([SKILL]), never ([DILEMMA]), or
     * only now and then ([NEITHER]).
     */
    private fun rescueAcross(
        previous: Ground,
        ground: Ground,
        animal: Item,
        items: List<Item>,
    ): Int = rescueHop(
        Surface(previous.start, previous.end, previous.top),
        Surface(ground.start, ground.end, ground.top),
        listOf(previous, ground),
        animal,
        items,
    )

    /** What [rescueAcross] says of a hop from [from] to [to], grounds or platforms that stand still, with the [grounds] around. */
    private fun rescueHop(
        from: Surface,
        to: Surface,
        grounds: List<Ground>,
        animal: Item,
        items: List<Item>,
    ): Int {
        sandbox.clear()
        sandbox.append(Piece(grounds, emptyList(), grounds.last().end, blocks = pieceBlocks, platforms = piecePlatforms))
        val speed = movement.speedAt(meters(from.end))
        val windowSteps = ceil(PRESS_WINDOW_SECONDS / JumpLab.STEP).toInt()
        val lastPress = windowSteps + floor(movement.coyoteTime / JumpLab.STEP).toInt()
        val startX = max(from.start + half.toDouble(), from.end - half - speed.toDouble() * (windowSteps + 1) * JumpLab.STEP)
        val rule = LandingRule(to, items, speed)
        // After landing, on past the animal and what watches over it.
        var farthest = animal.x
        for (item in items) if (!item.kind.adds && item.x > to.start && item.x < to.end) farthest = max(farthest, item.x)
        val runAfter = ceil((farthest + TILE - to.start) / (speed * JumpLab.STEP)).toInt()
        val y = from.top.toFloat()
        val lowest = to.lowest() - GIVE_UP_BELOW
        val rescued = { landing: JumpLab.Landing -> rule.good(landing) && landing.touchedWatched }
        if (fair(lastPress, y, startX, items, watch = animal, runAfter = runAfter, giveUpBelow = lowest, good = rescued)) return SKILL
        val now = any(lastPress, y, startX, items, watch = animal, runAfter = runAfter, giveUpBelow = lowest, good = rescued)
        return if (now) NEITHER else DILEMMA
    }

    /**
     * Whether a jump from [ground] rescues [animal] on the [platform] and touches nothing that takes points away, running on
     * after it lands, off the platform and down to the ground: as [rescueAcross] says it.
     */
    private fun rescueOnPlatform(
        ground: Ground,
        platform: Platform,
        animal: Item,
        negative: Item,
    ): Int {
        sandbox.clear()
        sandbox.append(Piece(listOf(ground), emptyList(), ground.end, blocks = pieceBlocks, platforms = piecePlatforms))
        val speed = movement.speedAt(meters(platform.start)).toDouble()
        val windowSteps = ceil(PLATFORM_WINDOW_SECONDS / JumpLab.STEP).toInt()
        val startX = max(ground.start + half.toDouble(), platform.start - half - speed * (windowSteps + 1) * JumpLab.STEP)
        // Up to passing under its end, and after landing, on until she is off it.
        val lastPress = ceil((platform.end - startX) / (speed * JumpLab.STEP)).toInt()
        val runAfter = ceil((platform.end - platform.start + 2 * TILE) / (speed * JumpLab.STEP)).toInt()
        val items = listOf(negative)
        val y = ground.top.toFloat()
        fun rescued(landing: JumpLab.Landing) = landing.landed && landing.touchedWatched
        if (fair(lastPress, y, startX, items, watch = animal, runAfter = runAfter) { rescued(it) }) return SKILL
        return if (any(lastPress, y, startX, items, watch = animal, runAfter = runAfter) { rescued(it) }) NEITHER else DILEMMA
    }

    /**
     * How wide the widest gap is, as a share of the longest jump at [meters] with [difficulty]: from the settings' start to
     * their end, and a little more towards the minute 30 (ADR 0011), still fair by the validator.
     */
    private fun gapFraction(meters: Double, difficulty: Float): Float =
        course.gapFractionStart + (course.gapFractionEnd - course.gapFractionStart) * difficulty + GAP_LATE * lateDifficultyAt(meters)

    /** How many groups of animals the gaps of the stretches of tension ask for every 100 m at [difficulty] (ADR 0011). */
    private fun positivesAt(difficulty: Float): Float =
        course.positivesPer100m + (course.positivesPer100mEnd - course.positivesPer100m) * difficulty

    /** The share of the calm pieces and of the platforms of the gaps with animals at [difficulty]: fewer as it grows (ADR 0011). */
    private fun calmPositivesAt(difficulty: Float): Float =
        course.calmPositivesShare + (course.calmPositivesShareEnd - course.calmPositivesShare) * difficulty

    /** Perritos and gatitos: on a platform with a reward if [withPlatform] and it fits (then true), or a row on the ground. */
    private fun positives(
        previous: Ground,
        ground: Ground,
        items: MutableList<Item>,
        withRow: Boolean,
        withPlatform: Boolean,
        difficulty: Float,
    ): Boolean {
        // Never in the air (Fangusk: «En el aire no puede aparecer ningún animal, salvo que esté en una plataforma»).
        if (withPlatform && rewardPlatform(previous, ground, items)) return true
        if (withRow) row(ground, ground.start + ROW_FROM_TILE * TILE, items, rowSize(difficulty))
        return false
    }

    /**
     * A platform with its reward (ADR 0008), semisolid like the others since Fangusk accepted it (ADR 0009: «Vale, acepto
     * tu recomendación»): over [ground], high enough to run under, with a gatito sitting or a perrito walking on top,
     * which a jump with the finger held reaches, from before it or through it from below. Without jumping she runs under
     * it; on it, she picks the reward up and drops back to the same ground. Whether it went in.
     */
    private fun rewardPlatform(previous: Ground, ground: Ground, items: MutableList<Item>): Boolean {
        val speed = movement.speedAt(meters(ground.start))
        val tiles = itemRandom.nextInt(PLATFORM_MIN_TILES, PLATFORM_MAX_TILES + 1)
        // Time to land from the gap and take off again before it, and room to drop back to the ground after it.
        val from = ground.start + course.landingMarginTiles * TILE + (speed * PLATFORM_RUN_UP_SECONDS).toInt()
        val to = ground.end - (speed * PLATFORM_DROP_SECONDS).toInt() - tiles * TILE
        if (to < from) return false
        val start = from + itemRandom.nextInt(0, (to - from) / TILE + 1) * TILE
        // Its top where the block's was: as high over the ground, so the reward sits just as high.
        val top = ground.top + BLOCK_CLEARANCE + TILE + itemRandom.nextInt(0, PLATFORM_EXTRA_HEIGHT + 1)
        val platform = Platform(start, start + tiles * TILE, top)
        // Nothing that takes points away under it or near it, and no block by it.
        if (items.any { !it.kind.adds && it.x > platform.start - ROW_CLEARANCE && it.x < platform.end + ROW_CLEARANCE }) return false
        if (pieceBlocks.any { it.end > platform.start - TILE && it.start < platform.end + TILE }) return false
        piecePlatforms += platform
        if (!reachable(ground, platform) || !fairGap(previous, ground, items, path = null)) {
            piecePlatforms -= platform
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
    private fun reachable(ground: Ground, platform: Platform): Boolean {
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

    /**
     * How many animals a row of the stretches of tension has at [difficulty]: 2 to 4 at the start and 1 or 2 at full
     * difficulty, so that the one watched over is one of every 2 or so (ADR 0011).
     */
    private fun rowSize(difficulty: Float): Int {
        val most = ROW_MAX - (difficulty * (ROW_MAX - ROW_MAX_END)).roundToInt()
        val least = if (difficulty < SHORT_ROWS_FROM_DIFFICULTY) ROW_MIN else 1
        return itemRandom.nextInt(least, max(least, most) + 1)
    }

    private fun row(
        ground: Ground,
        from: Int,
        items: MutableList<Item>,
        count: Int = itemRandom.nextInt(ROW_MIN, ROW_MAX + 1),
    ) {
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
        val rule = LandingRule(to, items, speed)
        val y = from.topAt(time).toFloat()
        val startX = max(from.startAt(time) + half, from.endAt(time) - half - speed.toDouble() * (windowSteps + 1) * JumpLab.STEP)
        val lastPress = windowSteps + coyoteSteps
        // Falling past where [to] can be at any time, a jump has no landing on it left to look for.
        val lowest = to.lowest() - GIVE_UP_BELOW
        if (small(from, speed)) {
            // From a small platform she jumps again wherever she landed on it: from its start and from its very end, with
            // what is left of it and the coyote time, it has to be fair (GEN-26).
            val step = speed.toDouble() * JumpLab.STEP
            val near = from.startAt(time) + half
            val far = from.endAt(time) - half - 1
            for (fromX in doubleArrayOf(near, far)) {
                val presses = ceil((from.endAt(time) - fromX) / step).toInt() + coyoteSteps
                if (!fair(presses, y, fromX, items, time, giveUpBelow = lowest) { rule.good(it) }) return false
            }
            if (path != null) recordGoodPath(path, y, near, items, ceil((from.endAt(time) - near) / step).toInt() + coyoteSteps, rule)
            return true
        }
        return fair(lastPress, y, startX, items, time, giveUpBelow = lowest) { rule.good(it) }.also {
            if (it && path != null) recordGoodPath(path, y, startX, items, lastPress, rule)
        }
    }

    /** The path of one of the good jumps from [from] to [to], as [fairHop] counts them, into [path]; empty if there is none. */
    private fun goodPath(
        from: Surface,
        to: Surface,
        grounds: List<Ground>,
        items: List<Item>,
        path: MutableList<Pair<Double, Float>>,
    ) {
        sandbox.clear()
        sandbox.append(Piece(grounds, emptyList(), grounds.last().end, blocks = pieceBlocks, platforms = piecePlatforms))
        val speed = movement.speedAt(meters(from.end))
        val windowSteps = ceil(PRESS_WINDOW_SECONDS / JumpLab.STEP).toInt()
        val coyoteSteps = floor(movement.coyoteTime / JumpLab.STEP).toInt()
        val rule = LandingRule(to, items, speed)
        val startX = if (small(from, speed)) {
            from.start + half.toDouble()
        } else {
            max(from.start + half.toDouble(), from.end - half - speed.toDouble() * (windowSteps + 1) * JumpLab.STEP)
        }
        val lastPress = ceil((from.end - startX) / (speed * JumpLab.STEP)).toInt() + coyoteSteps
        path.clear()
        recordGoodPath(path, from.top.toFloat(), startX, items, lastPress, rule)
    }

    /** Whether [surface] is a small platform (GEN-26): narrower than the shortest ground at [speed], where nothing else fits. */
    private fun small(surface: Surface, speed: Float): Boolean = surface.ledge && surface.end - surface.start < shortestGround(speed) * TILE

    /**
     * Where a landing on [to] is good (GEN-10): with margin after its start, and before what there is to jump on it with
     * the whole run-up to jump it still ahead, or past all of it (GEN-23, where the jump over the gap leaves an egg or a
     * fish behind); either way, with room to run up to the next jump. On a moving platform, where it is when she lands.
     */
    private inner class LandingRule(private val to: Surface, items: List<Item>, speed: Float) {
        private val margin = course.landingMarginTiles * TILE

        /** On a small platform (GEN-26) anywhere she stands on it is good: she jumps again at once. */
        private val small = small(to, speed)
        private val endShift = -speed.toDouble() * RUNWAY_SECONDS
        var beforeShift = endShift
            private set
        private var past = Double.POSITIVE_INFINITY

        init {
            var last = Double.NEGATIVE_INFINITY
            for (item in items) {
                if (!item.kind.adds && item.x > to.start && item.x < to.end && item.y - to.top < Item.SIZE) {
                    val before = item.x - Item.SIZE / 2 - half - speed.toDouble() * OBSTACLE_WINDOW_SECONDS - to.end
                    beforeShift = min(beforeShift, before)
                    last = max(last, item.x + Item.SIZE / 2 + half.toDouble())
                }
            }
            if (last > Double.NEGATIVE_INFINITY) past = last
        }

        fun good(landing: JumpLab.Landing): Boolean {
            val at = landing.time
            if (!landing.landed || abs(landing.top - to.topAt(at)) >= 1.0) return false
            // On it, and not on something else just as high after it.
            if (small) return landing.x > to.startAt(at) - half && landing.x < to.endAt(at) + half
            if (landing.x < to.startAt(at) + margin) return false
            val end = to.endAt(at)
            return landing.x <= end + beforeShift || (landing.x >= past && landing.x <= end + endShift)
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
        val on = Surface(mover.start, mover.end, mover.top, mover, ledge = true)
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
        watch: Item? = null,
        runAfter: Int = 0,
        giveUpBelow: Float = Float.NEGATIVE_INFINITY,
        good: (JumpLab.Landing) -> Boolean,
    ): Boolean {
        var fairPresses = 0
        for (press in lastPress downTo 0) {
            // Not enough moments left to make it fair: the rest need not be tried.
            if (fairPresses + press + 1 < FAIR_PRESSES) return false
            var holds = 0
            lab.tryHolds(sandbox, items, startX, y, runStart, press, maxHold, time, watch, runAfter, giveUpBelow) { hold, landing ->
                if (!landing.touchedNegative && good(landing)) holds++
                // Enough good holds for this press, or no longer possible: the rest need not be tried.
                holds < course.fairHoldSteps && holds + (maxHold - hold) >= course.fairHoldSteps
            }
            if (holds >= course.fairHoldSteps && ++fairPresses >= FAIR_PRESSES) return true
        }
        return false
    }

    /** Whether any jump of those [fair] tries is [good] and touches nothing that takes points away. */
    private inline fun any(
        lastPress: Int,
        y: Float,
        startX: Double,
        items: List<Item>,
        watch: Item? = null,
        runAfter: Int = 0,
        giveUpBelow: Float = Float.NEGATIVE_INFINITY,
        good: (JumpLab.Landing) -> Boolean,
    ): Boolean {
        var found = false
        for (press in lastPress downTo 0) {
            lab.tryHolds(sandbox, items, startX, y, runStart, press, maxHold, 0.0, watch, runAfter, giveUpBelow) { _, landing ->
                found = !landing.touchedNegative && good(landing)
                !found
            }
            if (found) return true
        }
        return false
    }

    /** The path of one of the jumps the [rule] counts as good, from the latest presses and middling holds, into [path]. */
    private fun recordGoodPath(
        path: MutableList<Pair<Double, Float>>,
        y: Float,
        startX: Double,
        items: List<Item>,
        lastPress: Int,
        rule: LandingRule,
    ) {
        for (press in lastPress downTo 0) {
            for (hold in maxHold / 2 downTo 0) {
                val landing = lab.tryJump(sandbox, items, startX, y, runStart, press, hold)
                if (!landing.touchedNegative && rule.good(landing)) {
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
        const val MAX_NEGATIVES_PER_PIECE = 3
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
        const val AIR_TRIES = 3
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
        const val ROW_MAX_END = 1
        const val SHORT_ROWS_FROM_DIFFICULTY = 0.5f
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
         * The gaps with platforms (ADR 0009): a hop of 1 tile at least, as wide as a gap of the moment at most; the
         * platforms up to this many tiles wider than the shortest ground, and a tile wider for each one
         * they are lower than what comes before them; each a tile lower than what comes before it in this share, at the
         * same height up to the next and higher in the rest (two tiles in this share of those at full difficulty), and at
         * most this many tiles over the highest ground; a perrito or a gatito on this share of them; and the farthest
         * jumps tried to see that the gap needs them.
         */
        const val MIN_HOP_TILES = 1
        const val HOP_TRIES = 64
        const val PLATFORM_EXTRA_WIDTH = 1
        const val PLATFORM_DOWN_SHARE = 0.25f
        const val PLATFORM_LEVEL_SHARE = 0.55f
        const val PLATFORM_UP_TWO_SHARE = 0.15f

        /**
         * How many platforms a gap has (GEN-24): one in this share of them at the start and in this other at full
         * difficulty; three in this share at full difficulty; two in the rest. And the share of those with two or three
         * that are a staircase.
         */
        const val ONE_PLATFORM_START = 0.5f
        const val ONE_PLATFORM_END = 0.15f
        const val THREE_PLATFORMS_END = 0.45f
        const val STAIRS_SHARE = 0.6f
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

        /**
         * The animals watched over (GEN-23): how far apart the animal and the huevo frito or the pescado are on the ground,
         * in pixels between their middles: in a dilemma, the egg right behind the animal, their boxes almost touching; when
         * it asks for skill, the egg before the animal, two and a half to four tiles; and on a platform with a reward.
         */
        val DILEMMA_DISTANCES = intArrayOf(14, 16, 18)
        val SKILL_DISTANCES = intArrayOf(40, 48, 56, 64)
        const val GUARD_ON_PLATFORM = 16

        /** Where the natural jump over the gap lands, from the ground's start: the animal of a dilemma, the egg of skill. */
        const val DILEMMA_ANIMAL_AT = 3 * TILE / 2
        const val SKILL_NEGATIVE_AT = 3 * TILE / 2

        /** How many tiles narrower than the widest the gap before animals watched over on the ground is at least. */
        const val GUARD_GAP_SLACK = 1

        /** An animal alone on a platform is watched over as often as a row (ADR 0011: one of every 3 animals in all). */
        const val SINGLE_ANIMAL_SHARE = 1f

        /**
         * More that takes points away (GEN-25, ADR 0011): over each hop between platforms, this share of the chance of the
         * obstacles in the air; at full difficulty this share of the gaps with a pair above and below the path instead of
         * one alone, a step apart across and this much room beyond her body; and this share, at full difficulty, of the
         * places where she lands after a hop with one there, so far in.
         */
        const val HOP_AIR_SHARE = 1f
        const val PAIR_SHARE = 0.8f
        const val PAIR_STAGGER = 4
        const val PAIR_MARGIN = 5
        const val LANDING_NEGATIVE_SHARE = 1f
        const val WHERE_SHE_LANDS = 20

        /** How far below the lowest a surface goes a jump that falls gives up looking for a landing on it. */
        const val GIVE_UP_BELOW = 4f

        /**
         * What grows towards the minute 30 (GDD-23, ADR 0011): calms of the fewest pieces from this share of it on; another
         * gap with platforms right after one, in this share of them at its end; the small platforms of one tile from this
         * share to this one; the moving ones up to this share; and the widest gap, this much more of the longest jump.
         */
        const val SHORT_CALMS_FROM = 0.5f
        const val MORE_PLATFORMS_LATE = 0.5f
        const val ONE_TILE_START = 0.5f
        const val ONE_TILE_LATE = 0.9f
        const val MOVER_SHARE_LATE = 0.9f
        const val GAP_LATE = 0.05f

        /** The small platforms (GEN-26): from this difficulty on, of this many tiles. */
        const val SMALL_FROM_DIFFICULTY = 0.5f
        const val SMALL_MIN_TILES = 1
        const val SMALL_MAX_TILES = 2

        /**
         * The ground of a piece whose animals may be watched over: room for the landing, the first animals and what
         * watches over them, in tiles, besides the landing spread and the run-up to the next jump.
         */
        const val GUARD_ROOM_TILES = 5

        /** What [rescue] says. */
        const val SKILL = 0
        const val DILEMMA = 1
        const val NEITHER = 2
    }
}
