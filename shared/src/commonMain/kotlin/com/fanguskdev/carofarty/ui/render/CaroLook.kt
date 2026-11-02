package com.fanguskdev.carofarty.ui.render

import com.fanguskdev.carofarty.game.CaroState
import com.fanguskdev.carofarty.game.Course
import com.fanguskdev.carofarty.game.GameRun
import com.fanguskdev.carofarty.game.RunPhase
import com.fanguskdev.carofarty.game.TILE
import kotlin.math.min

/**
 * Which of Caro's animations shows and how far into it (CHR-02 to CHR-06, docs/arte.md), stepped with the run so it looks
 * the same at any frame rate. Her stride follows the ground she covers: a cycle of corre every [runCycleTiles] tiles and
 * of corre-rapido, from [FAST_SPEED], every [fastCycleTiles], never faster than [MIN_FRAME_SECONDS] a frame. [runFrames]
 * and [fastFrames] are how many frames those two animations have; the art can say how many tiles each cycle covers.
 */
internal class CaroLook(
    private val runFrames: Int = DEFAULT_FRAMES,
    private val fastFrames: Int = DEFAULT_FRAMES,
    runCycleTiles: Float? = null,
    fastCycleTiles: Float? = null,
) {
    private val runCycleTiles = runCycleTiles ?: RUN_CYCLE_TILES
    private val fastCycleTiles = fastCycleTiles ?: FAST_CYCLE_TILES

    var pose = CaroPose.Espera
        private set
    private var seconds = 0f
    private var stride = 0.0

    /** Advances her look by [dt] seconds of [run]; [newRecord] says whether the run that ended beat a record. */
    fun step(dt: Float, run: GameRun, newRecord: Boolean) {
        val next = poseFor(run, newRecord)
        if (next == pose) {
            seconds += dt
        } else {
            pose = next
            seconds = 0f
        }
        if (pose.byDistance) {
            val fast = pose == CaroPose.CorreRapido
            val frames = if (fast) fastFrames else runFrames
            val pixelsPerFrame = (if (fast) fastCycleTiles else runCycleTiles) * TILE.toDouble() / frames
            stride += min(run.caro.speed * dt / pixelsPerFrame, dt / MIN_FRAME_SECONDS)
        }
    }

    /** The frame of [animation], the one of her pose, to draw now. */
    fun frame(animation: Animation): Int =
        if (pose.byDistance) animation.frame(stride.toInt()) else animation.frameAt((seconds * MILLIS).toLong(), pose.loops)

    private fun poseFor(run: GameRun, newRecord: Boolean): CaroPose {
        val caro = run.caro
        return when {
            run.phase == RunPhase.Ready -> CaroPose.Espera
            // Out of points she is sad; with a record, once she has been sad a moment, she celebrates it.
            caro.state == CaroState.Stopped -> {
                val celebrating = pose == CaroPose.Record || (pose == CaroPose.SinPuntos && seconds >= SAD_SECONDS)
                if (newRecord && celebrating) CaroPose.Record else CaroPose.SinPuntos
            }
            caro.state == CaroState.Fallen || (!caro.state.grounded && intoTheGap(run)) -> CaroPose.AlBarranco
            caro.knockedBack && caro.state.grounded -> CaroPose.Retroceso
            caro.state == CaroState.Landing -> CaroPose.Aterriza
            caro.state.grounded -> if (caro.speed >= FAST_SPEED) CaroPose.CorreRapido else CaroPose.Corre
            caro.vy > APEX_SPEED -> CaroPose.Salta
            // The top of a jump; walking off a ledge she just falls.
            caro.vy >= -APEX_SPEED && (pose == CaroPose.Salta || pose == CaroPose.Apice) -> CaroPose.Apice
            else -> CaroPose.Cae
        }
    }

    /**
     * Whether she is falling where nothing can catch her any more: over a gap and below the top of the next ground, whose
     * side would only let her slide down.
     */
    private fun intoTheGap(run: GameRun): Boolean {
        val caro = run.caro
        if (caro.vy > 0f) return false
        val grounds = run.course.grounds
        for (i in 0 until grounds.size) {
            val ground = grounds[i]
            if (ground.end <= caro.left) continue
            if (ground.start < caro.right) return false
            return caro.y < ground.top - Course.CORNER_TOLERANCE
        }
        return true
    }

    private companion object {
        const val DEFAULT_FRAMES = 6
        const val RUN_CYCLE_TILES = 3f
        const val FAST_CYCLE_TILES = 5f
        const val FAST_SPEED = 240f
        const val MIN_FRAME_SECONDS = 0.04

        /** How slowly she rises or falls at the top of a jump, in pixels a second: the apice (GameMaster: 45 to 60). */
        const val APEX_SPEED = 60f

        /** How long she is sad out of points before celebrating a record: the four frames of sin-puntos. */
        const val SAD_SECONDS = 0.6f
        const val MILLIS = 1000
    }
}
