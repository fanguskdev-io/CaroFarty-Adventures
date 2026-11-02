package com.fanguskdev.carofarty.audio.music

import kotlin.math.abs
import kotlin.math.min

/**
 * El Desierto, árabe (generador.DESIERTO and generador.Desierto): 4/4 with maqsum, baladi and saidi on the darbuka, D
 * hijaz all through (the augmented second E♭–F#), hijaz kar in B, a drone of tonic and fifth, oud, qanun, ney and riq,
 * with mordents, trills and slides.
 */
internal object Desert {
    // ------------------------------------------------------------------------------------------- instruments

    val oud = InstrumentRecipe(
        "oud",
        listOf(
            PluckRecipe("oud-250", 64, 16000, 0.35, brightness = 0.6, rho = 0.998, position = 0.11, seed = 71, body = 0.12, damping = 0.28),
            PluckRecipe("oud-500", 32, 16000, 0.30, brightness = 0.6, rho = 0.998, position = 0.11, seed = 72, body = 0.08, damping = 0.28),
        ),
        Envelope(1.0, 0.0, 1.0, 600.0, 60.0),
        volume = 0.7,
        decayScale = 0.5,
        refHz = 440.0,
    )
    val lowOud = InstrumentRecipe(
        "oud_grave",
        listOf(
            PluckRecipe(
                "oudg-125", 128, 16000, 0.45, brightness = 0.5, rho = 0.998, position = 0.13, seed = 73, body = 0.35, damping = 0.3,
            ),
            PluckRecipe(
                "oudg-250", 64, 16000, 0.40, brightness = 0.5, rho = 0.998, position = 0.13, seed = 74, body = 0.25, damping = 0.3,
            ),
        ),
        Envelope(1.0, 0.0, 1.0, 520.0, 45.0),
        volume = 0.9,
        decayScale = 0.3,
        refHz = 110.0,
    )
    val qanun = InstrumentRecipe(
        "qanun",
        listOf(
            PluckRecipe("qanun-500", 32, 16000, 0.25, brightness = 0.92, rho = 0.998, position = 0.07, seed = 75, damping = 0.32),
            PluckRecipe("qanun-1000", 16, 16000, 0.20, brightness = 0.92, rho = 0.998, position = 0.07, seed = 76, damping = 0.32),
        ),
        Envelope(1.0, 0.0, 1.0, 380.0, 40.0),
        volume = 0.55,
        decayScale = 0.5,
        refHz = 600.0,
    )
    val ney = InstrumentRecipe(
        "ney",
        listOf(
            BreathRecipe(
                "ney", 32, 16000, 40, 250,
                listOf(Harmonic(1.0, 1.0), Harmonic(2.0, 0.18), Harmonic(3.0, 0.1), Harmonic(4.0, 0.04)),
                breath = 0.3, bandLow = 1200.0, bandHigh = 5000.0, chiff = 0.4, chiffMs = 45.0, seed = 77,
            ),
        ),
        Envelope(55.0, 0.0, 1.0, 0.0, 120.0),
        volume = 0.62,
        vibrato = Vibrato(18.0, 5.0, 160.0),
        ornament = Ornament.slide(-1.0, 70.0),
    )
    val drone = InstrumentRecipe(
        "bordon",
        envelope = Envelope(400.0, 0.0, 1.0, 0.0, 500.0),
        volume = 0.4,
        chords = mapOf(
            Quality.Fifth to ChordRecipe(
                "bordon-quinta",
                Quality.Fifth.intervals,
                listOf(
                    Harmonic(1.0, 0.6), Harmonic(2.0, 0.5), Harmonic(3.0, 0.8), Harmonic(4.0, 0.45), Harmonic(5.0, 0.55),
                    Harmonic(6.0, 0.25), Harmonic(7.0, 0.3), Harmonic(8.0, 0.12), Harmonic(9.0, 0.15),
                ),
                rootCycles = 147,
                seconds = 2.0,
                detune = 1,
            ),
        ),
    )

