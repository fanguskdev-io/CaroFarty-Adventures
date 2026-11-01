package com.fanguskdev.carofarty.engine

/**
 * The only control of the game (ADR 0002): a finger on the screen, anywhere. The screen reports when the first finger
 * goes down and when the last one goes up; the simulation reads it once per step, so a tap is never lost between two
 * steps and a second finger changes nothing (PLY-10).
 */
class TouchInput {
    /** Whether a finger is on the screen right now. */
    var isHeld = false
        private set
    private var pressed = false

    fun press() {
        if (isHeld) return
        isHeld = true
        pressed = true
    }

    fun release() {
        isHeld = false
    }

    /** Whether a finger went down since the last call: true once per tap. */
    fun consumePress(): Boolean {
        val wasPressed = pressed
        pressed = false
        return wasPressed
    }

    fun clear() {
        isHeld = false
        pressed = false
    }
}
