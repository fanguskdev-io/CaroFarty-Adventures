package com.fanguskdev.carofarty.audio.music

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The pieces the port stands on: Python's random numbers, the platform-independent maths and the chip's tables. */
class PortBasicsTest {
    @Test
    fun theRandomNumbersAreThoseOfPython() {
        // Values printed by Python 3.9's random module for these seeds.
        val seven = PyRandom(7)
        assertEquals(0.32383276483316237, seven.random())
        assertEquals(0.15084917392450192, seven.random())
        assertEquals(0.6509344730398537, seven.random())
        val bits = PyRandom(7)
        assertContentEquals(longArrayOf(1390851128, 4071050724, 647892279), LongArray(3) { bits.nextBits32().toLong() and 0xFFFFFFFFL })
        val below = PyRandom(707)
        assertContentEquals(intArrayOf(0, 2, 2, 4, 5, 4, 59, 0), intArrayOf(2, 3, 4, 5, 6, 7, 100, 1).map { below.below(it) }.toIntArray())
        val big = PyRandom((1L shl 40) + 5)
        assertEquals(0.5043802970418443, big.random())
        assertEquals(0.2686044399723282, big.random())
        val gauss = PyRandom(7)
        val expected = doubleArrayOf(-12.28225384548482, 24.54871260079267, -10.852615909589026, -15.1232842718969, -44.64087313549284)
        for (e in expected) assertEquals(e, gauss.gauss(0.0, 48.0), 1e-12)
        val uniform = PyRandom(12345)
        assertEquals(-0.016676025490931765, uniform.uniform(-0.1, 0.1))
        assertEquals(-0.09796616610858633, uniform.uniform(-0.1, 0.1))
        assertEquals(4, PyRandom(99).range(1, 7))
    }

    @Test
    fun theMathsAgreeWithThePlatformsToTheLastBits() {
        val rng = PyRandom(1)
        var worst = 0.0
        repeat(20_000) {
            val x = rng.uniform(-60000.0, 60000.0)
            worst = max(worst, abs(DetMath.sin(x) - kotlin.math.sin(x)))
            worst = max(worst, abs(DetMath.cos(x) - kotlin.math.cos(x)))
            val small = rng.uniform(-700.0, 700.0)
            worst = max(worst, abs(DetMath.exp(small) / kotlin.math.exp(small) - 1))
            val positive = DetMath.exp(rng.uniform(-700.0, 700.0))
            worst = max(worst, abs(DetMath.ln(positive) - kotlin.math.ln(positive)) / max(1.0, abs(kotlin.math.ln(positive))))
            val y = rng.uniform(-30.0, 30.0)
            worst = max(worst, abs(DetMath.exp2(y) / 2.0.pow(y) - 1))
        }
        assertTrue(worst < 2e-15, "the deterministic maths stray $worst from the platform's")
        assertEquals(1.0, DetMath.pow(7.3, 0.0))
        assertEquals(0.0, DetMath.pow(0.0, 1.5))
        assertEquals(kotlin.math.sqrt(2.0), DetMath.pow(2.0, 0.5), 1e-15)
    }

    @Test
    fun aMidiNameIsItsNumber() {
        assertEquals(60, midi("C4"))
        assertEquals(78, midi("F#5"))
        assertEquals(58, midi("Bb3"))
        assertEquals(64, midi("E4"))
    }

    @Test
    fun scaleDegreesGoOnByOctaves() {
        val major = Scale(midi("E4"), Modes.major)
        assertEquals(midi("E4"), major.note(0))
        assertEquals(midi("E5"), major.note(7))
        assertEquals(midi("D#4"), major.note(-1))
        assertEquals(0, major.degree(midi("E4")))
        assertEquals(-1, major.degree(midi("D#4")))
        // A note out of the scale gets the degree of the closest note of the scale below it.
        assertEquals(1, major.degree(midi("G4")))
        val pentatonic = Scale(midi("A4"), Modes.minorPentatonic)
        assertEquals(midi("A5"), pentatonic.note(5))
        assertEquals(-2, pentatonic.degree(midi("E4")))
    }
}
