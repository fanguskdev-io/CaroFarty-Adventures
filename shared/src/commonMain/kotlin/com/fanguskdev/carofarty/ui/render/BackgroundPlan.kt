package com.fanguskdev.carofarty.ui.render

import com.fanguskdev.carofarty.engine.PixelViewport
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.random.Random

/**
 * A layer of the background for the [BackgroundPlan]: how fast it moves with the camera, how wide its strip is and, for
 * each of its variants, the columns from where it starts to differ from the base to where it stops ([LayerVariant]).
 */
internal class PlanLayer(val speed: Double, val width: Int, val variants: List<IntRange>)

/**
 * When the far and the middle layer show one of their variants instead of their base. Fangusk, of the cabin of the snow:
 * «intercala aleatoriamente entre cercana y lejana». So: one at a time, never in two layers at once, in one or the other at
 * random, the next one [MIN_SECONDS] to [MAX_SECONDS] after the last one came in and never before it has gone. A variant
 * takes a whole round of its layer's strip, a lap, and the plan is made with the run's [seed] as the camera goes forward,
 * so the same run shows the same. [speedAt] says how fast the camera goes where it is, in pixels a second, and
 * [restCamera] is the camera at the start, as the layers are drawn (paintScenery).
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
        val gap = MIN_SECONDS + random.nextDouble() * (MAX_SECONDS - MIN_SECONDS)
        val start = max(lastEnter + gap * speedAt(lastEnter), lastExit + MARGIN_SECONDS * speedAt(lastExit))
        val layer = choices[random.nextInt(choices.size)]
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

    private companion object {
        /**
         * The time from one variant coming in to planning the next. A variant waits for the next round of its strip, up to
         * some 3 s more, so they come every 8 to 15 seconds, as GameMaster proposed.
         */
        const val MIN_SECONDS = 8.0
        const val MAX_SECONDS = 12.0

        /** At least this long between one variant going and the next coming in. */
        const val MARGIN_SECONDS = 1.0

        /** The widest screen, in game pixels: a variant is on the screen from its first column reaching its right edge. */
        const val SCREEN = PixelViewport.MAX_WIDTH.toDouble()

        /** How far ahead of the camera it plans. */
        const val HORIZON = 4096.0
    }
}
