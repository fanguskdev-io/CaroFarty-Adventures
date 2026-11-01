package com.fanguskdev.carofarty.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import carofarty.shared.generated.resources.Res
import com.fanguskdev.carofarty.engine.PixelViewport
import com.fanguskdev.carofarty.game.GameConfig
import com.fanguskdev.carofarty.game.GameRun
import com.fanguskdev.carofarty.game.RunPhase
import com.fanguskdev.carofarty.storage.KeyValueStore
import com.fanguskdev.carofarty.storage.Records
import com.fanguskdev.carofarty.ui.render.CARO_HEIGHT
import com.fanguskdev.carofarty.ui.render.GreyPalette
import com.fanguskdev.carofarty.ui.render.PixelBuffer
import com.fanguskdev.carofarty.ui.render.SafeInsets
import com.fanguskdev.carofarty.ui.render.paintBackground
import com.fanguskdev.carofarty.ui.render.paintCaro
import com.fanguskdev.carofarty.ui.render.paintDebug
import com.fanguskdev.carofarty.ui.render.paintHud
import com.fanguskdev.carofarty.ui.render.paintPanel
import com.fanguskdev.carofarty.ui.render.paintWorld
import kotlinx.coroutines.isActive
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.TimeSource

/**
 * The game: the settings of `files/ajustes.json` (TEC-07), a run, the loop of a frame (TEC-05), the finger (PLY-10) and the
 * drawing (TEC-04): the world in a low resolution buffer scaled by an integer, shifted by the fraction of the camera so it
 * scrolls smoothly, and Caro, the marker and the panels on top, on the same pixel grid but without the shift. The camera
 * (PLY-13) keeps Caro [CARO_SCREEN_X] pixels from the left edge, and goes up only when her head would come closer than
 * [HEAD_ROOM] to the top: a dead zone as tall as the screen, so it never moves while she runs on the ground.
 */
@Composable
internal fun GameScreen(
    store: KeyValueStore,
    debugBuild: Boolean,
    debugAtStart: Boolean = false,
    autopilot: Boolean = false,
) {
    val openedAt = remember { TimeSource.Monotonic.markNow() }
    val config by produceState<GameConfig?>(null) { value = GameConfig.parse(Res.readBytes(SETTINGS).decodeToString()) }
    val loaded = config
    if (loaded == null) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }
    val scope = rememberCoroutineScope()
    val records = remember(store) { Records(store) }
    val session = remember(loaded) { GameSession(loaded, scope, records, debugBuild, debugAtStart, autopilot, openedAt) }
    val world = remember { PixelBuffer() }
    val safe = WindowInsets.safeDrawing

    LaunchedEffect(session) {
        while (isActive) withFrameNanos { session.onFrame(it) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { session.pause() }
    PlatformBackHandler(enabled = session.running) { session.pause() }

    Canvas(
        Modifier.fillMaxSize().pointerInput(session) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val down = event.changes.count { it.pressed }
                    for (change in event.changes) {
                        if (change.changedToDown()) session.fingerDown(change.position.x, change.position.y, first = down == 1)
                    }
                    if (down == 0) session.fingersUp()
                }
            }
        },
    ) {
        session.frame.longValue
        val viewport = PixelViewport.fit(size.width.toInt(), size.height.toInt())
        session.viewport = viewport
        session.insets =
            SafeInsets(
                left = ceil(safe.getLeft(this, layoutDirection).toFloat() / viewport.scale).toInt(),
                top = ceil(safe.getTop(this).toFloat() / viewport.scale).toInt(),
                right = ceil(safe.getRight(this, layoutDirection).toFloat() / viewport.scale).toInt(),
            )
        val drawing = TimeSource.Monotonic.markNow()
        drawGame(session, world, viewport)
        session.drawn(drawing.elapsedNow())
    }
}

private fun DrawScope.drawGame(session: GameSession, world: PixelBuffer, viewport: PixelViewport) {
    val run = session.run
    val caro = run.caro
    val x = caro.previousX + (caro.x - caro.previousX) * session.alpha
    val y = caro.previousY + (caro.y - caro.previousY) * session.alpha
    val camera = x - CARO_SCREEN_X
    val cameraLeft = floor(camera).toLong()
    val fraction = camera - cameraLeft
    // Both rounded the same way, so while the camera goes up with her she stays still on the screen and nothing shakes.
    val feetY = y.roundToInt()
    val cameraBottom = cameraBottom(feetY, viewport.height)
    val width = viewport.width + 1
    val image =
        world.paint(width, viewport.height) {
            paintWorld(run, cameraLeft, cameraBottom, width, viewport.height, GreyPalette.of(run.scenario), session.showDebug, x, y)
        }

    drawRect(Color.Black)
    val left = viewport.offsetX.toFloat()
    val top = viewport.offsetY.toFloat()
    val scale = viewport.scale.toFloat()
    clipRect(left, top, left + viewport.width * scale, top + viewport.height * scale) {
        paintBackground(camera, REST_CAMERA, cameraBottom, viewport, GreyPalette.of(run.scenario))
        val shift = (fraction * viewport.scale).roundToInt()
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(width, viewport.height),
            dstOffset = IntOffset(viewport.offsetX - shift, viewport.offsetY),
            dstSize = IntSize(width * viewport.scale, viewport.height * viewport.scale),
            filterQuality = FilterQuality.None,
        )
        withTransform({
            translate(left, top)
            scale(scale, scale, Offset.Zero)
        }) {
            paintCaro(
                CARO_SCREEN_X,
                viewport.height + cameraBottom - feetY,
                caro.state,
                stride =
                (caro.meters * STRIDES_PER_METER).toInt() % 2 == 0,
            )
            if (run.phase != RunPhase.Ready) {
                paintHud(session.pointsText, session.metersText, viewport.width, session.insets, warn = session.lowPoints)
            }
            if (session.panel.isNotEmpty()) paintPanel(session.panel, viewport.width, viewport.height)
            if (session.showDebug) paintDebug(session.debugLines, viewport.height, session.insets)
        }
    }
}

/** The world y at the bottom of a screen [height] pixels tall with Caro's feet at [feetY] (PLY-13). */
internal fun cameraBottom(feetY: Int, height: Int): Int = max(0, feetY + CARO_HEIGHT + HEAD_ROOM - height)

/** Where Caro runs, in game pixels from the left edge (ADR 0003). */
private const val CARO_SCREEN_X = 96

/** The camera before the first tap, with Caro at the start: where the far band sits on the game's pixel grid. */
private const val REST_CAMERA = GameRun.START_X - CARO_SCREEN_X

/** The game pixels the camera leaves above Caro's head before going up. */
internal const val HEAD_ROOM = 8
private const val STRIDES_PER_METER = 4
private const val SETTINGS = "files/ajustes.json"
