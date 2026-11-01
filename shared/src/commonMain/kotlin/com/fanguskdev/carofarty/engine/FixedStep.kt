package com.fanguskdev.carofarty.engine

/**
 * The fixed step of the simulation (TEC-05): the world always advances in steps of [step] seconds, 60 per second, so the
 * same taps give the same game on a 60 Hz phone and on a 120 Hz one. What is left between two steps is returned as a
 * fraction for interpolating the drawing.
 */
class FixedStep(val step: Double = 1.0 / 60, private val maxSteps: Int = 5) {
    private var accumulator = 0.0

    /**
     * Runs [update] once for every whole step that fits in [elapsed] seconds plus what was left before, at most [maxSteps]
     * times (after a long pause the game does not try to catch up), and returns how far the next step is, from 0 to 1.
     */
    fun advance(elapsed: Double, update: () -> Unit): Float {
        accumulator += elapsed
        var steps = 0
        while (accumulator + EPSILON >= step && steps < maxSteps) {
            update()
            accumulator -= step
            steps++
        }
        // After a long pause, what did not fit is dropped instead of being caught up later.
        if (steps == maxSteps && accumulator >= step) accumulator %= step
        if (accumulator < 0.0) accumulator = 0.0
        return (accumulator / step).toFloat()
    }

    fun reset() {
        accumulator = 0.0
    }

    private companion object {
        /** What the sums of a step's decimals can be off by: a frame of exactly one step is one step. */
        const val EPSILON = 1e-9
    }
}
