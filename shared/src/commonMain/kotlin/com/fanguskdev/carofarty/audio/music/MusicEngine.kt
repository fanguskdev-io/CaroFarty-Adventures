package com.fanguskdev.carofarty.audio.music

import kotlin.math.max
import kotlin.math.min

/**
 * The music at 32 kHz, bar by bar as it plays (the prototype's maquetas.plan, generador.renderizar and the chip's mix).
 *
 * A bar is decided [LOOKAHEAD] samples before it starts: its level, whether it is tense, whether a section starts (and
 * then it is composed), and its notes go to the channels' queues with their times. Knowing them that early is what lets
 * a note fade out (8 ms) right before the next one on its channel, even when the next one is humanized 5 ms early.
 *
 * Nothing here allocates once it is built: the audio thread calls [render].
 */
internal class MusicEngine(
    val style: MusicStyle,
    val bank: MusicBank,
    seed: Long,
    private val tempoRises: Boolean = true,
) {
    val stats = VoiceStats()
    val composer = Composer(style, bank, seed)
    private val plan = PyRandom(seed * 101)
    private val peek = PyRandom(0)
    private val steps = style.stepsPerBar
    private val beat = style.stepsPerBeat

    private val queues = Array(Chip.MUSIC_VOICES) { NoteQueue() }
    private val voices = Array(Chip.MUSIC_VOICES) { Voice(bank.instruments, stats) }
    private val effects = Array(Chip.EFFECT_VOICES) { EffectVoice() }
    private val channel = Array(Chip.MUSIC_VOICES) { DoubleArray(BLOCK) }
    private val effectMix = DoubleArray(BLOCK)
    private val gains = Array(Chip.MUSIC_VOICES) { Ramp() }
    private val toEcho = BooleanArray(Chip.MUSIC_VOICES).also { for (c in style.echoChannels) it[c] = true }
    private val echo = Echo(style.echo)
    private val dc = DcBlocker()
    private val layerOf = IntArray(Chip.MUSIC_VOICES) { c -> style.layers.indexOfFirst { c in it } }
    private val layerOn = BooleanArray(MusicStyle.LAYERS)
    private val warning = Effects.fewPointsWarning()
    private val warningGain = WARNING_RMS / Effects.activeRms(warning)

    // The section being played and the notes of its 8 bars.
    private val notes = Array(Chip.MUSIC_VOICES) { NoteList(SECTION_CAPACITY) }
    private val order = Array(Chip.MUSIC_VOICES) { IntArray(SECTION_CAPACITY) }
    private val cursor = IntArray(Chip.MUSIC_VOICES)
    private val chords = IntArray(Composer.BARS)
    private var sectionId = 0
    private var sectionKind = -1
    private var sectionBars = 0
    private var sectionSemi = 0.0

    // Time, in samples at 32 kHz.
    var now = 0L
        private set
    private var nextBar = 0.0
    private var lastBarStart = -1.0
    private var lastBarSemi = -1.0
    private var lastBarLevel = -1
    private var barsAtTop = 0

    // What the game asks for.
    private var requestedLevel = 1
    private var level = 1
    private var lowerFor = 0
    private var tense = false
    private var warningAt = Double.NaN
    private var endAt = Double.NaN
    private var stopped = false

    /** The kind of the section playing (see [Composer]), or −1 before the first bar. */
    val currentSection: Int get() = sectionKind

    /** Sections started so far. */
    val sections: Int get() = sectionId

    /** When the warning of few points sounds (samples), or NaN. */
    val warningTime: Double get() = warningAt

    /** When the layers start fading out at the end (samples), or NaN. */
    val endTime: Double get() = endAt

    /** The volume of channel [c] now (its layer's, and the end's fade). */
    fun channelGain(c: Int): Double = gains[c].valueAt(now)

    /** The tempo of the section playing. */
    val sectionBpm: Double get() = if (sectionSemi > 0) Chip.RATE * 60.0 / (sectionSemi * beat) else style.bpm

    /** Whether the music has faded out and its echo has died away. */
    val finished: Boolean get() = !endAt.isNaN() && now >= endAt + Chip.ms(FADE_MS) + TAIL && effects.none { it.busy }

    /** Whether the effect voices sound (the tests that compare the music alone turn them off). */
    var playEffects = true

    /** Something to watch while the music plays, for the tests: each section as it is composed. */
    var onSection: ((kind: Int, start: Double, notes: Array<NoteList>) -> Unit)? = null

    /** And each note as it is scheduled: its channel and the slot of its queue. */
    var onNote: ((channel: Int, queue: NoteQueue, slot: Int) -> Unit)? = null

    /** And each channel's samples as its voice makes them, before the layers' volume. */
    var onChannel: ((channel: Int, from: Long, samples: DoubleArray, count: Int) -> Unit)? = null

    fun setLevel(value: Int) {
        requestedLevel = value.coerceIn(1, MAX_LEVEL)
    }

    /**
     * Few points left (or not any more): the warning sounds on the next beat and the tense version starts on the first
     * bar from there; without it, the next bar goes back to the music with an A.
     */
    fun setTense(value: Boolean) {
        if (value == tense || !endAt.isNaN()) return
        tense = value
        if (value) {
            warningAt = nextBeat(now.toDouble())
            effects[0].play(warning, warningAt.toLong(), warningGain)
        } else {
            warningAt = Double.NaN
            effects[0].stop()
        }
    }

    /** The run is over: on the next beat every layer fades out in 200 ms, and the music stops; a warning yet to come does not. */
    fun end() {
        if (!endAt.isNaN()) return
        if (!warningAt.isNaN() && warningAt > now) effects[0].stop()
        endAt = nextBeat(now.toDouble())
        val from = endAt.toLong()
        val to = (endAt + Chip.ms(FADE_MS)).toLong()
        for (c in 0 until Chip.MUSIC_VOICES) gains[c].fadeTo(0.0, from, to)
    }

    /** The first beat at or after [time] (maquetas._siguiente_tiempo), on the bars already decided or the next one. */
    private fun nextBeat(time: Double): Double {
        for (k in 0 until style.beatsPerBar) {
            if (lastBarStart < 0) break
            val t = lastBarStart + beat * k * lastBarSemi
            if (t >= time) return t
        }
        return max(time, nextBar)
    }

    /**
     * Renders [count] samples at 32 kHz into [out]. Each bar is decided exactly [LOOKAHEAD] samples before it starts,
     * whatever the size of the buffers, so the music depends only on the seed and on the samples at which the game's
     * calls took effect.
     */
    fun render(out: DoubleArray, count: Int) {
        var done = 0
        while (done < count) {
            decideBars()
            var n = min(BLOCK, count - done)
            if (!stopped) n = min(n.toLong(), decisionOf(nextBar) - now).toInt()
            for (c in 0 until Chip.MUSIC_VOICES) voices[c].render(queues[c], now, channel[c], n)
            onChannel?.let { listen -> for (c in 0 until Chip.MUSIC_VOICES) listen(c, now, channel[c], n) }
            for (k in 0 until n) {
                val t = now + k
                var dry = 0.0
                var send = 0.0
                for (c in 0 until Chip.MUSIC_VOICES) {
                    val y = gains[c].apply(channel[c][k], t)
                    dry += y
                    if (toEcho[c]) send += y
                }
                out[done + k] = dc.process(dry + echo.process(send))
            }
            for (k in 0 until n) effectMix[k] = 0.0
            if (playEffects) for (e in effects) e.mix(now, effectMix, n)
            val master = style.masterGain
            for (k in 0 until n) out[done + k] = out[done + k] * master + effectMix[k]
            // The first 10 ms fade in, as the prototype's demos do, so that playback never starts on a step.
            if (now < FADE_IN) for (k in 0 until n) if (now + k < FADE_IN) out[done + k] *= (now + k).toDouble() / FADE_IN
            now += n
            done += n
        }
    }

    /** The sample at which the bar starting at [start] is decided. */
    private fun decisionOf(start: Double): Long = start.toLong() - LOOKAHEAD

    /** Decides the bars whose time has come. */
    private fun decideBars() {
        while (!stopped && now >= decisionOf(nextBar)) {
            if (!endAt.isNaN() && nextBar >= endAt + Chip.ms(FADE_MS)) {
                stopped = true
                return
            }
            openBar(nextBar)
        }
    }

    private fun openBar(start: Double) {
        if (requestedLevel < level) {
            lowerFor++
            if (lowerFor >= HYSTERESIS_BARS) {
                level = requestedLevel
                lowerFor = 0
            }
        } else {
            level = requestedLevel
            lowerFor = 0
        }
        val entersMelody = lastBarLevel >= 0 && level >= MELODY_LEVEL && lastBarLevel < MELODY_LEVEL
        val tension = !warningAt.isNaN() && start >= warningAt
        var kind = -1
        if (sectionKind < 0) {
            kind = Composer.A
        } else if (tension && sectionKind != Composer.TENSE) {
            kind = Composer.TENSE
        } else if (!tension && sectionKind == Composer.TENSE) {
            kind = Composer.A
        } else if (entersMelody) {
            kind = Composer.A
        } else if (sectionBars == Composer.BARS) {
            kind = chooseNext(sectionKind, plan)
        }
        if (kind >= 0) startSection(kind, start)
        val semi = sectionSemi
        barsAtTop = if (level >= MAX_LEVEL) barsAtTop + 1 else 0
        val intensity = when {
            tension -> 0.95
            level >= MAX_LEVEL -> INTENSITY_CYCLE[(INTENSITY_PEAK + barsAtTop - 1) % INTENSITY_CYCLE.size]
            else -> 0.88 + 0.03 * (level - 1)
        }
        scheduleBar(sectionBars, start, semi, intensity)
        layers(level, tension, start, semi)
        sectionBars++
        lastBarStart = start
        lastBarSemi = semi
        lastBarLevel = level
        nextBar = start + steps * semi
    }

    private fun startSection(kind: Int, start: Double) {
        if (sectionKind >= 0 && sectionBars < Composer.BARS) cutSection(sectionBars * steps)
        val bpm = if (kind == Composer.TENSE || !tempoRises) {
            if (!tempoRises || lastBarSemi < 0) style.bpm else Chip.RATE * 60.0 / (beat * lastBarSemi)
        } else {
            style.bpm * (1 + min(MAX_TEMPO_RISE, TEMPO_RISE * (level - 1)))
        }
        sectionSemi = Chip.RATE * 60.0 / (bpm * beat)
        sectionId++
        sectionKind = kind
        sectionBars = 0
        // The next section is chosen when this one ends, as in the prototype; the bass that walks towards it needs to
        // know it now, so the choice is made ahead on a copy of the generator (the same one if the level stays).
        peek.copyFrom(plan)
        val guess = chooseNext(kind, peek)
        composer.compose(kind, composer.startChordOf(guess), chords, notes)
        for (c in 0 until Chip.MUSIC_VOICES) sortByPosition(notes[c], order[c])
        cursor.fill(0)
        onSection?.invoke(kind, start, notes)
        composer.arrangement.mutate()
    }

    /** The section playing is cut short at [limit] sixteenths: its notes are held at most until there (× the gate). */
    private fun cutSection(limit: Int) {
        for (c in 0 until Chip.MUSIC_VOICES) {
            val gate = Composer.gate[c]
            voices[c].cutSection(sectionId, limit, gate)
            val q = queues[c]
            for (i in 0 until q.size) {
                val s = q.slot(i)
                if (q.section[s] == sectionId && q.pos[s] + q.steps[s] > limit) {
                    q.length[s] = min(q.length[s], max(1, ((limit - q.pos[s]) * q.semi[s] * gate).toInt()))
                }
            }
        }
    }

    /**
     * The section after one of [kind] (maquetas.TRANSICIONES), never the same twice. At level 3 the melody is the next
     * layer to come in, with its own A, so A is not chosen then (the prototype knows when the melody will come; a run
     * does not).
     */
    private fun chooseNext(kind: Int, rng: PyRandom): Int {
        val next = TRANSITIONS_TO[kind]
        val weight = TRANSITIONS_WEIGHT[kind]
        var options = 0
        for (i in next.indices) if (next[i] != kind || kind == Composer.TENSE) options++
        val avoidA = level == MELODY_LEVEL - 1 && options > 1
        var total = 0.0
        for (i in next.indices) if (allowed(next[i], kind, avoidA)) total += weight[i]
        var r = rng.uniform(0.0, total)
        var last = kind
        for (i in next.indices) {
            if (!allowed(next[i], kind, avoidA)) continue
            last = next[i]
            r -= weight[i]
            if (r <= 0) return next[i]
        }
        return last
    }

    private fun allowed(candidate: Int, kind: Int, avoidA: Boolean): Boolean =
        (candidate != kind || kind == Composer.TENSE) && !(avoidA && candidate == Composer.A)

    /** The notes of bar [b] of the section, with their times, velocities and lengths, into the channels' queues. */
    private fun scheduleBar(
        b: Int,
        start: Double,
        semi: Double,
        intensity: Double,
    ) {
        val swing = style.swing
        for (c in 0 until Chip.MUSIC_VOICES) {
            val list = notes[c]
            val ord = order[c]
            val q = queues[c]
            val firstNew = q.size
            while (cursor[c] < list.size && list.pos[ord[cursor[c]]] < (b + 1) * steps) {
                val i = ord[cursor[c]]
                cursor[c]++
                if (list.dur[i] < 0 || list.pos[i] < b * steps) continue
                val p = list.pos[i]
                val local = p - steps * (p / steps)
                val pair = 2 * (local / 2)
                val rest = local - pair
                val at = if (rest < 1) pair + rest * 2 * swing else pair + 2 * swing + (rest - 1) * 2 * (1 - swing)
                val time = max(0.0, start + at * semi + list.offset[i]).toLong()
                val accent = if (p % steps == 0) {
                    1.06
                } else if (p % beat == 0) {
                    1.0
                } else {
                    0.93
                }
                val vel = list.vel[i] * accent * intensity * list.spread[i]
                val held = max(1, (list.dur[i] * semi * Composer.gate[c]).toInt())
                val instrument = bank.instruments[list.instrument[i]]
                val s = q.push()
                q.start[s] = time
                q.length[s] = held
                q.instrument[s] = list.instrument[i]
                q.pitch[s] = list.pitch[i]
                q.quality[s] = list.quality[i]
                q.ornament[s] = list.ornament[i]
                q.ornamentSemitones[s] = list.ornamentSemitones[i]
                q.ornamentHz[s] = list.ornamentHz[i]
                q.ornamentMs[s] = list.ornamentMs[i]
                q.gain[s] = vel * instrument.recipe.volume * style.channelVolumes[c]
                q.section[s] = sectionId
                q.pos[s] = p
                q.steps[s] = list.dur[i]
                q.semi[s] = semi
                // Keep the queue in time order (the prototype sorts each channel by time, keeping ties in order).
                var j = q.size - 1
                while (j > firstNew && q.start[q.slot(j - 1)] > q.start[q.slot(j)]) {
                    swap(q, q.slot(j - 1), q.slot(j))
                    j--
                }
            }
            onNote?.let { listen -> for (j in firstNew until q.size) listen(c, q, q.slot(j)) }
        }
    }

    /** Each layer sounds from its level, and all of them in the tense version; a change fades in or out over one beat. */
    private fun layers(
        level: Int,
        tension: Boolean,
        start: Double,
        semi: Double,
    ) {
        if (!endAt.isNaN()) return
        for (l in 0 until MusicStyle.LAYERS) {
            val on = level >= l + 1 || tension
            if (lastBarLevel < 0) {
                layerOn[l] = on
                for (c in 0 until Chip.MUSIC_VOICES) if (layerOf[c] == l) gains[c].set(if (on) 1.0 else 0.0)
            } else if (on != layerOn[l]) {
                layerOn[l] = on
                val from = start.toLong()
                val to = (start + beat * semi).toLong()
                for (c in 0 until Chip.MUSIC_VOICES) if (layerOf[c] == l) gains[c].fadeTo(if (on) 1.0 else 0.0, from, to)
            }
        }
    }

    private fun sortByPosition(list: NoteList, out: IntArray) {
        for (i in 0 until list.size) {
            out[i] = i
            var j = i
            while (j > 0 && list.pos[out[j - 1]] > list.pos[out[j]]) {
                val t = out[j - 1]
                out[j - 1] = out[j]
                out[j] = t
                j--
            }
        }
    }

    private fun swap(q: NoteQueue, a: Int, b: Int) {
        swapLong(q.start, a, b)
        swapInt(q.length, a, b)
        swapInt(q.instrument, a, b)
        swapInt(q.pitch, a, b)
        swapInt(q.quality, a, b)
        swapInt(q.ornament, a, b)
        swapDouble(q.ornamentSemitones, a, b)
        swapDouble(q.ornamentHz, a, b)
        swapDouble(q.ornamentMs, a, b)
        swapDouble(q.gain, a, b)
        swapInt(q.section, a, b)
        swapInt(q.pos, a, b)
        swapInt(q.steps, a, b)
        swapDouble(q.semi, a, b)
    }

    /**
     * A channel's volume (the layers and the final fade), linear from [from] to [to] between two times, as the
     * prototype's `automatizar` multiplies each track by its curve.
     */
    private class Ramp {
        private var from = 1.0
        private var to = 1.0
        private var start = 0L
        private var end = 0L

        fun set(value: Double) {
            from = value
            to = value
            start = 0
            end = 0
        }

        /** Goes from the value it will have at [at] to [target] between [at] and [until]. */
        fun fadeTo(target: Double, at: Long, until: Long) {
            from = valueAt(at)
            to = target
            start = at
            end = max(at + 1, until)
        }

        fun valueAt(t: Long): Double = when {
            t < start -> from
            t >= end -> to
            else -> from + (to - from) / (end - start) * (t - start)
        }

        fun apply(x: Double, t: Long): Double = when {
            t < start -> if (from == 1.0) x else x * from
            t >= end -> if (to == 1.0) x else x * to
            else -> x * (from + (to - from) / (end - start) * (t - start))
        }
    }

    companion object {
        /** Samples rendered at a time: 8 ms. */
        const val BLOCK = 256

        /** How early a bar is decided: 16 ms, more than the 5 ms of humanization plus the 8 ms of a cut. */
        const val LOOKAHEAD = 512

        const val MAX_LEVEL = 5
        const val MELODY_LEVEL = 4
        const val HYSTERESIS_BARS = 2
        const val FADE_MS = 200.0
        const val TEMPO_RISE = 0.03
        const val MAX_TEMPO_RISE = 0.12

        /** The fade in at the start: 10 ms. */
        const val FADE_IN = 320

        /** The echo's tail after the fade, before the music counts as finished: 2 s. */
        const val TAIL = 64_000

        /** The warning's level: 1 dB under the music's −20 dBFS, as in the prototype's demos. */
        val WARNING_RMS = Chip.fromDb(-21.0)

        private const val SECTION_CAPACITY = 512

        /** 44 bars, about a minute and a half: 16 rising, 8 at the top, 4 falling and 16 calm (generador.INTENSIDAD_CICLO). */
        val INTENSITY_CYCLE = DoubleArray(44) { i ->
            when {
                i < 16 -> 0.86 + 0.14 * i / 15
                i < 24 -> 1.0
                i < 28 -> 1.0 - 0.14 * (i - 23) / 4
                else -> 0.86
            }
        }

        /** Where the cycle starts when the top level is reached: at its peak, the level-5 intensity of 1.0. */
        private const val INTENSITY_PEAK = 16

        // maquetas.TRANSICIONES: from A, A', B, bridge and tension to the next sections and their weights.
        private val TRANSITIONS_TO = arrayOf(
            intArrayOf(Composer.A2, Composer.B),
            intArrayOf(Composer.B, Composer.BRIDGE),
            intArrayOf(Composer.A, Composer.A2, Composer.BRIDGE),
            intArrayOf(Composer.A, Composer.B),
            intArrayOf(Composer.TENSE),
        )
        private val TRANSITIONS_WEIGHT = arrayOf(
            doubleArrayOf(0.5, 0.5),
            doubleArrayOf(0.6, 0.4),
            doubleArrayOf(0.4, 0.3, 0.3),
            doubleArrayOf(0.7, 0.3),
            doubleArrayOf(1.0),
        )

        private fun swapLong(x: LongArray, a: Int, b: Int) {
            val t = x[a]
            x[a] = x[b]
            x[b] = t
        }

        private fun swapInt(x: IntArray, a: Int, b: Int) {
            val t = x[a]
            x[a] = x[b]
            x[b] = t
        }

        private fun swapDouble(x: DoubleArray, a: Int, b: Int) {
            val t = x[a]
            x[a] = x[b]
            x[b] = t
        }
    }
}
