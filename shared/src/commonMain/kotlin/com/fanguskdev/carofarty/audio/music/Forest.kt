package com.fanguskdev.carofarty.audio.music

import kotlin.math.min

/**
 * El Bosque, selvático (generador.BOSQUE and generador.Bosque): 12/8 in A dorian with melodies in the minor pentatonic,
 * a log drum with the 3:2 polyrhythm, congas and bongo, an interlocking marimba (or kalimba), pan or reed flute, and
 * birds, a frog and the rain stick now and then.
 */
internal object Forest {
    // ------------------------------------------------------------------------------------------- instruments

    private val wood = listOf(Partial(1.0, 1.0, 0.18, 0.55), Partial(2.6, 0.25, 0.025, 0.0), Partial(4.1, 0.1, 0.012, 0.0))

    val logDrum = InstrumentRecipe(
        "tronco",
        listOf(
            PartialsRecipe("tronco-125", 128, 16000, 20, wood, Strike(0.5, 6.0, 1800.0)),
            PartialsRecipe("tronco-250", 64, 16000, 40, wood, Strike(0.5, 6.0, 1800.0)),
        ),
        Envelope(1.0, 0.0, 1.0, 260.0, 40.0),
        volume = 1.0,
        decayScale = 0.3,
        refHz = 150.0,
    )
    val jungle = InstrumentRecipe(
        "selva",
        envelope = Envelope(260.0, 2000.0, 0.8, 0.0, 450.0),
        volume = 0.4,
        chords = listOf(Quality.Minor7, Quality.Dominant7, Quality.Major, Quality.Minor, Quality.Major7).associateWith {
            ChordRecipe(
                "selva-${it.pythonName}",
                it.intervals,
                listOf(Harmonic(1.0, 1.0), Harmonic(2.0, 0.3), Harmonic(3.0, 0.15), Harmonic(4.0, 0.05)),
            )
        },
    )
    val marimba = InstrumentRecipe(
        "marimba",
        listOf(
            PartialsRecipe(
                "marimba",
                32,
                16000,
                100,
                listOf(Partial(1.0, 1.0, 0.5, 0.8), Partial(4.0, 0.55, 0.05, 0.02), Partial(10.0, 0.18, 0.012, 0.0)),
                Strike(0.35, 4.0, 3000.0),
            ),
        ),
        Envelope(1.0, 0.0, 1.0, 420.0, 45.0),
        volume = 0.9,
        decayScale = 0.6,
        refHz = 400.0,
    )
    val kalimba = InstrumentRecipe(
        "kalimba",
        listOf(
            PartialsRecipe(
                "kalimba",
                32,
                16000,
                60,
                listOf(
                    Partial(1.0, 1.0, 0.6, 0.7),
                    Partial(2.0, 0.12, 0.08, 0.02),
                    Partial(5.9, 0.4, 0.025, 0.0),
                    Partial(8.3, 0.12, 0.012, 0.0),
                ),
                Strike(0.25, 3.0, 6000.0),
            ),
        ),
        Envelope(1.0, 0.0, 1.0, 700.0, 80.0),
        volume = 0.6,
        decayScale = 0.4,
        refHz = 600.0,
    )
    val panFlute = InstrumentRecipe(
        "flauta_pan",
        listOf(
            BreathRecipe(
                "flauta-pan", 32, 16000, 40, 250, listOf(Harmonic(1.0, 1.0), Harmonic(2.0, 0.08), Harmonic(3.0, 0.12)),
                breath = 0.4, bandLow = 1500.0, bandHigh = 6000.0, chiff = 0.55, chiffMs = 40.0, seed = 111,
            ),
        ),
        Envelope(25.0, 0.0, 1.0, 0.0, 90.0),
        volume = 0.62,
        vibrato = Vibrato(10.0, 5.2, 220.0),
        ornament = Ornament.slide(-0.6, 45.0),
    )
    val reedFlute = InstrumentRecipe(
        "flauta_cana",
        listOf(
            BreathRecipe(
                "flauta-cana", 32, 16000, 40, 250,
                listOf(Harmonic(1.0, 1.0), Harmonic(2.0, 0.3), Harmonic(3.0, 0.35), Harmonic(4.0, 0.1), Harmonic(5.0, 0.12)),
                breath = 0.18, bandLow = 1200.0, bandHigh = 5000.0, chiff = 0.3, chiffMs = 30.0, seed = 112,
            ),
        ),
        Envelope(30.0, 0.0, 1.0, 0.0, 90.0),
        volume = 0.58,
        vibrato = Vibrato(14.0, 5.5, 200.0),
        ornament = Ornament.slide(-0.8, 50.0),
    )
    val lowConga =
        drum(
            "conga_grave",
            MembraneRecipe("conga-g", 16000, 190.0, 164.8, 0.03, 0.22, 0.4, 0.25, 0.006, 3500.0, 0.25, 101, 1.5),
            20.0,
            0.75,
        )
    val highConga =
        drum(
            "conga_aguda",
            MembraneRecipe("conga-a", 16000, 250.0, 220.0, 0.025, 0.16, 0.3, 0.3, 0.006, 4000.0, 0.25, 102, 1.5),
            20.0,
            0.62,
        )
    val bongo =
        drum("bongo", MembraneRecipe("bongo", 16000, 360.0, 329.6, 0.015, 0.08, 0.18, 0.35, 0.005, 6000.0, 0.3, 103, 1.5), 15.0, 0.5)
    val shaker = InstrumentRecipe("shaker", envelope = Envelope(7.0, 25.0, 0.2, 30.0, 8.0), volume = 0.15, noise = 26)
    val rainStick = InstrumentRecipe(
        "palo_lluvia",
        listOf(RainStickRecipe("palo-lluvia")),
        Envelope(0.0, 0.0, 1.0, 0.0, 60.0),
        volume = 0.25,
        fixedHz = 32000.0,
        texture = true,
    )
    val whistle = bird("silbo", Call.Whistle, 0.11)
    val trill = bird("trino", Call.Trill, 0.08)
    val warble = bird("gorjeo", Call.Warble, 0.1)
    val coo = bird("arrullo", Call.Coo, 0.13)
    val frog = bird("rana", Call.Frog, 0.12)

