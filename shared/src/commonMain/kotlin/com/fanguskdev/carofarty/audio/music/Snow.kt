package com.fanguskdev.carofarty.audio.music

import kotlin.math.abs
import kotlin.math.min

/**
 * La Nieve, navideña (generador.NIEVE and generador.Nieve): a waltz in 3/4 in E major with secondary dominants and the
 * minor plagal cadence IV–iv–I, a walking bass, strings or choir, sleigh bells always (they are its identity, so they
 * are in C0), brush, celesta, glockenspiel and bells.
 */
internal object Snow {
    // ------------------------------------------------------------------------------------------- instruments

    private val stringHarmonics = (1..10).map { Harmonic(it.toDouble(), 1.0 / DetMath.pow(it.toDouble(), 1.25)) }
    private val choirHarmonics = Synthesis.formants(
        220.0,
        listOf(Triple(350.0, 120.0, 1.0), Triple(850.0, 220.0, 0.45), Triple(2600.0, 400.0, 0.08)),
    )
    private val chordQualities = listOf(
        Quality.Major,
        Quality.Minor,
        Quality.Dominant7,
        Quality.Minor7,
        Quality.Major7,
        Quality.Minor6,
        Quality.Major6,
        Quality.Diminished7,
    )

    private fun chords(prefix: String, harmonics: List<Harmonic>): Map<Quality, SampleRecipe> =
        chordQualities.associateWith { ChordRecipe("$prefix-${it.pythonName}", it.intervals, harmonics) }

    val doubleBass = InstrumentRecipe(
        "contrabajo",
        listOf(
            PluckRecipe(
                "contrabajo", 128, 8000, 0.45, brightness = 0.25, rho = 0.998, position = 0.22, seed = 51, body = 0.5, damping = 0.35,
            ),
        ),
        Envelope(2.0, 0.0, 1.0, 650.0, 45.0),
        volume = 0.95,
        decayScale = 0.3,
        refHz = 80.0,
    )
    val strings =
        InstrumentRecipe(
            "cuerdas",
            envelope = Envelope(180.0, 2000.0, 0.8, 0.0, 350.0),
            volume = 0.4,
            chords = chords("cuerdas", stringHarmonics),
        )
    val choir =
        InstrumentRecipe(
            "coro",
            envelope = Envelope(320.0, 2500.0, 0.85, 0.0, 500.0),
            volume = 0.42,
            chords = chords("coro", choirHarmonics),
        )
    val celesta = InstrumentRecipe(
        "celesta",
        listOf(
            FmRecipe(
                "celesta", 32, 32000, 100, modulatorRatio = 4.0, indexStart = 1.3, tau = 0.035, indexLoop = 0.06,
                strike = Strike(
                    0.08,
                    2.0,
                    9000.0,
                ),
            ),
        ),
        Envelope(1.0, 0.0, 1.0, 520.0, 90.0),
        volume = 0.6,
        decayScale = 0.3,
        refHz = 1000.0,
    )
    val glockenspiel = InstrumentRecipe(
        "glockenspiel",
        listOf(
            PartialsRecipe(
                "glock-grave",
                64,
                32000,
                75,
                listOf(
                    Partial(1.0, 1.0, 1.0, 0.9),
                    Partial(2.76, 0.45, 0.06, 0.0),
                    Partial(5.40, 0.22, 0.03, 0.0),
                    Partial(8.93, 0.10, 0.015, 0.0),
                ),
                Strike(0.2, 2.0, 8000.0),
            ),
            PartialsRecipe(
                "glock-agudo",
                32,
                32000,
                150,
                listOf(Partial(1.0, 1.0, 1.0, 0.9), Partial(2.76, 0.40, 0.06, 0.0), Partial(5.40, 0.18, 0.03, 0.0)),
                Strike(0.15, 2.0, 9000.0),
            ),
        ),
        Envelope(1.0, 0.0, 1.0, 900.0, 140.0),
        volume = 0.62,
        decayScale = 0.35,
        refHz = 900.0,
    )

