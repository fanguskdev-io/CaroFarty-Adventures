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

/** What Caro's face shows for a moment after picking something up (CHR-07). */
enum class Reaction {
    None,
    Happy,
    Disgusted,
}

/** Something the screen may want to show or play, drained once per frame. */
sealed interface RunEvent {
    /** She picked up [kind], worth [points], whose centre was at ([x], [y]) in the world: where its effect plays (VFX-03). */
    data class Picked(
        val kind: ItemKind,
        val points: Int,
        val x: Double = 0.0,
        val y: Double = 0.0,
    ) : RunEvent

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
    private val pieces: PieceSource = DirectPieces(CourseGenerator(config, seed, runStart = START_X, scenario)),
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

    /** Her face after the last thing she picked up: happy for a rescued animal, disgusted for food, for a moment. */
    var reaction = Reaction.None
        private set
    private var reactionTime = 0f

    /** How many animals she has rescued in this run, for the end (Fangusk: «salvando el mundo, patita a patita»). */
    var rescued = 0
        private set

    /** What took her points away the last time: the end says something else when a bird took the last ones. */
    var lastTaken: ItemKind? = null
        private set

    /** The seconds left of her blinking after an egg or a fish, in which eggs and fish do not take points away. */
    var blinking = 0f
        private set

    /** Metres run, the distance of the record. */
    val meters: Double get() = max(0.0, caro.meters)

    init {
        course.append(pieces.nextOrNull() ?: error("the source of pieces gave no start"))
        caro.place(START_X, course.groundTopUnder(START_X - 1, START_X + 1)?.toFloat() ?: 0f)
        course.reach = START_X
        course.previousReach = START_X
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
        // The moving platforms go on with the run (ADR 0009), before she moves on them.
        course.time += dt
        caro.step(dt, pressed, held, course)
        // How far she has got: what moves the collectibles that fly (Flight); a push back does not move them back.
        course.previousReach = course.reach
        course.reach = max(course.reach, caro.x)
        for (i in 0 until course.items.size) course.items[i].walk?.step(dt)
        reactionTime -= dt
        if (reactionTime <= 0f) reaction = Reaction.None
        blinking = max(0f, blinking - dt)
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
            if (item.taken || !overlaps(caro, item, config.items.box(item.kind), config.movement, course.reach)) continue
            // Blinking, she goes through eggs and fish; the animals she still rescues.
            if (!item.kind.adds && blinking > 0f) continue
            item.taken = true
            val value = item.kind.points(config.score)
            events += RunEvent.Picked(item.kind, value, item.xAt(course.reach), item.yAt(course.reach))
            reaction = if (item.kind.adds) Reaction.Happy else Reaction.Disgusted
            reactionTime = config.items.reactionSeconds
            if (item.kind.adds) rescued++ else lastTaken = item.kind
            if (!item.kind.adds) {
                caro.knockBack()
                blinking = config.items.blinkSeconds
            }
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
