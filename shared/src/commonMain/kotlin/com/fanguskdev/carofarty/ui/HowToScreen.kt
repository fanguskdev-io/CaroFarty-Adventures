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
import com.fanguskdev.carofarty.ui.render.GreyPalette
import com.fanguskdev.carofarty.ui.render.Icon
import com.fanguskdev.carofarty.ui.render.MenuLayout
import com.fanguskdev.carofarty.ui.render.PixelBuffer
import com.fanguskdev.carofarty.ui.render.PixelFont
import com.fanguskdev.carofarty.ui.render.PixelFont.drawText
import com.fanguskdev.carofarty.ui.render.paintButton
import com.fanguskdev.carofarty.ui.render.paintCaro
import com.fanguskdev.carofarty.ui.render.paintCaroSprite
import com.fanguskdev.carofarty.ui.render.paintIconButton
import com.fanguskdev.carofarty.ui.render.paintMiniScenery
import com.fanguskdev.carofarty.ui.render.paintShade
import com.fanguskdev.carofarty.ui.render.paintWorld
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A page of how to play (GDD-17, UI-12): its title and number, its small run playing in a frame, with the finger when it
 * is down and the points floating up from what she picks up, its two lines and the buttons to go through the pages. The
 * run is drawn into [buffer], like the world of a run.
 */
internal fun DrawScope.paintHowTo(session: GameSession, buffer: PixelBuffer) {
    val layout = session.layout
    paintShade(layout.width, layout.height)
    val title = Texts.HOW_TO_TITLE
    drawText(title, (layout.width - PixelFont.width(title, 2)) / 2, layout.back.top + 2, GreyPalette.record, GreyPalette.shadow, 2)
    paintIconButton(layout.back, Icon.Back)
    val number = "${session.howToPage + 1}/${TutorialScene.PAGES}"
    drawText(number, layout.corner.right - PixelFont.width(number), layout.back.top + 6, GreyPalette.text, GreyPalette.shadow)
    val buttons = session.howToButtons()
    val texts = session.howToButtonTexts()
    val lines = Texts.howTo(session.howToPage)
    val linesTop = buttons.first().top - MenuLayout.GAP - lines.size * PixelFont.LINE
    val top = layout.back.bottom + MenuLayout.GAP
    val tall = linesTop - MenuLayout.GAP - top
    val across = min(layout.width - 2 * SIDE, tall * WIDE / TALL)
    val box = Box((layout.width - across) / 2, top, across, tall)
    session.tutorial?.let { paintScene(it, box, session.art, buffer, session.millis) }
    var y = linesTop
    for ((i, line) in lines.withIndex()) {
        drawText(
            line,
            (layout.width - PixelFont.width(line)) / 2,
            y,
            if (i ==
                0
            ) {
                GreyPalette.record
            } else {
                GreyPalette.text
            },
            GreyPalette.shadow,
        )
        y += PixelFont.LINE
    }
    for (i in buttons.indices) paintButton(buttons[i], texts[i], lit = i == buttons.size - 1)
}

/** The small run of a page in its [box]: the scenery, the ground and what is on it, Caro, the finger and the points. */
private fun DrawScope.paintScene(
    scene: TutorialScene,
    box: Box,
    art: Art?,
    buffer: PixelBuffer,
    millis: Long,
) {
    drawRect(GreyPalette.ink, Offset(box.left - 1f, box.top - 1f), Size(box.width + 2f, box.height + 2f))
    val run = scene.run
    val caro = run.caro
    val cameraLeft = floor(caro.x).toLong() - CARO_X
    val feetY = caro.y.roundToInt()
    val cameraBottom = cameraBottom(feetY, box.height)
    val palette = GreyPalette.of(scene.scenario)
    clipRect(box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat()) {
        val ground = box.bottom - GROUND + cameraBottom
        paintMiniScenery(box, art?.scenario(scene.scenario), palette, ground, millis, withGround = false)
        val image = buffer.paint(box.width, box.height) {
            paintWorld(run, cameraLeft, cameraBottom, box.width, box.height, palette, art, millis, false, caro.x, caro.y)
        }
        drawImage(
            image,
            IntOffset.Zero,
            IntSize(box.width, box.height),
            IntOffset(box.left, box.top),
            IntSize(box.width, box.height),
            filterQuality = FilterQuality.None,
        )
        val feet = box.top + box.height + cameraBottom - feetY
        val caroArt = art?.caro
        if (caroArt != null) {
            paintCaroSprite(caroArt, scene.scenario, scene.look, run.reaction, box.left + CARO_X, feet)
        } else {
            paintCaro(box.left + CARO_X, feet, caro.state, stride = (caro.meters * 4).toInt() % 2 == 0)
        }
        for (floating in scene.floating) {
            val x = box.left + (floating.x - cameraLeft).roundToInt() - PixelFont.width(floating.text) / 2
            val y = box.top + box.height + cameraBottom - floating.y.roundToInt() - (floating.age * RISE).roundToInt()
            drawText(floating.text, x, y, if (floating.good) GreyPalette.good else GreyPalette.bad, GreyPalette.shadow)
        }
        if (scene.page == TutorialScene.JUMPING || scene.page == TutorialScene.AVOIDING) paintFinger(box, scene.held)
    }
}

/** The finger in a corner of the page: down when Caro's jump is held, so the length of the jump can be seen. */
private fun DrawScope.paintFinger(box: Box, down: Boolean) {
    val x = box.right - FINGER - MARGIN
    val y = box.top + MARGIN
    drawRect(GreyPalette.panel, Offset(x.toFloat(), y.toFloat()), Size(FINGER.toFloat(), FINGER.toFloat()))
    val inner = if (down) FINGER - 4 else FINGER - 8
    val offset = (FINGER - inner) / 2
    drawRect(
        if (down) GreyPalette.record else GreyPalette.text,
        Offset((x + offset).toFloat(), (y + offset).toFloat()),
        Size(inner.toFloat(), inner.toFloat()),
    )
    val word = if (down) "MANTÉN" else "TOCA"
    drawText(word, x - MARGIN - PixelFont.width(word), y + (FINGER - PixelFont.HEIGHT) / 2, GreyPalette.text, GreyPalette.shadow)
}

private const val SIDE = 24
private const val WIDE = 26
private const val TALL = 10
private const val CARO_X = 48
private const val GROUND = 32
private const val RISE = 30f
private const val FINGER = 14
private const val MARGIN = 4