    /** A carillon bell: the pitch heard is its second partial's, so its root is twice the cycle's. */
    val bell = InstrumentRecipe(
        "campana",
        listOf(
            PartialsRecipe(
                "campana",
                64,
                32000,
                300,
                listOf(
                    Partial(1.0, 0.45, 1.2, 0.3),
                    Partial(2.0, 1.0, 0.9, 0.55),
                    Partial(2.4, 0.45, 0.35, 0.0),
                    Partial(3.0, 0.3, 0.6, 0.12),
                    Partial(4.0, 0.55, 0.5, 0.18),
                    Partial(5.33, 0.22, 0.15, 0.0),
                    Partial(6.0, 0.12, 0.2, 0.03),
                ),
                Strike(0.12, 3.0, 7000.0),
                rootScale = 2.0,
            ),
        ),
        Envelope(1.0, 0.0, 1.0, 1700.0, 300.0),
        volume = 0.5,
        decayScale = 0.3,
        refHz = 900.0,
    )
    val sleighBells = InstrumentRecipe(
        "cascabeles_suaves",
        listOf(
            MetalRecipe(
                "cascabel", 32000,
                listOf(
                    MetalPartial(4180.0, 0.25, 0.04),
                    MetalPartial(5320.0, 0.2, 0.035),
                    MetalPartial(6650.0, 0.2, 0.03),
                    MetalPartial(7900.0, 0.15, 0.025),
                ),
                0.14, noise = 0.6, noiseTau = 0.022, highPassHz = 5000.0, shakes = doubleArrayOf(0.0, 0.011), seed = 91,
            ),
        ),
        Envelope(0.0, 0.0, 1.0, 0.0, 15.0),
        volume = 0.16,
        fixedHz = 32000.0,
    )
    val shake = InstrumentRecipe(
        "sacudida",
        listOf(
            MetalRecipe(
                "sacudida", 32000,
                listOf(
                    MetalPartial(4180.0, 0.2, 0.05),
                    MetalPartial(5320.0, 0.2, 0.05),
                    MetalPartial(6650.0, 0.15, 0.04),
                    MetalPartial(7900.0, 0.12, 0.03),
                ),
                0.5, noise = 0.6, noiseTau = 0.03, highPassHz = 5000.0, shakes = DoubleArray(14) { 0.025 * it }, seed = 92,
            ),
        ),
        Envelope(0.0, 0.0, 1.0, 0.0, 40.0),
        volume = 0.12,
        fixedHz = 32000.0,
    )
    val kick = InstrumentRecipe(
        "bombo",
        listOf(MembraneRecipe("bombo", 16000, 150.0, 48.0, 0.028, 0.13, 0.32, noise = 0.25, noiseTau = 0.003, noiseCutoff = 3000.0)),
        Envelope(0.0, 0.0, 1.0, 0.0, 20.0),
        volume = 0.95,
        fixedHz = 16000.0,
    )
    val triangle = InstrumentRecipe(
        "triangulo",
        listOf(
            MetalRecipe(
                "triangulo",
                32000,
                listOf(MetalPartial(3150.0, 0.5, 0.9), MetalPartial(5380.0, 0.35, 0.6), MetalPartial(8100.0, 0.2, 0.4)),
                1.2,
                noise = 0.05,
                noiseTau = 0.003,
            ),
        ),
        Envelope(0.0, 0.0, 1.0, 0.0, 30.0),
        volume = 0.18,
        fixedHz = 32000.0,
    )
    val brush = InstrumentRecipe("escobilla", envelope = Envelope(6.0, 50.0, 0.3, 80.0, 20.0), volume = 0.17, noise = 24)

    // --------------------------------------------------------------------------------------------------- data

    /**
     * The level of the whole mix, as the prototype normalizes its demos: the 84 s of the demo run (seed 7) at −20 dBFS
     * RMS unless the peak would pass −1.3 dBFS; here its peak would pass −1.3 dBFS first, so it sits at −21.4 dBFS.
     */
    private const val MASTER_GAIN = 2.7909

