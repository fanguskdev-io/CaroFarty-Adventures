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

    @Volatile private var rewind = false

    override fun play(wav: ByteArray, volume: Float) {
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
        // A phone without a way to play it (or an emulator without sound) leaves the game silent, never closed.
        val track =
            runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(max(minimum, CHUNK * 4))
                    .build()
                    .also {
                        it.setVolume(volume)
                        it.play()
                    }
            }.getOrNull() ?: return
        this.track = track
        running = true
        val samples = sound.samples
        writer =
            Thread {
                var position = 0
                while (running && samples.isNotEmpty()) {
                    if (rewind) {
                        rewind = false
                        position = 0
                    }
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
        runCatching { track?.pause() }
    }

    override fun resume() {
        runCatching { track?.play() }
    }

    override fun restart() {
        val track = track ?: return
        rewind = true
        runCatching {
            track.pause()
            track.flush()
            track.play()
        }
    }

    override fun stop() {
        running = false
        val track = track ?: return
        runCatching {
            track.pause()
            track.flush()
        }
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
        val id = runCatching { pool.load(File(folder, "sonido-$name.wav").apply { writeBytes(wav) }.path, 1) }.getOrNull()
        if (id != null && id > 0) ids[name] = id
    }

    override fun play(name: String, rate: Float, volume: Float) {
        val id = ids[name] ?: return
        runCatching { pool.play(id, volume, volume, 1, 0, rate) }
    }

    override fun release() {
        pool.release()
        ids.clear()
    }

    private companion object {
        const val MAX_STREAMS = 6
    }
}