    /** The darbuka's three strokes, tuned to D2, A5 and D5: dum (low), tek (high) and ka (muted). */
    val dum = InstrumentRecipe(
        "dum",
        listOf(
            MembraneRecipe(
                "dum2", 16000, 110.0, 73.4, 0.05, 0.3, 0.6, noise = 0.12, noiseTau = 0.004, noiseCutoff = 1500.0,
                partial = 0.15, seed = 81, partialRatio = 1.5,
            ),
        ),
        Envelope(0.0, 0.0, 1.0, 0.0, 30.0),
        volume = 0.9,
        fixedHz = 16000.0,
    )
    val tek = InstrumentRecipe(
        "tek",
        listOf(
            MembraneRecipe(
                "tek2", 16000, 930.0, 880.0, 0.006, 0.025, 0.12, noise = 0.9, noiseTau = 0.008, noiseCutoff = 9000.0,
                partial = 0.4, seed = 82, partialRatio = 1.5,
            ),
        ),
        Envelope(0.0, 0.0, 1.0, 0.0, 10.0),
        volume = 0.45,
        fixedHz = 16000.0,
    )
    val ka = InstrumentRecipe(
        "ka",
        listOf(
            MembraneRecipe(
                "ka", 16000, 620.0, 587.3, 0.008, 0.03, 0.12, noise = 0.5, noiseTau = 0.006, noiseCutoff = 5000.0,
                partial = 0.3, seed = 83, partialRatio = 1.5,
            ),
        ),
        Envelope(0.0, 0.0, 1.0, 0.0, 10.0),
        volume = 0.3,
        fixedHz = 16000.0,
    )
    val riq = InstrumentRecipe(
        "riq",
        listOf(
            MetalRecipe(
                "riq", 32000,
                listOf(
                    MetalPartial(5200.0, 0.3, 0.06),
                    MetalPartial(6900.0, 0.25, 0.05),
                    MetalPartial(8400.0, 0.2, 0.04),
                    MetalPartial(3700.0, 0.15, 0.05),
                ),
                0.25, noise = 0.55, noiseTau = 0.035, highPassHz = 4500.0, shakes = doubleArrayOf(0.0, 0.012), seed = 84,
            ),
        ),
        Envelope(0.0, 0.0, 1.0, 0.0, 15.0),
        volume = 0.2,
        fixedHz = 32000.0,
    )
    val cymbals = InstrumentRecipe(
        "crotalo",
        listOf(
            MetalRecipe(
                "crotalo",
                32000,
                listOf(
                    MetalPartial(2640.0, 0.6, 0.5),
                    MetalPartial(5810.0, 0.3, 0.25),
                    MetalPartial(7380.0, 0.25, 0.18),
                    MetalPartial(9900.0, 0.1, 0.1),
                ),
                1.0,
                noise = 0.1,
                noiseTau = 0.004,
            ),
        ),
        Envelope(0.0, 0.0, 1.0, 0.0, 20.0),
        volume = 0.22,
        fixedHz = 32000.0,
    )

    // --------------------------------------------------------------------------------------------------- data

    /**
     * The level of the whole mix, as the prototype normalizes its demos: the 84 s of the demo run (seed 7) at −20 dBFS
     * RMS unless the peak would pass −1.3 dBFS; here its peak reaches −1.3 dBFS just before, so it sits at −20.1 dBFS.
     */
    private const val MASTER_GAIN = 2.2611

