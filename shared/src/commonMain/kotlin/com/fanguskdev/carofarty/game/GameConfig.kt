package com.fanguskdev.carofarty.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.exp
import kotlin.math.max

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
    val hazards: HazardConfig = HazardConfig(),
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
     * [slowRampMeters]. Each ramp covers about two thirds of its way in its metres. And, since the beta 7, on past the first
     * minutes (Fangusk: «la velocidad creo debería de ajustarse a mayor; una partida continua de 30 minutos debería de ser
     * muy difícil»): from [lateFromMeters] on, slowly towards [lateSpeed] over [lateRampMeters], about 550 px/s by the
     * minute 30 and never more than [lateSpeed], its ceiling (ADR 0011, GDD-23).
     */
    val startSpeed: Float = 160f,
    val quickSpeed: Float = 240f,
    val maxSpeed: Float = 360f,
    val quickRampMeters: Float = 300f,
    val slowRampMeters: Float = 2500f,
    val lateSpeed: Float = 560f,
    val lateFromMeters: Float = 3000f,
    val lateRampMeters: Float = 15000f,
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
    /**
     * The push back when she picks up an egg or a fish (Fangusk: «un pequeño retroceso»): for [knockbackSeconds] she brakes
     * from going back at [knockbackSpeed] to her running speed. It is real: if it leaves her without ground, she falls.
     */
    val knockbackSeconds: Float = 0.3f,
    val knockbackSpeed: Float = 40f,
    /**
     * The footings of PLY-18 (ADR 0011): on the ice of the Nieve she runs [iceSpeed] times as fast and keeps it through
     * the jump that takes off from it (Fangusk: «algo más rápida (+10–15 %) y saltos más largos»); on the quicksand of the
     * Desierto she sinks [quicksandSink] pixels and jumps [quicksandJump] times as fast up, about 30 % lower.
     */
    val iceSpeed: Float = 1.12f,
    val quicksandJump: Float = 0.79f,
    val quicksandSink: Int = 3,
) {
    /** The running speed after [meters] of the run. */
    fun speedAt(meters: Double): Float {
        val quick = (quickSpeed - startSpeed) * (1 - exp(-meters / quickRampMeters))
        val slow = (maxSpeed - quickSpeed) * (1 - exp(-meters / slowRampMeters))
        val late = (lateSpeed - maxSpeed) * (1 - exp(-max(0.0, meters - lateFromMeters) / lateRampMeters))
        return (startSpeed + quick + slow + late).toFloat()
    }
}

/** How the course is generated (GDD-07, ADR 0004). Lengths and heights in tiles. */
@Serializable
data class CourseConfig(
    val startFlatTiles: Int = 30,
    val minGroundTiles: Int = 3,
    /** The longest ground (ADR 0009, Fangusk: «suelos más cortos, de 3 a 10 tiles»): shorter grounds, more gaps. */
    val maxGroundTiles: Int = 10,
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
    /** And how many more until what keeps growing after it is full (ADR 0011, GDD-23): about the minute 30. */
    val lateDifficultyMeters: Float = 50000f,
    /**
     * The rhythm (GDD-08): a stretch of tension this many metres long, then a calm one of this many easy pieces, 2 or 3
     * since the beta 6 (GEN-24).
     */
    val tensionMinMeters: Int = 150,
    val tensionMaxMeters: Int = 250,
    val calmMinPieces: Int = 2,
    val calmMaxPieces: Int = 3,
    /**
     * The gaps that only platforms cross (ADR 0009): one of every this many challenges, one of every 2 since the beta 6
     * (GEN-24), with up to this many platforms, more of them with the difficulty.
     */
    val platformEveryMin: Int = 2,
    val platformEveryMax: Int = 2,
    val platformMaxCount: Int = 3,
    /**
     * The moving platforms (ADR 0009): from this speed on, in pixels a second, in this share of the gaps with platforms;
     * since the beta 6, from 225 px/s and in a bit more than half (GEN-24).
     */
    val moverFromSpeed: Float = 225f,
    val moverShare: Float = 0.6f,
    /**
     * The obstacles in the air that Fangusk asked for (ADR 0007): from what difficulty they appear, how often a gap gets
     * one (growing with the difficulty) and what share of them are solid blocks instead of a huevo frito or a pescado.
     */
    val airFromDifficulty: Float = 0.05f,
    val airChanceStart: Float = 0.35f,
    val airChanceEnd: Float = 0.9f,
    val airBlockShare: Float = 0.25f,
    /**
     * How many collectibles that take points away a stretch of tension asks for every 100 m, from the start to full
     * difficulty, besides the ones that watch over the animals: more since the beta 6 (GEN-23, Fangusk: «más huevos fritos
     * y pescado»).
     */
    val negativesPer100mStart: Float = 2.5f,
    val negativesPer100mEnd: Float = 20f,
    /**
     * How many groups of perritos and gatitos the gaps of a stretch of tension ask for every 100 m, at the start and at full
     * difficulty (ADR 0011, Fangusk: «hay muy pocos huevos y pescados en comparación a animalitos»; «quiero que sea un
     * riesgo real quedarse sin puntos»): fewer at the start since the beta 8, and by full difficulty shorter rows, watched
     * over.
     */
    val positivesPer100m: Float = 2f,
    val positivesPer100mEnd: Float = 3.15f,
    /**
     * The share of the calm pieces with perritos and gatitos, at the start and at full difficulty, and with it of the
     * platforms of the gaps that only platforms cross and of the ground after them (ADR 0011).
     */
    val calmPositivesShare: Float = 0.6f,
    val calmPositivesShareEnd: Float = 0.15f,
    /**
     * Of the groups of perritos and gatitos, the share that goes on a platform with a reward instead of a row (ADR 0008),
     * at the start and at full difficulty: fewer by then since the beta 8 (ADR 0011).
     */
    val rewardPlatformShare: Float = 0.35f,
    val rewardPlatformShareEnd: Float = 0.1f,
    /**
     * The animals watched over (GEN-23, ADR 0010): the share of the groups of perritos and gatitos of the stretches of
     * tension, a row or a platform with a reward, where one gets a huevo frito or a pescado by it, from the start to full
     * difficulty, so that it comes to one of every 4 animals or so; and the share of those on the ground that leave no way
     * to rescue the animal without touching it, a dilemma; the rest ask for skill.
     */
    val guardedShareStart: Float = 0.05f,
    val guardedShareEnd: Float = 1f,
    val guardedDilemmaShare: Float = 0.8f,
    /** How far inside the next ground a valid jump has to land. */
    val landingMarginTiles: Int = 1,
    /** How many hold lengths, in steps of the simulation, have to work for a gap to count as fair. */
    val fairHoldSteps: Int = 4,
    val validatorAttempts: Int = 8,
)

