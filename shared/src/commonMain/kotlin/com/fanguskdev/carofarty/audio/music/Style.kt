package com.fanguskdev.carofarty.audio.music

/** The modes the scenarios use, as semitones over the tonic (generador.MODOS). */
internal object Modes {
    val major = intArrayOf(0, 2, 4, 5, 7, 9, 11)
    val mixolydian = intArrayOf(0, 2, 4, 5, 7, 9, 10)
    val dorian = intArrayOf(0, 2, 3, 5, 7, 9, 10)
    val minor = intArrayOf(0, 2, 3, 5, 7, 8, 10)

    /** The maqam hijaz without quarter tones, the same as the phrygian dominant. */
    val hijaz = intArrayOf(0, 1, 4, 5, 7, 8, 10)

    /** Hijaz kar: two augmented seconds. */
    val doubleHarmonic = intArrayOf(0, 1, 4, 5, 7, 8, 11)
    val minorPentatonic = intArrayOf(0, 3, 5, 7, 10)
    val majorPentatonic = intArrayOf(0, 2, 4, 7, 9)
}

/** A MIDI note from its name: "C4" is 60, "F#5" 78, "Bb3" 58. */
internal fun midi(name: String): Int {
    val letter = when (name[0].uppercaseChar()) {
        'C' -> 0
        'D' -> 2
        'E' -> 4
        'F' -> 5
        'G' -> 7
        'A' -> 9
        'B' -> 11
        else -> throw IllegalArgumentException("not a note: $name")
    }
    var i = 1
    var alteration = 0
    while (i < name.length && (name[i] == '#' || name[i] == 'b')) {
        alteration += if (name[i] == '#') 1 else -1
        i++
    }
    return 12 * (name.substring(i).toInt() + 1) + letter + alteration
}

/**
 * A tonic (MIDI) and a mode (generador.Escala). Degrees count from 0 = the tonic and go on by octaves: 7 is the tonic
 * above in a seven-note mode, −1 the last degree below. Nothing here allocates.
 */
internal class Scale(val tonic: Int, val intervals: IntArray) {
    private val size = intervals.size

    fun note(degree: Int): Int = tonic + 12 * degree.floorDiv(size) + intervals[degree.mod(size)]

    /** The degree of a note of the scale, or of the closest note of the scale below it. */
    fun degree(note: Int): Int {
        var g = size * (note - tonic).floorDiv(12) - size
        while (note(g + 1) <= note) g++
        return g
    }
}

/** The kinds of section: A (with Caro's motif), A' (a variation of A), B, the bridge and the tense version. */
internal enum class SectionKind { A, A2, B, Bridge, Tense }

/** A chord as pitch classes over the tonic (root first), the colours the melody may also land on, and its sampled quality. */
internal class ChordDef(
    val name: String,
    val pitchClasses: IntArray,
    val colors: IntArray = intArrayOf(),
    quality: Quality? = null,
) {
    val quality: Quality = quality ?: Quality.of(pitchClasses)
}

/** A note of Caro's motif in a scenario: position and length in sixteenths, MIDI note and its ornament. */
internal class MotifNoteAbsolute(
    val pos: Int,
    val dur: Int,
    val pitch: Int,
    val ornament: Ornament? = null,
)

/** A note of a scenario's motif: position and length in sixteenths, and steps of the scale over its first note. */
internal class MotifNote(val pos: Int, val dur: Int, val step: Int)

