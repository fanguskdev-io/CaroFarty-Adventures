package com.fanguskdev.carofarty.audio.music

import kotlin.math.abs
import kotlin.math.min

/**
 * What the three scenarios share (generador.Compositor): harmony from a second-order Markov chain, a melody in cells
 * of 2 bars built from Caro's motif and the scenario's motifs, and the humanization of every note. The scenario's
 * [arrangement] writes its parts. It draws from Python's generator ([rng]) in the prototype's order, so a seed gives
 * the same choices as in Python.
 *
 * It composes a section when the section starts, on the audio thread: everything it needs is allocated here, once.
 */
internal class Composer(val style: MusicStyle, val bank: MusicBank, seed: Long) {
    val rng = PyRandom(seed)

    /** Sixteenths in a bar (P in the prototype) and in a beat. */
    val steps = style.stepsPerBar
    val beat = style.stepsPerBeat
    val tonic = style.tonic

    private val chordNames = style.chords.map { it.name }
    val chordCount = chordNames.size
    val chordQuality = IntArray(chordCount) { style.chords[it].quality.ordinal }
    private val chordRoot = IntArray(chordCount) { style.chords[it].pitchClasses[0] }
    private val chordTone =
        Array(chordCount) { c -> BooleanArray(12).also { for (p in style.chords[c].pitchClasses) it[p.mod(12)] = true } }
    private val chordAdmits = Array(chordCount) { c ->
        chordTone[c].copyOf().also { for (p in style.chords[c].colors) it[p.mod(12)] = true }
    }

    /** The index of a chord of the style by its name. */
    fun chord(name: String): Int {
        val i = chordNames.indexOf(name)
        require(i >= 0) { "${style.name} has no chord $name" }
        return i
    }

    private fun baseOf(kind: Int): Int = if (kind == A2) A else kind

    private fun <T> byKind(kind: Int, table: Map<SectionKind, T>): T = table[SectionKind.entries[kind]] ?: table.getValue(SectionKind.A)

    private val markovNext = Array(KINDS) { kind ->
        val table = byKind(baseOf(kind), style.markov)
        Array(chordCount) { from -> table[chordNames[from]].orEmpty().map { chord(it.first) }.toIntArray() }
    }
    private val markovWeight = Array(KINDS) { kind ->
        val table = byKind(baseOf(kind), style.markov)
        Array(chordCount) { from -> table[chordNames[from]].orEmpty().map { it.second }.toDoubleArray() }
    }
    private val phrase = BooleanArray(chordCount * chordCount * chordCount).also {
        for ((a, b, c) in style.phrases) it[(chord(a) * chordCount + chord(b)) * chordCount + chord(c)] = true
    }
    private val startChord = IntArray(KINDS) { kind ->
        chord(style.start[SectionKind.entries[kind]] ?: style.start.getValue(SectionKind.A))
    }
    private val cadence = Array(KINDS) { kind ->
        BooleanArray(chordCount).also { set -> for (name in byKind(baseOf(kind), style.cadences)) set[chord(name)] = true }
    }
    private val firstCadenceOfA = chord(style.cadences.getValue(SectionKind.A)[0])
    private val harmonicRhythm = IntArray(KINDS) { style.harmonicRhythm[SectionKind.entries[it]] ?: 1 }

    /** The scale of each kind of section. */
    val scales = Array(KINDS) { Scale(tonic, byKind(it, style.modes)) }

    /** The MIDI range of a role ("bass", "pad", "c2", "c3"). */
    fun range(role: String): IntRange = style.ranges[role] ?: throw IllegalArgumentException("${style.name} has no range for $role")

    private val melodyLow = range("c3").first
    private val melodyHigh = range("c3").last

    // Caro's motif as this scenario plays it.
    val leitCount = style.leitmotif.size
    val leitPos = IntArray(leitCount) { style.leitmotif[it].pos }
    val leitDur = IntArray(leitCount) { style.leitmotif[it].dur }
    val leitPitch = IntArray(leitCount) { style.leitmotif[it].pitch }
    private val leitOrnament = Array(leitCount) { style.leitmotif[it].ornament }
    private val leitCells: Int

