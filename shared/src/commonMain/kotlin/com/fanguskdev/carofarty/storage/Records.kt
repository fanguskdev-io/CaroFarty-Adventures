package com.fanguskdev.carofarty.storage

import com.fanguskdev.carofarty.game.Scenario

/** A run in the records: its metres, the most points it had and the day it was played, as yyyymmdd (0 if unknown). */
data class RecordRun(val meters: Int, val points: Int, val day: Int)

/**
 * How a run went against the records of its scenario, for the end screen (UI-06): its metres and points, the best distance
 * of the scenario after it, and its place among the best runs, from 1, or 0 if it did not get in.
 */
data class RunResult(
    val meters: Int,
    val maxPoints: Int,
    val bestMeters: Int,
    val place: Int,
) {
    /** A new record: the longest run of the scenario. */
    val newRecord: Boolean get() = place == 1
}

/**
 * The records (GDD-13, META-01), by scenario, as Fangusk decided after playing the beta (2026-11-02): the [SIZE] best runs
 * of each scenario by metres, each with the most points it had and its day, which a better run pushes down and the last
 * one out («mejor 5 por cada»). Kept on the phone. The records of before, only the best distance and the most points,
 * come in as a run without a day.
 */
class Records(private val store: KeyValueStore) {
    /** The best runs of [scenario], the longest first. */
    fun best(scenario: Scenario): List<RecordRun> {
        val count = store.getInt(key(scenario, COUNT), -1)
        if (count < 0) return older(scenario)
        return (0 until count.coerceAtMost(SIZE)).map { i ->
            RecordRun(
                meters = store.getInt(key(scenario, "$i$METERS"), 0),
                points = store.getInt(key(scenario, "$i$POINTS"), 0),
                day = store.getInt(key(scenario, "$i$DAY"), 0),
            )
        }
    }

    /** The best distance of [scenario], or 0 if it has no runs. */
    fun meters(scenario: Scenario): Int = best(scenario).firstOrNull()?.meters ?: 0

    /** Writes down a finished run, played on [day], and says how it compares. A run of 0 m does not get in. */
    fun submit(
        scenario: Scenario,
        meters: Int,
        maxPoints: Int,
        day: Int = 0,
    ): RunResult {
        val runs = best(scenario).toMutableList()
        // After the runs as long or longer: a tie keeps the older one first.
        var place = runs.indexOfFirst { it.meters < meters }.let { if (it < 0) runs.size else it }
        if (meters > 0 && place < SIZE) {
            runs.add(place, RecordRun(meters, maxPoints, day))
            save(scenario, runs.take(SIZE))
            place += 1
        } else {
            place = 0
        }
        return RunResult(meters, maxPoints, runs.firstOrNull()?.meters ?: 0, place)
    }

    private fun save(scenario: Scenario, runs: List<RecordRun>) {
        runs.forEachIndexed { i, run ->
            store.putInt(key(scenario, "$i$METERS"), run.meters)
            store.putInt(key(scenario, "$i$POINTS"), run.points)
            store.putInt(key(scenario, "$i$DAY"), run.day)
        }
        store.putInt(key(scenario, COUNT), runs.size)
    }

    /** The records of the first versions, the best distance and the most points, as one run without a day. */
    private fun older(scenario: Scenario): List<RecordRun> {
        val name = scenario.name.lowercase()
        val meters = store.getInt("record_${name}_meters", 0)
        return if (meters > 0) listOf(RecordRun(meters, store.getInt("record_${name}_points", 0), 0)) else emptyList()
    }

    private fun key(scenario: Scenario, what: String) = "records_${scenario.name.lowercase()}_$what"

    companion object {
        /** How many runs of each scenario are kept: five, as Fangusk asked. */
        const val SIZE = 5
        private const val COUNT = "count"
        private const val METERS = "_meters"
        private const val POINTS = "_points"
        private const val DAY = "_day"
    }
}