    private fun drum(
        name: String,
        recipe: MembraneRecipe,
        releaseMs: Double,
        volume: Double,
    ) = InstrumentRecipe(name, listOf(recipe), Envelope(0.0, 0.0, 1.0, 0.0, releaseMs), volume = volume, fixedHz = 16000.0)

    /** A bird (or the frog) seeded by the length of its Spanish name, as in the prototype. */
    private fun bird(kind: String, call: Call, volume: Double) = InstrumentRecipe(
        "pajaro_$kind",
        listOf(BirdRecipe("pajaro-$kind", call, seed = kind.length.toLong())),
        Envelope(0.0, 0.0, 1.0, 0.0, 30.0),
        volume = volume,
        fixedHz = 32000.0,
    )

    // --------------------------------------------------------------------------------------------------- data

    /**
     * The level of the whole mix, as the prototype normalizes its demos: the 84 s of the demo run (seed 7) at −20 dBFS
     * RMS unless the peak would pass −1.3 dBFS; here its peak stays at −2.2 dBFS.
     */
    private const val MASTER_GAIN = 0.7756

    val style = MusicStyle(
        name = "bosque",
        tonic = midi("A4"),
        modes = mapOf(SectionKind.A to Modes.minorPentatonic),
        bpm = 120.0,
        stepsPerBeat = 6,
        beatsPerBar = 4,
        swing = 0.5,
        strongSteps = intArrayOf(0, 12),
        // A dorian (A B C D E F# G): i, IV major (the dorian colour), ♭VII, v, ♭III and ii
        chords = listOf(
            ChordDef("i", intArrayOf(0, 3, 7, 10), intArrayOf(2, 5)),
            ChordDef("IV", intArrayOf(5, 9, 0, 3), intArrayOf(7, 2)),
            ChordDef("bVII", intArrayOf(10, 2, 5), intArrayOf(0, 7)),
            ChordDef("v", intArrayOf(7, 10, 2, 5), intArrayOf(0)),
            ChordDef("bIII", intArrayOf(3, 7, 10), intArrayOf(2, 5)),
            ChordDef("ii", intArrayOf(2, 5, 9), intArrayOf(7, 0)),
        ),
        markov = mapOf(
            SectionKind.A to mapOf(
                "i" to listOf("IV" to 3.0, "bVII" to 2.5, "v" to 1.2, "bIII" to 1.0),
                "IV" to listOf("i" to 3.0, "bVII" to 1.5, "v" to 1.0),
                "bVII" to listOf("i" to 2.5, "IV" to 2.0, "bIII" to 1.0),
                "v" to listOf("i" to 2.0, "IV" to 1.5, "bVII" to 1.0),
                "bIII" to listOf("IV" to 2.0, "bVII" to 2.0, "i" to 1.0),
                "ii" to listOf("v" to 2.0, "i" to 1.0),
            ),
        ),
        phrases = listOf(
            Triple("i", "IV", "i"),
            Triple("IV", "i", "bVII"),
            Triple("i", "bVII", "IV"),
            Triple("bVII", "IV", "i"),
            Triple("bIII", "IV", "i"),
        ),
        start = mapOf(SectionKind.A to "i", SectionKind.A2 to "i", SectionKind.B to "IV", SectionKind.Bridge to "bIII"),
        cadences = mapOf(
            SectionKind.A to listOf("IV", "bVII", "v"),
            SectionKind.B to listOf("bVII", "v"),
            SectionKind.Bridge to listOf("bVII", "IV"),
        ),
        harmonicRhythm = mapOf(SectionKind.A to 2, SectionKind.A2 to 2, SectionKind.B to 2, SectionKind.Bridge to 2),
        roles = mapOf("bass" to logDrum, "pad" to jungle, "c2" to marimba, "c2b" to kalimba, "c3" to panFlute, "c3b" to reedFlute),
        ranges = mapOf(
            "bass" to midi("E2")..midi("A3"),
            "pad" to midi("E3")..midi("D4"),
            "c2" to midi("G3")..midi("E5"),
            "c3" to midi("E5")..midi("A6"),
        ),
        // Caro's motif in the minor pentatonic and in 12/8 (two bars as long as before)
        leitmotif = listOf(
            MotifNoteAbsolute(0, 2, midi("C6")), MotifNoteAbsolute(2, 2, midi("D6")), MotifNoteAbsolute(4, 4, midi("E6")),
            MotifNoteAbsolute(8, 2, midi("D6")), MotifNoteAbsolute(10, 6, midi("G6")), MotifNoteAbsolute(16, 2, midi("E6")),
            MotifNoteAbsolute(18, 2, midi("D6")), MotifNoteAbsolute(20, 4, midi("C6")), MotifNoteAbsolute(24, 4, midi("A5")),
            MotifNoteAbsolute(28, 2, midi("C6")), MotifNoteAbsolute(30, 4, midi("D6")), MotifNoteAbsolute(34, 2, midi("E5")),
            MotifNoteAbsolute(36, 4, midi("G5")), MotifNoteAbsolute(40, 8, midi("A5")),
        ),
        motifs = listOf(
            listOf(
                MotifNote(0, 2, 0),
                MotifNote(2, 2, 1),
                MotifNote(4, 2, 2),
                MotifNote(6, 4, 4),
                MotifNote(10, 2, 3),
                MotifNote(12, 6, 2),
                MotifNote(18, 2, 1),
                MotifNote(20, 4, 0),
            ),
            listOf(
                MotifNote(0, 6, 0),
                MotifNote(6, 6, 2),
                MotifNote(12, 4, 4),
                MotifNote(16, 2, 3),
                MotifNote(18, 6, 2),
                MotifNote(24, 12, 1),
                MotifNote(36, 6, 0),
                MotifNote(42, 6, -1),
            ),
            listOf(
                MotifNote(0, 4, 0),
                MotifNote(4, 4, 2),
                MotifNote(8, 4, 1),
                MotifNote(12, 2, 3),
                MotifNote(14, 2, 4),
                MotifNote(16, 8, 2),
            ),
        ),
        echo = EchoSettings(4, 0x30, 0x22),
        echoChannels = intArrayOf(1, 2, 5),
        channelVolumes = doubleArrayOf(0.378, 0.759, 1.0, 0.629, 0.452, 0.48),
        layers = listOf(intArrayOf(0, 1, 2), intArrayOf(3), intArrayOf(4), intArrayOf(5)),
        percussionChannels = intArrayOf(2, 3),
        masterGain = MASTER_GAIN,
        arrangement = Parts,
    )