    private val motifCount = style.motifs.size
    private val motifPos = Array(motifCount) { m -> IntArray(style.motifs[m].size) { style.motifs[m][it].pos } }
    private val motifDur = Array(motifCount) { m -> IntArray(style.motifs[m].size) { style.motifs[m][it].dur } }
    private val motifStep = Array(motifCount) { m -> IntArray(style.motifs[m].size) { style.motifs[m][it].step } }

    private val melodyInstrument = bank.role("c3")
    private val melodyInstrumentB = bank.role("c3b")
    private val percussion = BooleanArray(Chip.MUSIC_VOICES).also { for (c in style.percussionChannels) it[c] = true }

    // What changes from section to section.
    private val uses = IntArray(KINDS)
    private val progressions = Array(KINDS) { IntArray(BARS) }
    private var lastMotif = -1

    // Scratch, so that composing allocates nothing.
    private val sequence = IntArray(BARS)
    private val weights = DoubleArray(chordCount)
    private val candidates = IntArray(chordCount)
    private val toneScratch = IntArray(MAX_TONES)
    private val shapePos = IntArray(MAX_MOTIF)
    private val shapeDur = IntArray(MAX_MOTIF)
    private val shapeStep = IntArray(MAX_MOTIF)
    private val melody = NoteList(MELODY_CAPACITY)
    private val cell = NoteList(MELODY_CAPACITY)
    private val ornamented = NoteList(MELODY_CAPACITY)
    private val cleaned = NoteList(MELODY_CAPACITY)
    private val melodyOut = NoteList(MELODY_CAPACITY)

    /** The scenario's parts. */
    val arrangement: Arrangement

    init {
        var longest = 0
        for (i in 0 until leitCount) longest = maxOf(longest, leitPos[i] + leitDur[i])
        leitCells = -((-longest).floorDiv(2 * steps))
        // As the prototype: the progressions of A, B and the bridge first, then the scenario's state.
        progression(A, progressions[A])
        progression(B, progressions[B])
        progression(BRIDGE, progressions[BRIDGE])
        arrangement = style.arrangement.create(this)
    }

    /** The chord a section of [kind] starts on (generador's `inicio`; the tense version uses A's). */
    fun startChordOf(kind: Int): Int = startChord[kind]

    // ------------------------------------------------------------------------------------------------- harmony

    /** Python's `elegir` over the next chords of [b] after [a]: phrases weigh 2.5 times more, going back to [a] 0.3. */
    private fun nextChord(base: Int, a: Int, b: Int): Int {
        val next = markovNext[base][b]
        val w = markovWeight[base][b]
        var total = 0.0
        for (i in next.indices) {
            val c = next[i]
            var p = w[i]
            if (phrase[(a * chordCount + b) * chordCount + c]) {
                p *= 2.5
            } else if (c == a) {
                p *= 0.3
            }
            weights[i] = p
            total += p
        }
        var r = rng.uniform(0.0, total)
        if (next.isEmpty()) return b
        for (i in next.indices) {
            r -= weights[i]
            if (r <= 0) return next[i]
        }
        return next[next.size - 1]
    }

    /** Whether Caro's motif, as it is, fits the first bars: its strong notes on the chord or its colours. */
    private fun motifFits(chords: IntArray): Boolean {
        for (i in 0 until leitCount) {
            if (isStrong(leitPos[i], leitDur[i]) && !admits(chords[leitPos[i] / steps], leitPitch[i])) return false
        }
        return true
    }

    /** 8 bars of chords that start where they should and end on a cadence (generador.progresion). */
    private fun progression(kind: Int, out: IntArray) {
        val base = baseOf(kind)
        val rhythm = harmonicRhythm[kind]
        val k = BARS / rhythm
        repeat(MAX_TRIES) {
            var a = firstCadenceOfA
            var b = startChord[kind]
            sequence[0] = b
            var length = 1
            while (length < k) {
                val c = nextChord(base, a, b)
                sequence[length++] = c
                a = b
                b = c
            }
            if (!cadence[base][sequence[length - 1]] || distinct(sequence, length) < min(3, k)) return@repeat
            for (i in 0 until BARS) out[i] = sequence[i / rhythm]
            if (base == A && !motifFits(out)) return@repeat
            return
        }
        error("no progression for ${style.name} $kind")
    }