    val style = MusicStyle(
        name = "desierto",
        tonic = midi("D4"),
        modes = mapOf(SectionKind.A to Modes.hijaz, SectionKind.B to Modes.doubleHarmonic, SectionKind.Tense to Modes.hijaz),
        bpm = 480000.0 / 4320,
        stepsPerBeat = 4,
        beatsPerBar = 4,
        swing = 0.5,
        strongSteps = intArrayOf(0, 8),
        // D hijaz (D E♭ F# G A B♭ C): I, ♭II (the Phrygian cadence ♭II–I), iv and ♭vii
        chords = listOf(
            ChordDef("I", intArrayOf(0, 4, 7), intArrayOf(1, 5, 8, 10)),
            ChordDef("bII", intArrayOf(1, 5, 8), intArrayOf(0, 4)),
            ChordDef("iv", intArrayOf(5, 8, 0), intArrayOf(1, 4)),
            ChordDef("bvii", intArrayOf(10, 1, 5), intArrayOf(0, 8)),
        ),
        markov = mapOf(
            SectionKind.A to mapOf(
                "I" to listOf("bII" to 3.0, "iv" to 2.0, "bvii" to 1.5),
                "bII" to listOf("I" to 4.0, "iv" to 0.5),
                "iv" to listOf("bII" to 2.0, "I" to 2.0, "bvii" to 1.0),
                "bvii" to listOf("bII" to 2.0, "I" to 2.0),
            ),
            SectionKind.B to mapOf(
                "iv" to listOf("bII" to 2.0, "I" to 2.0),
                "I" to listOf("iv" to 2.0, "bII" to 3.0),
                "bII" to listOf("I" to 4.0, "iv" to 1.0),
            ),
        ),
        phrases = listOf(Triple("I", "bII", "I"), Triple("iv", "bII", "I"), Triple("bvii", "bII", "I"), Triple("I", "iv", "bII")),
        start = mapOf(SectionKind.A to "I", SectionKind.A2 to "I", SectionKind.B to "iv", SectionKind.Bridge to "bvii"),
        cadences = mapOf(SectionKind.A to listOf("bII", "bvii"), SectionKind.B to listOf("bII"), SectionKind.Bridge to listOf("bII")),
        harmonicRhythm = mapOf(SectionKind.A to 2, SectionKind.A2 to 2, SectionKind.B to 1, SectionKind.Bridge to 2),
        roles = mapOf("bass" to lowOud, "pad" to drone, "c2" to qanun, "c3" to oud, "c3b" to ney),
        ranges = mapOf("bass" to midi("D2")..midi("D3"), "c2" to midi("D4")..midi("A5"), "c3" to midi("G4")..midi("D6")),
        // Caro's motif in hijaz: the augmented second E♭–F# at the start, a mordent, a trill and a slide
        leitmotif = listOf(
            MotifNoteAbsolute(0, 1, midi("Eb5")), MotifNoteAbsolute(1, 1, midi("F#5")),
            MotifNoteAbsolute(2, 3, midi("A5"), Ornament.mordent(1.0)), MotifNoteAbsolute(5, 1, midi("F#5")),
            MotifNoteAbsolute(6, 4, midi("Bb5"), Ornament.trill(2.0, 13.0, 250.0)), MotifNoteAbsolute(10, 2, midi("A5")),
            MotifNoteAbsolute(12, 2, midi("F#5")), MotifNoteAbsolute(14, 2, midi("Eb5")), MotifNoteAbsolute(16, 3, midi("D5")),
            MotifNoteAbsolute(19, 1, midi("Eb5")), MotifNoteAbsolute(20, 2, midi("F#5")), MotifNoteAbsolute(22, 2, midi("A4")),
            MotifNoteAbsolute(24, 2, midi("Bb4")), MotifNoteAbsolute(26, 6, midi("D5"), Ornament.slide(-4.0, 80.0)),
        ),
        motifs = listOf(
            listOf(
                MotifNote(0, 2, 0),
                MotifNote(2, 1, 1),
                MotifNote(3, 1, 0),
                MotifNote(4, 2, -1),
                MotifNote(6, 2, 0),
                MotifNote(8, 6, 2),
                MotifNote(14, 2, 1),
            ),
            listOf(
                MotifNote(0, 2, 0), MotifNote(2, 2, 1), MotifNote(4, 2, 2), MotifNote(6, 2, 3), MotifNote(8, 8, 4),
                MotifNote(16, 2, 3), MotifNote(18, 2, 2), MotifNote(20, 4, 1), MotifNote(24, 8, 0),
            ),
            listOf(MotifNote(0, 3, 4), MotifNote(3, 1, 3), MotifNote(4, 2, 2), MotifNote(6, 2, 1), MotifNote(8, 8, 0)),
        ),
        echo = EchoSettings(3, 0x2C, 0x18),
        echoChannels = intArrayOf(1, 5),
        channelVolumes = doubleArrayOf(0.115, 0.326, 1.0, 0.138, 0.241, 0.191),
        layers = listOf(intArrayOf(0, 1), intArrayOf(2, 3), intArrayOf(4), intArrayOf(5)),
        percussionChannels = intArrayOf(2, 3),
        masterGain = MASTER_GAIN,
        arrangement = Parts,
    )

    private object Parts : ArrangementRecipe {
        override val instruments get() = listOf(dum, tek, ka, riq, cymbals)

        override fun create(composer: Composer): Arrangement = DesertArrangement(composer)
    }

    /** The drone's note, an octave under the tonic. */
    val droneNote = midi("D3")

