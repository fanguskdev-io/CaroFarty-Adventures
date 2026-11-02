package com.fanguskdev.carofarty.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.fanguskdev.carofarty.ui.render.Art
import com.fanguskdev.carofarty.ui.render.Box
import com.fanguskdev.carofarty.ui.render.CARO_HEIGHT
import com.fanguskdev.carofarty.ui.render.GreyPalette
import com.fanguskdev.carofarty.ui.render.Icon
import com.fanguskdev.carofarty.ui.render.MenuLayout
import com.fanguskdev.carofarty.ui.render.PixelBuffer
import com.fanguskdev.carofarty.ui.render.PixelFont
import com.fanguskdev.carofarty.ui.render.PixelFont.drawText
import com.fanguskdev.carofarty.ui.render.UiArt
import com.fanguskdev.carofarty.ui.render.digitsWidth
import com.fanguskdev.carofarty.ui.render.drawDigits
import com.fanguskdev.carofarty.ui.render.drawFrame
import com.fanguskdev.carofarty.ui.render.drawPiece
import com.fanguskdev.carofarty.ui.render.drawTextAround
import com.fanguskdev.carofarty.ui.render.drawUiSprite
import com.fanguskdev.carofarty.ui.render.paintButton
import com.fanguskdev.carofarty.ui.render.paintCaro
import com.fanguskdev.carofarty.ui.render.paintCaroSprite
import com.fanguskdev.carofarty.ui.render.paintIconButton
import com.fanguskdev.carofarty.ui.render.paintMenuBackground
import com.fanguskdev.carofarty.ui.render.paintMiniScenery
import com.fanguskdev.carofarty.ui.render.paintPanelBox
import com.fanguskdev.carofarty.ui.render.paintRibbon
import com.fanguskdev.carofarty.ui.render.paintWorld
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A page of how to play (GDD-17, UI-12), as GameMaster's mockup: the way back, the ribbon and the button forward on top;
 * in the panel, two scenes in their frames, each a small run playing with the finger when it is down and the points
 * floating up from what she picks up, and their two lines under them; and the page dots under the panel. The runs are
 * drawn into [buffer], like the world of a run.
 */
internal fun DrawScope.paintHowTo(session: GameSession, buffer: PixelBuffer) {
    val layout = session.layout
    val art = session.art
    val ui = art?.ui
    val pressed = session.pressed
    paintMenuBackground(ui, layout.width, layout.height)
    paintIconButton(layout.back, Icon.Back, ui, pressed = layout.back == pressed)
    paintRibbon(layout.ribbon(Texts.HOW_TO_TITLE), Texts.HOW_TO_TITLE, ui)
    val forward = session.forwardText()
    val forwardBox = layout.forward(forward)
    paintButton(forwardBox, forward, ui, pressed = forwardBox == pressed, lit = true)
    val panel = layout.howToPanel
    paintPanelBox(panel, ui)
    val text = ui?.colors?.text ?: GreyPalette.text
    val shadow = ui?.colors?.shadow ?: GreyPalette.shadow
    val marco = ui?.piece("marco")
    for ((i, scene) in session.tutorial.withIndex()) {
        val frame = layout.frames[i]
        if (marco == null) drawRect(GreyPalette.ink, Offset(frame.left - 1f, frame.top - 1f), Size(frame.width + 2f, frame.height + 2f))
        paintScene(scene, marco?.inside(frame) ?: frame, art, buffer, session.millis, session.alpha)
        if (marco != null) drawPiece(marco, frame)
        for ((j, line) in Texts.howTo(scene.index).withIndex()) {
            drawTextAround(line, frame.centreX, frame.bottom + MenuLayout.CAPTION_GAP + j * PixelFont.LINE, text, shadow)
        }
    }
    paintPageDots(session.howToPage, panel, ui)
}

/** The dots under the panel, one for each page, the one that shows lit. */
private fun DrawScope.paintPageDots(page: Int, panel: Box, ui: UiArt?) {
    val top = panel.bottom + MenuLayout.DOTS_GAP
    for (i in 0 until TutorialScene.PAGES) {
        val x = panel.centreX - DOTS_LEFT + i * DOTS_APART
        val dots = ui?.dots
        if (dots != null) {
            drawUiSprite(dots, if (i == page) "activo" else "inactivo", x, top)
        } else {
            val color = if (i == page) GreyPalette.text else GreyPalette.panel
            drawRect(color, Offset(x + 1f, top + 1f), Size(DOT - 2f, DOT - 2f))
        }
    }
}

/**
 * A small run in its [box]: the scenery, the ground and what is on it, Caro, the points floating up and, in the scene of
 * jumping, the finger. The camera follows her, with the ground low in the box and her head free to leave it a little at
 * the top of a long jump before it goes up with her.
 */
