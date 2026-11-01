package com.fanguskdev.carofarty.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.exp

/**
 * Every number that tunes the game (TEC-07), read from `files/ajustes.json` of the resources so it can change without
 * touching the code. The defaults here are the starting values of docs/movimiento.md and docs/generacion.md; a key
 * missing from the file keeps its default. Units: game pixels (a tile is 16 px and a metre), seconds and tiles.
 */
@Serializable
data class GameConfig(
    val movement: MovementConfig = MovementConfig(),
    val course: CourseConfig = CourseConfig(),
    val score: ScoreConfig = ScoreConfig(),
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** The settings in [text], or the defaults if the file cannot be read: the game always starts. */
        fun parse(text: String): GameConfig = runCatching { json.decodeFromString(serializer(), text) }.getOrDefault(GameConfig())
    }
}

/** How Caro runs and jumps (GDD-05, ADR 0002). */
@Serializable
data class MovementConfig(
    val startSpeed: Float = 120f,
    val maxSpeed: Float = 200f,
    /** How many metres it takes the speed to cover about two thirds of the way from [startSpeed] to [maxSpeed]. */
    val speedRampMeters: Float = 1200f,
    val jumpSpeed: Float = 230f,
    /** The gravity while the finger stays down, the jump is rising and [holdWindow] has not run out. */
    val holdGravity: Float = 300f,
    val holdWindow: Float = 0.36f,
    val gravity: Float = 1600f,
    val maxFallSpeed: Float = 300f,
    val coyoteTime: Float = 0.08f,
    val jumpBuffer: Float = 0.1f,
    /** Caro's collision box, a little smaller than her sprite (CHR-08). */
    val hitboxWidth: Float = 10f,
    val hitboxHeight: Float = 20f,
) {
    /** The running speed after [meters] of the run. */
    fun speedAt(meters: Double): Float = (maxSpeed - (maxSpeed - startSpeed) * exp(-meters / speedRampMeters)).toFloat()
}

/** How the course is generated (GDD-07, ADR 0004). Lengths and heights in tiles. */
@Serializable
data class CourseConfig(
    val startFlatTiles: Int = 30,
    val minGroundTiles: Int = 3,
    val maxGroundTiles: Int = 20,
    /** The ground's top, in tiles above the bottom edge of the minimum area (ADR 0003). */
    val minGroundTop: Int = 2,
    val maxGroundTop: Int = 6,
    val maxStepUp: Int = 2,
    val maxStepDown: Int = 3,
    val minGapTiles: Int = 2,
    /** The widest gap, as a fraction of the longest jump at the speed she will have there: it grows with the distance. */
    val gapFractionStart: Float = 0.45f,
    val gapFractionEnd: Float = 0.75f,
    /** How many metres it takes the difficulty to reach its end values. */
    val difficultyRampMeters: Float = 2500f,
    val restEveryMinMeters: Int = 150,
    val restEveryMaxMeters: Int = 250,
    val restTiles: Int = 12,
    val negativesPer100mStart: Float = 2f,
    val negativesPer100mEnd: Float = 8f,
    val positivesPer100m: Float = 8f,
    /** How far inside the next ground a valid jump has to land. */
    val landingMarginTiles: Int = 1,
    /** How many hold lengths, in steps of the simulation, have to work for a gap to count as fair. */
    val fairHoldSteps: Int = 4,
    val validatorAttempts: Int = 8,
)

/** The points (GDD-10, GDD-13). The values of the collectibles are a proposal until Fangusk confirms them. */
@Serializable
data class ScoreConfig(
    val startPoints: Int = 10,
    val perrito: Int = 1,
    val gatito: Int = 1,
    val huevo: Int = -2,
    val pescado: Int = -3,
)