    private object Parts : ArrangementRecipe {
        override val instruments get() = listOf(lowConga, highConga, bongo, shaker, rainStick, whistle, trill, warble, coo, frog)

        override fun create(composer: Composer): Arrangement = ForestArrangement(composer)
    }

    // Tension: i–ii–IV–v over a pedal on E (the fifth).
    val tensionChords = listOf("i", "ii", "IV", "v")
    const val TENSION_PEDAL = 7

    /** The log drum: (position, length, degree of the pentatonic over the root) in 24 steps, on the four beats. */
    val logFigures = listOf(
        arrayOf(intArrayOf(0, 4, 0), intArrayOf(6, 2, 0), intArrayOf(12, 4, 3), intArrayOf(18, 2, 0), intArrayOf(20, 4, 4)),
        arrayOf(
            intArrayOf(0, 6, 0),
            intArrayOf(6, 4, 0),
            intArrayOf(10, 2, 3),
            intArrayOf(12, 6, 0),
            intArrayOf(18, 4, 2),
            intArrayOf(22, 2, 1),
        ),
        arrayOf(
            intArrayOf(0, 4, 0),
            intArrayOf(6, 4, 1),
            intArrayOf(10, 2, 2),
            intArrayOf(12, 4, 3),
            intArrayOf(18, 4, 2),
            intArrayOf(22, 2, 1),
        ),
    )

