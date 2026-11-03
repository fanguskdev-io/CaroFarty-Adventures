package com.fanguskdev.carofarty.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.Process
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.math.min

internal actual fun musicPlayer(): MusicPlayer = AndroidMusicPlayer()

/**
 * The music on Android: an AudioTrack fed by a thread of its own, with the samples of a WAV round and round, so the loop
 * has no gap (MediaPlayer leaves one), or with what a stream makes as it plays. While the track is paused it takes
 * nothing (its write gives 0 at once instead of waiting), so the thread sleeps until it plays again instead of going
 * round and round.
 */
private class AndroidMusicPlayer : MusicPlayer {
    private var track: AudioTrack? = null
    private var writer: Thread? = null

    @Volatile private var running = false

    @Volatile private var paused = false

    @Volatile private var rewind = false

    /** Where the writer sleeps while the track is paused, woken when it plays again or stops. */
    private val lock = ReentrantLock()
    private val woken = lock.newCondition()

    /** The phone's own rate, if the music knows how to make it exactly; else 48 000 Hz, which the phone converts. */
    override val streamRate: Int =
        runCatching { AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC) }
            .getOrNull()
            ?.takeIf { it == CD_RATE || it == STREAM_RATE }
            ?: STREAM_RATE

    override fun play(wav: ByteArray, volume: Float) {
        stop()
        val sound = readWav(wav) ?: return
        val track = open(sound.rate, sound.channels, volume) ?: return
        val samples = sound.samples
        if (samples.isEmpty()) return
        feed(track) {
            var position = 0
            while (running) {
                if (rewind) {
                    rewind = false
                    position = 0
                }
                val written = track.write(samples, position, min(CHUNK, samples.size - position))
                if (written < 0) break
                if (written == 0) waitWhilePaused()
                position = (position + written) % samples.size
            }
        }
    }

    override fun stream(source: PcmSource, rate: Int, volume: Float) {
        stop()
        val track = open(rate, 1, volume) ?: return
        val buffer = ShortArray(CHUNK)
        feed(track) {
            while (running) {
                source.render(buffer, 0, CHUNK)
                // All of it, even if the track is paused halfway: the music never skips what it made.
                var done = 0
                while (running && done < CHUNK) {
                    val written = track.write(buffer, done, CHUNK - done)
                    if (written < 0) return@feed
                    if (written == 0) waitWhilePaused()
                    done += written
                }
            }
        }
    }

    /**
     * The writer waits while the track takes nothing: paused, until it plays again (a little at a time, so it never
     * misses it); full for a moment, a few milliseconds.
     */
    private fun waitWhilePaused() {
        lock.withLock {
            if (running) woken.await(if (paused) PAUSED_WAIT_MILLIS else FULL_WAIT_MILLIS, TimeUnit.MILLISECONDS)
        }
    }

    /** A track playing [rate] samples a second of [channels], or null if the phone has no way to play it. */
    private fun open(rate: Int, channels: Int, volume: Float): AudioTrack? {
        val mask = if (channels == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val minimum = AudioTrack.getMinBufferSize(rate, mask, AudioFormat.ENCODING_PCM_16BIT)
        val attributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        val format =
            AudioFormat.Builder()
                .setSampleRate(rate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(mask)
                .build()
        // A phone without a way to play it (or an emulator without sound) leaves the game silent, never closed.
        return runCatching {
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
        }.getOrNull()
    }

    /** Starts the thread that writes into [track], with the priority of audio, doing [writing] while the music goes on. */
    private fun feed(track: AudioTrack, writing: () -> Unit) {
        this.track = track
        running = true
        writer =
            Thread {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                writing()
            }.apply {
                name = "music"
                isDaemon = true
                start()
            }
    }

    override fun pause() {
        paused = true
        runCatching { track?.pause() }
    }

    override fun resume() {
        paused = false
        runCatching { track?.play() }
        lock.withLock { woken.signalAll() }
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
        lock.withLock { woken.signalAll() }
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
        const val STREAM_RATE = 48_000
        const val CD_RATE = 44_100

        /** How long the writer sleeps at a time while the track is paused, or full. */
        const val PAUSED_WAIT_MILLIS = 50L
        const val FULL_WAIT_MILLIS = 5L
    }
}

internal actual fun soundPlayer(): SoundPlayer = AndroidSoundPlayer()

/**
 * The sounds on Android: a SoundPool, made for the short sounds of games. It reads files, so each sound is written to the
 * app's cache first (java.io.tmpdir there). They start on a thread of their own with the priority of audio, as on iOS:
 * on some phones SoundPool.play stops whoever calls it for some milliseconds, and a Xiaomi Mi 10 lost two or three frames
 * at every jump and every landing, where they sound (GameMaster's analysis of a recording). The game only leaves a
 * message, from the pool of Android's, so starting one makes nothing.
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
    private val thread = HandlerThread("sounds", Process.THREAD_PRIORITY_AUDIO).apply { start() }
    private val handler =
        object : Handler(thread.looper) {
            override fun handleMessage(message: Message) {
                val started = System.nanoTime()
                val volume = Float.fromBits(message.arg2)
                runCatching { pool.play(message.what, volume, volume, 1, 0, Float.fromBits(message.arg1)) }
                took((System.nanoTime() - started) / NANOS_IN_MILLI)
            }
        }

    /** The longest SoundPool.play took on its thread in the last two windows of [WINDOW_NANOS], for the debugging line. */
    @Volatile private var slowest = 0.0
    private var windowStart = 0L
    private var windowLongest = 0.0
    private var lastWindowLongest = 0.0

    override val slowestStartMillis: Double get() = slowest

    private fun took(millis: Double) {
        val now = System.nanoTime()
        if (now - windowStart > WINDOW_NANOS) {
            lastWindowLongest = windowLongest
            windowLongest = 0.0
            windowStart = now
        }
        windowLongest = max(windowLongest, millis)
        slowest = max(windowLongest, lastWindowLongest)
    }

    override fun load(name: String, wav: ByteArray) {
        val folder = System.getProperty("java.io.tmpdir") ?: return
        val id = runCatching { pool.load(File(folder, "sonido-$name.wav").apply { writeBytes(wav) }.path, 1) }.getOrNull()
        if (id != null && id > 0) ids[name] = id
    }

    override fun play(name: String, rate: Float, volume: Float) {
        val id = ids[name] ?: return
        handler.sendMessage(handler.obtainMessage(id, rate.toRawBits(), volume.toRawBits()))
    }

    override fun release() {
        ids.clear()
        handler.post { pool.release() }
        thread.quitSafely()
    }

    private companion object {
        const val MAX_STREAMS = 6
        const val NANOS_IN_MILLI = 1_000_000.0
        const val WINDOW_NANOS = 2_000_000_000L
    }
}

internal actual fun blockingGc(): LongArray? {
    val count = Debug.getRuntimeStat("art.gc.blocking-gc-count")?.toLongOrNull() ?: return null
    val millis = Debug.getRuntimeStat("art.gc.blocking-gc-time")?.toLongOrNull() ?: return null
    return longArrayOf(count, millis)
}
