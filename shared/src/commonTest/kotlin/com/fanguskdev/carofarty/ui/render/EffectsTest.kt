package com.fanguskdev.carofarty.ui.render

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The effects of a run (VFX-01, VFX-03, VFX-09, ART-16): how many at once, the leaves of the Bosque and the calima. */
class EffectsTest {
    @Test
    fun atMostSixEffectsAndANewOneTakesThePlaceOfTheOldest() {
        val effects = WorldEffects()
        repeat(8) { effects.add(Effect.DustUp, it * 10.0, 32, millis = it * 100L) }
        assertEquals(WorldEffects.MAX, effects.size)
        effects.limit = 3
        assertEquals(3, effects.size, "trimmed (VFX-09)")
        effects.limit = 0
        effects.add(Effect.Burst, 0.0, 0, 0)
        assertEquals(0, effects.size)
    }

    @Test
    fun fourToSixLeavesAndOneThatLeavesComesBack() {
        val leaves = Leaves(Random(5))
        leaves.advance(0, camera = 0.0, width = 400, height = 180)
        assertTrue(leaves.size in Leaves.MIN..Leaves.MAX)
        // She runs for a long while: they go by and come back, always as many and always near the screen.
        var camera = 0.0
        for (step in 1..600) {
            camera += 6.0
            leaves.advance(step * 16L, camera, 400, 180)
            for (i in 0 until leaves.size) {
                assertTrue(leaves.left(i) in -60..460 && leaves.top(i) in -60..180, "leaf $i at ${leaves.left(i)}, ${leaves.top(i)}")
            }
        }
        val count = leaves.size
        leaves.limit = 0
        assertEquals(0, leaves.size, "the first to go when the frames do not keep up")
        leaves.limit = Leaves.MAX
        assertEquals(count, leaves.size)
    }

    @Test
    fun theCalimaMovesPairsOfRowsAPixelEachWayAtMost() {
        val first = Haze.at(0)
        assertEquals((Haze.LAST - Haze.FIRST + 1) / Haze.ROWS, first.size)
        for (millis in 0L until 1_200L step 50) assertTrue(Haze.at(millis).all { it in -1..1 })
        // Each pair a twelfth of a turn after the one above it, and the whole a turn every 1.2 s.
        assertEquals(1, first[3])
        assertEquals(-1, first[9])
        assertEquals(Haze.at(100).toList(), Haze.at(1_300).toList())
    }
}
