package com.fanguskdev.carofarty.game

import kotlin.math.max

/**
 * Caro's states (PLY-09). [Waiting] until the first tap; [Running] and [Landing] are on the ground; [Jumping] rises while
 * [Falling] goes down; [Fallen] (into a gap) and [Stopped] (out of points) end the run.
 *
 * Waiting → Running; Running or Landing → Jumping (a tap) or Falling (no ground); Jumping → Falling (at the top);
 * Falling → Landing (ground), Jumping (a tap within the coyote time) or Fallen (below the screen); Landing → Running.
 */
enum class CaroState {
    Waiting,
    Running,
    Landing,
    Jumping,
    Falling,
    Fallen,
    Stopped,
    ;

    val grounded: Boolean get() = this == Running || this == Landing
    val over: Boolean get() = this == Fallen || this == Stopped
}

/**
 * Caro's body (PLY-01 to PLY-04): she runs on her own, faster with the distance, and jumps the moment a finger goes down;
 * keeping it down makes the jump higher and longer (ADR 0002). [x] is the centre of her feet in world pixels (it grows
 * for ever, hence a Double) and [y] her feet's height above the bottom edge of the minimum area.
 */
class Caro(private val config: MovementConfig) {
    var x = 0.0
        private set
    var y = 0f
        private set
    var vy = 0f
        private set
    var speed = 0f
        private set
    var state = CaroState.Waiting
        private set

    /** Whether a tap now would jump: on the ground, or just off an edge within the coyote time (PLY-04). */
    val mayStillJump: Boolean get() = state.grounded || (state == CaroState.Falling && coyote > 0f)

    /** Whether a tap in the step of [dt] seconds that comes is the last that would jump: the end of the coyote time. */
    fun lastChanceToJump(dt: Float): Boolean = state == CaroState.Falling && coyote > 0f && coyote <= dt * LAST_STEP_SLACK

    /** How many jumps she has made, and how many came from the coyote time or the jump buffer (PLY-04, for the debug line). */
    var jumps = 0
        private set
    var coyoteJumps = 0
        private set
    var bufferedJumps = 0
        private set

    /** Where she was a step ago, for drawing between two steps. */
    var previousX = 0.0
        private set
    var previousY = 0f
        private set

    /** Where the run began: the distance is counted from here. */
    var startX = 0.0
        private set

    private var holdTime = 0f
    private var coyote = 0f
    private var buffer = 0f
    private var landingTime = 0f
    private var knockback = 0f

    /** Whether the push back of an egg or a fish is going on. */
    val knockedBack: Boolean get() = knockback > 0f

    /** How much of the longest hold her last jump has had, from 0 to 1: the meter of the hint fills with it. */
    val holdShare: Float get() = (holdTime / config.holdWindow).coerceIn(0f, 1f)

    /**
     * How far behind where she would be without the push back she is, in pixels: half of it shows on the screen as her
     * going back, and comes back quickly ([cameraLag]); with [previousLag], the one of the step before, for the drawing.
     */
    var cameraLag = 0f
        private set
    var previousLag = 0f
        private set

    private val halfWidth = config.hitboxWidth / 2

    val left: Double get() = x - halfWidth
    val right: Double get() = x + halfWidth
    val top: Float get() = y + config.hitboxHeight

    /** Metres run since the start (a tile is a metre). */
    val meters: Double get() = (x - startX) / TILE

    /**
     * Puts her standing at ([atX], [atY]), waiting for the first tap, with the run counted from [runStart] (the jump
     * validator places her mid-run, so her speed is the one she would have there).
     */
    fun place(atX: Double, atY: Float, runStart: Double = atX) {
        x = atX
        y = atY
        previousX = atX
        previousY = atY
        startX = runStart
        vy = 0f
        speed = 0f
        holdTime = 0f
        coyote = 0f
        buffer = 0f
        landingTime = 0f
        knockback = 0f
        cameraLag = 0f
        previousLag = 0f
        state = CaroState.Waiting
        jumps = 0
        coyoteJumps = 0
        bufferedJumps = 0
    }

    /** Takes on all of [other]'s state, so that two tries of the validator can go on from the same moment. */
    fun copyFrom(other: Caro) {
        x = other.x
        y = other.y
        vy = other.vy
        speed = other.speed
        state = other.state
        jumps = other.jumps
        coyoteJumps = other.coyoteJumps
        bufferedJumps = other.bufferedJumps
        previousX = other.previousX
        previousY = other.previousY
        startX = other.startX
        holdTime = other.holdTime
        coyote = other.coyote
        buffer = other.buffer
        landingTime = other.landingTime
        knockback = other.knockback
        cameraLag = other.cameraLag
        previousLag = other.previousLag
    }

    /** The first tap: she starts running, without jumping (ADR 0002). */
    fun start() {
        if (state == CaroState.Waiting) state = CaroState.Running
    }