    /** The interlocking marimba: the low part turns in 3 notes and the high one in 4 (indices of the chord's notes). */
    val marimbaLow = intArrayOf(0, 2, 1)
    val marimbaHigh = intArrayOf(5, 4, 6, 4)

    /** The kalimba in B, on the grid of three. */
    val kalimbaCycle = intArrayOf(0, 2, 4, 2, 1, 3)

    /** Congas on the beats (the «2») and bongo on the grid of three (the «3»); shaker in quavers. */
    val lowCongaHits = intArrayOf(0, 12)
    val lowCongaVelocities = doubleArrayOf(0.95, 0.85)
    val highCongaHits = intArrayOf(6, 18)
    val highCongaVelocities = doubleArrayOf(0.7, 0.75)
    val bongoHits = intArrayOf(4, 8, 16, 20)

    // The birds: which, on what steps, how likely, how far apart (bars), and the frog and the rain stick.
    const val BIRD_PROBABILITY = 0.38
    const val BIRD_GAP = 2
    const val FROG_PROBABILITY = 0.1
    const val RAIN_STICK_PROBABILITY = 0.45
    val birdSteps = intArrayOf(2, 6, 10, 14, 18)
}

/** The parts of the Bosque (generador.Bosque), note for note. */
internal class ForestArrangement(private val c: Composer) : Arrangement() {
    private val rng = c.rng
    private val steps = c.steps
    private val logDrum = c.bank.indexOf(Forest.logDrum)
    private val jungle = c.bank.indexOf(Forest.jungle)
    private val marimba = c.bank.indexOf(Forest.marimba)
    private val kalimba = c.bank.indexOf(Forest.kalimba)
    private val panFlute = c.bank.indexOf(Forest.panFlute)
    private val lowConga = c.bank.indexOf(Forest.lowConga)
    private val highConga = c.bank.indexOf(Forest.highConga)
    private val bongo = c.bank.indexOf(Forest.bongo)
    private val shaker = c.bank.indexOf(Forest.shaker)
    private val rainStick = c.bank.indexOf(Forest.rainStick)
    private val birds = intArrayOf(
        c.bank.indexOf(Forest.whistle),
        c.bank.indexOf(Forest.trill),
        c.bank.indexOf(Forest.warble),
        c.bank.indexOf(Forest.coo),
    )
    private val frog = c.bank.indexOf(Forest.frog)
    private val bassRange = c.range("bass")
    private val padRange = c.range("pad")
    private val c2Range = c.range("c2")
    private val c3Range = c.range("c3")
    private val tensionChords = IntArray(Composer.BARS) { c.chord(Forest.tensionChords[it % Forest.tensionChords.size]) }
    private val tones = IntArray(64)
    private val options = IntArray(4)
    private val hitInstrument = IntArray(24)
    private val hitVelocity = DoubleArray(24)
    private var bongoRotation = 0
    private var fill = true
    private var lastBird = -1
    private var barsWithoutBird = 0