    private fun distinct(x: IntArray, n: Int): Int {
        var count = 0
        for (i in 0 until n) {
            var seen = false
            for (j in 0 until i) if (x[j] == x[i]) seen = true
            if (!seen) count++
        }
        return count
    }

    /** The chords of a section (generador.Compositor.acordes): a new progression every three uses; A' changes 1 or 2. */
    private fun chordsFor(kind: Int, out: IntArray) {
        val n = uses[kind]
        uses[kind] = n + 1
        val base = baseOf(kind)
        if (n != 0 && n % 3 == 0) progression(base, progressions[base])
        progressions[base].copyInto(out)
        if (kind != A2) return
        val rhythm = harmonicRhythm[A2]
        val times = 1 + rng.below(2)
        repeat(times) {
            val i = rng.range(1, 7 / rhythm) * rhythm
            val before = out[i - 1]
            val after = out[min(7, i + rhythm)]
            var count = 0
            val next = markovNext[A][before]
            for (c in next) if (c != out[i] && markovNext[A][c].contains(after)) candidates[count++] = c
            if (count > 0) {
                val c = candidates[rng.below(count)]
                for (j in i until min(BARS, i + rhythm)) out[j] = c
            }
        }
    }

    // ------------------------------------------------------------------------------------------ chords and roots

    /** Acorde.admite: the note is in the chord or among its colours. */
    fun admits(chord: Int, note: Int): Boolean = chordAdmits[chord][(note - tonic).mod(12)]

    /** Acorde.tonos: the notes of the chord (without colours) from [low] to [high], into [out]; returns how many. */
    fun tones(
        chord: Int,
        low: Int,
        high: Int,
        out: IntArray,
    ): Int {
        var n = 0
        for (m in low..high) if (chordTone[chord][(m - tonic).mod(12)]) out[n++] = m
        return n
    }

    /** Escenario.fuerte: a strong position of the bar, or a note of a beat or longer. */
    fun isStrong(pos: Int, dur: Int): Boolean {
        val inBar = pos.mod(steps)
        for (s in style.strongSteps) if (s == inBar) return true
        return dur >= beat
    }

    /** Compositor.raiz: the root of [chord] closest to [previous] between [low] and [high] (or in the octave from [low]). */
    fun root(
        chord: Int,
        low: Int,
        high: Int,
        previous: Int,
    ): Int {
        var best = Int.MIN_VALUE
        var bestDistance = Int.MAX_VALUE
        val pc = chordRoot[chord]
        for (m in low..high) {
            if ((m - tonic - pc).mod(12) == 0 && abs(m - previous) < bestDistance) {
                best = m
                bestDistance = abs(m - previous)
            }
        }
        if (best != Int.MIN_VALUE) return best
        for (m in low until low + 12) {
            if ((m - tonic - pc).mod(12) == 0 && abs(m - previous) < bestDistance) {
                best = m
                bestDistance = abs(m - previous)
            }
        }
        return best
    }

    /** The note of [chord] in the melody's range closest to [target], the lower one if two are as close; [fallback] if none. */
    private fun nearestTone(chord: Int, target: Int, fallback: Int = target): Int {
        val n = tones(chord, melodyLow, melodyHigh, toneScratch)
        if (n == 0) return fallback
        var best = toneScratch[0]
        for (i in 1 until n) if (abs(toneScratch[i] - target) < abs(best - target)) best = toneScratch[i]
        return best
    }

    // -------------------------------------------------------------------------------------------- motif shapes

    /**
     * _transformar and _colocar: [count] notes of the shape in the scratch (positions, lengths, steps of the scale)
     * placed from [anchor] at [from]: the same, inverted or backwards (the steps reversed, the rhythm kept).
     */
    private fun placeShape(
        scale: Scale,
        count: Int,
        transform: Int,
        anchor: Int,
        from: Int,
        out: NoteList,
    ) {
        val g0 = scale.degree(anchor)
        for (i in 0 until count) {
            val step = when (transform) {
                INVERSION -> -shapeStep[i]
                RETROGRADE -> shapeStep[count - 1 - i] - shapeStep[count - 1]
                else -> shapeStep[i]
            }
            out.add(from + shapePos[i], shapeDur[i], scale.note(g0 + step))
        }
    }

