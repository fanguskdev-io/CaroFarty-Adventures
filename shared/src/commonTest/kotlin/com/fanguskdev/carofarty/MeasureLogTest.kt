package com.fanguskdev.carofarty

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The log of the app that measures (GOD-27): the lines of CaroFarty G's measure, with the phone's clock as a test's. */
class MeasureLogTest {
    private var clock = 1_000_000_000L
    private var rate = 60f
    private val written = mutableListOf<String>()
    private val log = MeasureLog({ clock }, { rate }) { written += it }
    private val lines = arrayOf("FPS 60 · PEOR 17 MS · LENTOS 0,0 %", "SALTOS 3 · COYOTE 0 · BUFFER 0")

    /** [count] frames of [millis] by the phone's clock, the game's clock going [ahead] times as fast. */
    private fun frames(
        count: Int,
        millis: Double,
        ahead: Double = 1.0,
        game: LongArray = longArrayOf(0L),
    ) {
        repeat(count) {
            clock += (millis * MILLI).toLong()
            game[0] += (millis * MILLI * ahead).toLong()
            log.frame(game[0], lines, pieceWaits = 0)
        }
    }

    @Test
    fun everyTenSecondsItsLinesAsCaroFartyGLeavesThem() {
        frames(601, millis = 1000.0 / 60)
        assertEquals("MEDIDA tasa 60 Hz", written.first())
        assertEquals("MEDIDA FPS 60 · PEOR 17 MS · LENTOS 0,0 % | SALTOS 3 · COYOTE 0 · BUFFER 0", written[1])
        assertEquals("MEDIDA real FPS 60 · PEOR 17 MS · LENTOS 0,0 % · RELOJ +0 MS · ESPERAS 0 · 60 HZ", written[2])
        assertEquals(3, written.size, "nothing more until the next 10 s")
    }

    @Test
    fun aStopAndAJumpWithTheMomentItsFrameBegan() {
        frames(10, millis = 1000.0 / 60)
        frames(1, millis = 150.0)
        assertTrue("MEDIDA parón 150 ms" in written)
        log.moment("salto")
        assertEquals("MEDIDA salto $clock", written.last())
        log.away(true)
        log.away(false)
        assertEquals(listOf("MEDIDA fuera", "MEDIDA vuelve"), written.takeLast(2))
    }

    @Test
    fun aNewRateAndTheGameClockThatRunsAhead() {
        val game = longArrayOf(0L)
        frames(60, millis = 1000.0 / 60, game = game)
        rate = 120f
        frames(541, millis = 1000.0 / 60, ahead = 1.004, game = game)
        assertTrue("MEDIDA tasa 120 Hz" in written)
        // 0,4 % ahead for 9 s, as the Nothing's Godot ran: some 36 ms.
        val real = written.last { it.startsWith("MEDIDA real") }
        assertTrue(real.contains("RELOJ +36 MS") || real.contains("RELOJ +35 MS"), real)
    }

    private companion object {
        const val MILLI = 1_000_000.0
    }
}
