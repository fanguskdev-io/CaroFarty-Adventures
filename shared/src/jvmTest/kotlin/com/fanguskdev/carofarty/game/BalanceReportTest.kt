package com.fanguskdev.carofarty.game

import java.io.File
import java.util.Locale
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The balance of the runs (GEN-23, GEN-24, GEN-25; ADR 0009 to 0011): how long they last and how they end, in the
 * Bosque, for the careful pilot of the tests and for people who copy it badly, a little late now and then, holding a
 * little more or a little less and missing a jump from time to time: a clumsy one and a skilled one, each also going for
 * every animal, rescuing the one in front of a huevo frito or a pescado before jumping it and jumping onto the platforms
 * with an animal. Writes `build/metricas/equilibrio.md`, which docs/metricas.md copies.
 */
class BalanceReportTest {
    private val config = GameConfig()
    private val out = File("build/metricas").apply { mkdirs() }

    /**
     * How a person errs: in how many jumps she does not press at all, in how many she presses 4 to 8 steps late (67 to
     * 133 ms) and the rest how late at most, and how many steps more or less she holds the finger.
     */
    private class Mistakes(
        val miss: Float,
        val late: Float,
        val lateMin: Int,
        val lateMax: Int,
        val reaction: Int,
        val holdError: Int,
    )

    /** A player who takes the pilot's advice with a person's [mistakes], with its own seed so it errs the same every time. */
    private class Player(
        private val run: GameRun,
        seed: Long,
        private val mistakes: Mistakes?,
        greedy: Boolean,
    ) {
        private val pilot = Pilot(run, greedy = greedy)
        private val random = Random(seed)
        private var waiting = 0
        private var holdLeft = 0
        private var pendingHold = 0
        private var blindFor = 0

        fun step() {
            var pressed = false
            var held = false
            when {
                run.phase == RunPhase.Ready -> pressed = true
                holdLeft > 0 -> {
                    holdLeft--
                    held = true
                }
                waiting > 0 -> if (--waiting == 0) {
                    pressed = true
                    held = pendingHold > 0
                    holdLeft = pendingHold - 1
                }
                blindFor > 0 -> blindFor--
                else -> {
                    pilot.decide()
                    if (pilot.pressed) {
                        pilot.forget()
                        pendingHold = pilot.plannedHold
                        if (mistakes != null) mistake(mistakes)
                        if (waiting == 0 && blindFor == 0) {
                            pressed = true
                            held = pendingHold > 0
                            holdLeft = pendingHold - 1
                        }
                    }
                }
            }
            run.step(JumpLab.STEP, pressed, held)
        }

        /** What a person does instead of the pilot's jump: misses it now and then, or presses it late and holds it unevenly. */
        private fun mistake(mistakes: Mistakes) {
            if (random.nextFloat() < mistakes.miss) {
                blindFor = BLIND_STEPS
                return
            }
            val late = random.nextFloat() < mistakes.late
            waiting = if (late) random.nextInt(mistakes.lateMin, mistakes.lateMax + 1) else random.nextInt(0, mistakes.reaction + 1)
            pendingHold = (pendingHold + random.nextInt(-mistakes.holdError, mistakes.holdError + 1)).coerceAtLeast(0)
        }

        private companion object {
            /** The moment she looks away when she misses a jump. */
            const val BLIND_STEPS = 24
        }
    }

    private class Outcome(
        val meters: Int,
        val seconds: Float,
        val cause: EndCause?,
        val rescued: Int,
        val touched: Int,
    )

    private fun play(seed: Long, mistakes: Mistakes?, greedy: Boolean): Outcome {
        val run = GameRun(config, Scenario.Bosque, seed)
        val player = Player(run, seed * 31 + 7, mistakes, greedy)
        var steps = 0
        var rescued = 0
        var touched = 0
        while (run.phase != RunPhase.Over && steps < MAX_STEPS) {
            player.step()
            for (event in run.events) if (event is RunEvent.Picked) if (event.points > 0) rescued++ else touched++
            run.events.clear()
            steps++
        }
        return Outcome(run.meters.toInt(), steps * JumpLab.STEP, run.endCause, rescued, touched)
    }

