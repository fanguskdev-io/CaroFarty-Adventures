package com.fanguskdev.carofarty.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

internal actual fun musicPlayer(): MusicPlayer = IosMusicPlayer()

/** The music on iOS: an AVAudioPlayer that loops for ever. It respects the silent switch, like any game. */
private class IosMusicPlayer : MusicPlayer {
    private var player: AVAudioPlayer? = null

    override fun play(wav: ByteArray) {
        stop()
        val player = audioPlayer(wav) ?: return
        player.numberOfLoops = -1
        player.prepareToPlay()
        player.play()
        this.player = player
    }

    override fun pause() {
        player?.pause()
    }

    override fun resume() {
        player?.play()
    }

    override fun stop() {
        player?.stop()
        player = null
    }
}

internal actual fun soundPlayer(): SoundPlayer = IosSoundPlayer()

/** The sounds on iOS: an AVAudioPlayer ready for each, played again from its start each time. */
private class IosSoundPlayer : SoundPlayer {
    private val players = HashMap<String, AVAudioPlayer>()

    override fun load(name: String, wav: ByteArray) {
        val player = audioPlayer(wav) ?: return
        player.prepareToPlay()
        players[name] = player
    }

    override fun play(name: String) {
        val player = players[name] ?: return
        player.currentTime = 0.0
        player.play()
    }

    override fun release() {
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
