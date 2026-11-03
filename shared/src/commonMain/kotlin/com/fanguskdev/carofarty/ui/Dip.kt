package com.fanguskdev.carofarty.ui

import kotlin.math.min

/**
 * The dip between screens (VFX-05; Fangusk approved it with the polish, GameMaster: «cortos, unos 150–250 ms a ciruela
 * oscura, sin destellos»): the screen goes to plum in [OUT] s, the change happens under it, and the new screen comes out
 * of it in [IN] s. While it goes to plum the screen that is going takes no taps; the new one takes them as it comes out.
 */
internal class Dip {
    private var change: (() -> Unit)? = null
    private var time = 0.0
    private var going = false

    /** Whether the screen is going to plum, with its change still to come: what is under it is not to be touched. */
    val closing: Boolean get() = change != null

    /** How much the plum covers the screen, from 0 to 1. */
    val shade: Float
        get() = when {
            !going -> 0f
            change != null -> (time / OUT).toFloat().coerceIn(0f, 1f)
            else -> (1.0 - (time - OUT) / IN).toFloat().coerceIn(0f, 1f)
        }

    /** The screen goes to plum and [change] happens under it; a dip already closing keeps its own. */
    fun start(change: () -> Unit) {
        if (closing) return
        this.change = change
        time = 0.0
        going = true
    }

    /** [seconds] later: the change, once the plum covers everything, and the end of the dip. */
    fun step(seconds: Double) {
        if (!going) return
        time += min(seconds, LONGEST_STEP)
        val pending = change
        if (pending != null && time >= OUT) {
            change = null
            pending()
        }
        if (change == null && time >= OUT + IN) going = false
    }

    companion object {
        const val OUT = 0.09
        const val IN = 0.14

        /** A frame that takes long does not skip the plum. */
        private const val LONGEST_STEP = 0.05
    }
}