/**
 * A scenario's music as data, like the prototype's Escenario tables: scale, tonic, tempo, metre, swing, chords, Markov
 * chain, phrases, starts and cadences, harmonic rhythm, instruments, ranges, Caro's motif, motifs, echo, volumes and
 * layers. What a scenario plays bar by bar (bass, pad, percussion, C2 and the tense version) is its [arrangement]: code
 * with its own pattern tables, as each Compositor subclass is in the prototype.
 *
 * - The metre is [beatsPerBar] beats of [stepsPerBeat] sixteenths: 4 × 4 in 4/4, 3 × 4 in 3/4, 4 × 6 in 12/8.
 * - [markov] is by section kind (A, B, bridge; A' uses A's), each chord to its next ones in order with their weights; A
 *   is required and is the fallback. [cadences] and [harmonicRhythm] (bars per chord) work the same way.
 * - [roles]: the instruments by role: "bass", "pad", "c2", "c3" and "c3b" (the melody in B), and whatever the
 *   arrangement asks for. [ranges]: the MIDI ranges by role ("bass", "pad", "c2", "c3").
 * - [layers]: the channels of each layer, C0 to C3; C0 always sounds, C1 from level 2, C2 from 3, C3 from 4.
 * - [masterGain]: the level of the whole mix, set so a run sits at about −20 dBFS RMS like the prototype's demos.
 */
internal class MusicStyle(
    val name: String,
    val tonic: Int,
    val modes: Map<SectionKind, IntArray>,
    val bpm: Double,
    val stepsPerBeat: Int,
    val beatsPerBar: Int,
    val swing: Double,
    val strongSteps: IntArray,
    val chords: List<ChordDef>,
    val markov: Map<SectionKind, Map<String, List<Pair<String, Double>>>>,
    val phrases: List<Triple<String, String, String>>,
    val start: Map<SectionKind, String>,
    val cadences: Map<SectionKind, List<String>>,
    val harmonicRhythm: Map<SectionKind, Int>,
    val roles: Map<String, InstrumentRecipe>,
    val ranges: Map<String, IntRange>,
    val leitmotif: List<MotifNoteAbsolute>,
    val motifs: List<List<MotifNote>>,
    val echo: EchoSettings,
    val echoChannels: IntArray,
    val channelVolumes: DoubleArray,
    val layers: List<IntArray>,
    val percussionChannels: IntArray,
    val masterGain: Double,
    val arrangement: ArrangementRecipe,
) {
    /** Sixteenths in a bar: 16 in 4/4, 12 in 3/4, 24 in 12/8. */
    val stepsPerBar: Int get() = stepsPerBeat * beatsPerBar

    init {
        require(channelVolumes.size == Chip.MUSIC_VOICES) { "$name: one volume per music channel" }
        require(layers.size == LAYERS) { "$name: the layers are C0 to C3" }
        require(SectionKind.A in markov && SectionKind.A in cadences && SectionKind.A in modes) { "$name: section A is required" }
        for (kind in listOf(SectionKind.A, SectionKind.A2, SectionKind.B, SectionKind.Bridge)) {
            require(kind in start) { "$name: no starting chord for $kind" }
        }
        for (layer in layers) for (c in layer) require(c in 0 until Chip.MUSIC_VOICES) { "$name: channel $c" }
        require(leitmotif.isNotEmpty() && motifs.isNotEmpty()) { "$name: Caro's motif and the motifs are required" }
    }

    companion object {
        const val LAYERS = 4
    }
}

/** What a scenario plays besides the melody, as code with its own data (the Compositor subclasses of the prototype). */
internal interface ArrangementRecipe {
    /** Instruments the arrangement plays by name, besides those of the style's roles. */
    val instruments: List<InstrumentRecipe>

    fun create(composer: Composer): Arrangement
}

/**
 * The parts of a scenario, called by the [Composer] when a section starts. They write notes into the lists they are
 * given and must not allocate: they run on the audio thread.
 */
internal abstract class Arrangement {
    /** Something changes every section (the fill, a rotation): called after each section is composed. */
    open fun mutate() {}

    /** Channels 0 to 4 of a section of [kind] over its 8 [chords], heading for [nextChord] (chord indices). */
    abstract fun parts(
        kind: Int,
        chords: IntArray,
        nextChord: Int,
        out: Array<NoteList>,
    )

    /** Ornaments of the melody of a section, from [notes] into [out] (which is empty). */
    open fun ornaments(
        notes: NoteList,
        scale: Scale,
        kind: Int,
        out: NoteList,
    ) {
        notes.copyInto(out)
    }

    /** The six channels of [bars] bars of the tense version. */
    abstract fun tension(bars: Int, out: Array<NoteList>)
}