    override fun mutate() {
        if (rng.random() < 0.5) bongoRotation = (bongoRotation + 4) % 12 else fill = !fill
    }

    /** The log drum on the beats, from each chord's root in the pentatonic. */
    private fun logDrum(
        chords: IntArray,
        scale: Scale,
        figure: Array<IntArray>,
        out: NoteList,
    ) {
        val low = bassRange.first
        val high = bassRange.last
        var previous = low + 5
        for (b in 0 until Composer.BARS) {
            val root = c.root(chords[b], low, high - 7, previous)
            val g0 = scale.degree(root)
            for (note in figure) {
                val n = if (note[2] != 0) scale.note(g0 + note[2]) else root
                out.add(b * 24 + note[0], note[1], n, if (note[0] == 0 || note[0] == 12) 0.95 else 0.78, logDrum)
            }
            previous = root
        }
    }

    /** The «selva» pad: one chord a bar, held over when the next has the same root. */
    private fun pad(chords: IntArray, bars: Int, out: NoteList) {
        for (b in 0 until bars) {
            val chord = chords[b]
            val root = c.root(chord, padRange.first, padRange.last, padRange.first + 4)
            val last = out.size - 1
            if (last >= 0 && out.pitch[last] == root && out.pos[last] + out.dur[last] == b * 24) {
                out.dur[last] += 24
            } else {
                out.add(b * 24, 24, root, 0.75, jungle, c.chordQuality[chord])
            }
        }
    }

    /** Birds, a frog now and then and the rain stick: never the same bird twice in a row, never too often. */
    private fun ambience(bars: Int, out: NoteList) {
        var busyUntil = -1
        if (rng.random() < Forest.RAIN_STICK_PROBABILITY) {
            val b0 = if (rng.below(2) == 0) 0 else 4
            out.add(b0 * 24, 48, 0, 0.7, rainStick)
            busyUntil = b0 * 24 + 48
        }
        for (b in 0 until bars) {
            barsWithoutBird++
            val pos = b * 24 + Forest.birdSteps[rng.below(Forest.birdSteps.size)]
            if (pos < busyUntil || barsWithoutBird < Forest.BIRD_GAP || rng.random() > Forest.BIRD_PROBABILITY) continue
            val bird: Int
            if (rng.random() < Forest.FROG_PROBABILITY) {
                bird = frog
            } else {
                var n = 0
                for (k in birds) if (k != lastBird) options[n++] = k
                bird = options[rng.below(n)]
            }
            out.add(pos, 12, 0, rng.uniform(0.6, 0.95), bird)
            lastBird = bird
            barsWithoutBird = 0
            busyUntil = pos + 12
        }
    }

