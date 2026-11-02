package com.fanguskdev.carofarty.audio

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WavTest {
    @Test
    fun theWavOfTheSynthesizerReadsBack() {
        val pcm = shortArrayOf(0, 1, -1, 32_767, -32_768, 1234)
        val sound = assertNotNull(readWav(wav(pcm)))
        assertEquals(1, sound.channels)
        assertEquals(SAMPLE_RATE, sound.rate)
        assertContentEquals(pcm, sound.samples)
    }

    @Test
    fun whatIsNotAWavOfSixteenBitsIsNothing() {
        assertNull(readWav(ByteArray(0)))
        assertNull(readWav("RIFF\u0000\u0000\u0000\u0000AVI ".encodeToByteArray()))
        val eightBits = wav(shortArrayOf(1, 2)).also { it[34] = 8 }
        assertNull(readWav(eightBits))
    }

    @Test
    fun chunksBeforeTheDataAreSkipped() {
        val plain = wav(shortArrayOf(5, 6, 7))
        // A "LIST" chunk of 3 bytes (and its padding byte) between "fmt " and "data", as many editors write.
        val list = "LIST".encodeToByteArray() + byteArrayOf(3, 0, 0, 0, 1, 2, 3, 0)
        val withList = plain.copyOfRange(0, 36) + list + plain.copyOfRange(36, plain.size)
        assertContentEquals(shortArrayOf(5, 6, 7), assertNotNull(readWav(withList)).samples)
    }
}
