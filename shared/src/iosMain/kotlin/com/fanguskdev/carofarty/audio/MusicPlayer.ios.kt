package com.fanguskdev.carofarty.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioEngineManualRenderingModeOffline
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerNode
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create

/**
 * Where every AVAudioPlayer is made and told what to do: a queue of its own, because playing a sound can keep the thread
 * that asks for it busy for some milliseconds, and on the main one that showed as stutters in the run. Everything the
 * players below keep is only touched from here.
 */
private val audioQueue = dispatch_queue_create("com.fanguskdev.carofarty.audio", null)

private fun onAudioQueue(block: () -> Unit) = dispatch_async(audioQueue, block)

internal actual fun musicPlayer(): MusicPlayer = IosMusicPlayer()

/**
 * The music on iOS, which respects the silent switch, like any game: an AVAudioPlayer that loops a WAV for ever, or an
 * AVAudioEngine whose player node plays what a stream makes, in a few buffers that are made again on this queue as each
 * one ends, never on the thread of the sound card. [offline] is for the tests, where the simulator gives the process no
 * sound card: the engine then plays only what [pull] asks of it.
 */
internal class IosMusicPlayer(private val offline: Boolean = false) : MusicPlayer {
    private var player: AVAudioPlayer? = null
    private var engine: AVAudioEngine? = null
    private var node: AVAudioPlayerNode? = null

    /** Which stream is playing: the buffers of one that was stopped are not made again. */
    private var generation = 0

    override val streamRate: Int = STREAM_RATE

    override fun play(wav: ByteArray, volume: Float) = onAudioQueue {
        stopAll()
        player =
            audioPlayer(wav)?.apply {
                numberOfLoops = -1
                this.volume = volume
                prepareToPlay()
                play()
            }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun stream(source: PcmSource, rate: Int, volume: Float) = onAudioQueue {
        stopAll()
        val format = AVAudioFormat(standardFormatWithSampleRate = rate.toDouble(), channels = 1u)
        val engine = AVAudioEngine()
        val node = AVAudioPlayerNode()
        engine.attachNode(node)
        engine.connect(node, to = engine.mainMixerNode, format = format)
        node.volume = volume
        if (offline) engine.enableManualRenderingMode(AVAudioEngineManualRenderingModeOffline, format, FRAMES.toUInt(), null)
        if (!runCatching { engine.startAndReturnError(null) }.getOrDefault(false)) return@onAudioQueue
        this.engine = engine
        this.node = node
        val mine = ++generation
        val samples = ShortArray(FRAMES)
        repeat(BUFFERS) {
            val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = FRAMES.toUInt())
            schedule(node, buffer, source, samples, mine)
        }
        node.play()
    }

    /** Fills [buffer] with what [source] makes and queues it; when it has played, the same again, while it is [mine]. */
    @OptIn(ExperimentalForeignApi::class)
    private fun schedule(
        node: AVAudioPlayerNode,
        buffer: AVAudioPCMBuffer,
        source: PcmSource,
        samples: ShortArray,
        mine: Int,
    ) {
        source.render(samples, 0, FRAMES)
        buffer.frameLength = FRAMES.toUInt()
        val channel = buffer.floatChannelData?.get(0) ?: return
        for (i in 0 until FRAMES) channel[i] = samples[i] * TO_FLOAT
        node.scheduleBuffer(buffer) {
            onAudioQueue { if (generation == mine) schedule(node, buffer, source, samples, mine) }
        }
    }

    override fun pause() = onAudioQueue {
        player?.pause()
        node?.pause()
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun resume() = onAudioQueue {
        player?.play()
        val engine = engine ?: return@onAudioQueue
        // A call or another app may have stopped the engine while the game was away.
        if (!engine.running) runCatching { engine.startAndReturnError(null) }
        node?.play()
    }

    override fun restart() = onAudioQueue {
        player?.let {
            it.currentTime = 0.0
            it.play()
        }
    }

    override fun stop() = onAudioQueue { stopAll() }

    /** For the tests, with [offline]: the engine plays [frames] samples, as the sound card would ask for them. */
    @OptIn(ExperimentalForeignApi::class)
    internal fun pull(frames: Int) = onAudioQueue {
        val engine = engine ?: return@onAudioQueue
        val buffer = AVAudioPCMBuffer(pCMFormat = engine.manualRenderingFormat, frameCapacity = frames.toUInt())
        engine.renderOffline(frames.toUInt(), buffer, null)
    }

    private fun stopAll() {
        player?.stop()
        player = null
        generation++
        node?.stop()
        engine?.stop()
        node = null
        engine = null
    }

    private companion object {
        const val STREAM_RATE = 48_000

        /** Each buffer, some 85 ms; three of them queued, so the music is made a quarter of a second ahead. */
        const val FRAMES = 4096
        const val BUFFERS = 3
        const val TO_FLOAT = 1f / 32_768f
    }
}

internal actual fun soundPlayer(): SoundPlayer = IosSoundPlayer()

/** The sounds on iOS: an AVAudioPlayer ready for each, played again from its start each time. */
private class IosSoundPlayer : SoundPlayer {
    private val players = HashMap<String, AVAudioPlayer>()

    override fun load(name: String, wav: ByteArray) = onAudioQueue {
        audioPlayer(wav)?.let {
            it.enableRate = true
            it.prepareToPlay()
            players[name] = it
        }
    }

    override fun play(name: String, rate: Float, volume: Float) = onAudioQueue {
        players[name]?.let {
            it.currentTime = 0.0
            it.rate = rate
            it.volume = volume
            it.play()
        }
    }

    override fun release() = onAudioQueue {
        for (player in players.values) player.stop()
        players.clear()
    }
}

/** An AVAudioPlayer for the WAV in [wav], or null if iOS cannot read it. */
@OptIn(ExperimentalForeignApi::class)
private fun audioPlayer(wav: ByteArray): AVAudioPlayer? {
    if (wav.isEmpty()) return null
    val data = wav.usePinned { NSData.dataWithBytes(it.addressOf(0), wav.size.toULong()) }
    return runCatching { AVAudioPlayer(data = data, error = null) }.getOrNull()
}

internal actual fun blockingGc(): LongArray? = null
