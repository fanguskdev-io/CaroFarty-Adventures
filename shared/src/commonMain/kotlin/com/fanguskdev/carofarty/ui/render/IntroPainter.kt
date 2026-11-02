package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import com.fanguskdev.carofarty.game.CaroState
import com.fanguskdev.carofarty.game.Reaction
import com.fanguskdev.carofarty.game.Scenario
import com.fanguskdev.carofarty.game.TILE
import com.fanguskdev.carofarty.ui.render.PixelFont.drawText

/**
 * What happens in the scenario of the title screen (UI-07), all of it from the time alone, so it costs nothing to keep
 * and plays the same on every phone: the ground goes by at [SPEED] with Caro running on it, as in the cards, and every
 * [EVERY_MILLIS] she reaches an animal, by turns a gatito sitting and a perrito strolling her way, slower than she runs.
 * She rescues it as in a run: it goes, her face is happy for a moment and its «+1» floats up. Each animal comes in at
 * the right just in time to be reached on its turn, so they never meet on the way.
 */
internal object IntroShow {
    /** How fast the ground goes by, in pixels a second: the pace of the cards, a slow parallax. */
    const val SPEED = 60L

    /** How fast a perrito strolls, as in a run (ADR 0008). */
    const val WALK = 24L
    const val EVERY_MILLIS = 5000L

    /** From Caro's middle to the animal's when she reaches it: about where their boxes touch in a run. */
    const val REACH = 12

    /** How long her happy face and the «+1» last after a rescue, as in a run and in how to play. */
    const val HAPPY_MILLIS = 400L
    const val FLOAT_MILLIS = 700L

    /** On the first frame, the first animals are already on their way. */
    const val START_MILLIS = 3000L

    /** A perrito every other animal, a gatito the rest. */
    fun isPerrito(k: Long): Boolean = k % 2 == 1L

    /** When Caro reaches animal [k], in the show's milliseconds. */
    fun reachedAt(k: Long): Long = (k + 1) * EVERY_MILLIS

    /** Where the middle of animal [k] is on the screen at [show] milliseconds, with Caro at [caroX]. */
    fun x(k: Long, show: Long, caroX: Int): Int {
        val approach = if (isPerrito(k)) SPEED - WALK else SPEED
        return caroX + REACH + ((reachedAt(k) - show) * approach / SECOND).toInt()
    }

    /** The next animal Caro reaches after [show]. */
    fun next(show: Long): Long = show / EVERY_MILLIS

    /** How many animals at most can be on their way at once, on the widest screen: from the next one on, those to look at. */
    const val AT_ONCE = 3

    /** How long ago Caro rescued the last animal, at [show]; -1 before the first. */
    fun sinceRescue(show: Long): Long = if (show < EVERY_MILLIS) -1 else show.mod(EVERY_MILLIS)

    const val SECOND = 1000L
}

/**
 * The title screen (UI-07), the animated title Fangusk chose, as GameMaster described it: the title over the sky of the
 * menus with the clouds of the kit drifting; along the bottom, the scenario she played last ([scenario]) going by slowly
 * with Caro running in it in its clothes and the animals of [IntroShow]; and in the middle [prompt], «TOCA PARA EMPEZAR»,
 * blinking. Without the interface's art, over the dark veil of the prototype's menus, and Caro in grey.
 */
internal fun DrawScope.paintIntro(
    layout: MenuLayout,
    art: Art?,
    scenario: Scenario,
    prompt: String,
    millis: Long,
) {
    val ui = art?.ui
    if (ui == null) paintShade(layout.width, layout.height) else paintMenuSky(layout.width, layout.height, ui, driftMillis = millis)
    paintIntroScene(layout, art, scenario, millis)
    paintTitleImage(layout.title, ui)
    if (millis % BLINK_MILLIS < SHOWN_MILLIS) {
        val box = layout.prompt(prompt)
        val text = ui?.colors?.text ?: GreyPalette.text
        val shadow = ui?.colors?.shadow ?: GreyPalette.shadow
        drawText(prompt, box.left, box.top, text, shadow, MenuLayout.PROMPT_SIZE)
    }
}

