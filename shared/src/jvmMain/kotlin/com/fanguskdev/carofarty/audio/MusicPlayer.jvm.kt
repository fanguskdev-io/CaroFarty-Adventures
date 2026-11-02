package com.fanguskdev.carofarty.audio

import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

internal actual fun musicPlayer(): MusicPlayer = DesktopMusicPlayer()

/** The music on the desktop: a Clip that loops for ever, or silence if the computer has no way to play it. */
private class DesktopMusicPlayer : MusicPlayer {
    private var clip: Clip? = null

    override fun play(wav: ByteArray) {
        stop()
        clip =
            runCatching {
                AudioSystem.getClip().apply {
                    open(AudioSystem.getAudioInputStream(ByteArrayInputStream(wav)))
                    loop(Clip.LOOP_CONTINUOUSLY)
                }
            }.getOrNull()
    }

    override fun pause() {
        clip?.stop()
    }

    override fun resume() {
        clip?.loop(Clip.LOOP_CONTINUOUSLY)
    }

    override fun stop() {
        clip?.close()
        clip = null
    }
}

internal actual fun soundPlayer(): SoundPlayer = DesktopSoundPlayer()

/** The sounds on the desktop: a Clip for each, played again from its start each time. */
private class DesktopSoundPlayer : SoundPlayer {
    private val clips = HashMap<String, Clip>()

    override fun load(name: String, wav: ByteArray) {
        val clip =
            runCatching {
                AudioSystem.getClip().apply { open(AudioSystem.getAudioInputStream(ByteArrayInputStream(wav))) }
            }.getOrNull() ?: return
        clips[name] = clip
    }

    override fun play(name: String) {
        val clip = clips[name] ?: return
        clip.stop()
        clip.framePosition = 0
        clip.start()
    }

    override fun release() {
        for (clip in clips.values) clip.close()
        clips.clear()
    }
}