    // Tension: chords in a loop, the pedal (semitones over the tonic) and the qanun's tremolo on E♭ and F#.
    val tensionChords = listOf("I", "bII", "I", "bII")
    const val TENSION_PEDAL = 0
    const val TREMOLO_LOW = 1
    const val TREMOLO_HIGH = 4

    // The low oud's ostinatos: (position, length, degree of the scale over the chord's root).
    val bassFigures = listOf(
        arrayOf(
            intArrayOf(0, 3, 0),
            intArrayOf(3, 1, 1),
            intArrayOf(4, 2, 2),
            intArrayOf(6, 2, 1),
            intArrayOf(8, 3, 0),
            intArrayOf(11, 1, 1),
            intArrayOf(12, 2, 2),
            intArrayOf(14, 2, 3),
        ),
        arrayOf(
            intArrayOf(0, 2, 0),
            intArrayOf(2, 2, 0),
            intArrayOf(6, 2, 1),
            intArrayOf(8, 2, 0),
            intArrayOf(10, 2, 2),
            intArrayOf(12, 4, 0),
        ),
        arrayOf(intArrayOf(0, 4, 3), intArrayOf(4, 2, 2), intArrayOf(6, 2, 1), intArrayOf(8, 6, 0), intArrayOf(14, 2, -1)),
    )

    // The qanun's figures: (position, length, degree over the root).
    val qanunFigures = listOf(
        arrayOf(
            intArrayOf(0, 1, 0), intArrayOf(1, 1, 1), intArrayOf(2, 1, 2), intArrayOf(3, 1, 3), intArrayOf(4, 1, 4), intArrayOf(5, 1, 3),
            intArrayOf(6, 1, 2), intArrayOf(7, 1, 1), intArrayOf(8, 4, 0), intArrayOf(12, 2, 2), intArrayOf(14, 2, 1),
        ),
        arrayOf(
            intArrayOf(0, 1, 4), intArrayOf(1, 1, 4), intArrayOf(2, 1, 4), intArrayOf(3, 1, 4), intArrayOf(4, 2, 5), intArrayOf(6, 2, 4),
            intArrayOf(
                8,
                1,
                2,
            ),
            intArrayOf(9, 1, 2), intArrayOf(10, 1, 2), intArrayOf(11, 1, 2), intArrayOf(12, 2, 1), intArrayOf(14, 2, 0),
        ),
        arrayOf(
            intArrayOf(0, 2, 2),
            intArrayOf(2, 2, 1),
            intArrayOf(4, 4, 0),
            intArrayOf(8, 1, 1),
            intArrayOf(9, 1, 2),
            intArrayOf(10, 2, 3),
            intArrayOf(12, 4, 2),
        ),
    )
    val qanunBySection = intArrayOf(0, 1, 2, 0) // A, A', B, bridge

    // The darbuka in a bar of 16: dum and tek by rhythm; the ka fills.
    const val NONE = 0
    const val DUM = 1
    const val TEK = 2
    const val KA = 3
    private fun rhythm(vararg strokes: Pair<Int, Int>): IntArray = IntArray(16).also { for ((p, s) in strokes) it[p] = s }

    val maqsum = rhythm(0 to DUM, 2 to TEK, 6 to TEK, 8 to DUM, 12 to TEK)
    val baladi = rhythm(0 to DUM, 2 to DUM, 6 to TEK, 8 to DUM, 12 to TEK)
    val saidi = rhythm(0 to DUM, 2 to TEK, 6 to DUM, 8 to DUM, 12 to TEK)

    /** The rhythm of each kind of section: A, A', B, bridge and the tense version. */
    val rhythmBySection = arrayOf(maqsum, baladi, saidi, maqsum, maqsum)
    val kaSteps = intArrayOf(4, 10, 14)
    val fillSteps = intArrayOf(10, 11, 12, 13, 14, 15)
    val fillStrokes = intArrayOf(TEK, KA, TEK, KA, TEK, TEK)
    val strokeVelocity = doubleArrayOf(0.0, 0.95, 0.8, 0.42)

    // The riq: the off-beat (with the rotation), accents on 2 and 4, and finger cymbals every four bars.
    const val RIQ_OFFBEAT = 2
    val riqAccents = intArrayOf(4, 12)
    const val CYMBALS_EVERY = 4
}

