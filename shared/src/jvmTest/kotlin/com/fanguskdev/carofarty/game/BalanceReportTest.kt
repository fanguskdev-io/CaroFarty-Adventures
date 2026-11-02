package com.fanguskdev.carofarty.game

import java.io.File
import java.util.Locale
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The balance of the runs (GEN-23, GEN-24; ADR 0009 and 0010): how long they last and how they end, in the Bosque, for
 * three players: the careful pilot of the tests; a person who copies it badly, a little late now and then, holding a
 * little more or a little less and missing a jump from time to time; and the same person going for every animal, who
 * rescues the one in front of a huevo frito or a pescado before jumping it and jumps onto the platforms with an animal.
 * Writes `build/metricas/equilibrio.md`, which docs/metricas.md copies.
 */
class BalanceReportTest {
    private val config = GameConfig()
    private val out = File("build/metricas").apply { mkdirs() }

    /** A player who takes the pilot's advice with a person's mistakes, with its own seed so it errs the same every time. */
    private class Player(
        private val run: GameRun,
        seed: Long,
        private val careless: Boolean,
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
                        if (careless) mistake()
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
        private fun mistake() {
            if (random.nextFloat() < MISS) {
                blindFor = BLIND_STEPS
                return
            }
            waiting = if (random.nextFloat() < LATE) random.nextInt(LATE_MIN, LATE_MAX + 1) else random.nextInt(0, REACTION + 1)
            pendingHold = (pendingHold + random.nextInt(-HOLD_ERROR, HOLD_ERROR + 1)).coerceAtLeast(0)
        }

        private companion object {
            /** A jump in a hundred not made at all, and the moment she looks away. */
            const val MISS = 0.01f
            const val BLIND_STEPS = 24

            /** One jump in ten pressed between 4 and 8 steps late (67 to 133 ms), the rest up to 2 steps late. */
            const val LATE = 0.1f
            const val LATE_MIN = 4
            const val LATE_MAX = 8
            const val REACTION = 2

            /** The finger held up to 3 steps (50 ms) more or less than the pilot meant. */
            const val HOLD_ERROR = 3
        }
    }

    private class Outcome(
        val meters: Int,
        val seconds: Float,
        val cause: EndCause?,
        val rescued: Int,
        val touched: Int,
    )

    private fun play(seed: Long, careless: Boolean, greedy: Boolean): Outcome {
        val run = GameRun(config, Scenario.Bosque, seed)
        val player = Player(run, seed * 31 + 7, careless, greedy)
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
        val careful = (1L..CAREFUL_RUNS).map { play(it, careless = false, greedy = false) }
        val players = (1L..PLAYER_RUNS).map { play(it, careless = true, greedy = false) }
        val greedy = (1L..PLAYER_RUNS).map { play(it, careless = true, greedy = true) }
        assertTrue(careful.none { it.cause == EndCause.Fall }, "the careful pilot never falls")
        val text = buildString {
            appendLine(
                "| Quién | Partidas | Metros (mediana) | Metros (1.er y 3.er cuartil) | Duración (mediana) | Por barranco | " +
                    "Sin puntos | Siguen a los 10 min | Rescates por minuto | Restas por minuto |",
            )
            appendLine("|---|---|---|---|---|---|---|---|---|---|")
            appendLine(row("Piloto cuidadoso", careful))
            appendLine(row("Jugador que a veces falla", players))
            appendLine(row("El mismo, a por todos los animales", greedy))
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
        const val CAREFUL_RUNS = 6L
        const val PLAYER_RUNS = 80L
        const val MAX_STEPS = 10 * 60 * 60
        const val SECONDS_PER_MINUTE = 60.0
        const val PERCENT = 100.0
    }
}
