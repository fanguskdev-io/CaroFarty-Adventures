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
import com.fanguskdev.carofarty.storage.Records
import com.fanguskdev.carofarty.ui.render.Box
import com.fanguskdev.carofarty.ui.render.IntroShow
import com.fanguskdev.carofarty.ui.render.MenuLayout
import com.fanguskdev.carofarty.ui.render.PanelLine
import com.fanguskdev.carofarty.ui.render.SafeInsets
import com.fanguskdev.carofarty.ui.render.panelBottom
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Draws the real screens into PNG files with Skia, without a window (build/capturas/): the title screen, the start
 * screen, how to play, the records, runs and their ends, in the three scenarios; and checks the pixel grid at six
 * proportions of screen, from 4:3 to 21:9 (TEC-04, UI-04, UI-14): every game pixel is a square of identical phone pixels.
 */
@OptIn(ExperimentalComposeUiApi::class)
class SnapshotsTest {
    private val out = File("build/capturas").apply { mkdirs() }
    private val frame = 16_666_667L

    /** A scene of the whole app, driven frame by frame, with taps at given frames: how many frames held, and where. */
    private class Script(val frames: Int, val taps: Map<Int, Tap> = emptyMap(), val debug: Boolean = false)

    private class Tap(val hold: Int, val at: Offset? = null)

    private companion object {
        /** The tap on the title screen that comes before the scripts, and the frame where they start. */
        const val TITLE_TAP = 2
        const val TITLE_FRAMES = 8

        /** How long after the rescue the capture of it is taken. */
        const val RESCUE_SHOWN_MILLIS = 200L
    }