    /** Loads motif [m] into the scratch, only its notes before [before]; returns how many. */
    private fun loadMotif(m: Int, before: Int = Int.MAX_VALUE): Int {
        var n = 0
        for (i in motifPos[m].indices) {
            if (motifPos[m][i] < before) {
                shapePos[n] = motifPos[m][i]
                shapeDur[n] = motifDur[m][i]
                shapeStep[n] = motifStep[m][i]
                n++
            }
        }
        return n
    }

    /** _pasos_de: the notes of Caro's motif before [before] as steps of [scale] over the first; returns how many. */
    fun loadLeitSteps(scale: Scale, before: Int): Int {
        var n = 0
        var g0 = 0
        for (i in 0 until leitCount) {
            if (leitPos[i] >= before) continue
            if (n == 0) g0 = scale.degree(leitPitch[i])
            shapePos[n] = leitPos[i]
            shapeDur[n] = leitDur[i]
            shapeStep[n] = scale.degree(leitPitch[i]) - g0
            n++
        }
        return n
    }

    /** The first note of Caro's motif. */
    val leitFirstPitch: Int get() = leitPitch[0]

    /** Places the shape loaded by [loadLeitSteps] as it is, from [anchor] at [from], into [out]; returns its first index. */
    fun placeLoaded(
        scale: Scale,
        count: Int,
        anchor: Int,
        from: Int,
        out: NoteList,
    ): Int {
        val first = out.size
        placeShape(scale, count, SAME, anchor, from, out)
        return first
    }

    // ------------------------------------------------------------------------------------------------- melody

    /** _armonizar: strong notes from [first] on move to the closest note of their chord (or colour), 3 degrees at most. */
    fun harmonize(
        notes: NoteList,
        first: Int,
        scale: Scale,
        chords: IntArray,
        chordCountInList: Int,
        skipFlagged: Boolean = false,
    ) {
        for (i in first until notes.size) {
            if (skipFlagged && notes.flag[i]) continue
            val pos = notes.pos[i]
            val ac = chords[min(chordCountInList - 1, pos / steps)]
            var n = notes.pitch[i]
            if (isStrong(pos, notes.dur[i]) && !admits(ac, n)) {
                val g = scale.degree(n)
                for (d in NEIGHBOURS) {
                    val c = scale.note(g + d)
                    if (admits(ac, c)) {
                        n = c
                        break
                    }
                }
            }
            notes.pitch[i] = n
        }
    }

    /** _al_rango: the notes from [first] on, moved by octaves into [low]–[high] as a whole, then one by one. */
    fun toRange(
        notes: NoteList,
        first: Int,
        low: Int,
        high: Int,
    ) {
        if (notes.size <= first) return
        var top = notes.pitch[first]
        var bottom = notes.pitch[first]
        for (i in first until notes.size) {
            top = maxOf(top, notes.pitch[i])
            bottom = minOf(bottom, notes.pitch[i])
        }
        var shift = 0
        while (top + shift > high && bottom + shift - 12 >= low - 3) shift -= 12
        while (bottom + shift < low && top + shift + 12 <= high + 3) shift += 12
        for (i in first until notes.size) {
            var n = notes.pitch[i] + shift
            while (n > high + 2) n -= 12
            while (n < low - 2) n += 12
            notes.pitch[i] = n
        }
    }

    /** _limpiar: sorted by position and pitch, without overlaps, cut at [until]. */
    private fun clean(notes: NoteList, until: Int, out: NoteList) {
        cleaned.clear()
        for (i in 0 until notes.size) if (notes.pos[i] < until) cleaned.append(notes, i)
        cleaned.sortByPositionPitch()
        out.clear()
        for (i in 0 until cleaned.size) {
            val p = cleaned.pos[i]
            val next = if (i + 1 < cleaned.size) cleaned.pos[i + 1] else until
            if (next <= p) continue
            val j = out.append(cleaned, i)
            out.dur[j] = min(cleaned.dur[i], next - p)
        }
    }

