package com.fanguskdev.carofarty.game

import kotlin.math.max

/** The three scenarios she chooses from (ADR 0006). In the grey prototype only their colours change. */
enum class Scenario {
    Bosque,
    Nieve,
    Desierto,
}

/** Where a run is: waiting for the first tap, running, or over. */
enum class RunPhase {
    Ready,
    Running,
    Over,
}

/** Why a run ended (GDD-14): a fall into a gap or no points left. */
enum class EndCause {
    Fall,
    NoPoints,
}

/** Something the screen may want to show or play, drained once per frame. */
sealed interface RunEvent {
    data class Picked(val kind: ItemKind, val points: Int) : RunEvent

    data class Ended(val cause: EndCause) : RunEvent
}

/**
 * One game (GDD-02, GDD-13, GDD-14): Caro on a course generated for this [seed] in [scenario], the points she starts with,
 * what she picks up and how it ends. It knows nothing about the screen: it advances by fixed steps with whether a finger
 * went down and whether it is held, and leaves what happened in [events].
 */
class GameRun(
    val config: GameConfig,
    val scenario: Scenario,
    val seed: Long,
    private val pieces: PieceSource = DirectPieces(CourseGenerator(config, seed, runStart = START_X)),
) {
    val caro = Caro(config.movement)
    val course = Course()

    var phase = RunPhase.Ready
        private set
    var points = config.score.startPoints
        private set
    var maxPoints = points
        private set
    var endCause: EndCause? = null
        private set
    val events = ArrayList<RunEvent>()

    /** Metres run, the distance of the record. */
    val meters: Double get() = max(0.0, caro.meters)

    init {
        course.append(pieces.nextOrNull() ?: error("the source of pieces gave no start"))
        caro.place(START_X, course.groundTopUnder(START_X - 1, START_X + 1)?.toFloat() ?: 0f)
        fillAhead(Int.MAX_VALUE)
    }

    /** Advances the run by [dt] seconds. The first tap only starts the run (ADR 0002). */
    fun step(dt: Float, pressed: Boolean, held: Boolean) {
        when (phase) {
            RunPhase.Ready -> {
                if (pressed) {
                    phase = RunPhase.Running
                    caro.start()
                }
                return
            }
            RunPhase.Over -> return
            RunPhase.Running -> Unit
        }
        fillAhead(MAX_PIECES_PER_STEP)
        // If the pieces made in the background ever fell behind, she waits for them rather than run off the course.
        if (course.end - caro.x < MIN_AHEAD) return
        caro.step(dt, pressed, held, course)
        for (i in 0 until course.items.size) course.items[i].walk?.step(dt)
        pickUp()
        course.dropBefore(caro.x - BEHIND)
        if (caro.state == CaroState.Fallen) end(EndCause.Fall)
    }

    /** Stops generating ahead for a run that is left before its end (another scenario, another game). */
    fun close() = pieces.close()

    /** Takes [amount] points away as touching something would, with the same end at 0: a tool of the debug builds (TEC-10). */
    fun takeAway(amount: Int) {
        if (phase == RunPhase.Running) add(-amount)
    }

    private fun pickUp() {
        for (i in 0 until course.items.size) {
            val item = course.items[i]
            if (item.taken || !overlaps(caro, item, config.items.box(item.kind), config.movement)) continue
            item.taken = true
            val value = item.kind.points(config.score)
            events += RunEvent.Picked(item.kind, value)
            if (add(value)) return
        }
    }

    /** Adds [value] to the points, which never go below 0, and ends the run when none are left. Whether it ended. */
    private fun add(value: Int): Boolean {
        points = max(0, points + value)
        maxPoints = max(maxPoints, points)
        if (points > 0) return false
        caro.stop()
        end(EndCause.NoPoints)
        return true
    }

    private fun end(cause: EndCause) {
        if (phase == RunPhase.Over) return
        phase = RunPhase.Over
        endCause = cause
        events += RunEvent.Ended(cause)
        pieces.close()
    }

    /** Generates up to [maxPieces] pieces while the course does not reach two screens ahead (GEN-03). */
    private fun fillAhead(maxPieces: Int) {
        var made = 0
        while (course.end < caro.x + AHEAD && made < maxPieces) {
            course.append(pieces.nextOrNull() ?: return)
            made++
        }
    }

    companion object {
        /** Where she starts, in world pixels: the run is measured from here. */
        const val START_X = 0.0

        /** How far ahead the course is generated (two of the widest screens, ADR 0003) and how far behind it is kept. */
        const val AHEAD = 2 * 448 + 128
        const val BEHIND = 256.0

        /** The least course ahead of her for the run to go on. */
        const val MIN_AHEAD = 256

        /** A step takes at most this many pieces, so no frame pays for many at once. */
        const val MAX_PIECES_PER_STEP = 1
    }
}
