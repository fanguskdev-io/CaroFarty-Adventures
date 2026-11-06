package com.fanguskdev.carofarty

import kotlin.math.floor
import kotlin.math.max

/**
 * The log of the app that measures, for the benchmark against CaroFarty G (GOD-27, GameMaster's
 * benchmark-1.0-vs-godot.md): the same lines that CaroFarty G's measure leaves (its juego/juego.gd and
 * docs/rendimiento.md), so that one script reads both. The game tells it of each frame and of the moments the benchmark
 * times; [now] is the phone's monotonic clock in nanoseconds, SurfaceFlinger's (System.nanoTime on Android),
 * [refreshRate] the screen's rate, and [write] where each line goes.
 *
 * - Every 10 s: «MEDIDA» and the debugging lines, joined by « | »; and «MEDIDA real», the frames by the phone's own
 *   clock: how many a second and the longest since the last line, the share of the slow ones since the app opened, how
 *   far the game's clock has gone from the phone's (+, ahead), the steps the run waited for its pieces and the rate.
 * - Each jump and landing, and each time the run's sound pauses and goes on, with the moment its frame began.
 * - At once: a stop (a frame of more than 100 ms), the app going away and coming back, and a new rate.
 *
 * Nothing of it changes the game: only the app that measures has one.
 */
class MeasureLog(private val now: () -> Long, private val refreshRate: () -> Float, private val write: (String) -> Unit) {
    private var measuredAt = now()
    private var started = false

    /** When this frame began, by the phone's clock; and where the game's clock is. */
    private var frameClock = 0L
    private var gameNanos = 0L

    /**
     * The frames by the phone's own clock, in nanoseconds: when the first, the last and the last line's began, how many
     * and how many slow, how many and the longest since that line; the game's clock at the first, and the screen's rate.
     */
    private var realFirst = 0L
    private var realLast = 0L
    private var realLineAt = 0L
    private var realFrames = 0
    private var realSlow = 0
    private var realLineFrames = 0
    private var realWorst = 0L
    private var nanosFirst = 0L
    private var rate = 0L

    /**
     * A frame begins, [nanos] on the game's clock, the one it goes by; with the debugging [lines] as they are and the
     * steps the run has waited for its pieces ([pieceWaits]).
     */
    fun frame(nanos: Long, lines: Array<String>, pieceWaits: Int) {
        val at = now()
        frameClock = at
        gameNanos = nanos
        if (!started) {
            started = true
            realFirst = at
            realLineAt = at
            nanosFirst = nanos
        } else {
            val frame = at - realLast
            realFrames++
            realLineFrames++
            realWorst = max(realWorst, frame)
            if (frame > SLOW_NANOS) realSlow++
            if (frame > STOP_NANOS) write("MEDIDA parón ${rounded(frame / NANOS_IN_MILLI)} ms")
        }
        realLast = at
        if (realFrames % RATE_FRAMES == 0) {
            val rate = rounded(refreshRate().toDouble())
            if (rate != this.rate) {
                this.rate = rate
                write("MEDIDA tasa $rate Hz")
            }
        }
        if (at >= measuredAt + EVERY_NANOS) {
            measuredAt = at
            write("MEDIDA " + lines.joinToString(" | "))
            write("MEDIDA real " + realLine(pieceWaits))
        }
    }

    /** Something the benchmark times happened in this frame: [name] is «salto», «aterrizaje», «pausa» or «sigue». */
    fun moment(name: String) {
        write("MEDIDA $name $frameClock")
    }

    /** The app went away, or came back. */
    fun away(away: Boolean) {
        write(if (away) "MEDIDA fuera" else "MEDIDA vuelve")
    }

    private fun realLine(pieceWaits: Int): String {
        val seconds = max((realLast - realLineAt) / NANOS_IN_SECOND, MIN_SECONDS)
        val share = PERCENT * realSlow / max(realFrames, 1)
        val ahead = rounded((gameNanos - nanosFirst) / NANOS_IN_MILLI - (realLast - realFirst) / NANOS_IN_MILLI)
        val sign = if (ahead >= 0) "+" else ""
        val line = "FPS ${rounded(realLineFrames / seconds)} · PEOR ${rounded(realWorst / NANOS_IN_MILLI)} MS · " +
            "LENTOS ${tenths(share)} % · RELOJ $sign$ahead MS · ESPERAS $pieceWaits · $rate HZ"
        realLineAt = realLast
        realLineFrames = 0
        realWorst = 0L
        return line
    }

    private companion object {
        const val EVERY_NANOS = 10_000_000_000L

        /** A slow frame and a stop, as CaroFarty G's measure counts them; and how often it looks at the rate, in frames. */
        const val SLOW_NANOS = 25_000_000L
        const val STOP_NANOS = 100_000_000L
        const val RATE_FRAMES = 60
        const val NANOS_IN_MILLI = 1e6
        const val NANOS_IN_SECOND = 1e9
        const val MIN_SECONDS = 1e-6
        const val PERCENT = 100.0
        const val TENTHS = 10

        /** [value] rounded to the nearest, halves away from zero, as Godot's roundi. */
        fun rounded(value: Double): Long = if (value < 0) -floor(-value + HALF).toLong() else floor(value + HALF).toLong()

        const val HALF = 0.5

        /** [value] with one decimal and a comma, as Spanish writes it. */
        fun tenths(value: Double): String {
            val tenths = rounded(value * TENTHS)
            return "${tenths / TENTHS},${tenths % TENTHS}"
        }
    }
}