    /**
     * Draws [script] into `build/capturas/[name].png` and returns its last frame. The game opens on the title screen: unless
     * the scene is of it ([pastTheTitle] false), a tap there comes first and the script starts [TITLE_FRAMES] later.
     */
    private fun render(
        name: String,
        width: Int,
        height: Int,
        script: Script,
        store: MemoryStore = MemoryStore(),
        pastTheTitle: Boolean = true,
        quit: (() -> Unit)? = null,
    ): Image {
        var image: Image? = null
        val shift = if (pastTheTitle) TITLE_FRAMES else 0
        val taps = script.taps.mapKeys { it.key + shift } + if (pastTheTitle) mapOf(TITLE_TAP to Tap(hold = 2)) else emptyMap()
        ImageComposeScene(width, height, Density(3f)) {
            if (script.debug) GameScreen(store, debugBuild = true, debugAtStart = true) else App(store, quit = quit)
        }.use { scene ->
            var time = 0L
            val centre = Offset(width * 0.6f, height * 0.6f)
            var finger = centre
            var releaseAt = -1
            for (index in 0 until script.frames + shift) {
                taps[index]?.let { tap ->
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

    /** A store where she has already seen how to play, so the game opens on the start screen. */
    private fun seen(scenario: Scenario = Scenario.Bosque): MemoryStore = MemoryStore().also {
        Choices(it).tutorialSeen = true
        Choices(it).scenario = scenario
    }

    /** The point of a screen [width] × [height] at the centre of [box] of the menus, in its pixels. */
    private fun at(width: Int, height: Int, box: (MenuLayout) -> Box): Offset {
        val viewport = PixelViewport.fit(width, height)
        val target = box(MenuLayout(viewport.width, viewport.height, SafeInsets()))
        return Offset(
            viewport.offsetX + (target.left + target.width / 2f) * viewport.scale,
            viewport.offsetY + (target.top + target.height / 2f) * viewport.scale,
        )
    }

    /** A tap on the card of [scenario] at frame 10, which starts a run there. */
    private fun startIn(scenario: Scenario, width: Int = 2400, height: Int = 1080): Map<Int, Tap> =
        mapOf(10 to Tap(hold = 2, at = at(width, height) { it.cards[scenario.ordinal] }))

    @Test
    fun theStartScreenKeepsThePixelGridAtSixProportions() {
        val screens = listOf(
            Triple("16x9", 1280, 720),
            Triple("20x9", 2400, 1080),
            Triple("21x9", 2520, 1080),
            Triple("iphone17pro", 2622, 1206),
            Triple("pixel10proxl", 2992, 1344),
            Triple("ipad", 2752, 2064),
        )
        for ((name, width, height) in screens) {
            val image = render("inicio-$name", width, height, Script(frames = 30), seen())
            assertEquals(width, image.width)
            assertPixelGrid(image, name)
        }
    }

    @Test
    fun howToPlayTheFirstTimeTheGameOpens() {
        val image = render("como-se-juega-1", 2400, 1080, Script(frames = 90))
        assertPixelGrid(image, "como-se-juega")
        val next = Tap(hold = 2, at = at(2400, 1080) { it.forward("SIGUIENTE") })
        render("como-se-juega-2", 2400, 1080, Script(frames = 150, taps = mapOf(30 to next)))
        // From the start screen, the last page ends in «¡ENTENDIDO!»; and the button forward, pressed.
        val open = Tap(hold = 2, at = at(2400, 1080) { it.howToButton })
        val held = Tap(hold = 100, at = at(2400, 1080) { it.forward("SIGUIENTE") })
        render("como-se-juega-desde-inicio", 2400, 1080, Script(frames = 120, taps = mapOf(10 to open, 30 to next)), seen())
        render("como-se-juega-pulsado", 2400, 1080, Script(frames = 40, taps = mapOf(10 to open, 30 to held)), seen())
    }

    @Test
    fun theStartScreenWithAButtonPressed() {
        // The card of the scenario she played last, in its golden frame, pressed.
        val held = Tap(hold = 100, at = at(2400, 1080) { it.cards[0] })
        render("inicio-pulsado", 2400, 1080, Script(frames = 30, taps = mapOf(10 to held)), seen())
        render("inicio-320x180", 320 * 3, 180 * 3, Script(frames = 30), seen())
    }

    @Test
    fun theStartScreenAsksBeforeLeavingTheGame() {
        var left = false
        val exit = Tap(hold = 2, at = at(2400, 1080) { it.exit })
        val question = listOf(PanelLine(Texts.QUIT_QUESTION, size = 2))
        val answers = { layout: MenuLayout ->
            layout.row(
                listOf(Texts.YES, Texts.NO),
                panelBottom(question, layout.height) + MenuLayout.GAP,
                atLeast = MenuLayout.ANSWER_WIDTH,
            )
        }
        val no = Tap(hold = 2, at = at(2400, 1080) { answers(it)[1] })
        val yes = Tap(hold = 2, at = at(2400, 1080) { answers(it)[0] })
        render("inicio-salir", 2400, 1080, Script(frames = 40, taps = mapOf(10 to exit)), seen(), quit = { left = true })
        assertFalse(left, "it only asks")
        render("inicio-salir-no", 2400, 1080, Script(frames = 60, taps = mapOf(10 to exit, 30 to no)), seen(), quit = { left = true })
        assertFalse(left, "«NO» stays")
        render("inicio-salir-si", 2400, 1080, Script(frames = 60, taps = mapOf(10 to exit, 30 to yes)), seen(), quit = { left = true })
        assertTrue(left, "«SÍ» leaves")
    }

    @Test
    fun theRecordsOfEachScenario() {
        val store = seen(Scenario.Nieve)
        val records = Records(store)
        records.submit(Scenario.Nieve, meters = 1234, maxPoints = 56, day = 20261102)
        records.submit(Scenario.Nieve, meters = 321, maxPoints = 18, day = 20261101)
        records.submit(Scenario.Nieve, meters = 87, maxPoints = 12, day = 20261031)
        for ((i, meters) in listOf(2410, 977, 742, 615, 403, 120).withIndex()) {
            records.submit(Scenario.Bosque, meters = meters, maxPoints = 30 - i, day = 20261102 - i)
        }
        val open = mapOf(10 to Tap(hold = 2, at = at(2400, 1080) { it.recordsButton }))
        render("records", 2400, 1080, Script(frames = 40, taps = open), store)
        val names = Scenario.entries.map { Texts.scenario(it) }
        for (scenario in listOf(Scenario.Bosque, Scenario.Desierto)) {
            val tab = Tap(hold = 2, at = at(2400, 1080) { it.tabs(names)[scenario.ordinal] })
            render("records-${scenario.name.lowercase()}", 2400, 1080, Script(frames = 40, taps = open + mapOf(20 to tab)), store)
        }
    }

    @Test
    fun aRunAndItsTwoEnds() {
        val start = startIn(Scenario.Bosque)
        render("jugando", 2400, 1080, Script(frames = 150, taps = start), seen())
        render("fin", 2400, 1080, Script(frames = 60 * 9, taps = start), seen())
        render("depuracion", 2400, 1080, Script(frames = 150, taps = start, debug = true), seen())
        // With the debugging tools, two taps on the points take the 10 of the start away.
        val points = Offset(60f, 50f)
        val noPoints = start + mapOf(40 to Tap(hold = 2, at = points), 50 to Tap(hold = 2, at = points))
        render("fin-sin-puntos", 2400, 1080, Script(frames = 90, taps = noPoints, debug = true), seen())
    }

    @Test
    fun theTitleScreenOfTheScenarioPlayedLast() {
        for (scenario in Scenario.entries) {
            val name = "titulo-${scenario.name.lowercase()}"
            assertPixelGrid(render(name, 2400, 1080, Script(frames = 30), seen(scenario), pastTheTitle = false), name)
        }
        render("titulo-320x180", 320 * 3, 180 * 3, Script(frames = 30), seen(), pastTheTitle = false)
        render("titulo-ipad", 2752, 2064, Script(frames = 30), seen(Scenario.Nieve), pastTheTitle = false)
        // A moment after Caro reaches the first animal: her happy face and its «+1».
        val rescue = ((IntroShow.reachedAt(0) - IntroShow.START_MILLIS + RESCUE_SHOWN_MILLIS) * 60 / 1000).toInt()
        render("titulo-rescate", 2400, 1080, Script(frames = rescue + 1), seen(), pastTheTitle = false)
    }

    @Test
    fun theThreeScenarios() {
        for (scenario in Scenario.entries) {
            val name = scenario.name.lowercase()
            render("escenario-$name", 2400, 1080, Script(frames = 240, taps = startIn(scenario)), seen(scenario))
        }
    }
}
