package com.fanguskdev.carofarty.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/**
 * Android's audio focus for the game (QA-07): asked for as a game's, so a call that rings or a voice that speaks takes it
 * for a while, and another app's music for good; either way the game is told, and pauses (also for a voice, which Android
 * would only make quieter). Its calls come on the main thread.
 */
class AndroidAudioFocus(context: Context) : AudioFocus {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var held = false

    override var listener: AudioFocus.Listener? = null

    private val changes =
        AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> listener?.regained()
                AudioManager.AUDIOFOCUS_LOSS -> {
                    // For good: it is not given back, so it is let go, and asked for again when the music plays again.
                    hold(false)
                    listener?.lost()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> listener?.lost()
            }
        }

    private val request: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                ).setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(changes)
                .build()
        } else {
            null
        }

    override fun hold(holding: Boolean) {
        if (holding == held) return
        held = holding
        val request = request
        runCatching {
            if (holding) {
                val granted =
                    if (request != null) manager.requestAudioFocus(request) else requestBefore26()
                held = granted == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else if (request != null) {
                manager.abandonAudioFocusRequest(request)
            } else {
                abandonBefore26()
            }
        }
    }

    /** Android 7, with the way of asking it had. */
    @Suppress("DEPRECATION")
    private fun requestBefore26(): Int = manager.requestAudioFocus(changes, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

    @Suppress("DEPRECATION")
    private fun abandonBefore26() {
        manager.abandonAudioFocus(changes)
    }
}