    val style = MusicStyle(
        name = "nieve",
        tonic = midi("E4"),
        modes = mapOf(SectionKind.A to Modes.major),
        bpm = 100.0,
        stepsPerBeat = 4,
        beatsPerBar = 3,
        swing = 0.5,
        strongSteps = intArrayOf(0),
        // E major with the Christmas turns: VI7 (V7/ii), III7 (V7/vi), II7 (V7/V), I7 (V7/IV) and iv (minor plagal)
        chords = listOf(
            ChordDef("I", intArrayOf(0, 4, 7), intArrayOf(11, 2, 9)),
            ChordDef("I7", intArrayOf(0, 4, 7, 10), intArrayOf(2, 9)),
            ChordDef("IV", intArrayOf(5, 9, 0), intArrayOf(4, 7, 2), Quality.Major7),
            ChordDef("iv", intArrayOf(5, 8, 0), intArrayOf(2), Quality.Minor6),
            ChordDef("ii", intArrayOf(2, 5, 9, 0), intArrayOf(4, 7)),
            ChordDef("V7", intArrayOf(7, 11, 2, 5), intArrayOf(9, 4)),
            ChordDef("vi", intArrayOf(9, 0, 4, 7), intArrayOf(11, 2)),
            ChordDef("VI7", intArrayOf(9, 1, 4, 7), intArrayOf(11, 2)),
            ChordDef("III7", intArrayOf(4, 8, 11, 2), intArrayOf(6)),
            ChordDef("II7", intArrayOf(2, 6, 9, 0), intArrayOf(4, 11)),
        ),
        markov = mapOf(
            SectionKind.A to mapOf(
                "I" to listOf("vi" to 2.5, "VI7" to 2.0, "IV" to 2.0, "I7" to 1.5, "ii" to 1.0, "III7" to 1.0),
                "vi" to listOf("ii" to 3.0, "II7" to 2.0, "IV" to 1.5),
                "VI7" to listOf("ii" to 4.0),
                "ii" to listOf("V7" to 4.0, "II7" to 0.5),
                "II7" to listOf("V7" to 3.0, "ii" to 1.0),
                "V7" to listOf("I" to 4.0, "vi" to 1.0),
                "IV" to listOf("iv" to 3.0, "V7" to 1.5, "I" to 1.0, "ii" to 1.0),
                "iv" to listOf("I" to 4.0),
                "I7" to listOf("IV" to 4.0),
                "III7" to listOf("vi" to 4.0),
            ),
        ),
        phrases = listOf(
            Triple("I", "vi", "ii"), Triple("vi", "ii", "V7"), Triple("ii", "V7", "I"), Triple("I", "VI7", "ii"),
            Triple("VI7", "ii", "V7"), Triple("I7", "IV", "iv"), Triple("IV", "iv", "I"), Triple("III7", "vi", "II7"),
            Triple("vi", "II7", "V7"),
        ),
        start = mapOf(SectionKind.A to "I", SectionKind.A2 to "I", SectionKind.B to "IV", SectionKind.Bridge to "vi"),
        cadences = mapOf(
            SectionKind.A to listOf("V7", "iv"),
            SectionKind.B to listOf("V7", "iv"),
            SectionKind.Bridge to listOf("V7", "iv"),
        ),
        harmonicRhythm = mapOf(SectionKind.A to 1, SectionKind.A2 to 1, SectionKind.B to 1, SectionKind.Bridge to 1),
        roles = mapOf("bass" to doubleBass, "pad" to strings, "c2" to celesta, "c3" to glockenspiel, "c3b" to bell),
        ranges = mapOf(
            "bass" to midi("E2")..midi("B3"),
            "pad" to midi("E3")..midi("D#4"),
            "c2" to midi("E5")..midi("E7"),
            "c3" to midi("B4")..midi("E6"),
        ),
        // Caro's motif as a waltz: two jumps up, the long C#, and the fall that bounces down to the E
        leitmotif = listOf(
            MotifNoteAbsolute(0, 2, midi("F#5")), MotifNoteAbsolute(2, 2, midi("G#5")), MotifNoteAbsolute(4, 4, midi("B5")),
            MotifNoteAbsolute(8, 4, midi("G#5")), MotifNoteAbsolute(12, 8, midi("C#6")), MotifNoteAbsolute(20, 2, midi("B5")),
            MotifNoteAbsolute(22, 2, midi("G#5")), MotifNoteAbsolute(24, 4, midi("F#5")), MotifNoteAbsolute(28, 4, midi("E5")),
            MotifNoteAbsolute(32, 2, midi("F#5")), MotifNoteAbsolute(34, 2, midi("G#5")), MotifNoteAbsolute(36, 2, midi("B4")),
            MotifNoteAbsolute(38, 2, midi("C#5")), MotifNoteAbsolute(40, 8, midi("E5")),
        ),
        motifs = listOf(
            listOf(MotifNote(0, 4, 0), MotifNote(4, 4, 1), MotifNote(8, 4, 2), MotifNote(12, 12, 4)),
            listOf(MotifNote(0, 8, 4), MotifNote(8, 4, 3), MotifNote(12, 4, 2), MotifNote(16, 4, 1), MotifNote(20, 4, 0)),
            listOf(MotifNote(0, 2, 0), MotifNote(2, 2, 1), MotifNote(4, 8, 3)),
        ),
        echo = EchoSettings(6, 0x48, 0x2C),
        echoChannels = intArrayOf(1, 4, 5),
        channelVolumes = doubleArrayOf(0.085, 0.359, 1.0, 0.227, 0.101, 0.19),
        layers = listOf(intArrayOf(0, 1, 2), intArrayOf(3), intArrayOf(4), intArrayOf(5)),
        percussionChannels = intArrayOf(2, 3),
        masterGain = MASTER_GAIN,
        arrangement = Parts,
    )

