package com.fanguskdev.carofarty.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
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

/** The music on iOS: an AVAudioPlayer that loops for ever. It respects the silent switch, like any game. */
private class IosMusicPlayer : MusicPlayer {
    private var player: AVAudioPlayer? = null

    override fun play(wav: ByteArray) = onAudioQueue {
        player?.stop()
        player =
            audioPlayer(wav)?.apply {
                numberOfLoops = -1
                prepareToPlay()
                play()
            }
    }

    override fun pause() = onAudioQueue { player?.pause() }

    override fun resume() = onAudioQueue { player?.play() }

    override fun restart() = onAudioQueue {
        player?.let {
            it.currentTime = 0.0
            it.play()
        }
    }

    override fun stop() = onAudioQueue {
        player?.stop()
        player = null
    }
}

internal actual fun soundPlayer(): SoundPlayer = IosSoundPlayer()

/** The sounds on iOS: an AVAudioPlayer ready for each, played again from its start each time. */
private class IosSoundPlayer : SoundPlayer {
    private val players = HashMap<String, AVAudioPlayer>()

    override fun load(name: String, wav: ByteArray) = onAudioQueue {
        audioPlayer(wav)?.let {
            it.prepareToPlay()
            players[name] = it
        }
    }

    override fun play(name: String) = onAudioQueue {
        players[name]?.let {
            it.currentTime = 0.0
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
