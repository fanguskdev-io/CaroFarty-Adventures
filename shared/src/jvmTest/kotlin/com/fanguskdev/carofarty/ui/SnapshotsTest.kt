package com.fanguskdev.carofarty.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import com.fanguskdev.carofarty.App
import com.fanguskdev.carofarty.engine.PixelViewport
import com.fanguskdev.carofarty.game.Scenario
import com.fanguskdev.carofarty.storage.Choices
import com.fanguskdev.carofarty.storage.MemoryStore
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Draws the real screen into PNG files with Skia, without a window (build/capturas/), and checks the pixel grid at six
 * proportions of screen, from 4:3 to 21:9 (TEC-04, UI-04, UI-14): every game pixel is a square of identical phone pixels.
 */
@OptIn(ExperimentalComposeUiApi::class)
class SnapshotsTest {
    private val out = File("build/capturas").apply { mkdirs() }
    private val frame = 16_666_667L

    /** A scene of the whole app, driven frame by frame, with taps at given frames: how many frames held, and where. */
    private class Script(val frames: Int, val taps: Map<Int, Tap> = emptyMap(), val debug: Boolean = false)

    private class Tap(val hold: Int, val at: Offset? = null)

    private fun render(
        name: String,
        width: Int,
        height: Int,
        script: Script,
        store: MemoryStore = MemoryStore(),
    ): Image {
        var image: Image? = null
        ImageComposeScene(width, height, Density(3f)) {
            if (script.debug) GameScreen(store, debugBuild = true, debugAtStart = true) else App(store)
        }.use { scene ->
            var time = 0L
            val centre = Offset(width * 0.6f, height * 0.6f)
            var finger = centre
            var releaseAt = -1
            for (index in 0 until script.frames) {
                script.taps[index]?.let { tap ->
                    finger = tap.at ?: centre
                    scene.sendPointerEvent(PointerEventType.Press, finger)
                    releaseAt = index + tap.hold
                }
                if (index == releaseAt) scene.sendPointerEvent(PointerEventType.Release, finger)
                image = scene.render(time)
                time += frame
                if (index % 30 == 0) Thread.sleep(5) // lets the pieces made in the background arrive
            }
        }
        val result = image ?: error("nothing was drawn")
        File(out, "$name.png").writeBytes(result.encodeToData(EncodedImageFormat.PNG)?.bytes ?: error("PNG"))
        return result
    }

    /** Every game pixel of [image] is a square of [PixelViewport.scale] identical pixels. */
    private fun assertPixelGrid(image: Image, name: String) {
        val viewport = PixelViewport.fit(image.width, image.height)
        val bitmap = Bitmap.makeFromImage(image)
        val s = viewport.scale
        for (gy in 0 until viewport.height) {
            for (gx in 0 until viewport.width) {
                val x0 = viewport.offsetX + gx * s
                val y0 = viewport.offsetY + gy * s
                val colour = bitmap.getColor(x0, y0)
                for (dy in 0 until s) {
                    for (dx in 0 until s) {
                        if (bitmap.getColor(x0 + dx, y0 + dy) != colour) fail("$name: the game pixel ($gx, $gy) is not one colour")
                    }
                }
            }
        }
    }

    @Test
    fun theReadyScreenKeepsThePixelGridAtSixProportions() {
        val screens = listOf(
            Triple("16x9", 1280, 720),
            Triple("20x9", 2400, 1080),
            Triple("21x9", 2520, 1080),
            Triple("iphone17pro", 2622, 1206),
            Triple("pixel10proxl", 2992, 1344),
            Triple("ipad", 2752, 2064),
        )
        for ((name, width, height) in screens) {
            val image = render("listo-$name", width, height, Script(frames = 30))
            assertEquals(width, image.width)
            assertPixelGrid(image, name)
        }
    }

    @Test
    fun aRunAndItsTwoEnds() {
        val start = mapOf(10 to Tap(hold = 2))
        render("jugando", 2400, 1080, Script(frames = 150, taps = start))
        render("fin", 2400, 1080, Script(frames = 60 * 9, taps = start))
        render("depuracion", 2400, 1080, Script(frames = 150, taps = start, debug = true))
        // With the debugging tools, two taps on the points take the 10 of the start away.
        val points = Offset(60f, 50f)
        val noPoints = start + mapOf(40 to Tap(hold = 2, at = points), 50 to Tap(hold = 2, at = points))
        render("fin-sin-puntos", 2400, 1080, Script(frames = 90, taps = noPoints, debug = true))
    }

    @Test
    fun theThreeScenarios() {
        for (scenario in Scenario.entries) {
            val store = MemoryStore().also { Choices(it).scenario = scenario }
            val name = scenario.name.lowercase()
            render("escenario-$name", 2400, 1080, Script(frames = 240, taps = mapOf(10 to Tap(hold = 2))), store)
            render("escenario-$name-listo", 1280, 720, Script(frames = 30), store)
        }
    }
}
