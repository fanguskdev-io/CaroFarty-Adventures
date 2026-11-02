package com.fanguskdev.carofarty.audio.music

import com.fanguskdev.carofarty.game.Scenario
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * No clicks (AUD-24): the check of the prototype's verificar.py on the demo run of each scenario. A click is an
 * isolated jump in the second difference, much bigger than the signal's around it; channel by channel, a jump that is
 * not where a note starts (where it is its attack) would be a click: a note cut badly, a loop that does not join, a
 * section that ends on a held note. In the mix, the moments where something changes (a section, a layer, the tension,
 * the end) are checked.
 */
class MusicCleanTest {
    @Test
    fun noChannelClicksOutsideTheStartOfANote() {
        for (scenario in Scenario.entries) {
            val stream = Runs.stream(scenario)
            val engine = stream.engine
            val total = (Runs.SECONDS * Chip.RATE).toInt()
            val tracks = Array(Chip.MUSIC_VOICES) { FloatArray(total) }
            val starts = Array(Chip.MUSIC_VOICES) { ArrayList<Long>() }
            val textures = ArrayList<LongRange>()
            engine.onChannel = { c, from, samples, count ->
                for (k in 0 until count) if (from + k < total) tracks[c][(from + k).toInt()] = samples[k].toFloat()
            }
            engine.onNote = { c, q, s ->
                starts[c].add(q.start[s])
                if (engine.bank.instruments[q.instrument[s]].recipe.texture) textures.add(q.start[s]..q.start[s] + q.length[s])
            }
            Runs.render(stream, Runs.SECONDS, Runs.demo)
            var odd = 0
            for (c in 0 until Chip.MUSIC_VOICES) {
                val onsets = starts[c].sorted()
                val margin = Chip.ms(3.0)
                for (k in Clicks.find(tracks[c], minimum = 0.005)) {
                    if (textures.any { k in it }) continue
                    val near = onsets.binarySearch(k - margin.toLong()).let { if (it < 0) -it - 1 else it }
                    if (near < onsets.size && onsets[near] <= k + margin) continue
                    if (odd++ < 10) println("${scenario.name}: channel $c jumps at %.4f s".format(k.toDouble() / Chip.RATE))
                }
            }
            assertTrue(odd == 0, "${scenario.name}: $odd jumps outside the start of a note")
        }
    }

    @Test
    fun theMixDoesNotClickWhereSomethingChanges() {
        for (scenario in Scenario.entries) {
            val stream = Runs.stream(scenario)
            val moments = ArrayList<Double>()
            stream.engine.onSection = { _, start, _ -> moments.add(start / Chip.RATE) }
            val pcm = Runs.render(stream, Runs.SECONDS, Runs.demo)
            moments.addAll(Runs.demo.map { it.seconds })
            moments.add(stream.engine.warningTime / Chip.RATE)
            val mix = FloatArray(pcm.size) { pcm[it] / 32767f }
            val zones = moments.map { maxOf(0.0, it - 0.05)..it + 0.25 }
            val clicks = Clicks.find(mix, zones = zones)
            assertTrue(clicks.isEmpty(), "${scenario.name}: clicks at ${clicks.take(6).map { it.toDouble() / Chip.RATE }}")
            // The run starts and ends in silence.
            assertTrue(abs(mix[0]) < 0.002 && abs(mix[mix.size - 1]) < 0.002, "${scenario.name}: the edges are not silent")
        }
    }

    /** verificar.chasquidos, sample for sample. */
    private object Clicks {
        fun find(
            x: FloatArray,
            rate: Int = Chip.RATE,
            threshold: Double = 10.0,
            minimum: Double = 0.02,
            windowMs: Double = 12.0,
            zones: List<ClosedFloatingPointRange<Double>>? = null,
        ): List<Long> {
            val n = x.size - 2
            val d2 = DoubleArray(n) { x[it + 2].toDouble() - 2.0 * x[it + 1] + x[it] }
            val w = maxOf(8, (rate * windowMs / 1000).toInt())
            val sums = DoubleArray(n + 1)
            for (i in 0 until n) sums[i + 1] = sums[i] + d2[i] * d2[i]
            val ranges = zones?.map { maxOf(0, (it.start * rate).toInt() - 2) until minOf(n, (it.endInclusive * rate).toInt() - 2) }
                ?: listOf(0 until n)
            val silence = maxOf(3, (rate * 0.003).toInt())
            val found = ArrayList<Long>()
            for (range in ranges) {
                for (i in range) {
                    val v = abs(d2[i])
                    if (v < minimum) continue
                    if (i + 2 >= silence && (i + 2 - silence until i + 2).all { abs(x[it]) < 1e-3 }) continue
                    val i0 = maxOf(0, i - w)
                    val i1 = minOf(n, i + w + 1)
                    val energy = sums[i1] - sums[i0] - v * v
                    val local = sqrt(energy / maxOf(1, i1 - i0 - 1))
                    if (v > threshold * local) found.add((i + 2).toLong())
                }
            }
            return found
        }
    }
}
