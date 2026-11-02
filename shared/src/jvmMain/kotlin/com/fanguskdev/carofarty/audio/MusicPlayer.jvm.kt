package com.fanguskdev.carofarty.audio

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.log10

/**
 * Where every Clip is opened and told what to do: a thread of its own, so the window never waits for the sound card.
 * Everything the players below keep is only touched from here.
 */
private val audioThread =
    Executors.newSingleThreadExecutor { task -> Thread(task, "audio").apply { isDaemon = true } }

private fun onAudioThread(block: () -> Unit) {
    audioThread.execute { runCatching(block) }
}

internal actual fun musicPlayer(): MusicPlayer = DesktopMusicPlayer()

/** The music on the desktop: a Clip that loops for ever, or silence if the computer has no way to play it. */
private class DesktopMusicPlayer : MusicPlayer {
    private var clip: Clip? = null

    override fun play(wav: ByteArray, volume: Float) = onAudioThread {
        clip?.close()
        clip =
            clip(wav)?.apply {
                setVolume(volume)
                loop(Clip.LOOP_CONTINUOUSLY)
            }
    }

    override fun pause() = onAudioThread { clip?.stop() }

    override fun resume() = onAudioThread { clip?.loop(Clip.LOOP_CONTINUOUSLY) }

    override fun restart() = onAudioThread {
        clip?.let {
            it.stop()
            it.framePosition = 0
            it.loop(Clip.LOOP_CONTINUOUSLY)
        }
    }

    override fun stop() = onAudioThread {
        clip?.close()
        clip = null
    }
}

internal actual fun soundPlayer(): SoundPlayer = DesktopSoundPlayer()

/** The sounds on the desktop: a Clip for each, played again from its start each time. */
private class DesktopSoundPlayer : SoundPlayer {
    private val clips = HashMap<String, Clip>()

    override fun load(name: String, wav: ByteArray) = onAudioThread {
        clip(wav)?.let { clips[name] = it }
    }

    override fun play(name: String, rate: Float, volume: Float) = onAudioThread {
        clips[name]?.let {
            it.stop()
            it.framePosition = 0
            it.setVolume(volume)
            it.start()
        }
    }

    override fun release() = onAudioThread {
        for (clip in clips.values) clip.close()
        clips.clear()
    }
}

/** Its volume, from 0 to 1, if the computer lets it change; the desktop does not change the speed of a sound. */
private fun Clip.setVolume(volume: Float) {
    if (!isControlSupported(FloatControl.Type.MASTER_GAIN)) return
    val gain = getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl ?: return
    gain.value = (DECIBELS * log10(volume.coerceIn(MIN_VOLUME, 1f))).coerceIn(gain.minimum, gain.maximum)
}

private const val DECIBELS = 20f
private const val MIN_VOLUME = 0.0001f

/** A Clip open with the WAV in [wav], or null if the computer cannot play it. */
private fun clip(wav: ByteArray): Clip? =
    runCatching { AudioSystem.getClip().apply { open(AudioSystem.getAudioInputStream(ByteArrayInputStream(wav))) } }.getOrNull()
