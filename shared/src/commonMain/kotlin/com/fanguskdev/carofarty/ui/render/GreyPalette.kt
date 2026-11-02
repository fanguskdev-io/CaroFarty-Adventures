package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.graphics.Color
import com.fanguskdev.carofarty.game.Item
import com.fanguskdev.carofarty.game.ItemKind
import com.fanguskdev.carofarty.game.Scenario

/**
 * The colours of the grey prototype (M1): greys tinted by scenario so the three can be told apart, and plain colours for
 * the collectibles. The final art replaces all of it in M2 and M3.
 */
internal class GreyPalette(
    val sky: Color,
    val far: Color,
    val groundTop: Color,
    val groundBody: Color,
    val groundLine: Color,
) {
    companion object {
        val ink = Color(0xFF1E1E24)
        val text = Color(0xFFF4F4F4)
        val shadow = Color(0xFF2A2A30)
        val panel = Color(0xCC1E1E24)

        /** Over the moving scenery of the menus, so their words read. */
        val shade = Color(0x661E1E24)
        val good = Color(0xFF3FAF5A)
        val bad = Color(0xFFD9483B)
        val record = Color(0xFFFFD44A)
        val debug = Color(0xFFFF3BD5)

        private val bosque = GreyPalette(Color(0xFFA9C2AE), Color(0xFF8DA693), Color(0xFF8E9A90), Color(0xFF6B756D), Color(0xFF5C655E))
        private val nieve = GreyPalette(Color(0xFFBCCAD8), Color(0xFFA0AFBF), Color(0xFFE6EAEE), Color(0xFF8D96A0), Color(0xFF7B848E))
        private val desierto = GreyPalette(Color(0xFFD8C9A6), Color(0xFFC2B18C), Color(0xFFB8A888), Color(0xFF92846A), Color(0xFF80735B))

        fun of(scenario: Scenario): GreyPalette = when (scenario) {
            Scenario.Bosque -> bosque
            Scenario.Nieve -> nieve
            Scenario.Desierto -> desierto
        }

        /** The fill of a collectible; a gatito's, the colour of its coat (ADR 0008). */
        fun itemFill(item: Item): Color = when (item.kind) {
            ItemKind.Perrito -> perrito
            ItemKind.Gatito -> coats[item.coat]
            ItemKind.Huevo -> huevo
            ItemKind.Pescado -> pescado
        }

        /** The colour of the letter on a collectible: dark, or light on the darkest coats. */
        fun itemInk(item: Item): Color = if (item.kind == ItemKind.Gatito && (item.coat == BLACK || item.coat == CHOCOLATE)) text else ink

        private val perrito = Color(0xFFC9955B)
        private val huevo = Color(0xFFF5F0DC)
        private val pescado = Color(0xFF6FA8DC)

        /** The eight coats of the gatitos, in the order of Item.COATS: a hint of each until their drawings arrive. */
        private val coats = listOf(
            Color(0xFFF0A040),
            Color(0xFF8A9BB0),
            Color(0xFFE9C29A),
            Color(0xFFF3DDB0),
            Color(0xFF3A3A40),
            Color(0xFFF4F4F4),
            Color(0xFF7B4A2E),
            Color(0xFFE9DCC6),
        )
        private const val BLACK = 4
        private const val CHOCOLATE = 6

        /** The letter on a collectible, so they can be told apart before they have their drawings. */
        fun itemLetter(kind: ItemKind): String = when (kind) {
            ItemKind.Perrito -> "P"
            ItemKind.Gatito -> "G"
            ItemKind.Huevo -> "H"
            ItemKind.Pescado -> "F"
        }
    }
}
