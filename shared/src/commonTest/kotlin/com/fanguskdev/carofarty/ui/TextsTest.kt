package com.fanguskdev.carofarty.ui

import com.fanguskdev.carofarty.game.EndCause
import com.fanguskdev.carofarty.game.ItemKind
import com.fanguskdev.carofarty.game.Scenario
import kotlin.test.Test
import kotlin.test.assertEquals

class TextsTest {
    @Test
    fun theEndSaysHowSheLostWithAJoke() {
        // Fangusk's titles, by how and where she lost, and GameMaster's for a bird.
        assertEquals("¡UPS! ¡AL AGUA!", Texts.endTitle(EndCause.Fall, Scenario.Bosque, by = null))
        assertEquals("¡UPS! ¡AL AGUA!", Texts.endTitle(EndCause.Fall, Scenario.Nieve, by = ItemKind.Huevo))
        assertEquals("¡UPS! ¡AL CAÑÓN!", Texts.endTitle(EndCause.Fall, Scenario.Desierto, by = null))
        assertEquals("¡PUAJ! DEMASIADO MAL OLOR", Texts.endTitle(EndCause.NoPoints, Scenario.Bosque, by = ItemKind.Pescado))
        assertEquals("¡AY! ¡QUÉ PICOTAZO!", Texts.endTitle(EndCause.NoPoints, Scenario.Desierto, by = ItemKind.Pajaro))
    }

    @Test
    fun andHowManyAnimalsSheSaved() {
        assertEquals("¡LOS ANIMALITOS TE ESPERAN!", Texts.saved(0))
        assertEquals("HAS SALVADO 1 ANIMALITO", Texts.saved(1))
        assertEquals("HAS SALVADO 23 ANIMALITOS", Texts.saved(23))
    }
}
