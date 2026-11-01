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
        state = CaroState.Waiting
        jumps = 0
        coyoteJumps = 0
        bufferedJumps = 0
    }

    /** The first tap: she starts running, without jumping (ADR 0002). */
    fun start() {
        if (state == CaroState.Waiting) state = CaroState.Running
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
        var next = x + speed * dt
        val wall = terrain.wallAhead(right, next + halfWidth, y, y + config.hitboxHeight)
        if (wall != null) next = wall - halfWidth.toDouble()
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
        if (ground == null || ground < y) {
            state = CaroState.Falling
            coyote = config.coyoteTime
            vy = 0f
            return
        }
        y = ground.toFloat()
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

    private fun land(ground: Int) {
        y = ground.toFloat()
        vy = 0f
        landingTime = LANDING_TIME
        state = CaroState.Landing
    }

    companion object {
        /** Below this height she has left the screen through a gap. */
        const val FALL_LIMIT = -40f

        /** How long the landing lasts, for its animation: she can jump during it. */
        const val LANDING_TIME = 0.1f
    }
}