    /** What the Nieve plays besides the melody, with its pattern tables (generador.NIEVE's tension, figuras and ritmos). */
    private object Parts : ArrangementRecipe {
        override val instruments get() = listOf(choir, sleighBells, shake, kick, triangle, brush)

        override fun create(composer: Composer): Arrangement = SnowArrangement(composer)
    }

    // Tension: vi–II7–ii–V7 over a pedal on B (the dominant, 7 semitones over the tonic).
    val tensionChords = listOf("vi", "II7", "ii", "V7")
    const val TENSION_PEDAL = 7

    // The waltz figures of the celesta: (position, length, index of the chord's note) in 12 steps.
    val celestaFigures = listOf(
        listOf(intArrayOf(4, 2, 2), intArrayOf(6, 2, 3), intArrayOf(8, 2, 1), intArrayOf(10, 2, 2)), // the «oom-pah» of 2 and 3
        listOf(
            intArrayOf(0, 2, 0),
            intArrayOf(2, 2, 1),
            intArrayOf(4, 2, 2),
            intArrayOf(6, 2, 3),
            intArrayOf(8, 2, 4),
            intArrayOf(10, 2, 3),
        ),
        listOf(intArrayOf(4, 4, 2), intArrayOf(8, 1, 3), intArrayOf(9, 1, 4), intArrayOf(10, 1, 5), intArrayOf(11, 1, 4)), // music box
    )
    val celestaBySection = intArrayOf(0, 1, 2, 0) // A, A', B, bridge

    // Sleigh bells in quavers, accent on 1, and a shake every four bars.
    val bellSteps = intArrayOf(0, 2, 4, 6, 8, 10)
    val bellVelocities = doubleArrayOf(0.85, 0.62, 0.48)
    const val SHAKE_EVERY = 4

    // The brush on 2 and 3.
    val brushSteps = intArrayOf(4, 8)
    val brushVelocities = doubleArrayOf(0.6, 0.5)
}

/** The parts of the Nieve (generador.Nieve), note for note. */
internal class SnowArrangement(private val c: Composer) : Arrangement() {
    private val rng = c.rng
    private val steps = c.steps
    private val bass = c.bank.indexOf(Snow.doubleBass)
    private val strings = c.bank.indexOf(Snow.strings)
    private val choir = c.bank.indexOf(Snow.choir)
    private val celesta = c.bank.indexOf(Snow.celesta)
    private val glockenspiel = c.bank.indexOf(Snow.glockenspiel)
    private val bells = c.bank.indexOf(Snow.sleighBells)
    private val shake = c.bank.indexOf(Snow.shake)
    private val kick = c.bank.indexOf(Snow.kick)
    private val triangle = c.bank.indexOf(Snow.triangle)
    private val brush = c.bank.indexOf(Snow.brush)
    private val bassRange = c.range("bass")
    private val padRange = c.range("pad")
    private val c2Range = c.range("c2")
    private val c3Range = c.range("c3")
    private val tensionChords = IntArray(Composer.BARS) { c.chord(Snow.tensionChords[it % Snow.tensionChords.size]) }
    private val figures = Snow.celestaFigures.map { figure -> Array(figure.size) { figure[it] } }.toTypedArray()
    private val tones = IntArray(64)
    private val others = IntArray(64)
    private var fill = true

    override fun mutate() {
        fill = rng.random() < 0.7
    }

