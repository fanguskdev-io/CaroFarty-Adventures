package com.fanguskdev.carofarty.game

import kotlin.math.abs
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
        assertTrue(start.items.isEmpty())
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
    fun someObstaclesHaveAPerritoOrAGatitoAboveThatAQuickTapMisses() {
        val generator = CourseGenerator(config, seed = 11)
        val all = Course()
        repeat(400) { all.append(generator.next()) }
        val movement = config.movement
        val rewards = all.items.count { obstacle ->
            val ground = all.groundTopUnder(obstacle.x - 1.0, obstacle.x + 1.0) ?: return@count false
            val onTheGround = !obstacle.kind.adds && obstacle.y - ground < Item.SIZE
            // Above her head at the top of a quick tap: only a longer jump gets it.
            val missed = ground + QUICK_TAP_HEIGHT + movement.hitboxHeight
            onTheGround && all.items.any { it.kind.adds && abs(it.x - obstacle.x) < CHOICE_REACH && it.y - Item.SIZE / 2 > missed }
        }
        assertTrue(rewards > 0, "no obstacle has a reward for a long jump")
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
        const val SAME_RUN = 4668162340810115065L

        /** How high a quick tap of 3 steps goes (docs/movimiento.md), and how far from its obstacle a long jump's top can be. */
        const val QUICK_TAP_HEIGHT = 24
        const val CHOICE_REACH = 4 * TILE

        /**
         * Pieces are made in the background (BufferedPieces) and the run uses about one a second, so a piece only has to
         * be far quicker than that, also in the unoptimised test builds of iOS (about 8 ms there; 0,4 ms on the JVM).
         */
        const val MAX_MILLIS_PER_PIECE = 40.0
    }
}