    /** Congas on the beats, bongo on the grid of three and shaker in what is left; denser in the tense version. */
    private fun drums(
        bars: Int,
        final: Boolean,
        dense: Boolean,
        out: NoteList,
    ) {
        for (b in 0 until bars) {
            hitInstrument.fill(-1)
            for (half in 0 until 12) {
                val p = 2 * half
                hitInstrument[p] = shaker
                hitVelocity[p] = 0.4 + (if (p % 6 == 0) 0.12 else 0.0)
            }
            for (p in Forest.bongoHits) {
                val q = if ((p + bongoRotation) % 6 != 0) (p + bongoRotation) % 24 else p
                hitInstrument[q] = bongo
                hitVelocity[q] = 0.62
            }
            for (k in Forest.lowCongaHits.indices) {
                hitInstrument[Forest.lowCongaHits[k]] = lowConga
                hitVelocity[Forest.lowCongaHits[k]] = Forest.lowCongaVelocities[k]
            }
            for (k in Forest.highCongaHits.indices) {
                hitInstrument[Forest.highCongaHits[k]] = highConga
                hitVelocity[Forest.highCongaHits[k]] = Forest.highCongaVelocities[k]
            }
            if (dense) {
                for (half in 0 until 12) {
                    val p = 2 * half + 1
                    if (hitInstrument[p] < 0) {
                        hitInstrument[p] = shaker
                        hitVelocity[p] = 0.3
                    }
                }
                hitInstrument[22] = highConga
                hitVelocity[22] = 0.6
            }
            if (final && b == bars - 1 && fill) {
                for (j in FILL.indices) {
                    hitInstrument[FILL[j]] = if (j % 2 == 1) highConga else lowConga
                    hitVelocity[FILL[j]] = 0.6 + 0.07 * j
                }
            }
            for (p in 0 until 24) if (hitInstrument[p] >= 0) out.add(b * 24 + p, 4, 0, hitVelocity[p], hitInstrument[p])
        }
    }

    /** The marimba in two interlocking parts (3 against 4), or the kalimba on the grid of three in B. */
    private fun interlocking(chords: IntArray, kind: Int, out: NoteList) {
        for (b in 0 until Composer.BARS) {
            val count = c.tones(chords[b], c2Range.first, c2Range.last, tones)
            if (kind == Composer.B) {
                val cycle = Forest.kalimbaCycle
                for (j in 0 until 6) {
                    val k = cycle[(j + b) % cycle.size]
                    out.add(b * 24 + 4 * j, 4, tones[min(count - 1, k + 2)], 0.65, kalimba)
                }
                continue
            }
            for (j in 0 until 12) {
                val k: Int
                val v: Double
                if (j % 2 == 0) {
                    k = Forest.marimbaLow[(j / 2 + b * 6) % Forest.marimbaLow.size]
                    v = 0.72
                } else {
                    k = Forest.marimbaHigh[(j / 2 + b * 6) % Forest.marimbaHigh.size]
                    v = 0.52
                }
                out.add(b * 24 + 2 * j, 2, tones[min(count - 1, k)], v, marimba)
            }
        }
    }

    override fun parts(
        kind: Int,
        chords: IntArray,
        nextChord: Int,
        out: Array<NoteList>,
    ) {
        val scale = c.scales[kind]
        logDrum(chords, scale, Forest.logFigures[rng.below(Forest.logFigures.size)], out[0])
        pad(chords, Composer.BARS, out[1])
        ambience(Composer.BARS, out[2])
        drums(Composer.BARS, final = true, dense = false, out = out[3])
        interlocking(chords, kind, out[4])
    }

    /** Quick grace notes before some long notes, as on a pan flute. */
    override fun ornaments(
        notes: NoteList,
        scale: Scale,
        kind: Int,
        out: NoteList,
    ) = graceNotes(notes, scale, out, probability = 0.2, rng = rng)

    override fun tension(bars: Int, out: Array<NoteList>) {
        val scale = c.scales[Composer.TENSE]
        var pedal = 0
        for (m in bassRange.first..bassRange.last) {
            if ((m - c.tonic - Forest.TENSION_PEDAL).mod(12) == 0) {
                pedal = m
                break
            }
        }
        for (b in 0 until bars) {
            for (quarter in 0 until 6) out[0].add(b * 24 + 4 * quarter, 4, pedal, if (quarter % 3 == 0) 0.95 else 0.75, logDrum)
        }
        pad(tensionChords, bars, out[1])
        drums(bars, final = false, dense = true, out = out[3])
        for (b in 0 until bars) {
            val count = c.tones(tensionChords[b], c2Range.first, c2Range.last, tones)
            for (j in 0 until 12) {
                out[4].add(b * 24 + 2 * j, 2, tones[min(count - 1, 3 + j % 2)], 0.5 + (if (j % 3 == 0) 0.15 else 0.0), marimba)
            }
        }
        risingMotif(c, scale, bars, before = 16, chords = tensionChords, instrument = panFlute, out = out[5], c3 = c3Range)
    }

    private companion object {
        val FILL = intArrayOf(18, 20, 21, 22, 23)
    }
}
