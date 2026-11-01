package com.fanguskdev.carofarty.game

import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class CourseGeneratorTest {
    private val config = GameConfig()
    private val course = config.course

    private fun pieces(seed: Long, count: Int): List<Piece> {
        val generator = CourseGenerator(config, seed)
        return List(count) { generator.next() }
    }

    @Test
    fun theFirstPieceIsTheFlatStart() {
        val start = pieces(seed = 1, count = 1).single()

        assertEquals(1, start.grounds.size)
        assertTrue(start.grounds.single().start < 0, "there is ground behind her at the start")
        assertEquals(course.startFlatTiles * TILE, start.end)
        assertTrue(start.items.isNotEmpty(), "perritos and gatitos within reach from the first seconds")
        assertTrue(start.items.all { it.kind.adds && it.x > 0 }, "nothing that takes points away, all of it ahead of her")
    }

    @Test
    fun stretchesOfTensionAndCalmTakeTurns() {
        // A calm piece: flat or the narrowest gap at the same height, and nothing that takes points away.
        val generator = CourseGenerator(config, seed = 5)
        var previous = generator.next().grounds.last()
        var calmInARow = 0
        var calmStretches = 0
        while (previous.end < 5_000 * TILE) {
            val piece = generator.next()
            val ground = piece.grounds.first()
            val calm =
                ground.start - previous.end <= course.minGapTiles * TILE && ground.top == previous.top && piece.items.all { it.kind.adds }
            calmInARow = if (calm) calmInARow + 1 else 0
            if (calmInARow == course.calmMinPieces) calmStretches++
            previous = piece.grounds.last()
        }
        // One every 150 to 250 m of tension at least: about 20 in 5 km.
        assertTrue(calmStretches >= 5_000 / (course.tensionMaxMeters + CALM_METERS), "$calmStretches calm stretches")
    }

    @Test
    fun neverThreeDifficultPiecesAlikeInARow() {
        val generator = CourseGenerator(config, seed = 9)
        var previous = generator.next().grounds.last()
        val shapes = ArrayDeque<String?>()
        repeat(2_000) {
            val piece = generator.next()
            val ground = piece.grounds.first()
            val gap = (ground.start - previous.end) / TILE
            val negatives = piece.items.filter { !it.kind.adds }.map { it.x < ground.start }.toSet()
            val blocks = piece.blocks.isNotEmpty()
            val difficult = gap >= DIFFICULT_GAP_TILES || negatives.isNotEmpty() || blocks
            shapes.addLast(if (difficult) "$gap ${ground.top - previous.top} $negatives $blocks" else null)
            if (shapes.size > 3) shapes.removeFirst()
            if (shapes.size == 3 && shapes.all { it != null }) {
                assertTrue(shapes.toSet().size > 1, "three alike in a row at ${ground.start}: ${shapes.first()}")
            }
            previous = piece.grounds.last()
        }
    }

    @Test
    fun theSameSeedGivesTheSameCourse() {
        fun describe(piece: Piece) =
            piece.grounds.map { Triple(it.start, it.end, it.top) } to piece.items.map { Triple(it.kind, it.x, it.y) }

        assertEquals(pieces(seed = 42, count = 200).map(::describe), pieces(seed = 42, count = 200).map(::describe))
    }

    @Test
    fun theSameSeedGivesTheSameRunOnEveryPlatform() {
        // The same number on the JVM, in the Android host tests and in the iOS simulator: the same seed gives the same run
        // on any phone (GEN-05). If the generator or the jump change on purpose, the number changes with them.
        var hash = FNV_OFFSET
        fun mix(value: Long) {
            hash = (hash xor value) * FNV_PRIME
        }
        val generator = CourseGenerator(config, seed = 2026)
        repeat(300) {
            val piece = generator.next()
            for (ground in piece.grounds) {
                mix(ground.start.toLong())
                mix(ground.end.toLong())
                mix(ground.top.toLong())
            }
            for (item in piece.items) {
                mix(item.kind.ordinal.toLong())
                mix(item.x.toLong())
                mix(item.y.toLong())
                mix(item.coat.toLong())
                mix(item.breed.toLong())
                mix((item.walk?.from ?: 0f).toLong())
                mix((item.walk?.to ?: 0f).toLong())
            }
            for (block in piece.blocks) {
                mix(block.start.toLong())
                mix(block.end.toLong())
                mix(block.bottom.toLong())
                mix(block.top.toLong())
            }
        }
        val run = GameRun(config, Scenario.Bosque, seed = 2026)
        Pilot(run).play(seconds = 60f)
        mix((run.caro.x * 1000).roundToLong())
        mix((run.caro.y * 1000).roundToLong())
        mix(run.points.toLong())
        assertEquals(SAME_RUN, hash)
    }

    @Test
    fun groundsFollowEachOtherWithinTheRules() {
        for (seed in 1L..3L) {
            var previous: Ground? = null
            for (piece in pieces(seed, count = 300)) {
                for (ground in piece.grounds) {
                    assertTrue(ground.top in course.minGroundTop * TILE..course.maxGroundTop * TILE, "height ${ground.top}")
                    assertTrue(ground.end - ground.start >= course.minGroundTiles * TILE, "a ground of ${ground.end - ground.start} px")
                    previous?.let { before ->
                        val gap = ground.start - before.end
                        assertTrue(gap == 0 || gap >= course.minGapTiles * TILE, "a gap of $gap px at ${before.end}")
                        if (gap == 0) assertEquals(before.top, ground.top, "a breather keeps the height")
                        val step = (ground.top - before.top) / TILE
                        assertTrue(step in -course.maxStepDown..course.maxStepUp, "a step of $step tiles at ${ground.start}")
                    }
                    previous = ground
                }
            }
        }
    }

    @Test
    fun collectiblesSitInTheirPieceAndNeverInsideTheGround() {
        val generator = CourseGenerator(config, seed = 7)
        val all = Course()
        repeat(300) { all.append(generator.next()) }
        val piecesItems = all.items
        assertTrue(piecesItems.any { it.kind.adds }, "there are perritos and gatitos")
        assertTrue(piecesItems.any { !it.kind.adds }, "there are eggs and fish")
        for (item in piecesItems) {
            val ground = all.groundTopUnder(item.x - 1.0, item.x + 1.0)
            if (ground != null) assertTrue(item.y - Item.SIZE / 2 >= ground, "an item inside the ground at ${item.x}")
        }
        assertTrue(piecesItems.zipWithNext().all { (a, b) -> a.x <= b.x }, "items in order")
    }

    @Test
    fun noAnimalFloats() {
        // Fangusk: «En el aire no puede aparecer ningún animal, salvo que esté en una plataforma».
        val generator = CourseGenerator(config, seed = 11)
        val all = Course()
        repeat(800) { all.append(generator.next()) }
        for (animal in all.items.filter { it.kind.adds }) {
            val onAGround = all.groundTopUnder(animal.x - 1.0, animal.x + 1.0) == animal.y - SITTING_HEIGHT
            val onABlock = all.blocks.any { animal.x >= it.start && animal.x < it.end && it.top == animal.y - SITTING_HEIGHT }
            assertTrue(onAGround || onABlock, "a ${animal.kind} in the air at ${animal.x}, ${animal.y}")
        }
    }

    @Test
    fun platformsWithARewardSitOverTheGroundWithinAJump() {
        val generator = CourseGenerator(config, seed = 17)
        val all = Course()
        repeat(800) { all.append(generator.next()) }
        val platforms = all.blocks.filter { block ->
            all.items.any {
                it.kind.adds &&
                    it.x >= block.start &&
                    it.x < block.end &&
                    it.y == block.top + SITTING_HEIGHT
            }
        }
        assertTrue(platforms.isNotEmpty(), "there are platforms with a reward")
        for (platform in platforms) {
            val ground = all.groundTopUnder(platform.start.toDouble(), platform.end.toDouble())
            assertTrue(ground != null, "a platform over a ground, never over a gap")
            assertTrue(platform.top - ground <= HIGHEST_JUMP - SITTING_HEIGHT, "a platform at ${platform.start} out of reach")
        }
    }

    @Test
    fun obstaclesInTheAirAppearAndLeaveRoomToRunUnder() {
        val generator = CourseGenerator(config, seed = 21)
        val all = Course()
        repeat(1_500) { all.append(generator.next()) }
        assertTrue(all.blocks.isNotEmpty(), "there are blocks in the air")
        // Eggs and fish over a gap, lower than the ceilings: on the path of a jump.
        val onThePath = all.items.count { item ->
            val before = all.grounds.lastOrNull { it.end <= item.x }
            !item.kind.adds && all.groundTopUnder(item.x - 1.0, item.x + 1.0) == null && before != null && item.y - before.top < 70
        }
        assertTrue(onThePath > 0, "there are eggs and fish on the path of a jump")
        val half = Item.SIZE / 2
        for (block in all.blocks) {
            for (ground in all.grounds) {
                if (block.end > ground.start && block.start < ground.end) {
                    assertTrue(block.bottom >= ground.top + BLOCK_CLEARANCE, "a block at ${block.start} too low to run under")
                }
            }
            for (item in all.items) {
                val inside =
                    item.x + half > block.start && item.x - half < block.end && item.y + half > block.bottom && item.y - half < block.top
                assertTrue(!inside, "a collectible inside the block at ${block.start}")
            }
        }
    }

    @Test
    fun perritosWalkOnTheirGroundAwayFromItsEdgesAndFromWhatTakesPointsAway() {
        val generator = CourseGenerator(config, seed = 13)
        val all = Course()
        repeat(800) { all.append(generator.next()) }
        assertTrue(all.items.any { it.walk != null }, "some perritos walk")
        assertTrue(all.items.any { it.kind == ItemKind.Perrito && it.walk == null }, "and some stay still")
        for (perrito in all.items) {
            val walk = perrito.walk ?: continue
            assertEquals(ItemKind.Perrito, perrito.kind)
            val ground = all.grounds.first { perrito.x >= it.start && perrito.x < it.end }
            val from = perrito.x + walk.from
            val to = perrito.x + walk.to
            assertTrue(from >= ground.start + 2 * TILE && to <= ground.end - 2 * TILE, "a perrito near the edge at ${perrito.x}")
            for (other in all.items) {
                val onTheGround = !other.kind.adds && other.x >= ground.start && other.x < ground.end && other.y - ground.top < Item.SIZE
                if (onTheGround) {
                    assertTrue(
                        other.x <= from - 3 * TILE || other.x >= to + 3 * TILE,
                        "a perrito by an obstacle at ${perrito.x}",
                    )
                }
            }
        }
        val coats = all.items.filter { it.kind == ItemKind.Gatito }.map { it.coat }.toSet()
        assertEquals((0 until Item.COATS).toSet(), coats, "the gatitos wear all eight coats")
        val breeds = all.items.filter { it.kind == ItemKind.Perrito }.map { it.breed }.toSet()
        assertEquals((0 until Item.BREEDS).toSet(), breeds, "the perritos come in all nine breeds")
    }

    @Test
    fun aCarefulPilotRunsKilometresWithoutFalling() {
        for (seed in 1L..3L) {
            val run = GameRun(config, Scenario.Bosque, seed)
            Pilot(run).play(seconds = 240f)
            assertTrue(run.endCause != EndCause.Fall, "seed $seed: fell at ${run.meters.toInt()} m")
            assertTrue(run.meters > 2_000, "seed $seed: only ${run.meters.toInt()} m")
        }
    }

    @Test
    fun theBackgroundGeneratorKeepsUpWithTheRunOnEveryPlatform() {
        val clock = TimeSource.Monotonic.markNow()
        val generator = CourseGenerator(config, seed = 100)
        repeat(1_000) { generator.next() }
        val millisPerPiece = clock.elapsedNow().inWholeMicroseconds / 1000.0 / 1_000
        println("CourseGenerator: 1000 piezas, ${ceil(millisPerPiece * 100) / 100} ms por pieza")
        assertTrue(millisPerPiece < MAX_MILLIS_PER_PIECE, "$millisPerPiece ms per piece")
    }

    @Test
    fun theDifficultyGrowsSmoothlyFromZeroToOne() {
        val generator = CourseGenerator(config, seed = 1)
        assertEquals(0f, generator.difficultyAt(0.0))
        assertEquals(0.5f, generator.difficultyAt(course.difficultyRampMeters / 2.0), 0.001f)
        assertEquals(1f, generator.difficultyAt(100_000.0))
    }

    private companion object {
        const val FNV_OFFSET = -3750763034362895579L
        const val FNV_PRIME = 1099511628211L
        const val SAME_RUN = 3697109979829440507L

        /** How high an animal's centre sits over what it stands on, and the highest jump of docs/movimiento.md. */
        const val SITTING_HEIGHT = 8
        const val HIGHEST_JUMP = 67

        /** The generator's measure of a difficult piece, and how long a calm stretch lasts at most, in metres. */
        const val DIFFICULT_GAP_TILES = 4
        const val CALM_METERS = 150

        /** The room a block leaves over a ground: her whole sprite, 24 px, and a little more. */
        const val BLOCK_CLEARANCE = 28

        /**
         * Pieces are made in the background (BufferedPieces) and the run uses about one a second, so a piece only has to
         * be far quicker than that, also in the unoptimised test builds of iOS (about 8 ms there; 0,4 ms on the JVM).
         */
        const val MAX_MILLIS_PER_PIECE = 40.0
    }
}
