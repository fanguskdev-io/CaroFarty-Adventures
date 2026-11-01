package com.fanguskdev.carofarty.game

import java.io.File
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The level metrics (GDD-06) and the difficulty curve (GDD-08), measured with the game's own jump. For each distance: the
 * speed, the difficulty, the widest gap the generator asks for, the widest that is fair by the validator's rule and the
 * widest that can be crossed at all, at the same height, two tiles up and three down. Writes `build/metricas/metricas.md`
 * and the curve, `build/metricas/curva-dificultad.svg`, which docs/metricas.md copies.
 */
class MetricsReportTest {
    private val config = GameConfig()
    private val movement = config.movement
    private val course = config.course
    private val lab = JumpLab(movement)
    private val generator = CourseGenerator(config, seed = 0)
    private val maxHold = ceil(movement.holdWindow / JumpLab.STEP).toInt()
    private val out = File("build/metricas").apply { mkdirs() }

    @Test
    fun theGeneratorNeverAsksForAGapWiderThanAFairOne() {
        val rows = DISTANCES.map { measure(it) }
        for (row in rows) assertTrue(row.asked <= row.fair[0], "a ${row.meters} m pide ${row.asked} tiles y lo justo es ${row.fair[0]}")
        out.resolve("metricas.md").writeText(table(rows))
        out.resolve("curva-dificultad.svg").writeText(curve())
    }

    @Test
    fun moreThingsThatTakePointsAwayAsTheRunGoesOn() {
        val counts = Array(BANDS.size) { IntArray(ItemKind.entries.size) }
        for (seed in 1L..SEEDS) {
            val generator = CourseGenerator(config, seed)
            var end = 0
            while (end < BANDS.last().last * TILE) {
                val piece = generator.next()
                for (item in piece.items) {
                    val band = BANDS.indexOfFirst { item.x / TILE in it }
                    if (band >= 0) counts[band][item.kind.ordinal]++
                }
                end = piece.end
            }
        }
        fun per100m(band: Int, kinds: List<ItemKind>): Float =
            kinds.sumOf { counts[band][it.ordinal] } * 100f / SEEDS / (BANDS[band].last - BANDS[band].first)
        val adding = ItemKind.entries.filter { it.adds }
        val taking = ItemKind.entries.filter { !it.adds }
        assertTrue(per100m(BANDS.lastIndex, taking) > per100m(0, taking), "the things that take points away should grow")
        assertTrue(BANDS.indices.all { per100m(it, adding) > 0f }, "there should be perritos and gatitos everywhere")

        val text = buildString {
            appendLine("| Tramo | Perritos | Gatitos | Huevos fritos | Pescados | Suman | Restan |")
            appendLine("|---|---|---|---|---|---|---|")
            for (band in BANDS.indices) {
                append("| ${BANDS[band].first} a ${BANDS[band].last} m | ")
                append(ItemKind.entries.joinToString(" | ") { format(per100m(band, listOf(it))) })
                appendLine(" | ${format(per100m(band, adding))} | ${format(per100m(band, taking))} |")
            }
        }
        out.resolve("frecuencias.md").writeText(text)
    }

    private class Row(
        val meters: Int,
        val speed: Float,
        val difficulty: Float,
        val asked: Int,
        val fair: List<Int>,
        val possible: List<Int>,
    )

    private fun measure(meters: Int): Row {
        val speed = movement.speedAt(meters.toDouble())
        val difficulty = generator.difficultyAt(meters.toDouble())
        val fraction = course.gapFractionStart + (course.gapFractionEnd - course.gapFractionStart) * difficulty
        val asked = maxOf(course.minGapTiles, floor(fraction * speed * lab.longestAirtime / TILE).toInt())
        val presses = STEPS.map { step -> (1..MAX_GAP_TILES).map { tiles -> goodPresses(meters, step, tiles) } }
        val fair = presses.map { byTiles -> widest(byTiles, CourseGenerator.FAIR_PRESSES) }
        val possible = presses.map { byTiles -> widest(byTiles, 1) }
        return Row(meters, speed, difficulty, asked, fair, possible)
    }

    /** The widest gap, in tiles, with at least [enough] good press moments; [byTiles] starts at a gap of one tile. */
    private fun widest(byTiles: List<Int>, enough: Int): Int = byTiles.indices.lastOrNull { byTiles[it] >= enough }?.plus(1) ?: 0

    /**
     * Like the validator: how many press moments at the end of the run-up let enough hold lengths land with margin on a
     * ground [step] tiles higher, across a gap of [tiles].
     */
    private fun goodPresses(meters: Int, step: Int, tiles: Int): Int {
        val fromTop = BASE_TOP * TILE
        val toTop = (BASE_TOP + step) * TILE
        val far = tiles * TILE
        val terrain = Course()
        terrain.append(Piece(listOf(Ground(-RUN_UP, 0, fromTop), Ground(far, far + RUN_UP, toTop)), emptyList(), far + RUN_UP))
        val runStart = -meters.toDouble() * TILE
        val speed = movement.speedAt(meters.toDouble())
        val windowSteps = ceil(CourseGenerator.PRESS_WINDOW_SECONDS / JumpLab.STEP).toInt()
        val lastPress = windowSteps + floor(movement.coyoteTime / JumpLab.STEP).toInt()
        val startX = -movement.hitboxWidth / 2.0 - speed * (windowSteps + 1) * JumpLab.STEP
        val minLand = far + course.landingMarginTiles * TILE
        var presses = 0
        for (press in 0..lastPress) {
            var holds = 0
            for (hold in 0..maxHold) {
                val landing = lab.tryJump(terrain, emptyList(), startX, fromTop.toFloat(), runStart, press, hold)
                if (landing.landed && landing.top == toTop && landing.x >= minLand) holds++
            }
            if (holds >= course.fairHoldSteps) presses++
        }
        return presses
    }

