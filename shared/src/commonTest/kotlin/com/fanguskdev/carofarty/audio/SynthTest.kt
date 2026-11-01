package com.fanguskdev.carofarty.audio

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SynthTest {
    @Test
    fun theFilterStaysStableAtItsHighestCutoff() {
        val filter = Svf()
        var loudest = 0f
        repeat(SAMPLE_RATE) { i ->
            filter.run(if (i % 2 == 0) 1f else -1f, cutoffHz = 5_000f, q = 0.7f)
            loudest = maxOf(loudest, abs(filter.low))
        }
        assertTrue(loudest.isFinite() && loudest < 10f, "the filter ran away: $loudest")
    }

    @Test
    fun thePulseWaveStaysWithinRangeAndAveragesToSilence() {
        val dt = 440f / SAMPLE_RATE
        var phase = 0f
        var sum = 0f
        repeat(SAMPLE_RATE) {
            val value = pulse(phase, dt, duty = 0.25f)
            assertTrue(value in -1.1f..1.1f, "out of range: $value")
            sum += value
            phase = (phase + dt) % 1f
        }
        assertTrue(abs(sum / SAMPLE_RATE) < 0.01f, "the pulse has an offset")
    }

    @Test
    fun midiNotesFollowTheEqualTemperament() {
        assertEquals(440f, midiHz(69))
        assertEquals(880f, midiHz(81), absoluteTolerance = 0.01f)
        assertEquals(261.63f, midiHz(60), absoluteTolerance = 0.01f)
    }

    @Test
    fun aFinishedSoundPeaksWhereAskedAndStartsAndEndsInSilence() {
        val sound = FloatArray(samples(0.2f)) { 0.3f }.finished(peak = 0.5f)

        assertEquals(0.5f, sound.maxOf { abs(it) }, absoluteTolerance = 0.001f)
        assertEquals(0f, sound.first())
        assertTrue(abs(sound.last()) < 0.01f)
    }

    @Test
    fun aWavFileHasItsHeaderAndItsSamplesInLittleEndian() {
        val bytes = wav(shortArrayOf(1, -2, 0x1234))

        assertEquals("RIFF", bytes.decodeToString(0, 4))
        assertEquals("WAVE", bytes.decodeToString(8, 12))
        assertEquals("data", bytes.decodeToString(36, 40))
        assertEquals(44 + 6, bytes.size)
        assertEquals(SAMPLE_RATE, int32(bytes, 24))
        assertEquals(6, int32(bytes, 40))
        assertEquals(0x34, bytes[48].toInt())
        assertEquals(0x12, bytes[49].toInt())
    }

    @Test
    fun pcmClampsWhatIsTooLoud() {
        assertTrue(toPcm16(floatArrayOf(2f, -2f, 0f)).contentEquals(shortArrayOf(32_767, -32_767, 0)))
    }

    private fun int32(bytes: ByteArray, offset: Int): Int = (0 until 4).sumOf { (bytes[offset + it].toInt() and 0xFF) shl (8 * it) }
}