    /** The walking bass in crotchets: the root, a note of the chord towards the next and a chromatic approach to its root. */
    private fun bass(chords: IntArray, nextChord: Int, out: NoteList) {
        val low = bassRange.first
        val high = bassRange.last
        var previous = low + 7
        for (b in 0 until Composer.BARS) {
            val chord = chords[b]
            val next = if (b < 7) chords[b + 1] else nextChord
            val root = c.root(chord, low, high - 6, previous)
            val target = c.root(next, low, high - 6, root)
            val count = c.tones(chord, low, high, tones)
            var n = 0
            for (i in 0 until count) if (tones[i] != root) others[n++] = tones[i]
            var towards = 0
            for (i in 0 until n) if ((others[i] - root) * (target - root) > 0) tones[towards++] = others[i]
            val pool = if (towards > 0) tones else others
            val poolSize = if (towards > 0) towards else n
            // min(hacia, key = |m − (root + target) / 2| + random()): one random number per candidate, in order.
            var second = pool[0]
            var bestKey = 0.0
            for (i in 0 until poolSize) {
                val key = abs(pool[i] - (root + target) / 2.0) + rng.random()
                if (i == 0 || key < bestKey) {
                    bestKey = key
                    second = pool[i]
                }
            }
            var third: Int
            if (target == root) {
                third = second + THIRD_STEPS[rng.below(3)]
            } else {
                third = if (target > second) target - 1 else target + 1
                if (third == second) third = if (target > second) target + 1 else target - 1
            }
            out.add(b * steps, 4, root, 0.95, bass)
            out.add(b * steps + 4, 4, second, 0.78, bass)
            out.add(b * steps + 8, 4, third, 0.78, bass)
            previous = root
        }
    }

    /** The pad: one chord a bar, held over when the next is the same. */
    private fun pad(
        chords: IntArray,
        bars: Int,
        instrument: Int,
        out: NoteList,
    ) {
        for (b in 0 until bars) {
            val chord = chords[b]
            val root = c.root(chord, padRange.first, padRange.last, padRange.first + 5)
            val quality = c.chordQuality[chord]
            val last = out.size - 1
            if (last >= 0 && out.pitch[last] == root && out.quality[last] == quality && out.pos[last] + out.dur[last] == b * steps) {
                out.dur[last] += steps
            } else {
                out.add(b * steps, steps, root, 0.8, instrument, quality)
            }
        }
    }

    /** The sleigh bells: in quavers with a shake every four bars, or in semiquavers when the sleigh speeds up. */
    private fun sleighBells(bars: Int, fast: Boolean, out: NoteList) {
        for (b in 0 until bars) {
            val count = if (fast) steps else Snow.bellSteps.size
            for (k in 0 until count) {
                val p = if (fast) k else Snow.bellSteps[k]
                var v = when {
                    p == 0 -> Snow.bellVelocities[0]
                    p % 4 == 0 -> Snow.bellVelocities[1]
                    else -> Snow.bellVelocities[2]
                }
                if (fast) v *= 0.7
                out.add(b * steps + p, 2, 0, v * (1 + rng.uniform(-0.15, 0.15)), bells)
            }
            if (!fast && b % Snow.SHAKE_EVERY == Snow.SHAKE_EVERY - 1) {
                var i = 0
                while (i < out.size) {
                    if (out.pos[i] >= b * steps + 8 && out.pos[i] < b * steps + 12) out.removeAt(i) else i++
                }
                out.add(b * steps + 8, 4, 0, 0.7, shake)
            }
        }
    }

    /** Brush on 2 and 3, a soft kick and the triangle; denser in the tense version. */
    private fun percussion(
        bars: Int,
        final: Boolean,
        dense: Boolean,
        out: NoteList,
    ) {
        for (b in 0 until bars) {
            val bar = b * steps
            if (dense) {
                out.add(bar, 4, 0, 0.5, kick)
                for (p in DENSE_BRUSH) out.add(bar + p, 2, 0, 0.42 + (if (p == 4 || p == 8) 0.15 else 0.0), brush)
                continue
            }
            if (b % 4 == 1 || b % 4 == 3 || b == 0) {
                if (b % 2 == 0) out.add(bar, 4, 0, 0.42, kick) else out.add(bar, 8, 0, 0.5, triangle)
            } else if (b % 4 == 2) {
                out.add(bar, 4, 0, 0.42, kick)
            }
            for (k in Snow.brushSteps.indices) out.add(bar + Snow.brushSteps[k], 4, 0, Snow.brushVelocities[k], brush)
            if (final && b == bars - 1 && fill) {
                var i = 0
                while (i < out.size) {
                    if (out.pos[i] >= bar + 6) out.removeAt(i) else i++
                }
                for (j in FILL.indices) out.add(bar + FILL[j], 2, 0, 0.45 + 0.06 * j, brush)
            }
        }
    }