/**
 * The points (GDD-10, GDD-13). Fangusk accepted the values of the collectibles on 2026-11-01 and, after the beta 7, chose
 * to start with 5 points and harder takes (ADR 0011): the egg takes 3 and the fish 5; and the birds, 8 (ENM-18).
 */
@Serializable
data class ScoreConfig(
    val startPoints: Int = 5,
    val perrito: Int = 1,
    val gatito: Int = 1,
    val huevo: Int = -3,
    val pescado: Int = -5,
    val pajaro: Int = -8,
    /** With this many points or fewer the marker turns red, a warning before losing (guía 1, principio 8). */
    val warnAtPoints: Int = 3,
)

/**
 * The new dangers Fangusk chose after the beta 7 (ADR 0011), laid out by the generator and tried by the validator like the
 * rest. For each, from what difficulty it comes and how often where it fits: at that difficulty, at full difficulty and
 * towards the minute 30; and how it moves.
 */
@Serializable
data class HazardConfig(
    /**
     * The eggs and the fish in the air that float (ENM-17, «suben y bajan de 8 a 16 px cada 1,5 s más o menos»): of those
     * on the way of a jump, this share; going up and down this many pixels in all, a round every [floatPeriod] seconds.
     */
    val floatFromDifficulty: Float = 0.2f,
    val floatShare: Float = 0.25f,
    val floatShareEnd: Float = 0.6f,
    val floatShareLate: Float = 0.8f,
    val floatTravelMin: Int = 8,
    val floatTravelMax: Int = 16,
    val floatPeriod: Float = 1.5f,
    /**
     * The birds (ENM-18; Fangusk: «desde dificultad media y cada vez más a menudo»), over this share of the gaps: of them,
     * this share stroll through the air and the rest fly at her, this many pixels a second faster than the ground, and
     * this share of those wave; they take [ScoreConfig.pajaro].
     */
    val birdFromDifficulty: Float = 0.5f,
    val birdShare: Float = 0.08f,
    val birdShareEnd: Float = 0.2f,
    val birdShareLate: Float = 0.4f,
    val birdStrollShare: Float = 0.6f,
    val birdMinSpeed: Float = 80f,
    val birdMaxSpeed: Float = 140f,
    val birdWaveShare: Float = 0.5f,
    /** The fish that leap out of the water (ENM-15), in this share of the gaps of the Bosque and the Nieve. */
    val leapFromDifficulty: Float = 0.25f,
    val leapShare: Float = 0.25f,
    val leapShareEnd: Float = 0.45f,
    val leapShareLate: Float = 0.65f,
    /** The platforms that fall (ENM-16), this share of those that stand still; they tremble this long and fall. */
    val fallFromDifficulty: Float = 0.2f,
    val fallShare: Float = 0.2f,
    val fallShareEnd: Float = 0.4f,
    val fallShareLate: Float = 0.6f,
    val trembleSeconds: Float = 0.3f,
    /**
     * The ice of the Nieve and the quicksand of the Desierto (PLY-18), at the end of this share of the grounds of the
     * gaps, before the next one (Fangusk: «antes de barrancos o bloques»); how they move her is in [MovementConfig].
     */
    val patchFromDifficulty: Float = 0.15f,
    val patchShare: Float = 0.15f,
    val patchShareEnd: Float = 0.3f,
    val patchShareLate: Float = 0.45f,
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
    /** How long Caro's face shows what she picked up (CHR-07): happy for a rescued animal, disgusted for food, in seconds. */
    val reactionSeconds: Float = 0.4f,
    /**
     * After an egg or a fish she blinks this long (Fangusk: «parpadeando durante un corto periodo de tiempo»), and in it
     * the eggs and the fish do not take points away; the animals she still rescues.
     */
    val blinkSeconds: Float = 1f,
) {
    fun box(kind: ItemKind): Int = if (kind.adds) positiveBox else negativeBox
}
