package com.fanguskdev.carofarty.game

import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/** The long checks, on the JVM, where they take seconds instead of minutes: GEN-11 and GEN-03. */
class LongRunsTest {
    private val config = GameConfig()

    @Test
    fun tenThousandPiecesInARowWithoutAnError() {
        val clock = TimeSource.Monotonic.markNow()
        var count = 0
        for (seed in 100L until 110L) {
            val generator = CourseGenerator(config, seed)
            repeat(1_000) {
                generator.next()
                count++
            }
        }
        val millisPerPiece = clock.elapsedNow().inWholeMicroseconds / 1000.0 / count
        println("GEN-11: $count piezas, ${ceil(millisPerPiece * 100) / 100} ms por pieza")
        assertEquals(10_000, count)
        assertTrue(millisPerPiece < 4.0, "$millisPerPiece ms per piece")
    }

    @Test
    fun aCarefulPilotNeverHasToTouchWhatTakesPointsAway() {
        // In every scenario, with its own dangers (ADR 0011): the fish that leap, the ice and the quicksand.
        for (scenario in Scenario.entries) {
            for (seed in 1L..5L) {
                val run = GameRun(config, scenario, seed)
                Pilot(run).play(seconds = 600f)
                val touched = run.events.filterIsInstance<RunEvent.Picked>().filter { it.points < 0 }
                println("GEN-07, $scenario, semilla $seed: ${run.meters.toInt()} m, ${touched.size} tocados")
                assertTrue(touched.isEmpty(), "$scenario, seed $seed: touched ${touched.size} by ${run.meters.toInt()} m")
                assertTrue(run.endCause != EndCause.Fall, "$scenario, seed $seed: fell at ${run.meters.toInt()} m")
            }
        }
    }

    @Test
    fun thirtyMinutesOfRunWithoutHolesNorOverlaps() {
        val run = GameRun(config, Scenario.Desierto, seed = 2026)
        val pilot = Pilot(run)
        var lastEnd = run.course.end
        repeat(30 * 60 * 60) {
            if (run.phase == RunPhase.Over) return@repeat
            pilot.step()
            val grounds = run.course.grounds
            for (i in 1 until grounds.size) assertTrue(grounds[i].start >= grounds[i - 1].end, "grounds overlap at ${grounds[i].start}")
            assertTrue(run.course.end >= lastEnd)
            lastEnd = run.course.end
        }
        println("GEN-03: ${run.meters.toInt()} m en 30 min, fin: ${run.endCause}")
        assertTrue(run.endCause != EndCause.Fall, "fell at ${run.meters.toInt()} m")
        assertTrue(run.meters > 18_000)
    }
}