    /** The melody of a section of 8 bars (generador.Compositor.melodia), as (position, length, note, ornament). */
    private fun melody(kind: Int, chords: IntArray, out: NoteList) {
        val p = steps
        val cellLength = 2 * p
        val scale = scales[kind]
        melody.clear()
        var previous = (melodyLow + melodyHigh).floorDiv(2)
        var hasPrevious = false
        var previousMotif = 0
        var previousFragment = false
        var previousTransform = SAME
        var previousAnchor = 0
        val plan = PLANS[kind]
        var c = 0
        while (c < 4) {
            val action = plan[c]
            val from = c * cellLength
            val chord = chords[min(7, 2 * c)]
            cell.clear()
            when (action) {
                LEIT -> {
                    for (i in 0 until leitCount) {
                        val j = cell.add(from + leitPos[i], leitDur[i], leitPitch[i])
                        cell.setOrnament(j, leitOrnament[i])
                        cell.flag[j] = true
                    }
                    c += leitCells - 1
                }
                LEIT_VARIATION -> {
                    val count = loadLeitSteps(scale, if (leitCells > 1) cellLength else p)
                    val transform = THREE_WAYS[rng.below(3)]
                    val anchor = nearestTone(chord, leitPitch[0] + VARIATION_OFFSETS[rng.below(4)])
                    placeShape(scale, count, transform, anchor, from, cell)
                    if (leitCells == 1) {
                        val n = loadMotif(rng.below(motifCount), before = p)
                        placeShape(scale, n, TWO_WAYS[rng.below(2)], cell.pitch[cell.size - 1], from + p, cell)
                    }
                }
                MOTIF, SEQUENCE, FRAGMENT -> {
                    val motif: Int
                    val fromFragment: Boolean
                    val transform: Int
                    val anchor: Int
                    if (action == SEQUENCE && hasPrevious) {
                        motif = previousMotif
                        fromFragment = previousFragment
                        transform = previousTransform
                        anchor = scale.note(scale.degree(previousAnchor) + SEQUENCE_STEPS[rng.below(3)])
                    } else {
                        val options = if (lastMotif >= 0 && motifCount > 1) motifCount - 1 else motifCount
                        var m = rng.below(options)
                        if (options < motifCount && m >= lastMotif) m++
                        motif = m
                        lastMotif = m
                        fromFragment = false
                        transform = FOUR_WAYS[rng.below(4)]
                        anchor = nearestTone(chord, previous + MOTIF_OFFSETS[rng.below(4)])
                    }
                    var count = loadMotif(motif, before = if (fromFragment) p else Int.MAX_VALUE)
                    var length = 0
                    for (i in 0 until count) length = maxOf(length, shapePos[i] + shapeDur[i])
                    var fragmentNow = fromFragment
                    if (action == FRAGMENT) {
                        count = loadMotif(motif, before = p)
                        fragmentNow = true
                        placeShape(scale, count, transform, anchor, from + p, cell)
                    } else if (length <= p) {
                        placeShape(scale, count, transform, anchor, from, cell)
                        val response = if (transform == SAME) INVERSION else SAME
                        val second = chords[min(7, 2 * c + 1)]
                        val anchor2 = nearestTone(second, cell.pitch[cell.size - 1], fallback = anchor)
                        placeShape(scale, count, response, anchor2, from + p, cell)
                    } else {
                        placeShape(scale, count, transform, anchor, from, cell)
                    }
                    hasPrevious = true
                    previousMotif = motif
                    previousFragment = fragmentNow
                    previousTransform = transform
                    previousAnchor = anchor
                }
                CADENCE -> {
                    val count = loadMotif(rng.below(motifCount), before = 3 * p / 4)
                    val transform = TWO_WAYS[rng.below(2)]
                    placeShape(scale, count, transform, nearestTone(chord, previous), from, cell)
                    val last = if (cell.size > 0) cell.pitch[cell.size - 1] else previous
                    val final = nearestTone(chords[7], last)
                    var i = 0
                    while (i < cell.size) {
                        if (cell.pos[i] >= from + p - 2) cell.removeAt(i) else i++
                    }
                    val length = when (rng.below(3)) {
                        0 -> p / 2
                        1 -> 2 * p / 3
                        else -> 3 * p / 4
                    }
                    cell.add(from + p, length, final)
                }
            }
            for (i in 0 until cell.size) melody.append(cell, i)
            if (melody.size > 0) previous = melody.pitch[melody.size - 1]
            c++
        }
        harmonize(melody, 0, scale, chords, BARS, skipFlagged = true)
        melody.sortByPositionLengthPitch()
        toRange(melody, 0, melodyLow, melodyHigh)
        ornamented.clear()
        arrangement.ornaments(melody, scale, kind, ornamented)
        clean(ornamented, BARS * p, out)
    }