    @Test
    fun howLongTheRunsLastAndHowTheyEnd() {
        val careful = (1L..CAREFUL_RUNS).map { play(it, mistakes = null, greedy = false) }
        val carefulGreedy = (1L..GREEDY_PILOT_RUNS).map { play(it, mistakes = null, greedy = true) }
        val clumsy = (1L..PLAYER_RUNS).map { play(it, CLUMSY, greedy = false) }
        val clumsyGreedy = (1L..PLAYER_RUNS).map { play(it, CLUMSY, greedy = true) }
        val skilled = (1L..SKILLED_RUNS).map { play(it, SKILLED, greedy = false) }
        val skilledGreedy = (1L..SKILLED_RUNS).map { play(it, SKILLED, greedy = true) }
        assertTrue(careful.none { it.cause != null }, "the careful pilot never falls nor runs out of points")
        assertTrue(careful.all { it.touched == 0 }, "nor touches what takes points away")
        val text = buildString {
            appendLine(
                "| Quién | Partidas | Metros (mediana) | Metros (1.er y 3.er cuartil) | Duración (mediana) | Por barranco | " +
                    "Sin puntos | Siguen a los 10 min | Rescates por minuto | Restas por minuto |",
            )
            appendLine("|---|---|---|---|---|---|---|---|---|---|")
            appendLine(row("Piloto cuidadoso", careful))
            appendLine(row("El piloto, a por todos los animales", carefulGreedy))
            appendLine(row("Jugador que a veces falla", clumsy))
            appendLine(row("El mismo, a por todos los animales", clumsyGreedy))
            appendLine(row("Jugador hábil", skilled))
            appendLine(row("El hábil, a por todos los animales", skilledGreedy))
        }
        out.resolve("equilibrio.md").writeText(text)
        println(text)
    }

    private fun row(who: String, runs: List<Outcome>): String {
        val meters = runs.map { it.meters }.sorted()
        val seconds = runs.map { it.seconds }.sorted()
        val minutes = runs.sumOf { it.seconds.toDouble() } / SECONDS_PER_MINUTE
        val quartiles = "${meters[meters.size / 4]}–${meters[meters.size * 3 / 4]}"
        val rescues = runs.sumOf { it.rescued } / minutes
        val touches = runs.sumOf { it.touched } / minutes
        return "| $who | ${runs.size} | ${meters[meters.size / 2]} | $quartiles | ${format(seconds[seconds.size / 2].toDouble())} s | " +
            "${share(runs, EndCause.Fall)} % | ${share(runs, EndCause.NoPoints)} % | ${share(runs, null)} % | " +
            "${format(rescues)} | ${format(touches)} |"
    }

    private fun share(runs: List<Outcome>, cause: EndCause?): String = format(runs.count { it.cause == cause } * PERCENT / runs.size)

    private fun format(value: Double): String = String.format(Locale.forLanguageTag("es"), "%.1f", value)

    private companion object {
        const val CAREFUL_RUNS = 10L

        /** More for the pilot going for every animal: how many of its runs end without points is the figure to watch (ADR 0011). */
        const val GREEDY_PILOT_RUNS = 40L
        const val PLAYER_RUNS = 80L
        const val SKILLED_RUNS = 40L

        /**
         * The clumsy player misses one jump in a hundred and presses one in ten late; the skilled one, one in five hundred
         * and one in thirty, and holds more evenly.
         */
        val CLUMSY = Mistakes(miss = 0.01f, late = 0.1f, lateMin = 4, lateMax = 8, reaction = 2, holdError = 3)
        val SKILLED = Mistakes(miss = 0.002f, late = 0.03f, lateMin = 3, lateMax = 6, reaction = 1, holdError = 2)
        const val MAX_STEPS = 10 * 60 * 60
        const val SECONDS_PER_MINUTE = 60.0
        const val PERCENT = 100.0
    }
}
