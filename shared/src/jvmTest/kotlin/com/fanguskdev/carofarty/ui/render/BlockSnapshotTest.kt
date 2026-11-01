package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toArgb
import com.fanguskdev.carofarty.game.Block
import com.fanguskdev.carofarty.game.GameConfig
import com.fanguskdev.carofarty.game.GameRun
import com.fanguskdev.carofarty.game.Ground
import com.fanguskdev.carofarty.game.Item
import com.fanguskdev.carofarty.game.ItemKind
import com.fanguskdev.carofarty.game.Piece
import com.fanguskdev.carofarty.game.PieceSource
import com.fanguskdev.carofarty.game.Scenario
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The obstacles in the air drawn in a fixed scene (ADR 0007): a block over a gap and an egg on the path of a jump, into
 * build/capturas/bloques.png. The block has to be there, with its dark outline.
 */
class BlockSnapshotTest {
    @Test
    fun aBlockAndAnEggInTheAirAreDrawn() {
        val block = Block(240, 272, bottom = 96, top = 112)
        val pieces = object : PieceSource {
            private var given = false

            override fun nextOrNull(): Piece? {
                if (given) return null
                given = true
                val grounds = listOf(Ground(-512, 200, 32), Ground(320, 4_000, 32))
                return Piece(grounds, listOf(Item(ItemKind.Huevo, 290, 76)), 4_000, blocks = listOf(block))
            }
        }
        val run = GameRun(GameConfig(), Scenario.Bosque, seed = 1, pieces = pieces)
        val width = 400
        val height = 180
        val image = PixelBuffer().paint(width, height) {
            paintWorld(run, cameraLeft = 0L, cameraBottom = 0, width, height, GreyPalette.of(Scenario.Bosque), false, 0.0, 32f)
        }
        val bitmap = image.asSkiaBitmap()
        // The block's outline at its top left corner, in the buffer (y grows down from the top).
        val outline = bitmap.getColor(block.start, height - block.top)
        assertTrue(outline == GreyPalette.ink.toArgb(), "the block's outline is drawn")
        File("build/capturas").mkdirs()
        val png = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG) ?: error("PNG")
        File("build/capturas/bloques.png").writeBytes(png.bytes)
    }
}
