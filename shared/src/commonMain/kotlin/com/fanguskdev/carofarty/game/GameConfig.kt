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
    val items: ItemConfig = ItemConfig(),
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
    /**
     * The speed rises the whole run, which is where the difficulty comes from (Fangusk, 2026-11-01: «se hace lento, parece
     * un paseo, estaría mejor que la velocidad fuese subiendo conforme avanza»): quickly from [startSpeed] towards
     * [quickSpeed] in the first [quickRampMeters], and from there on slower and slower towards [maxSpeed], over
     * [slowRampMeters]. Each ramp covers about two thirds of its way in its metres.
     */
    val startSpeed: Float = 160f,
    val quickSpeed: Float = 240f,
    val maxSpeed: Float = 360f,
    val quickRampMeters: Float = 300f,
    val slowRampMeters: Float = 2500f,
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
    fun speedAt(meters: Double): Float {
        val quick = (quickSpeed - startSpeed) * (1 - exp(-meters / quickRampMeters))
        val slow = (maxSpeed - quickSpeed) * (1 - exp(-meters / slowRampMeters))
        return (startSpeed + quick + slow).toFloat()
    }
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
    /** How many metres it takes the difficulty to reach its end values: in the first minute it barely moves. */
    val difficultyRampMeters: Float = 3000f,
    /** The rhythm (GDD-08): a stretch of tension this many metres long, then a calm one of this many easy pieces. */
    val tensionMinMeters: Int = 150,
    val tensionMaxMeters: Int = 250,
    val calmMinPieces: Int = 3,
    val calmMaxPieces: Int = 8,
    /**
     * The obstacles in the air that Fangusk asked for (ADR 0007): from what difficulty they appear, how often a gap gets
     * one (growing with the difficulty) and what share of them are solid blocks instead of a huevo frito or a pescado.
     */
    val airFromDifficulty: Float = 0.05f,
    val airChanceStart: Float = 0.35f,
    val airChanceEnd: Float = 0.9f,
    val airBlockShare: Float = 0.5f,
    val negativesPer100mStart: Float = 2f,
    val negativesPer100mEnd: Float = 8f,
    val positivesPer100m: Float = 8f,
    /** How far inside the next ground a valid jump has to land. */
    val landingMarginTiles: Int = 1,
    /** How many hold lengths, in steps of the simulation, have to work for a gap to count as fair. */
    val fairHoldSteps: Int = 4,
    val validatorAttempts: Int = 8,
)

/** The points (GDD-10, GDD-13). Fangusk accepted the values of the collectibles on 2026-11-01. */
@Serializable
data class ScoreConfig(
    val startPoints: Int = 10,
    val perrito: Int = 1,
    val gatito: Int = 1,
    val huevo: Int = -2,
    val pescado: Int = -3,
    /** With this many points or fewer the marker turns red, a warning before losing (guía 1, principio 8). */
    val warnAtPoints: Int = 3,
)

/** The collectibles' collision boxes, in pixels (CHR-08): generous for what adds points, strict for what takes them away. */
@Serializable
data class ItemConfig(
    val positiveBox: Int = 16,
    val negativeBox: Int = 10,
    /**
     * The perritos on the ground walk (Fangusk, ADR 0008): how fast, how many never do, how many tiles from their place
     * they go at most, and how long they walk and stop each time, in seconds.
     */
    val perritoWalkSpeed: Float = 24f,
    val perritoStillShare: Float = 0.3f,
    val perritoRangeTiles: Int = 2,
    val perritoWalkMin: Float = 1f,
    val perritoWalkMax: Float = 3f,
    val perritoStopMin: Float = 0.5f,
    val perritoStopMax: Float = 1.5f,
) {
    fun box(kind: ItemKind): Int = if (kind.adds) positiveBox else negativeBox
}
