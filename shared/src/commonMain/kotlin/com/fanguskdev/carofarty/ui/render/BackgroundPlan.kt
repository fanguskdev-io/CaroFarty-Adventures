package com.fanguskdev.carofarty.ui.render

import com.fanguskdev.carofarty.engine.PixelViewport
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random

/**
 * A layer of the background for the [BackgroundPlan]: how fast it moves with the camera, how wide its strip is and, for
 * each of its variants, the columns from where it starts to differ from the base to where it stops ([LayerVariant]).
 */
internal class PlanLayer(val speed: Double, val width: Int, val variants: List<IntRange>)

/**
 * When the far and the middle layer show one of their variants instead of their base. Fangusk, of the cabin of the snow:
 * «intercala aleatoriamente entre cercana y lejana». The rule GameMaster's agent proposed with the art: one at a time; the
 * first [FIRST_MIN] to [FIRST_MAX] seconds after the start; then, once one has left the screen, a pause of [PAUSE_MIN] to
 * [PAUSE_MAX] seconds; near or far at random, never three times running in the same layer. A variant takes a whole round
 * of its layer's strip, a lap, the next one coming in on the right, so it never appears at once. The plan is made with the
 * run's [seed] as the camera goes forward, so the same run shows the same. [speedAt] says how fast the camera goes where
 * it is, in pixels a second, and [restCamera] is the camera at the start, as the layers are drawn (paintScenery).
 */
internal class BackgroundPlan(
    seed: Long,
    private val layers: List<PlanLayer>,
    private val restCamera: Double,
    private val speedAt: (Double) -> Double,
) {
    private class Planned(
        val layer: Int,
        val lap: Long,
        val variant: Int,
        val enter: Double,
        val exit: Double,
    )

    private val random = Random(seed)
    private val planned = ArrayDeque<Planned>()
    private val choices = layers.indices.filter { layers[it].variants.isNotEmpty() }
    private var lastEnter = restCamera
    private var lastExit = restCamera
    private var lastLayer = -1
    private var sameLayer = 0

    /** Plans far enough ahead of [camera] and forgets what has gone behind it. */
    fun reach(camera: Double) {
        if (choices.isEmpty()) return
        while (planned.isNotEmpty() && planned.first().exit < camera) planned.removeFirst()
        while (lastEnter < camera + HORIZON) planNext()
    }

    /** The variant that the lap [lap] of the layer [layer] shows, or -1 for its base. */
    fun variant(layer: Int, lap: Long): Int {
        for (i in 0 until planned.size) {
            val it = planned[i]
            if (it.layer == layer && it.lap == lap) return it.variant
        }
        return -1
    }

    /** The windows of the camera in which each planned variant is on the screen, for the tests. */
    fun windows(): List<ClosedFloatingPointRange<Double>> = planned.map { it.enter..it.exit }

    private fun planNext() {
        val first = lastLayer < 0
        val pause = if (first) between(FIRST_MIN, FIRST_MAX) else between(PAUSE_MIN, PAUSE_MAX)
        val start = lastExit + pause * speedAt(lastExit)
        var layer = choices[random.nextInt(choices.size)]
        if (layer == lastLayer && sameLayer >= 2 && choices.size > 1) layer = choices.first { it != lastLayer }
        sameLayer = if (layer == lastLayer) sameLayer + 1 else 1
        lastLayer = layer
        val variants = layers[layer].variants
        val variant = random.nextInt(variants.size)
        val speed = layers[layer].speed
        val width = layers[layer].width
        val range = variants[variant]
        // Where the layer starts at rest, as paintScenery puts it, and the first lap whose variant comes in after start.
        val origin = floor(restCamera * speed)
        val lap = ceil(((start - restCamera) * speed + origin + SCREEN - range.first) / width).toLong()
        val enter = (lap * width + range.first - SCREEN - origin) / speed + restCamera
        val exit = (lap * width + range.last + 1 - origin) / speed + restCamera
        planned.addLast(Planned(layer, lap, variant, enter, exit))
        lastEnter = enter
        lastExit = exit
    }

    private fun between(low: Double, high: Double): Double = low + random.nextDouble() * (high - low)

    private companion object {
        /** When the first comes in, in seconds from the start, and the pause from one leaving to the next coming in. */
        const val FIRST_MIN = 3.0
        const val FIRST_MAX = 8.0
        const val PAUSE_MIN = 8.0
        const val PAUSE_MAX = 16.0

        /** The widest screen, in game pixels: a variant is on the screen from its first column reaching its right edge. */
        const val SCREEN = PixelViewport.MAX_WIDTH.toDouble()

        /** How far ahead of the camera it plans. */
        const val HORIZON = 4096.0
    }
}
