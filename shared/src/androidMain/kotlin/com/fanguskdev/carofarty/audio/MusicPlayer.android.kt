package com.fanguskdev.carofarty.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import java.io.File
import kotlin.math.max
import kotlin.math.min

internal actual fun musicPlayer(): MusicPlayer = AndroidMusicPlayer()

/**
 * The music on Android: the samples of the WAV fed to an AudioTrack by a thread of its own, round and round, so the loop
 * has no gap (MediaPlayer leaves one).
 */
private class AndroidMusicPlayer : MusicPlayer {
    private var track: AudioTrack? = null
    private var writer: Thread? = null

    @Volatile private var running = false

    override fun play(wav: ByteArray) {
        stop()
        val sound = readWav(wav) ?: return
        val channels = if (sound.channels == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val minimum = AudioTrack.getMinBufferSize(sound.rate, channels, AudioFormat.ENCODING_PCM_16BIT)
        val attributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        val format =
            AudioFormat.Builder()
                .setSampleRate(sound.rate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(channels)
                .build()
        val track =
            AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(max(minimum, CHUNK * 4))
                .build()
        this.track = track
        running = true
        track.play()
        val samples = sound.samples
        writer =
            Thread {
                var position = 0
                while (running && samples.isNotEmpty()) {
                    val written = track.write(samples, position, min(CHUNK, samples.size - position))
                    if (written < 0) break
                    position = (position + written) % samples.size
                }
            }.apply {
                name = "music"
                isDaemon = true
                start()
            }
    }

    override fun pause() {
        track?.pause()
    }

    override fun resume() {
        track?.play()
    }

    override fun stop() {
        running = false
        val track = track ?: return
        track.pause()
        track.flush()
        writer?.join(JOIN_MILLIS)
        track.release()
        this.track = null
        writer = null
    }

    private companion object {
        /** Samples written at a time: some 50 ms. */
        const val CHUNK = 2048
        const val JOIN_MILLIS = 500L
    }
}

internal actual fun soundPlayer(): SoundPlayer = AndroidSoundPlayer()

/**
 * The sounds on Android: a SoundPool, made for the short sounds of games. It reads files, so each sound is written to the
 * app's cache first (java.io.tmpdir there).
 */
private class AndroidSoundPlayer : SoundPlayer {
    private val pool =
        SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build()
    private val ids = HashMap<String, Int>()

    override fun load(name: String, wav: ByteArray) {
        val folder = System.getProperty("java.io.tmpdir") ?: return
        val file = runCatching { File(folder, "sonido-$name.wav").apply { writeBytes(wav) } }.getOrNull() ?: return
        ids[name] = pool.load(file.path, 1)
    }

    override fun play(name: String) {
        val id = ids[name] ?: return
        pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    override fun release() {
        pool.release()
        ids.clear()
    }

    private companion object {
        const val MAX_STREAMS = 6
    }
}