private fun DrawScope.paintScene(
    scene: TutorialScene,
    box: Box,
    art: Art?,
    buffer: PixelBuffer,
    millis: Long,
    alpha: Float,
) {
    val run = scene.run
    val caro = run.caro
    val x = caro.previousX + (caro.x - caro.previousX) * alpha
    val y = caro.previousY + (caro.y - caro.previousY) * alpha
    val cameraLeft = floor(x).toLong() - box.width / CARO_PART
    val feetY = y.roundToInt()
    val cameraBottom = max(TutorialScene.TOP - scene.groundShown, feetY + CARO_HEIGHT - SCENE_HEAD_OUT - box.height)
    val palette = GreyPalette.of(scene.scenario)
    clipRect(box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat()) {
        val ground = box.bottom - TutorialScene.TOP + cameraBottom
        paintMiniScenery(box, art?.scenario(scene.scenario), palette, ground, cameraLeft.toDouble(), withGround = false)
        val image = buffer.paint(box.width, box.height) {
            paintWorld(run, cameraLeft, cameraBottom, box.width, box.height, palette, art, millis, false, x, y, alpha)
        }
        drawImage(
            image,
            IntOffset.Zero,
            IntSize(box.width, box.height),
            IntOffset(box.left, box.top),
            IntSize(box.width, box.height),
            filterQuality = FilterQuality.None,
        )
        val feet = box.bottom + cameraBottom - feetY
        val caroX = box.left + box.width / CARO_PART
        // Blinking after an egg or a fish, as in a run.
        val shown = run.blinking <= 0f || (run.blinking * BLINKS_PER_SECOND).toInt() % 2 == 0
        val caroArt = art?.caro
        if (shown && caroArt != null) {
            paintCaroSprite(caroArt, scene.scenario, scene.look, run.reaction, caroX, feet)
        } else if (shown) {
            paintCaro(caroX, feet, caro.state, stride = (caro.meters * STRIDES_PER_METER).toInt() % 2 == 0)
        }
        val digits = art?.ui?.digits
        // The numbers go up over her, a little ahead, so they stay in the small scene while she runs on.
        for (floating in scene.floating) {
            val centre = caroX + FLOAT_AHEAD
            val top = box.bottom + cameraBottom - floating.y.roundToInt() - (floating.age * RISE).roundToInt() - FLOAT_UP
            if (digits != null) {
                drawDigits(digits, floating.text, floating.good, centre - digitsWidth(digits, floating.text) / 2, top)
            } else {
                val color = if (floating.good) GreyPalette.good else GreyPalette.bad
                drawText(floating.text, centre - PixelFont.width(floating.text) / 2, top, color, GreyPalette.shadow)
            }
        }
        val ui = art?.ui
        if (scene.index == TutorialScene.JUMPING) paintFinger(box, scene, ui, millis)
        if (scene.index == TutorialScene.ENDING) {
            // The other way a run ends: the points at 0, in the warning colour.
            val warning = ui?.colors?.warning ?: GreyPalette.bad
            drawText(NO_POINTS, box.right - NO_POINTS_RIGHT, box.top + NO_POINTS_DOWN, warning, ui?.colors?.shadow ?: GreyPalette.shadow, 2)
        }
    }
}

/**
 * The finger in the bottom right corner of the scene of jumping: up while she runs, tapping when the jump starts and
 * holding while the jump is held, so the length of the jump can be seen.
 */
private fun DrawScope.paintFinger(
    box: Box,
    scene: TutorialScene,
    ui: UiArt?,
    millis: Long,
) {
    val x = box.right - FINGER_FROM_EDGE
    val y = box.bottom - FINGER_FROM_EDGE
    val icons = ui?.icons
    if (icons != null) {
        val holding = scene.heldFor > TutorialScene.TAP_SECONDS
        val animation = icons.animation(if (holding) "dedo-mantiene" else "dedo-toca") ?: return
        val frame = when {
            holding -> animation.frameAt(millis, loops = true)
            scene.held || scene.touching > 0f -> animation.frame(1)
            else -> animation.frame(0)
        }
        drawFrame(icons.sheet, frame, x, y)
        return
    }
    val down = scene.held || scene.touching > 0f
    drawRect(GreyPalette.panel, Offset(x.toFloat(), y.toFloat()), Size(FINGER.toFloat(), FINGER.toFloat()))
    val inner = if (down) FINGER - 4 else FINGER - 8
    val offset = (FINGER - inner) / 2
    drawRect(
        if (down) GreyPalette.record else GreyPalette.text,
        Offset((x + offset).toFloat(), (y + offset).toFloat()),
        Size(inner.toFloat(), inner.toFloat()),
    )
}

/** Caro in the scenes: a third of the way in, so what comes can be seen. */
private const val CARO_PART = 3

/** How far her head may leave the top of a scene in a long jump before the camera goes up with her. */
private const val SCENE_HEAD_OUT = 10
private const val RISE = 30f

/** The numbers start a little over her head, and a little ahead of her middle. */
private const val FLOAT_UP = 4
private const val FLOAT_AHEAD = 8
private const val FINGER = 16
private const val FINGER_FROM_EDGE = 22
private const val BLINKS_PER_SECOND = 12
private const val STRIDES_PER_METER = 4
private const val NO_POINTS = "0"
private const val NO_POINTS_RIGHT = 14
private const val NO_POINTS_DOWN = 6

/** The page dots (GameMaster's mockup): 7 pixels, 11 from one to the next, the pair centred under the panel. */
private const val DOT = 7
private const val DOTS_APART = 11
private const val DOTS_LEFT = 9
