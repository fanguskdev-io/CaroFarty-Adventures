package com.fanguskdev.carofarty.audio.music

/**
 * The chip's envelope with its times in milliseconds (sinte.ADSR): a linear attack from 0 to 1; an exponential decay
 * with time constant [decayMs] down to [sustainLevel]; an exponential sustain with its own constant (0 holds the level);
 * and a linear release that takes [releaseMs] from 1. The chip's release is 8 ms; drivers made it longer with GAIN, and
 * that is a longer [releaseMs].
 */
internal class Envelope(
    val attackMs: Double = 0.0,
    val decayMs: Double = 0.0,
    val sustainLevel: Double = 1.0,
    val sustainMs: Double = 0.0,
    val releaseMs: Double = 8.0,
)

/** A vibrato made with the pitch, as a driver did: [cents] deep, at [hz], after [delayMs] and fading in over 150 ms. */
internal class Vibrato(val cents: Double, val hz: Double, val delayMs: Double)

/**
 * An ornament of one note, made with the pitch register (sinte.curva_adorno): a slide from [semitones] away that takes
 * [ms] to arrive; a mordent to the neighbour [semitones] away between 25 and 60 ms; or a trill with the neighbour at [hz]
 * for [ms].
 */
internal class Ornament(
    val kind: Int,
    val semitones: Double,
    val hz: Double = 0.0,
    val ms: Double = 0.0,
) {
    companion object {
        const val NONE = 0
        const val SLIDE = 1
        const val MORDENT = 2
        const val TRILL = 3

        fun slide(fromSemitones: Double, ms: Double) = Ornament(SLIDE, fromSemitones, ms = ms)

        fun mordent(semitones: Double) = Ornament(MORDENT, semitones)

        fun trill(semitones: Double, hz: Double, ms: Double) = Ornament(TRILL, semitones, hz, ms)
    }
}

/**
 * The kinds of chord a chord instrument has a sample of (sinte.CALIDADES), as semitones over the root; [pythonName] is
 * the prototype's name for it, which also names the sample, as there.
 */
internal enum class Quality(val intervals: IntArray, val pythonName: String) {
    Major(intArrayOf(0, 4, 7, 12), "maj"),
    Minor(intArrayOf(0, 3, 7, 12), "min"),
    Dominant7(intArrayOf(0, 4, 7, 10), "dom7"),
    Minor7(intArrayOf(0, 3, 7, 10), "min7"),
    Major7(intArrayOf(0, 4, 7, 11), "maj7"),
    Minor6(intArrayOf(0, 3, 7, 9), "min6"),
    Major6(intArrayOf(0, 4, 7, 9), "maj6"),
    Diminished7(intArrayOf(0, 3, 6, 9), "dim7"),
    Fifth(intArrayOf(0, 7, 12), "quinta"),
    ;

    companion object {
        /** The quality of a chord from its pitch classes, root first (generador._CALIDADES), or major if unknown. */
        fun of(pitchClasses: IntArray): Quality {
            val root = pitchClasses[0]
            val shape = pitchClasses.map { (it - root).mod(12) }.sorted()
            return when (shape) {
                listOf(0, 4, 7) -> Major
                listOf(0, 3, 7) -> Minor
                listOf(0, 4, 7, 10) -> Dominant7
                listOf(0, 3, 7, 10) -> Minor7
                listOf(0, 4, 7, 11) -> Major7
                listOf(0, 3, 7, 9) -> Minor6
                listOf(0, 4, 7, 9) -> Major6
                listOf(0, 3, 6, 9) -> Diminished7
                else -> Major
            }
        }
    }
}

/**
 * An instrument as data (sinte.Instrumento): its samples (one per register, or one per chord [Quality] for a chord
 * instrument), its envelope, its volume and how it plays.
 *
 * - [noise]: an index of [Chip.noiseRates]; the instrument is the chip's noise and has no sample (−1: it has samples).
 * - [decayScale]: higher notes die sooner; the envelope's decay and sustain times are multiplied by
 *   ([refHz] / f) ^ [decayScale].
 * - [fixedHz]: the pitch it always sounds at (drums with a sample); a note's height adds semitones to it (0: pitched).
 * - [ornament]: the ornament of every note that does not bring its own.
 * - [texture]: hundreds of grains (the rain stick): every grain is an attack, not a click.
 */
internal class InstrumentRecipe(
    val name: String,
    val samples: List<SampleRecipe> = emptyList(),
    val envelope: Envelope = Envelope(),
    val volume: Double = 1.0,
    val vibrato: Vibrato? = null,
    val noise: Int = -1,
    val decayScale: Double = 0.0,
    val refHz: Double = 440.0,
    val fixedHz: Double = 0.0,
    val chords: Map<Quality, SampleRecipe> = emptyMap(),
    val ornament: Ornament? = null,
    val texture: Boolean = false,
)

/** An instrument with its samples made. */
internal class Instrument(val recipe: InstrumentRecipe) {
    /** The samples by register, lowest root first. */
    val samples: Array<Sample>

    /** The chord samples by [Quality.ordinal], for a chord instrument. */
    val chordSamples: Array<Sample?>

    val isChords: Boolean get() = recipe.chords.isNotEmpty()

    init {
        val chords = arrayOfNulls<Sample>(Quality.entries.size)
        for ((quality, chord) in recipe.chords) chords[quality.ordinal] = chord.build()
        chordSamples = chords
        val built = if (recipe.samples.isNotEmpty()) recipe.samples.map { it.build() } else listOfNotNull(chords.firstOrNull { it != null })
        samples = built.sortedBy { it.root }.toTypedArray()
        require(recipe.noise >= 0 || samples.isNotEmpty()) { "${recipe.name} has no sound" }
    }

    /**
     * The sample for [hz] (sinte.Instrumento._muestra): the one with the closest root among those that do not need a
     * step over the top of the pitch register, or the highest if none fits.
     */
    fun pick(hz: Double): Sample {
        var best: Sample? = null
        var bestDistance = 0.0
        for (s in samples) {
            if (hz * s.rate / (s.root * Chip.RATE) > Chip.PITCH_MAX.toDouble() / Chip.PITCH_ONE) continue
            val distance = kotlin.math.abs(DetMath.ln(hz / s.root))
            if (best == null || distance < bestDistance) {
                best = s
                bestDistance = distance
            }
        }
        return best ?: samples[samples.size - 1]
    }
}