/** The parts of the Desierto (generador.Desierto), note for note. */
internal class DesertArrangement(private val c: Composer) : Arrangement() {
    private val rng = c.rng
    private val steps = c.steps
    private val lowOud = c.bank.indexOf(Desert.lowOud)
    private val drone = c.bank.indexOf(Desert.drone)
    private val qanun = c.bank.indexOf(Desert.qanun)
    private val oud = c.bank.indexOf(Desert.oud)
    private val strokes = intArrayOf(-1, c.bank.indexOf(Desert.dum), c.bank.indexOf(Desert.tek), c.bank.indexOf(Desert.ka))
    private val riq = c.bank.indexOf(Desert.riq)
    private val cymbals = c.bank.indexOf(Desert.cymbals)
    private val bassRange = c.range("bass")
    private val c2Range = c.range("c2")
    private val c3Range = c.range("c3")
    private val tensionChords = IntArray(Composer.BARS) { c.chord(Desert.tensionChords[it % Desert.tensionChords.size]) }
    private val fifth = Quality.Fifth.ordinal

    /** ms of a sixteenth at the base tempo, for the trills. */
    private val sixteenthMs = Chip.RATE * 60.0 / (c.style.bpm * c.style.stepsPerBeat) * 1000.0 / Chip.RATE
    private val bar = IntArray(16)
    private var riqRotation = 0
    private var fill = true

    override fun mutate() {
        if (rng.random() < 0.5) riqRotation = (riqRotation + 2) % 4 else fill = !fill
    }

    /** The low oud's ostinato from each chord's root, in degrees of the section's scale. */
    private fun bass(
        chords: IntArray,
        scale: Scale,
        figure: Array<IntArray>,
        out: NoteList,
    ) {
        val low = bassRange.first
        val high = bassRange.last
        var previous = low + 5
        for (b in 0 until Composer.BARS) {
            val root = c.root(chords[b], low, high - 5, previous)
            val g0 = scale.degree(root)
            for (note in figure) {
                out.add(b * 16 + note[0], note[1], scale.note(g0 + note[2]), if (note[0] == 0 || note[0] == 8) 0.95 else 0.8, lowOud)
            }
            previous = root
        }
    }

    /** The darbuka: the section's rhythm, the ka filling 4, 10 and 14 (and 15 now and then) and the fill of the last bar. */
    private fun darbuka(
        rhythm: IntArray,
        bars: Int,
        final: Boolean,
        dense: Boolean,
        out: NoteList,
    ) {
        for (b in 0 until bars) {
            rhythm.copyInto(bar)
            for (p in Desert.kaSteps) if (bar[p] == Desert.NONE) bar[p] = Desert.KA
            if (rng.random() < 0.3 && bar[15] == Desert.NONE) bar[15] = Desert.KA
            if (dense) {
                for (p in DENSE_KA) if (bar[p] == Desert.NONE) bar[p] = Desert.KA
                bar[14] = Desert.TEK
            }
            if (final && b == bars - 1 && fill) {
                for (k in Desert.fillSteps.indices) bar[Desert.fillSteps[k]] = Desert.fillStrokes[k]
            }
            for (p in 0 until 16) {
                val s = bar[p]
                if (s != Desert.NONE) out.add(b * 16 + p, 4, 0, Desert.strokeVelocity[s] * (if (dense) 0.9 else 1.0), strokes[s])
            }
        }
    }

