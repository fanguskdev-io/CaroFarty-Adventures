package com.fanguskdev.carofarty.audio

import kotlin.math.min

/**
 * Plays the music: a track round and round, a 16-bit PCM WAV file (the menus), or what a [PcmSource] makes while it plays
 * (the music of a run). Each platform does it its own way, on a thread of its own.
 */
interface MusicPlayer {
    /** Plays [wav] in a loop from its start, at [volume] (0 to 1), instead of whatever was playing. */
    fun play(wav: ByteArray, volume: Float = 1f)

    /** The rate a stream should have on this platform, in samples a second: the one it mixes at. */
    val streamRate: Int

    /** Plays what [source] makes, mono at [rate] samples a second and at [volume], instead of whatever was playing. */
    fun stream(source: PcmSource, rate: Int, volume: Float = 1f)

    fun pause()

    fun resume()

    /** The same track again from its start, without reading it again: a new run in the same scenario. */
    fun restart()

    fun stop()
}

/** The player of this platform. */
internal expect fun musicPlayer(): MusicPlayer

/** Something that makes 16-bit mono samples when they are asked for: the music made while she plays. */
fun interface PcmSource {
    /** Writes [frames] samples into [out] from [offset]. Called from the player's thread, never from the game's. */
    fun render(out: ShortArray, offset: Int, frames: Int)
}

/** Plays short sounds over the music, each from its start, several at once if need be. */
interface SoundPlayer {
    /** Gets [wav] ready to be played as [name]. */
    fun load(name: String, wav: ByteArray)

    /** Plays [name] from its start, if it is loaded, [rate] times as fast (and as high) and at [volume] (0 to 1). */
    fun play(name: String, rate: Float = 1f, volume: Float = 1f)

    fun release()
}

/** The player of sounds of this platform. */
internal expect fun soundPlayer(): SoundPlayer

/** A sound in 16-bit PCM: its [samples], interleaved if it has two [channels], at [rate] samples a second. */
class Pcm(val samples: ShortArray, val channels: Int, val rate: Int)

/** The sound in a WAV file of 16-bit PCM, mono or stereo, or null if it is not one. */
fun readWav(bytes: ByteArray): Pcm? {
    if (bytes.size < HEADER || bytes.decodeToString(0, 4) != "RIFF" || bytes.decodeToString(8, 12) != "WAVE") return null
    var format = 0
    var channels = 0
    var rate = 0
    var bits = 0
    var offset = HEADER
    while (offset + CHUNK_HEADER <= bytes.size) {
        val id = bytes.decodeToString(offset, offset + 4)
        val size = int32(bytes, offset + 4)
        val body = offset + CHUNK_HEADER
        if (size < 0) return null
        if (id == "fmt " && body + FMT_SIZE <= bytes.size) {
            format = int16(bytes, body)
            channels = int16(bytes, body + 2)
            rate = int32(bytes, body + 4)
            bits = int16(bytes, body + 14)
        } else if (id == "data") {
            if ((format != PCM && format != EXTENSIBLE) || bits != 16 || channels !in 1..2 || rate <= 0) return null
            val end = min(bytes.size.toLong(), body.toLong() + size).toInt()
            val samples = ShortArray((end - body) / 2) { int16(bytes, body + 2 * it).toShort() }
            return Pcm(samples, channels, rate)
        }
        // Chunks take an even number of bytes.
        offset = body + size + (size and 1)
    }
    return null
}

private fun int16(bytes: ByteArray, at: Int): Int = (bytes[at].toInt() and 0xFF) or (bytes[at + 1].toInt() shl 8)

private fun int32(bytes: ByteArray, at: Int): Int = (int16(bytes, at) and 0xFFFF) or (int16(bytes, at + 2) shl 16)

private const val HEADER = 12
private const val CHUNK_HEADER = 8
private const val FMT_SIZE = 16
private const val PCM = 1
private const val EXTENSIBLE = 0xFFFE
