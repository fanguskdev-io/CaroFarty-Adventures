package com.fanguskdev.carofarty.game

import kotlin.math.ceil

/**
 * A pilot that plays like a careful person: on the ground, when a gap or something that takes points away is close, it
 * tries every hold length with [JumpLab] from where Caro is and, once two or more land her safely past it, jumps with the
 * middle one. The tests use it to show the course is fair: if it can run for kilometres, it is; if it falls, the generator
 * let an unfair piece through. Debug builds can let it play by itself (TEC-10), so deciding allocates nothing.
 */
internal class Pilot(private val run: GameRun) {
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
        if (!caro.state.grounded) return -1
        val grounds = run.course.grounds
        var runway = -1
        for (i in grounds.indices) {
            val ground = grounds[i]
            if (runway < 0 && ground.start <= caro.x && caro.x < ground.end) {
                runway = ground.end
            } else if (runway >= 0 && ground.start == runway) {
                runway = ground.end
                break
            } else if (runway >= 0) {
                break
            }
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
        var count = 0
        for (hold in 0..maxHold) {
            val landing = lab.tryJump(run.course, items, caro.x, caro.y, GameRun.START_X, pressAfter = 0, hold = hold)
            if (landing.landed && !landing.touchedNegative && landing.x >= past) good[count++] = hold
        }
        return if (count >= 2) good[count / 2] else -1
    }

    private companion object {
        const val LOOK_AHEAD_SECONDS = 0.25f
    }
}