    override fun parts(
        kind: Int,
        chords: IntArray,
        nextChord: Int,
        out: Array<NoteList>,
    ) {
        bass(chords, nextChord, out[0])
        pad(chords, Composer.BARS, if (kind == Composer.A || kind == Composer.A2) strings else choir, out[1])
        sleighBells(Composer.BARS, fast = false, out = out[2])
        percussion(Composer.BARS, final = true, dense = false, out = out[3])
        val figure = figures[if (rng.random() < 0.75) Snow.celestaBySection[kind] else rng.below(figures.size)]
        for (b in 0 until Composer.BARS) {
            val count = c.tones(chords[b], c2Range.first, c2Range.last, tones)
            for (note in figure) out[4].add(b * steps + note[0], note[1], tones[min(note[2], count - 1)], 0.62, celesta)
        }
    }

    /** Now and then a grace note of a semiquaver before a long note, like a carillon. */
    override fun ornaments(
        notes: NoteList,
        scale: Scale,
        kind: Int,
        out: NoteList,
    ) = graceNotes(notes, scale, out, probability = 0.15, rng = rng)

    override fun tension(bars: Int, out: Array<NoteList>) {
        val scale = c.scales[Composer.TENSE]
        var pedal = 0
        for (m in bassRange.first..bassRange.last) {
            if ((m - c.tonic - Snow.TENSION_PEDAL).mod(12) == 0) {
                pedal = m
                break
            }
        }
        for (b in 0 until bars) {
            out[0].add(b * steps, 4, pedal, 0.95, bass)
            out[0].add(b * steps + 4, 4, pedal, 0.75, bass)
            out[0].add(b * steps + 8, 4, pedal, 0.75, bass)
        }
        pad(tensionChords, bars, choir, out[1])
        sleighBells(bars, fast = true, out = out[2])
        percussion(bars, final = true, dense = true, out = out[3])
        for (b in 0 until bars) {
            val count = c.tones(tensionChords[b], c2Range.first, c2Range.last, tones)
            for (p in 0 until steps) {
                out[4].add(b * steps + p, 1, tones[min(1 + p % 2, count - 1)], 0.45 + (if (p % 4 == 0) 0.15 else 0.0), celesta)
            }
        }
        risingMotif(c, scale, bars, before = 12, chords = tensionChords, instrument = glockenspiel, out = out[5], c3 = c3Range)
    }

    private companion object {
        val THIRD_STEPS = intArrayOf(-2, 2, 1)
        val DENSE_BRUSH = intArrayOf(2, 4, 6, 8, 10)
        val FILL = intArrayOf(6, 8, 9, 10, 11)
    }
}

/**
 * The adornos of the Nieve and the Bosque: before a long note (6 sixteenths or more) that follows a note of 3 or more
 * that ends right where it starts, with [probability], a grace note one degree up that takes the last sixteenth of the
 * previous note.
 */
internal fun graceNotes(
    notes: NoteList,
    scale: Scale,
    out: NoteList,
    probability: Double,
    rng: PyRandom,
) {
    out.clear()
    for (i in 0 until notes.size) {
        val p = notes.pos[i]
        val d = notes.dur[i]
        val last = out.size - 1
        if (i > 0 && d >= 6 && last >= 0 && out.dur[last] >= 3 && out.pos[last] + out.dur[last] == p && rng.random() < probability) {
            out.dur[last] -= 1
            out.add(p - 1, 1, scale.note(scale.degree(notes.pitch[i]) + 1))
        }
        out.append(notes, i)
    }
}

/**
 * The tense version's melody: the start of Caro's motif (its notes before [before]) every two bars, one degree higher
 * each time, harmonized and brought into the melody's range ([c3]).
 */
internal fun risingMotif(
    c: Composer,
    scale: Scale,
    bars: Int,
    before: Int,
    chords: IntArray,
    instrument: Int,
    out: NoteList,
    c3: IntRange,
) {
    val first = out.size
    var b = 0
    while (b < bars) {
        val count = c.loadLeitSteps(scale, before)
        val anchor = scale.note(scale.degree(c.leitFirstPitch) + (b / 2) % 4)
        c.placeLoaded(scale, count, anchor, b * c.steps, out)
        b += 2
    }
    c.harmonize(out, first, scale, chords, bars)
    c.toRange(out, first, c3.first, c3.last)
    for (i in first until out.size) {
        out.vel[i] = Composer.MELODY_VELOCITY
        out.instrument[i] = instrument
    }
}
