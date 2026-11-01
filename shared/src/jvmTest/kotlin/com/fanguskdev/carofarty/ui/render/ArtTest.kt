package com.fanguskdev.carofarty.ui.render

import com.fanguskdev.carofarty.game.Item
import com.fanguskdev.carofarty.game.Reaction
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The art that tools/arte.py left in the resources is complete and the game reads it (TEC-11, TEC-18). */
class ArtTest {
    private val art = runBlocking { Art.load() }

    @Test
    fun caroHasEveryPoseWithItsFacesAndHerClothes() {
        val caro = assertNotNull(art?.caro, "the art of Caro")
        for (pose in CaroPose.entries) {
            for (reaction in Reaction.entries) assertTrue(caro.animation(pose, reaction).size > 0, "$pose $reaction")
        }
        assertEquals(12, caro.anchorX)
        assertEquals(32, caro.anchorY)
    }

    @Test
    fun theCollectiblesComeInTheirCoatsAndBreeds() {
        val items = assertNotNull(art?.items, "the art of the collectibles")
        assertNotNull(items.gatito)
        assertNotNull(items.huevo)
        assertNotNull(items.pescado)
        assertNotNull(items.smell)
        assertEquals(Item.COATS, items.coats.size, "a sheet for each coat of the gatito")
        assertEquals(Item.BREEDS, items.breeds.size, "and the nine breeds of the perrito")
        assertEquals(1, items.gatito.emptyBelow, "the gatito is lowered a pixel to sit on the ground")
    }
}