    // ------------------------------------------------------------------------------------------------ sections

    /**
     * The notes of a section of [kind] (generador.Compositor.notas_seccion) into the six channels of [out], over its
     * chords [chords] (written here), heading for [nextChord]. Each note gets its humanization: [NoteList.offset]
     * (samples, ±5 ms) and [NoteList.spread] (±10 % of velocity); notes beyond the 8 bars get a length of −1.
     */
    fun compose(
        kind: Int,
        nextChord: Int,
        chords: IntArray,
        out: Array<NoteList>,
    ) {
        for (list in out) list.clear()
        if (kind == TENSE) {
            arrangement.tension(BARS, out)
        } else {
            chordsFor(kind, chords)
            arrangement.parts(kind, chords, nextChord, out)
            val melodyNotes = out[MELODY]
            melodyNotes.clear()
            melody(kind, chords, melodyOut)
            val instrument = if (kind == B) melodyInstrumentB else melodyInstrument
            for (i in 0 until melodyOut.size) {
                val j = melodyNotes.append(melodyOut, i)
                melodyNotes.vel[j] = MELODY_VELOCITY
                melodyNotes.instrument[j] = instrument
            }
        }
        val limit = BARS * steps
        val tight = Chip.ms(1.0).toDouble()
        val loose = Chip.ms(1.5).toDouble()
        val most = Chip.ms(5.0).toDouble()
        for (c in 0 until Chip.MUSIC_VOICES) {
            val list = out[c]
            for (i in 0 until list.size) {
                val p = list.pos[i]
                if (p >= limit || p < 0) {
                    list.dur[i] = -1
                    continue
                }
                list.dur[i] = min(list.dur[i], limit - p)
                val sigma = if (percussion[c]) tight else loose
                list.offset[i] = rng.gauss(0.0, sigma).coerceIn(-most, most)
                list.spread[i] = 1 + rng.uniform(-0.1, 0.1)
            }
        }
    }

    companion object {
        const val A = 0
        const val A2 = 1
        const val B = 2
        const val BRIDGE = 3
        const val TENSE = 4
        const val KINDS = 5
        const val BARS = 8
        const val MELODY = 5
        const val MELODY_VELOCITY = 0.92

        /** The gate of each channel: the share of its written length a note is held. */
        val gate = doubleArrayOf(0.92, 1.0, 1.0, 1.0, 0.9, 0.96)

        private const val MAX_TRIES = 20000
        private const val MAX_TONES = 128
        private const val MAX_MOTIF = 64
        private const val MELODY_CAPACITY = 256

        private const val SAME = 0
        private const val INVERSION = 1
        private const val RETROGRADE = 2
        private val THREE_WAYS = intArrayOf(SAME, SAME, INVERSION)
        private val FOUR_WAYS = intArrayOf(SAME, SAME, INVERSION, RETROGRADE)
        private val TWO_WAYS = intArrayOf(SAME, INVERSION)
        private val VARIATION_OFFSETS = intArrayOf(-3, 0, 2, 4)
        private val MOTIF_OFFSETS = intArrayOf(-2, 0, 0, 3)
        private val SEQUENCE_STEPS = intArrayOf(1, 2, -1)
        private val NEIGHBOURS = intArrayOf(1, -1, 2, -2, 3, -3)

        private const val LEIT = 0
        private const val MOTIF = 1
        private const val LEIT_VARIATION = 2
        private const val CADENCE = 3
        private const val SEQUENCE = 4
        private const val FRAGMENT = 5
        private const val NOTHING = 6

        /** The four cells of each kind of section (A, A', B, bridge). */
        private val PLANS = arrayOf(
            intArrayOf(LEIT, MOTIF, LEIT_VARIATION, CADENCE),
            intArrayOf(LEIT_VARIATION, MOTIF, MOTIF, CADENCE),
            intArrayOf(MOTIF, SEQUENCE, MOTIF, CADENCE),
            intArrayOf(NOTHING, FRAGMENT, NOTHING, FRAGMENT),
        )
    }
}
