package com.fanguskdev.carofarty.game

import kotlin.math.ceil

/**
 * A pilot for the tests that plays like a careful person: on the ground, when a gap or something that takes points away is
 * close, it tries every hold length with [JumpLab] from where Caro is and, once two or more land her safely past it, jumps
 * with the middle one. If it can run for kilometres, the course is fair; if it falls, the generator let an unfair piece
 * through.
 */
internal class Pilot(private val run: GameRun) {
    private val movement = run.config.movement
    private val lab = JumpLab(movement)
    private val maxHold = ceil(movement.holdWindow / JumpLab.STEP).toInt()
    private var holdLeft = 0

    /** Plays for up to [seconds] of game time, or until the run ends. */
    fun play(seconds: Float) {
        repeat((seconds / JumpLab.STEP).toInt()) {
            if (run.phase == RunPhase.Over) return
            step()
        }
    }

    fun step() {
        if (run.phase == RunPhase.Ready) {
            run.step(JumpLab.STEP, pressed = true, held = false)
            return
        }
        if (holdLeft > 0) {
            holdLeft--
            run.step(JumpLab.STEP, pressed = false, held = true)
            return
        }
        val hold = decide()
        if (hold != null) {
            holdLeft = hold - 1
            run.step(JumpLab.STEP, pressed = true, held = hold > 0)
        } else {
            run.step(JumpLab.STEP, pressed = false, held = false)
        }
    }

    private fun decide(): Int? {
        val caro = run.caro
        if (!caro.state.grounded) return null
        val ground = run.course.grounds.firstOrNull { it.start <= caro.x && caro.x < it.end } ?: return null
        val runway = run.course.grounds.firstOrNull { it.start == ground.end }?.end ?: ground.end
        val obstacle = run.course.items.firstOrNull { !it.taken && !it.kind.adds && it.x > caro.right && it.x < runway }
        val target = obstacle?.x?.toDouble() ?: runway.toDouble()
        val distance = target - caro.right
        if (distance > caro.speed * LOOK_AHEAD_SECONDS) return null
        // She has to land with her whole body past the gap or the obstacle, not on the edge she jumped from.
        val half = movement.hitboxWidth / 2.0
        val past = if (obstacle != null) obstacle.x + Item.SIZE / 2.0 + half else runway + half
        val items = run.course.items.filter { !it.taken }
        val good = (0..maxHold).filter { hold ->
            val landing = lab.tryJump(run.course, items, caro.x, caro.y, GameRun.START_X, pressAfter = 0, hold = hold)
            landing.landed && !landing.touchedNegative && landing.x >= past
        }
        return if (good.size >= 2) good[good.size / 2] else null
    }

    private companion object {
        const val LOOK_AHEAD_SECONDS = 0.25f
    }
}
