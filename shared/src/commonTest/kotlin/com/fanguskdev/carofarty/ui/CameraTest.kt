package com.fanguskdev.carofarty.ui

import com.fanguskdev.carofarty.engine.PixelViewport
import com.fanguskdev.carofarty.game.GameConfig
import com.fanguskdev.carofarty.game.TILE
import com.fanguskdev.carofarty.ui.render.CARO_HEIGHT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CameraTest {
    @Test
    fun itDoesNotMoveWhileSheRunsOnTheHighestGround() {
        val highest = GameConfig().course.maxGroundTop * TILE
        for (height in PixelViewport.MIN_HEIGHT..PixelViewport.MAX_HEIGHT) assertEquals(0, cameraBottom(highest, height))
    }

    @Test
    fun itGoesUpOnTheHighestJumpOfTheShortestScreen() {
        val peak = GameConfig().course.maxGroundTop * TILE + LONGEST_JUMP_HEIGHT
        assertTrue(cameraBottom(peak, PixelViewport.MIN_HEIGHT) > 0)
    }

    @Test
    fun herHeadNeverComesCloserThanTheHeadRoomToTheTop() {
        for (height in PixelViewport.MIN_HEIGHT..PixelViewport.MAX_HEIGHT) {
            for (feet in -100..400) {
                val bottom = cameraBottom(feet, height)
                assertTrue(bottom >= 0)
                assertTrue(height + bottom - feet - CARO_HEIGHT >= HEAD_ROOM, "pies $feet, alto $height")
                if (bottom > 0) assertEquals(HEAD_ROOM, height + bottom - feet - CARO_HEIGHT)
            }
        }
    }

    private companion object {
        /** The highest jump of docs/movimiento.md, with the finger down the whole window. */
        const val LONGEST_JUMP_HEIGHT = 67
    }
}
