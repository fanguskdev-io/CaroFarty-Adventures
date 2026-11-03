package com.fanguskdev.carofarty.audio

/**
 * Sharing the sound with the rest of the phone (QA-07; Fangusk: «Okey adelante»): while the game's music sounds, the game
 * holds the phone's sound, so another app's music stops and a call that rings pauses the run; with its music off, it lets
 * it go and the music of other apps plays along with the game. Android's audio focus; null where there is none.
 */
interface AudioFocus {
    /** Holds the sound or lets it go; nothing if it already does. */
    fun hold(holding: Boolean)

    /** Told when another app takes the sound, for a while (a call, a voice) or for good (its music), and when it gives it back. */
    var listener: Listener?

    interface Listener {
        fun lost()

        fun regained()
    }
}
