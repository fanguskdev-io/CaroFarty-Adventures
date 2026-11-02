package com.fanguskdev.carofarty.game

import kotlin.math.abs
import kotlin.math.ceil

/**
 * A pilot that plays like a careful person: on the ground, when a gap or something that takes points away is close, it
 * tries every hold length with [JumpLab] from where Caro is and, once two or more land her safely past it, jumps with the
 * middle one. The tests use it to show the course is fair: if it can run for kilometres, it is; if it falls, the generator
 * let an unfair piece through. Debug builds can let it play by itself (TEC-10), so deciding allocates nothing.
 *
 * With [shortest], the way the scene of jumping in how to play shows it: from the edge, as late as it can, and with the
 * shortest hold that clears it, so a narrow gap takes a tap and a wide one a hold, and the jumps fit in the small scene.
 *
 * With [greedy], like a person who wants every animal (GEN-23, ADR 0010): before jumping a huevo frito or a pescado it
 * rescues the animal in front of it first, and then jumps if it still can; and it jumps onto the platforms with an animal
 * on them when it can land there, on a clean spot if there is one and else wherever. It is the player of the tests who
 * pays for the animals that are watched over.
 */
internal class Pilot(private val run: GameRun, private val shortest: Boolean = false, private val greedy: Boolean = false) {
    private val movement = run.config.movement
    private val lab = JumpLab(movement, run.config.items.negativeBox)
    private val maxHold = ceil(movement.holdWindow / JumpLab.STEP).toInt()
    private val good = IntArray(maxHold + 1)
    private val shortOf = IntArray(maxHold + 1)
    private var shortCount = 0
    private val dirtyOf = IntArray(maxHold + 1)
    private var dirtyCount = 0
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
            plannedHold = hold
            pressed = true
            held = hold > 0
        }
    }

    /** How many steps the jump it has just decided keeps the finger down: for a player of the tests who copies it badly. */
    var plannedHold = 0
        private set

    /** Forgets the jump it was holding: whoever uses it as advice jumps their own way. */
    fun forget() {
        holdLeft = 0
    }

    /** How many steps to keep the finger down on a jump that starts now, or -1 to keep running. */
    private fun jump(): Int {
        val caro = run.caro
        // Also just off an edge, within the coyote time: the validator counts those jumps, and so does a person.
        if (!caro.mayStillJump) return -1
        val runway = platformUnder(caro) ?: groundRunway(caro)
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
        if (greedy) {
            // The animal before the obstacle first; then, from where she is, the jump over it if there is still one.
            if (obstacle != null && animalBefore(caro, obstacle.x)) return -1
            val reward = rewardJump(caro)
            if (reward >= 0) return reward
        }
        val target = obstacle?.x ?: runway
        if (target - caro.right > caro.speed * LOOK_AHEAD_SECONDS) return -1
        // She has to land with her whole body past the gap or the obstacle, not on the edge she jumped from.
        val half = movement.hitboxWidth / 2.0
        val past = if (obstacle != null) obstacle.x + Item.SIZE / 2.0 + half else runway + half
        val count = fairHolds(caro, items, past, pressAfter = 0)
        // The last moment to jump from where she runs, or before what she would touch: past it, the coyote time or nothing.
        val edge = if (obstacle != null) obstacle.x - Item.SIZE / 2.0 else runway.toDouble()
        val lastMoment = !caro.state.grounded || caro.right + caro.speed * JumpLab.STEP >= edge
        if (!shortest) {
            if (count >= 2 || (count == 1 && lastMoment)) return good[count / 2]
            // Landing with no room to run up to the next jump only when there is no later moment to do better.
            if (lastMoment && shortCount > 0) return shortOf[shortCount / 2]
            // And right before an egg or a fish she could not jump then, only at the very last: off an edge, at the end of
            // the coyote time, which may still carry her past it (GEN-23).
            val lastChance = if (obstacle != null) lastMoment else caro.lastChanceToJump(JumpLab.STEP)
            return if (lastChance && dirtyCount > 0) dirtyOf[dirtyCount / 2] else -1
        }
        if (count == 0) return if (lastMoment && shortCount > 0) shortOf[0] else -1
        val first = good[0]
        // A step later, if she would still be on this side of the edge and could still clear it, it waits.
        val room = obstacle == null && caro.state.grounded && caro.right + caro.speed * JumpLab.STEP < runway
        return if (room && fairHolds(caro, items, past, pressAfter = 1) > 0) -1 else first
    }

    /** Whether an animal she has not rescued yet is on what she runs on, between her and what takes points away at [x]. */
    private fun animalBefore(caro: Caro, x: Int): Boolean {
        val items = run.course.items
        for (i in items.indices) {
            val item = items[i]
            if (item.taken || !item.kind.adds || item.currentX <= caro.left || item.currentX >= x) continue
            if (abs(item.y - Item.ABOVE_GROUND - caro.y) <= Course.CORNER_TOLERANCE) return true
        }
        return false
    }

    /**
     * The hold of a jump from the ground onto the platform ahead with an animal on it, when it comes close: the middle of
     * those that land on it touching nothing that takes points away or, if none does, of those that land on it anyway;
     * -1 to keep running.
     */
    private fun rewardJump(caro: Caro): Int {
        if (!caro.state.grounded || platformUnder(caro) != null) return -1
        val course = run.course
        val time = course.time
        val platforms = course.platforms
        for (i in platforms.indices) {
            val platform = platforms[i]
            val left = platform.leftAt(time)
            if (platform.rightAt(time) <= caro.right || left - caro.right > caro.speed * REWARD_LOOK_SECONDS) continue
            if (platform.topAt(time) <= caro.y + Course.CORNER_TOLERANCE || !hasAnimal(platform, time)) continue
            var clean = 0
            shortCount = 0
            for (hold in 0..maxHold) {
                val landing = lab.tryJump(course, course.items, caro.x, caro.y, GameRun.START_X, pressAfter = 0, hold = hold)
                if (!landing.landed || abs(landing.top - platform.topAt(landing.time)) >= 1.0) continue
                if (landing.touchedNegative) shortOf[shortCount++] = hold else good[clean++] = hold
            }
            // Before passing under its start, the last chance: then any landing on it.
            val last = left - caro.right <= caro.speed * JumpLab.STEP
            return when {
                clean >= 2 || (clean == 1 && last) -> good[clean / 2]
                last && shortCount > 0 -> shortOf[shortCount / 2]
                else -> -1
            }
        }
        return -1
    }

    /** Whether an animal she has not rescued yet stands on [platform] at [time]. */
    private fun hasAnimal(platform: Platform, time: Double): Boolean {
        val items = run.course.items
        val top = platform.topAt(time)
        for (i in items.indices) {
            val item = items[i]
            if (item.taken || !item.kind.adds || abs(item.y - Item.ABOVE_GROUND - top) > Course.CORNER_TOLERANCE) continue
            if (item.currentX >= platform.leftAt(time) && item.currentX <= platform.rightAt(time)) return true
        }
        return false
    }

    /** Where the grounds she runs on end: the one under any part of her, or the last one she left, and those right after it. */
    private fun groundRunway(caro: Caro): Int {
        val grounds = run.course.grounds
        var runway = -1
        for (i in grounds.indices) {
            val ground = grounds[i]
            if (ground.start >= caro.right) {
                if (runway >= 0 && ground.start == runway && caro.state.grounded) runway = ground.end
                break
            }
            runway = ground.end
        }
        return runway
    }

    /** Where the platform she stands on (or has just run off) ends, now; null if she is not on one (ADR 0009). */
    private fun platformUnder(caro: Caro): Int? {
        val platforms = run.course.platforms
        val time = run.course.time
        for (i in platforms.indices) {
            val platform = platforms[i]
            val onIt = abs(platform.topAt(time) - caro.y) <= Course.CORNER_TOLERANCE
            if (onIt && platform.rightAt(time) > caro.left - COYOTE_ROOM && platform.leftAt(time) < caro.right) {
                return platform.rightAt(time).toInt()
            }
        }
        return null
    }

    /**
     * Where what a landing at [top] around [x] at [time] stands on ends: a run of grounds or a platform; NaN on a block,
     * which she would run off over the gap.
     */
    private fun surfaceEnd(x: Double, top: Int, time: Double): Double {
        val half = movement.hitboxWidth / 2
        val grounds = run.course.grounds
        for (i in grounds.indices) {
            val ground = grounds[i]
            if (ground.top != top || ground.end <= x - half || ground.start >= x + half) continue
            // Grounds at the same height one after the other run on as one.
            var end = ground.end
            for (j in i + 1 until grounds.size) if (grounds[j].start == end && grounds[j].top == top) end = grounds[j].end else break
            return end.toDouble()
        }
        val platforms = run.course.platforms
        for (i in platforms.indices) {
            val platform = platforms[i]
            val over = platform.rightAt(time) > x - half && platform.leftAt(time) < x + half
            if (over && abs(platform.topAt(time) - top) < 1.0) return platform.rightAt(time)
        }
        return Double.NaN
    }

    /**
     * The hold lengths that land her safely [past] the gap or the obstacle, pressing [pressAfter] steps from now, in
     * [good]: on a ground or a platform, like the validator (on top of a block over the gap she would run off its end),
     * and with room to run up to the next jump, as the generator makes sure some do. How many. Those that land safely
     * but without that room, in [shortOf], [shortCount] of them; and those that land before an egg or a fish without the
     * run-up to jump it, in [dirtyOf], [dirtyCount] of them.
     */
    private fun fairHolds(
        caro: Caro,
        items: List<Item>,
        past: Double,
        pressAfter: Int,
    ): Int {
        var count = 0
        shortCount = 0
        dirtyCount = 0
        for (hold in 0..maxHold) {
            val landing = lab.tryJump(run.course, items, caro.x, caro.y, GameRun.START_X, pressAfter = pressAfter, hold = hold)
            if (!landing.landed || landing.touchedNegative || landing.x < past) continue
            val end = surfaceEnd(landing.x, landing.top, landing.time)
            if (end.isNaN()) continue
            // Room to run up to the next jump: to the end of what she lands on and, before an egg or a fish on it, the
            // whole run-up to jump it, as the generator lays them out (GEN-23).
            val room = end - landing.x >= caro.speed * RUN_UP_SECONDS
            val clear = nextNegative(landing.x, landing.top) - landing.x >= caro.speed * OBSTACLE_RUN_UP_SECONDS
            when {
                room && clear -> good[count++] = hold
                clear -> shortOf[shortCount++] = hold
                else -> dirtyOf[dirtyCount++] = hold
            }
        }
        return count
    }

    /** Where her body would touch the first egg or fish on what she lands on at [top], after [x]; infinity if none. */
    private fun nextNegative(x: Double, top: Int): Double {
        val items = run.course.items
        var first = Double.POSITIVE_INFINITY
        for (i in items.indices) {
            val item = items[i]
            if (item.taken || item.kind.adds || item.currentX <= x) continue
            if (abs(item.y - Item.ABOVE_GROUND - top) > Course.CORNER_TOLERANCE) continue
            first = minOf(first, item.currentX - Item.SIZE / 2 - movement.hitboxWidth / 2)
        }
        return first
    }

    private companion object {
        const val LOOK_AHEAD_SECONDS = 0.25f

        /** What a landing should leave ahead to run up to the next jump, in seconds: the generator's runway. */
        const val RUN_UP_SECONDS = 0.2f

        /** And before an egg or a fish on the ground, the generator's run-up to jump it. */
        const val OBSTACLE_RUN_UP_SECONDS = 0.4f

        /** Just off a platform's end she can still jump from it, within the coyote time. */
        const val COYOTE_ROOM = 24.0

        /** How far ahead, in seconds of running, the greedy pilot starts looking for a jump onto a platform with an animal. */
        const val REWARD_LOOK_SECONDS = 0.6f
    }
}
