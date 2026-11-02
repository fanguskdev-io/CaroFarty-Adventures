package com.fanguskdev.carofarty.audio

import com.fanguskdev.carofarty.game.Scenario
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.math.log2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameAudioTest {
    /** A player that keeps what it is given to play, so the test can listen to it. */
    private class FakeMusic : MusicPlayer {
        @Volatile var source: PcmSource? = null

        @Volatile var loop: ByteArray? = null

        override val streamRate = RATE

        override fun play(wav: ByteArray, volume: Float) {
            loop = wav
            source = null
        }

        override fun stream(source: PcmSource, rate: Int, volume: Float) {
            this.source = source
            loop = null
        }

        override fun pause() = Unit

        override fun resume() = Unit

        override fun restart() = Unit

        override fun stop() {
            source = null
            loop = null
        }
    }

    private class FakeSounds : SoundPlayer {
        val played = mutableListOf<String>()
        val rates = mutableListOf<Float>()

        override fun load(name: String, wav: ByteArray) = Unit

        override fun play(name: String, rate: Float, volume: Float) {
            played += name
            rates += rate
        }

        override fun release() = Unit
    }

    /** The loudest sample of the next [seconds] of [source], from 0 to 1. */
    private fun peak(source: PcmSource, seconds: Double): Double {
        val buffer = ShortArray(RATE / 10)
        var loudest = 0
        repeat((seconds * 10).toInt()) {
            source.render(buffer, 0, buffer.size)
            for (sample in buffer) loudest = maxOf(loudest, abs(sample.toInt()))
        }
        return loudest / 32_768.0
    }

    @Test
    fun aRunPlaysItsOwnMusicAfterItsJingleAndItFadesOutAtTheEnd() = runBlocking {
        val music = FakeMusic()
        val sounds = FakeSounds()
        val audio = GameAudio(music, sounds, this)
        withTimeout(TIMEOUT) { while (!audio.hasMusic(Scenario.Nieve)) delay(STEP) }
        audio.start(Scenario.Nieve, seed = 7)
        withTimeout(TIMEOUT) { while (music.source == null) delay(STEP) }
        assertEquals(null, music.loop, "the run plays the music made as she plays, not the loop")
        assertTrue("jingle-inicio-nieve" in sounds.played, "after its jingle")
        val source = music.source ?: error("no music")
        assertTrue(peak(source, 2.0) > AUDIBLE, "it is heard")
        audio.speed(300f)
        audio.lowPoints(true)
        assertTrue(peak(source, 4.0) > AUDIBLE, "and goes on with its layers and its tense version")
        audio.end(Scenario.Nieve, record = false, wait = 0)
        peak(source, 3.0)
        assertTrue(peak(source, 0.5) < SILENT, "the end fades it out")
        audio.release()
    }

    @Test
    fun aRescueInAStreakSoundsAStepHigher() = runBlocking {
        val sounds = FakeSounds()
        val audio = GameAudio(FakeMusic(), sounds, this)
        audio.play(Sound.Perrito, streak = 0)
        audio.play(Sound.Perrito, streak = 5)
        audio.play(Sound.Gatito, streak = 2)
        audio.play(Sound.Gatito, streak = 9)
        val semitones = sounds.rates.map { 12 * log2(it.toDouble()) }
        // Each one within ±40 cents of its step: 0, 12 for the perrito; 4, and 12 at the top, for the gatito.
        for ((heard, step) in semitones.zip(listOf(0, 12, 4, 12))) assertTrue(abs(heard - step) <= 0.4, "$heard for $step")
        audio.release()
    }

    @Test
    fun theStartScreenPlaysTheLoopOfItsScenario() = runBlocking {
        val music = FakeMusic()
        val audio = GameAudio(music, FakeSounds(), this)
        audio.music(Scenario.Bosque)
        withTimeout(TIMEOUT) { while (music.loop == null) delay(STEP) }
        assertEquals(null, music.source)
        audio.release()
    }

    private companion object {
        const val RATE = 48_000
        const val TIMEOUT = 20_000L
        const val STEP = 20L
        const val AUDIBLE = 0.05
        const val SILENT = 0.001
    }
}