    private fun table(rows: List<Row>): String = buildString {
        append("| Distancia | Velocidad | Dificultad | Pide el generador | Justo: igual / +2 / −3 ")
        appendLine("| Posible: igual / +2 / −3 | Restan cada 100 m |")
        appendLine("|---|---|---|---|---|---|---|")
        for (row in rows) {
            val negatives = course.negativesPer100mStart + (course.negativesPer100mEnd - course.negativesPer100mStart) * row.difficulty
            append("| ${row.meters} m | ${row.speed.toInt()} px/s | ${(row.difficulty * 100).toInt()} % | ${row.asked} tiles ")
            appendLine("| ${row.fair.joinToString(" / ")} | ${row.possible.joinToString(" / ")} | ${format(negatives)} |")
        }
    }

    /** The difficulty and the speed against the distance, as a small SVG for the docs. */
    private fun curve(): String = buildString {
        val plotWidth = WIDTH - LEFT - RIGHT
        val plotHeight = HEIGHT - TOP - BOTTOM
        fun x(meters: Int) = LEFT + plotWidth * meters / CURVE_METERS
        fun y(share: Float) = format(TOP + plotHeight * (1 - share))
        fun speedShare(meters: Int) =
            (movement.speedAt(meters.toDouble()) - movement.startSpeed) / (movement.maxSpeed - movement.startSpeed)
        val samples = 0..CURVE_METERS step CURVE_STEP
        val difficulty = samples.joinToString(" ") { "${x(it)},${y(generator.difficultyAt(it.toDouble()))}" }
        val speed = samples.joinToString(" ") { "${x(it)},${y(speedShare(it))}" }

        appendLine("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $WIDTH $HEIGHT" font-family="sans-serif" font-size="12">""")
        appendLine("""<rect width="$WIDTH" height="$HEIGHT" fill="#ffffff"/>""")
        for (meters in 0..CURVE_METERS step 1000) {
            appendLine("""<line x1="${x(meters)}" y1="$TOP" x2="${x(meters)}" y2="${TOP + plotHeight}" stroke="#dddddd"/>""")
            appendLine("""<text x="${x(meters)}" y="${TOP + plotHeight + 16}" text-anchor="middle">$meters m</text>""")
        }
        for (k in 0..4) {
            val share = k / 4f
            val pixels = movement.startSpeed + (movement.maxSpeed - movement.startSpeed) * share
            appendLine("""<line x1="$LEFT" y1="${y(share)}" x2="${LEFT + plotWidth}" y2="${y(share)}" stroke="#eeeeee"/>""")
            appendLine("""<text x="${LEFT - 6}" y="${y(share)}" dy="4" text-anchor="end">${k * 25} %</text>""")
            appendLine("""<text x="${LEFT + plotWidth + 6}" y="${y(share)}" dy="4">${pixels.toInt()} px/s</text>""")
        }
        appendLine("""<polyline points="$difficulty" fill="none" stroke="#c0392b" stroke-width="2.5"/>""")
        appendLine("""<polyline points="$speed" fill="none" stroke="#2c7fb8" stroke-width="2.5" stroke-dasharray="6 4"/>""")
        appendLine("""<text x="${LEFT + 8}" y="${TOP - 8}" fill="#c0392b">Dificultad (eje izquierdo)</text>""")
        appendLine("""<text x="${LEFT + plotWidth - 8}" y="${TOP - 8}" fill="#2c7fb8" text-anchor="end">Velocidad (eje derecho)</text>""")
        appendLine("""<text x="${LEFT + plotWidth / 2}" y="${HEIGHT - 6}" text-anchor="middle">Distancia recorrida</text>""")
        appendLine("</svg>")
    }

    private fun format(value: Float): String = String.format(Locale.ROOT, "%.1f", value)

    private companion object {
        val DISTANCES = listOf(0, 300, 600, 900, 1200, 1800, 2400, 3600, 5000)

        /** Stretches of the run, in metres, and how many runs of 5 km are counted for the frequencies. */
        val BANDS = listOf(0..500, 500..1000, 1000..2000, 2000..3500, 3500..5000)
        const val SEEDS = 20L

        /** The step to the next ground: the same height, the most it goes up and the most it goes down. */
        val STEPS = listOf(0, 2, -3)
        const val BASE_TOP = 5
        const val RUN_UP = 64 * TILE
        const val MAX_GAP_TILES = 14

        const val CURVE_METERS = 5000
        const val CURVE_STEP = 50
        const val WIDTH = 640
        const val HEIGHT = 280
        const val LEFT = 56
        const val RIGHT = 64
        const val TOP = 24
        const val BOTTOM = 40
    }
}