/** The scenario along the bottom of the title screen, its layers at their own pace, with Caro and the animals on its ground. */
private fun DrawScope.paintIntroScene(
    layout: MenuLayout,
    art: Art?,
    scenario: Scenario,
    millis: Long,
) {
    val box = layout.introScene
    val ground = layout.introGround
    val caroX = layout.introCaroX
    val show = millis + IntroShow.START_MILLIS
    val travelled = show * IntroShow.SPEED / IntroShow.SECOND
    clipRect(box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat()) {
        paintMiniScenery(box, art?.scenario(scenario), GreyPalette.of(scenario), ground, travelled.toDouble(), withGround = true)
        val items = art?.items
        val next = IntroShow.next(show)
        if (items != null) {
            for (k in next until next + IntroShow.AT_ONCE) {
                val x = IntroShow.x(k, show, caroX)
                if (x - ANIMAL_HALF < box.right) paintAnimal(items, k, x, ground, millis)
            }
        }
        val caro = art?.caro
        if (caro != null) {
            val reaction = if (IntroShow.sinceRescue(show) in 0 until IntroShow.HAPPY_MILLIS) Reaction.Happy else Reaction.None
            val animation = caro.animation(CaroPose.Corre, reaction)
            val cycle = animation.cycleTiles
            val frame = if (cycle != null && cycle > 0f) {
                animation.frame((travelled * animation.size / (cycle * TILE)).toInt())
            } else {
                animation.frameAt(millis, loops = true)
            }
            drawFrame(caro.sheet(scenario), frame, caroX - caro.anchorX, ground - caro.anchorY)
        } else {
            paintCaro(caroX, ground, CaroState.Running, stride = travelled / STRIDE % 2 == 0L)
        }
    }
    // The «+1» of the last rescue, floating up over her, a little ahead, as in how to play; out of the scenario if it rises
    // that far.
    val since = IntroShow.sinceRescue(show)
    if (since in 0 until IntroShow.FLOAT_MILLIS) {
        val top = ground - CARO_HEIGHT - FLOAT_UP - (since * RISE / IntroShow.SECOND).toInt()
        val digits = art?.ui?.digits
        if (digits != null) {
            drawDigits(digits, RESCUE, adding = true, x = caroX + FLOAT_AHEAD - digitsWidth(digits, RESCUE) / 2, y = top)
        } else {
            drawText(RESCUE, caroX + FLOAT_AHEAD - PixelFont.width(RESCUE) / 2, top, GreyPalette.good, GreyPalette.shadow)
        }
    }
}

/** Animal [k] of the show standing on [ground] with its middle at [x]: a gatito sitting or a perrito walking her way. */
private fun DrawScope.paintAnimal(
    items: ItemArt,
    k: Long,
    x: Int,
    ground: Int,
    millis: Long,
) {
    val turn = (k / 2).toInt()
    if (IntroShow.isPerrito(k)) {
        if (items.breeds.isEmpty()) return
        val breed = items.breeds[turn % items.breeds.size]
        val frame = breed.walk.frameAt(millis, loops = true)
        drawFrame(breed.sheet, frame, x - items.anchorX, ground - (breed.sheet.height - breed.walk.emptyBelow))
    } else {
        val animation = items.gatito ?: return
        val sheet = items.coats[turn % items.coats.size]
        drawFrame(sheet, animation.frameAt(millis, loops = true), x - items.anchorX, ground - (sheet.height - animation.emptyBelow))
    }
}

/** «TOCA PARA EMPEZAR» shows this long of every round of its blinking. */
private const val BLINK_MILLIS = 1200L
private const val SHOWN_MILLIS = 800L

/** Half an animal's drawing: one this far past the right edge has not come in yet. */
private const val ANIMAL_HALF = 8

/** The «+1» of a rescue: how far over her head it starts, how far ahead of her middle, and how fast it rises. */
private const val RESCUE = "+1"
private const val FLOAT_UP = 4
private const val FLOAT_AHEAD = 8
private const val RISE = 30L

/** The prototype's Caro changes stride every this many pixels. */
private const val STRIDE = 8L
