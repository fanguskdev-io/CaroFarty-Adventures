package com.fanguskdev.carofarty.game

import kotlin.math.ceil

/**
 * A pilot that plays like a careful person: on the ground, when a gap or something that takes points away is close, it
 * tries every hold length with [JumpLab] from where Caro is and, once two or more land her safely past it, jumps with the
 * middle one. The tests use it to show the course is fair: if it can run for kilometres, it is; if it falls, the generator
 * let an unfair piece through. Debug builds can let it play by itself (TEC-10), so deciding allocates nothing.
 *
 * With [shortest], the way the scene of jumping in how to play shows it: from the edge, as late as it can, and with the
 * shortest hold that clears it, so a narrow gap takes a tap and a wide one a hold, and the jumps fit in the small scene.
 */
internal class Pilot(private val run: GameRun, private val shortest: Boolean = false) {
    private val movement = run.config.movement
    private val lab = JumpLab(movement, run.config.items.negativeBox)
    private val maxHold = ceil(movement.holdWindow / JumpLab.STEP).toInt()
    private val good = IntArray(maxHold + 1)
    private var holdLeft = 0

    /** The finger it decided for the next step: whether it has just gone down, and whether it is down. */
    var pressed = false
        private set
    var held = false
        private set

    /** Plays for up to [seconds] of game time, or until the run ends. */
    fun play(seconds: Float) {
        repeat((seconds / JumpLab.STEP).toInt()) {
            if (run.phase == RunPhase.Over) return
            step()
        }
    }

    fun step() {
        decide()
        run.step(JumpLab.STEP, pressed, held)
    }

    /** Decides the finger for the next step of the run, leaving the run as it is. */
    fun decide() {
        pressed = false
        held = false
        if (run.phase == RunPhase.Ready) {
            pressed = true
            return
        }
        if (holdLeft > 0) {
            holdLeft--
            held = true
            return
        }
        val hold = jump()
        if (hold >= 0) {
            holdLeft = hold - 1
            pressed = true
            held = hold > 0
        }
    }

    /** How many steps to keep the finger down on a jump that starts now, or -1 to keep running. */
    private fun jump(): Int {
        val caro = run.caro
        // Also just off an edge, within the coyote time: the validator counts those jumps, and so does a person.
        if (!caro.mayStillJump) return -1
        val grounds = run.course.grounds
        var runway = -1
        for (i in grounds.indices) {
            val ground = grounds[i]
            if (ground.start >= caro.right) {
                if (runway >= 0 && ground.start == runway && caro.state.grounded) runway = ground.end
                break
            }
            // The ground under any part of her, or the last one she left.
            runway = ground.end
        }
        if (runway < 0) return -1
        val items = run.course.items
        var obstacle: Item? = null
        for (i in items.indices) {
            val item = items[i]
            if (!item.taken && !item.kind.adds && item.x > caro.right && item.x < runway) {
                obstacle = item
                break
            }
        }
        val target = obstacle?.x ?: runway
        if (target - caro.right > caro.speed * LOOK_AHEAD_SECONDS) return -1
        // She has to land with her whole body past the gap or the obstacle, not on the edge she jumped from.
        val half = movement.hitboxWidth / 2.0
        val past = if (obstacle != null) obstacle.x + Item.SIZE / 2.0 + half else runway + half
        val count = fairHolds(caro, items, past, pressAfter = 0)
        if (!shortest) return if (count >= 2) good[count / 2] else -1
        if (count == 0) return -1
        val first = good[0]
        // A step later, if she would still be on this side of the edge and could still clear it, it waits.
        val room = obstacle == null && caro.state.grounded && caro.right + caro.speed * JumpLab.STEP < runway
        return if (room && fairHolds(caro, items, past, pressAfter = 1) > 0) -1 else first
    }

    /** How many hold lengths land her safely [past] the gap or the obstacle, pressing [pressAfter] steps from now: in [good]. */
    private fun fairHolds(
        caro: Caro,
        items: List<Item>,
        past: Double,
        pressAfter: Int,
    ): Int {
        val half = movement.hitboxWidth / 2.0
        var count = 0
        for (hold in 0..maxHold) {
            val landing = lab.tryJump(run.course, items, caro.x, caro.y, GameRun.START_X, pressAfter = pressAfter, hold = hold)
            // On a ground, like the validator: on top of a block over the gap she would run off its end.
            val onAGround = run.course.groundTopUnder(landing.x - half, landing.x + half) == landing.top
            if (landing.landed && !landing.touchedNegative && landing.x >= past && onAGround) good[count++] = hold
        }
        return count
    }

    private companion object {
        const val LOOK_AHEAD_SECONDS = 0.25f
    }
}