    /** The riq on the off-beat and on 2 and 4, with finger cymbals every four bars (in semiquavers when dense). */
    private fun riq(bars: Int, dense: Boolean, out: NoteList) {
        for (b in 0 until bars) {
            for (p in 0 until 16) {
                val v = when {
                    dense -> 0.35 + (if (p % 4 == 0) 0.25 else 0.0)
                    (p + riqRotation) % 4 == Desert.RIQ_OFFBEAT -> 0.45
                    p == Desert.riqAccents[0] || p == Desert.riqAccents[1] -> 0.7
                    else -> continue
                }
                out.add(b * 16 + p, 2, 0, v, riq)
            }
            if (b % Desert.CYMBALS_EVERY == 0) {
                var i = 0
                while (i < out.size) {
                    if (out.pos[i] == b * 16) out.removeAt(i) else i++
                }
                out.add(b * 16, 8, 0, 0.6, cymbals)
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
        bass(chords, scale, Desert.bassFigures[rng.below(Desert.bassFigures.size)], out[0])
        out[1].add(0, 8 * 16, Desert.droneNote, 0.8, drone, fifth)
        riq(Composer.BARS, dense = false, out = out[2])
        darbuka(Desert.rhythmBySection[kind], Composer.BARS, final = true, dense = false, out = out[3])
        val figure = Desert.qanunFigures[if (rng.random() < 0.75) Desert.qanunBySection[kind] else rng.below(Desert.qanunFigures.size)]
        for (b in 0 until Composer.BARS) {
            if (kind == Composer.BRIDGE && b % 2 == 1) continue
            val root = c.root(chords[b], c2Range.first, c2Range.last - 7, c2Range.first + 4)
            val g0 = scale.degree(root)
            for (note in figure) out[4].add(b * 16 + note[0], note[1], scale.note(g0 + note[2]), 0.7, qanun)
        }
    }

    /** The ornaments of the maqam: trills and mordents on long notes, a short slide after a leap. */
    override fun ornaments(
        notes: NoteList,
        scale: Scale,
        kind: Int,
        out: NoteList,
    ) {
        out.clear()
        for (i in 0 until notes.size) {
            val j = out.append(notes, i)
            val d = notes.dur[i]
            val n = notes.pitch[i]
            var none = notes.ornament[i] == Ornament.NONE
            if (none && d >= 3) {
                val up = scale.note(scale.degree(n) + 1) - n
                val r = rng.random()
                if (r < 0.22) {
                    out.setOrnament(j, Ornament.TRILL, up.toDouble(), 13.0, min(350.0, d * sixteenthMs * 0.6))
                    none = false
                } else if (r < 0.45) {
                    out.setOrnament(j, Ornament.MORDENT, up.toDouble())
                    none = false
                }
            }
            if (none && i > 0 && abs(n - notes.pitch[i - 1]) >= 3 && rng.random() < 0.5) {
                out.setOrnament(j, Ornament.SLIDE, (notes.pitch[i - 1] - n).coerceIn(-4, 4).toDouble(), ms = 70.0)
            }
        }
    }

    override fun tension(bars: Int, out: Array<NoteList>) {
        val scale = c.scales[Composer.TENSE]
        var pedal = 0
        for (m in bassRange.first..bassRange.last) {
            if ((m - c.tonic - Desert.TENSION_PEDAL).mod(12) == 0) {
                pedal = m
                break
            }
        }
        for (b in 0 until bars) {
            for (half in 0 until 8) {
                val p = 2 * half
                out[0].add(b * 16 + p, 2, if (p == 14) pedal + 1 else pedal, if (p % 8 == 0) 0.95 else 0.72, lowOud)
            }
        }
        out[1].add(0, 16 * bars, Desert.droneNote, 0.8, drone, fifth)
        riq(bars, dense = true, out = out[2])
        darbuka(Desert.rhythmBySection[Composer.TENSE], bars, final = false, dense = true, out = out[3])
        var low = 0
        for (m in c2Range.first until c2Range.last) {
            if ((m - c.tonic).mod(12) == Desert.TREMOLO_LOW) {
                low = m
                break
            }
        }
        for (b in 0 until bars) {
            for (p in 0 until 16) {
                val h = if (p % 2 == 0) low else low + (Desert.TREMOLO_HIGH - Desert.TREMOLO_LOW)
                out[4].add(b * 16 + p, 1, h, 0.5 + (if (p % 4 == 0) 0.2 else 0.0), qanun)
            }
        }
        var b = 0
        while (b < bars) {
            val count = c.loadLeitSteps(scale, 10)
            val anchor = scale.note(scale.degree(c.leitFirstPitch) + (b / 2) % 4)
            val first = c.placeLoaded(scale, count, anchor, b * 16, out[5])
            for (i in first until out[5].size) {
                out[5].vel[i] = Composer.MELODY_VELOCITY
                out[5].instrument[i] = oud
                if (out[5].dur[i] >= 3) out[5].setOrnament(i, Ornament.MORDENT, 1.0)
            }
            b += 2
        }
    }

    private companion object {
        val DENSE_KA = intArrayOf(3, 5, 7, 9, 11, 13, 15)
    }
}
