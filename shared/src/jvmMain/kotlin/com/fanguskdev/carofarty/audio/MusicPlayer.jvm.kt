package com.fanguskdev.carofarty.audio

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.sound.sampled.Line
import javax.sound.sampled.SourceDataLine
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

/**
 * The music on the desktop: a Clip that loops for ever, or a line fed by a thread of its own with what a stream makes;
 * silence if the computer has no way to play them.
 */
private class DesktopMusicPlayer : MusicPlayer {
    private var clip: Clip? = null
    private var line: SourceDataLine? = null
    private var writer: Thread? = null

    @Volatile private var running = false

    override val streamRate: Int = STREAM_RATE

    override fun play(wav: ByteArray, volume: Float) = onAudioThread {
        stopAll()
        clip =
            clip(wav)?.apply {
                setVolume(volume)
                loop(Clip.LOOP_CONTINUOUSLY)
            }
    }

    override fun stream(source: PcmSource, rate: Int, volume: Float) = onAudioThread {
        stopAll()
        val format = AudioFormat(rate.toFloat(), BITS, 1, true, false)
        val line = runCatching { AudioSystem.getSourceDataLine(format).apply { open(format, rate * 2 * BUFFER_MILLIS / 1000) } }
            .getOrNull() ?: return@onAudioThread
        line.setVolume(volume)
        line.start()
        this.line = line
        running = true
        writer =
            Thread {
                val samples = ShortArray(FRAMES)
                val bytes = ByteArray(FRAMES * 2)
                while (running) {
                    source.render(samples, 0, FRAMES)
                    for (i in 0 until FRAMES) {
                        val sample = samples[i].toInt()
                        bytes[2 * i] = sample.toByte()
                        bytes[2 * i + 1] = (sample shr 8).toByte()
                    }
                    line.write(bytes, 0, bytes.size)
                }
            }.apply {
                name = "music"
                isDaemon = true
                start()
            }
    }

    override fun pause() = onAudioThread {
        clip?.stop()
        line?.stop()
    }

    override fun resume() = onAudioThread {
        clip?.loop(Clip.LOOP_CONTINUOUSLY)
        line?.start()
    }

    override fun restart() = onAudioThread {
        clip?.let {
            it.stop()
            it.framePosition = 0
            it.loop(Clip.LOOP_CONTINUOUSLY)
        }
    }

    override fun stop() = onAudioThread { stopAll() }

    private fun stopAll() {
        clip?.close()
        clip = null
        running = false
        line?.let {
            it.stop()
            it.flush()
            writer?.join(JOIN_MILLIS)
            it.close()
        }
        line = null
        writer = null
    }

    private companion object {
        const val STREAM_RATE = 48_000
        const val BITS = 16
        const val FRAMES = 2048
        const val BUFFER_MILLIS = 200
        const val JOIN_MILLIS = 500L
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
private fun Line.setVolume(volume: Float) {
    if (!isControlSupported(FloatControl.Type.MASTER_GAIN)) return
    val gain = getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl ?: return
    gain.value = (DECIBELS * log10(volume.coerceIn(MIN_VOLUME, 1f))).coerceIn(gain.minimum, gain.maximum)
}

private const val DECIBELS = 20f
private const val MIN_VOLUME = 0.0001f

/** A Clip open with the WAV in [wav], or null if the computer cannot play it. */
private fun clip(wav: ByteArray): Clip? =
    runCatching { AudioSystem.getClip().apply { open(AudioSystem.getAudioInputStream(ByteArrayInputStream(wav))) } }.getOrNull()

internal actual fun blockingGc(): LongArray? = null
