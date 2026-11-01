package com.fanguskdev.carofarty.storage

import com.fanguskdev.carofarty.game.Scenario

/** How a run went against the records of its scenario, for the end screen (UI-06). */
data class RunResult(
    val meters: Int,
    val maxPoints: Int,
    val bestMeters: Int,
    val bestPoints: Int,
    val newMeters: Boolean,
    val newPoints: Boolean,
) {
    val newRecord: Boolean get() = newMeters || newPoints
}

/**
 * The two records Fangusk asked for (GDD-13): the distance and the most points of a run, kept on the phone. They go by
 * scenario, as proposed in ADR 0006 and until Fangusk says otherwise.
 */
class Records(private val store: KeyValueStore) {
    fun meters(scenario: Scenario): Int = store.getInt(key(scenario, "meters"), 0)

    fun points(scenario: Scenario): Int = store.getInt(key(scenario, "points"), 0)

    /** Writes down a finished run and says how it compares. */
    fun submit(scenario: Scenario, meters: Int, maxPoints: Int): RunResult {
        val bestMeters = meters(scenario)
        val bestPoints = points(scenario)
        val newMeters = meters > bestMeters
        val newPoints = maxPoints > bestPoints
        if (newMeters) store.putInt(key(scenario, "meters"), meters)
        if (newPoints) store.putInt(key(scenario, "points"), maxPoints)
        return RunResult(meters, maxPoints, maxOf(bestMeters, meters), maxOf(bestPoints, maxPoints), newMeters, newPoints)
    }

    private fun key(scenario: Scenario, what: String) = "record_${scenario.name.lowercase()}_$what"
}