    /** An egg or a fish: she brakes and goes back a little (Fangusk), on the ground or in the air. */
    fun knockBack() {
        if (state != CaroState.Waiting && !state.over) knockback = config.knockbackSeconds
    }

    /** The run is over because she has no points left: she stops where she is. */
    fun stop() {
        if (!state.over) state = CaroState.Stopped
    }

    /**
     * Advances her by [dt] seconds. [pressed] is whether a finger went down since the last step and [held] whether it is
     * still down.
     */
    fun step(
        dt: Float,
        pressed: Boolean,
        held: Boolean,
        terrain: Terrain,
    ) {
        previousX = x
        previousY = y
        if (state == CaroState.Waiting || state.over) return
        if (pressed) buffer = config.jumpBuffer

        run(dt, terrain)
        if (buffer > 0f && (state.grounded || coyote > 0f)) jump(pressedNow = pressed)

        if (state.grounded) {
            stayOnGround(dt, terrain)
        } else {
            fly(dt, held, terrain)
        }
        coyote = max(0f, coyote - dt)
        buffer = max(0f, buffer - dt)
    }

    private fun run(dt: Float, terrain: Terrain) {
        speed = config.speedAt(meters)
        // On a moving platform she runs as on the ground, and the platform carries her along with it (ADR 0009).
        val carry = if (state.grounded) terrain.carryUnder(left, right, y).toFloat() else 0f
        var across = speed + carry
        if (knockback > 0f) {
            // From going back to running again, along the push.
            val done = 1f - knockback / config.knockbackSeconds
            across = carry - config.knockbackSpeed + (speed + config.knockbackSpeed) * done
            knockback = max(0f, knockback - dt)
        }
        previousLag = cameraLag
        cameraLag =
            if (across < speed + carry) {
                cameraLag + (speed + carry - across) * dt * LAG_SHOWN
            } else {
                cameraLag * (1f - dt / LAG_RECOVERY).coerceAtLeast(0f)
            }
        var next = x + across * dt
        if (across >= 0f) {
            val wall = terrain.wallAhead(right, next + halfWidth, y, y + config.hitboxHeight)
            if (wall != null) next = wall - halfWidth.toDouble()
        } else {
            val wall = terrain.wallBehind(left, next - halfWidth, y, y + config.hitboxHeight)
            if (wall != null) next = wall + halfWidth.toDouble()
        }
        x = next
    }

    private fun jump(pressedNow: Boolean) {
        jumps++
        when {
            !state.grounded -> coyoteJumps++
            !pressedNow -> bufferedJumps++
        }
        vy = config.jumpSpeed
        holdTime = 0f
        coyote = 0f
        buffer = 0f
        state = CaroState.Jumping
    }

    private fun stayOnGround(dt: Float, terrain: Terrain) {
        val ground = terrain.supportUnder(left, right, y)
        // A platform going down takes her with it: a little lower is still under her feet.
        if (ground == null || ground < y - STICK) {
            state = CaroState.Falling
            coyote = config.coyoteTime
            vy = 0f
            return
        }
        y = ground
        if (state == CaroState.Landing) {
            landingTime -= dt
            if (landingTime <= 0f) state = CaroState.Running
        }
    }

    private fun fly(dt: Float, held: Boolean, terrain: Terrain) {
        val holding = state == CaroState.Jumping && held && holdTime < config.holdWindow && vy > 0f
        if (holding) holdTime += dt
        vy = max(vy - (if (holding) config.holdGravity else config.gravity) * dt, -config.maxFallSpeed)
        val before = y
        y += vy * dt
        if (vy > 0f) {
            val ceiling = terrain.ceilingAbove(left, right, before + config.hitboxHeight, y + config.hitboxHeight)
            if (ceiling != null) {
                // A block over her head (ADR 0007): she stops going up and falls.
                y = ceiling - config.hitboxHeight
                vy = 0f
            }
        }
        if (vy <= 0f && state == CaroState.Jumping) state = CaroState.Falling
        if (vy <= 0f) {
            val ground = terrain.supportUnder(left, right, before)
            if (ground != null && before >= ground - Course.CORNER_TOLERANCE && y <= ground) land(ground)
        }
        if (y < FALL_LIMIT) state = CaroState.Fallen
    }

    private fun land(ground: Float) {
        y = ground
        vy = 0f
        landingTime = LANDING_TIME
        state = CaroState.Landing
    }

    companion object {
        /** Below this height she has left the screen through a gap. */
        const val FALL_LIMIT = -40f

        /** A step and a little: what is left of the coyote time after the steps before, which add up in floats. */
        private const val LAST_STEP_SLACK = 1.01f

        /** How long the landing lasts, for its animation: she can jump during it. */
        const val LANDING_TIME = 0.1f

        /** How far a moving platform can go down under her in a step and still keep her on it. */
        const val STICK = 2f

        /** The share of the push back that shows on the screen, and how fast the camera catches her up after, in seconds. */
        const val LAG_SHOWN = 0.5f
        const val LAG_RECOVERY = 0.15f
    }
}
