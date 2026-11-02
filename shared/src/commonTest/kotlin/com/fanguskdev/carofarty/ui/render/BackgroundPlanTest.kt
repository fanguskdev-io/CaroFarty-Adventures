package com.fanguskdev.carofarty.ui.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackgroundPlanTest {
    private val speed = 240.0
    private val layers = listOf(PlanLayer(0.3, 512, listOf(200 until 260)), PlanLayer(0.6, 512, listOf(100 until 180)))

    /** Every window in which a variant is on the screen while the camera goes from the start to [until]. */
    private fun windows(seed: Long, until: Double): List<ClosedFloatingPointRange<Double>> {
        val plan = BackgroundPlan(seed, layers, restCamera = -96.0, speedAt = { speed })
        val seen = mutableListOf<ClosedFloatingPointRange<Double>>()
        var camera = -96.0
        while (camera < until) {
            plan.reach(camera)
            seen += plan.windows()
            camera += 500
        }
        return seen.distinct().sortedBy { it.start }
    }

    @Test
    fun oneCabinAtATimeTheFirstSoonAndThenAPauseAfterEach() {
        for (seed in 1L..5L) {
            val windows = windows(seed, until = 300_000.0)
            assertTrue(windows.size > 30, "seed $seed: only ${windows.size} cabins")
            assertTrue((windows.first().start + 96) / speed >= 3.0, "the first one, not before 3 s")
            for (i in 1 until windows.size) {
                val pause = (windows[i].start - windows[i - 1].endInclusive) / speed
                assertTrue(pause >= 8.0, "seed $seed: only $pause s from one leaving to the next")
            }
        }
    }

    @Test
    fun neverThreeTimesRunningInTheSameLayer() {
        val plan = BackgroundPlan(7, layers, restCamera = -96.0, speedAt = { speed })
        val seen = mutableListOf<Int>()
        var camera = -96.0
        while (camera < 400_000) {
            plan.reach(camera)
            for (layer in layers.indices) {
                for (lap in 0L..3_000L) {
                    if (plan.variant(layer, lap) >= 0 && (seen.isEmpty() || seen.last() != layer * 10_000 + lap.toInt())) {
                        seen += layer * 10_000 + lap.toInt()
                    }
                }
            }
            camera += 2_000
        }
        val layersInOrder = seen.distinct().map { it / 10_000 }
        for (i in 2 until layersInOrder.size) {
            assertTrue(layersInOrder[i] != layersInOrder[i - 1] || layersInOrder[i] != layersInOrder[i - 2], "three running at $i")
        }
    }

    @Test
    fun aPlannedLapShowsItsVariantAndTheOthersTheirBase() {
        val plan = BackgroundPlan(3, layers, restCamera = -96.0, speedAt = { speed })
        plan.reach(-96.0)
        var shown = 0
        for (layer in layers.indices) {
            for (lap in -10L..2_000L) if (plan.variant(layer, lap) >= 0) shown++
        }
        assertEquals(plan.windows().size, shown, "one lap of one layer for each planned variant")
    }

    @Test
    fun withoutVariantsNothingIsPlanned() {
        val plan = BackgroundPlan(3, listOf(PlanLayer(0.3, 512, emptyList())), restCamera = -96.0, speedAt = { speed })
        plan.reach(10_000.0)
        assertEquals(-1, plan.variant(0, 5))
        assertTrue(plan.windows().isEmpty())
    }
}
