package com.fanguskdev.carofarty.audio

import kotlin.math.min

/**
 * The rescues in a row, which raise the sound of each rescue a step (GameMaster's rule for the sound, 2026-11-02):
 * perritos and gatitos together; it breaks and goes back to the start with an egg or a fish, with more than
 * [SECONDS] without a rescue, or at the end of the run; and at the top it stays at [TOP] while it goes on.
 */
internal class RescueStreak {
    /** How far up the next rescue's sound goes: 0 for the first of a row, up to [TOP]. */
    var step = 0
        private set
    private var last = Double.NEGATIVE_INFINITY

    /** A rescue at [seconds] of the run: the step of its sound. */
    fun rescued(seconds: Double): Int {
        step = if (seconds - last <= SECONDS) min(step + 1, TOP) else 0
        last = seconds
        return step
    }

    /** An egg, a fish or the end of the run. */
    fun broken() {
        step = 0
        last = Double.NEGATIVE_INFINITY
    }

    companion object {
        const val SECONDS = 2.0
        const val TOP = 5
    }
}
