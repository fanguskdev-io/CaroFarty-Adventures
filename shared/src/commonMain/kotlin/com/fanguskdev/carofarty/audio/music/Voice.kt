package com.fanguskdev.carofarty.audio.music

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * The notes waiting for one channel, in the order they start: a ring of columns allocated once. Each note keeps where
 * it was written (section, position and length in sixteenths, and the samples a sixteenth is held) so that its length
 * can be cut when its section is cut short.
 */
internal class NoteQueue(private val capacity: Int = 256) {
    val start = LongArray(capacity)
    val length = IntArray(capacity)
    val instrument = IntArray(capacity)
    val pitch = IntArray(capacity)
    val quality = IntArray(capacity)
    val ornament = IntArray(capacity)
    val ornamentSemitones = DoubleArray(capacity)
    val ornamentHz = DoubleArray(capacity)
    val ornamentMs = DoubleArray(capacity)
    val gain = DoubleArray(capacity)
    val section = IntArray(capacity)
    val pos = IntArray(capacity)
    val steps = IntArray(capacity)
    val semi = DoubleArray(capacity)
    var head = 0
        private set
    var size = 0
        private set

    /** The slot of the [i]-th waiting note. */
    fun slot(i: Int): Int = (head + i) % capacity

    /** Makes room for one more note at the end and returns its slot. */
    fun push(): Int {
        check(size < capacity) { "a channel has more than $capacity notes waiting" }
        val s = (head + size) % capacity
        size++
        return s
    }

    fun pop() {
        head = (head + 1) % capacity
        size--
    }

    fun clear() {
        head = 0
        size = 0
    }
}

/** What the voices count, for the tests: notes whose pitch went over the register's top, and notes cut too late. */
internal class VoiceStats {
    var pitchClamps = 0
    var lateCuts = 0
    var notes = 0
}

/**
 * One voice of the chip, playing one note at a time (sinte.tocar, Instrumento.render and renderizar's cut, sample by
 * sample): the sample through the 14-bit pitch register and the gaussian interpolation (or the noise generator), the
 * ADSR envelope, and, when a note is followed by the next one on its channel before its release is over, an 8 ms ramp
 * that ends right when the next one starts. The envelope's decays are multiplied sample by sample; the prototype raises
 * a power for each sample, and the two agree to about 1e-11.
 */
internal class Voice(private val instruments: Array<Instrument>, private val stats: VoiceStats) {
    var active = false
        private set

    // The note.
    private var started = 0L
    private var index = 0
    private var gain = 0.0
    private var data = EMPTY
    private var pos = 0
    private var step = 0
    private var end = 0
    private var loop = 0
    private var silent = false

    // Its pitch, when it moves (vibrato or ornament): updated every 32 samples.
    private var moving = false
    private var baseHz = 0.0
    private var ratio = 0.0
    private var vibratoCents = 0.0
    private var vibratoHz = 0.0
    private var vibratoDelay = 0.0
    private var hasVibrato = false
    private var ornament = Ornament.NONE
    private var ornamentSemitones = 0.0
    private var ornamentHz = 0.0
    private var ornamentMs = 0.0

    // The noise generator, for the noise instruments.
    private var noise = false
    private var lfsr = 0
    private var noiseCount = 0.0
    private var noiseStep = 0.0
    private var noiseValue = 0.0

    // The envelope, as the prototype's ADSR.generar.
    private var held = 0
    private var attack = 0
    private var attackEnd = 0
    private var decayEnd = 0
    private var decayFactor = 1.0
    private var sustainFactor = 1.0
    private var afterAttack = 1.0
    private var afterDecay = 1.0
    private var atRelease = 1.0
    private var release = 1
    private var total = 0
    private var level = 0.0
    private var decayMs = 0.0
    private var sustainMs = 0.0
    private var envelope = Envelope()

    // The cut: the note ends at [cutAt] (samples from its start), after a ramp of [cutLength].
    private var cutAt = -1
    private var cutLength = 0
    private var cutChecked = false

    // Where the note was written, to cut it if its section is cut short.
    var section = -1
        private set
    private var writtenPos = 0
    private var writtenSteps = 0
    private var semi = 0.0

    /** Renders [count] samples from time [from] (samples at 32 kHz) into [out], starting the notes of [queue] on time. */
    fun render(
        queue: NoteQueue,
        from: Long,
        out: DoubleArray,
        count: Int,
    ) {
        var i = 0
        while (i < count) {
            val now = from + i
            // A note that starts on the same sample as the next one is cut to a single silent sample (renderizar), so
            // only the last of those that are due is started.
            while (queue.size > 1 && queue.start[queue.slot(1)] <= now) queue.pop()
            if (queue.size > 0 && queue.start[queue.head] <= now) {
                start(queue, now)
                queue.pop()
            }
            if (active && !cutChecked && queue.size > 0) planCut(queue.start[queue.head], now)
            var span = count - i
            if (queue.size > 0) span = min(span.toLong(), queue.start[queue.head] - now).toInt()
            if (!active) {
                for (k in 0 until span) out[i + k] = 0.0
            } else {
                play(out, i, span)
            }
            i += span
        }
    }

    /** Whether the note sounds past [time]; if so, it will end there with the ramp (renderizar's cut). */
    private fun planCut(next: Long, now: Long) {
        cutChecked = true
        if (started + total <= next) return
        val length = max(1L, next - started).toInt()
        cutAt = length
        cutLength = min(Chip.CUT, length)
        if (started + cutAt - cutLength < now) stats.lateCuts++
    }

    private fun start(queue: NoteQueue, now: Long) {
        val s = queue.head
        val instrument = instruments[queue.instrument[s]]
        val recipe = instrument.recipe
        stats.notes++
        active = true
        started = queue.start[s]
        index = (now - started).toInt()
        gain = queue.gain[s]
        section = queue.section[s]
        writtenPos = queue.pos[s]
        writtenSteps = queue.steps[s]
        semi = queue.semi[s]
        cutAt = -1
        cutChecked = false
        envelope = recipe.envelope
        decayMs = envelope.decayMs
        sustainMs = envelope.sustainMs
        noise = recipe.noise >= 0
        if (noise) {
            lfsr = 0x4000 xor 1
            noiseCount = 0.0
            noiseStep = Chip.noiseRates[recipe.noise].toDouble() / Chip.RATE
            noiseValue = 0.0
            moving = false
        } else {
            val note = queue.pitch[s]
            val hz = if (recipe.fixedHz > 0) recipe.fixedHz * DetMath.exp2(note / 12.0) else Chip.hz(note.toDouble())
            if (recipe.decayScale != 0.0) {
                val e = DetMath.pow(recipe.refHz / hz, recipe.decayScale)
                decayMs *= e
                sustainMs *= e
            }
            val sample = if (instrument.isChords) {
                instrument.chordSamples[queue.quality[s]] ?: instrument.samples[0]
            } else {
                instrument.pick(
                    hz,
                )
            }
            data = sample.played
            end = sample.length * Chip.PITCH_ONE
            loop = if (sample.loopStart >= 0) (sample.length - sample.loopStart) * Chip.PITCH_ONE else 0
            pos = 0
            silent = false
            ratio = sample.rate / (sample.root * Chip.RATE)
            baseHz = hz
            val vibrato = recipe.vibrato
            hasVibrato = vibrato != null
            if (vibrato != null) {
                vibratoCents = vibrato.cents
                vibratoHz = vibrato.hz
                vibratoDelay = vibrato.delayMs / 1000.0
            }
            if (queue.ornament[s] != Ornament.NONE) {
                ornament = queue.ornament[s]
                ornamentSemitones = queue.ornamentSemitones[s]
                ornamentHz = queue.ornamentHz[s]
                ornamentMs = queue.ornamentMs[s]
            } else {
                val o = recipe.ornament
                ornament = o?.kind ?: Ornament.NONE
                ornamentSemitones = o?.semitones ?: 0.0
                ornamentHz = o?.hz ?: 0.0
                ornamentMs = o?.ms ?: 0.0
            }
            moving = hasVibrato || ornament != Ornament.NONE
            step = stepFor(baseHz)
        }
        planEnvelope(queue.length[s])
    }

    /** The pitch register for [hz]: f / root · rate / 32 000 in 1/4096, with its top. */
    private fun stepFor(hz: Double): Int {
        val register = round(hz * ratio * Chip.PITCH_ONE).toInt()
        if (register > Chip.PITCH_MAX) {
            stats.pitchClamps++
            return Chip.PITCH_MAX
        }
        return register
    }

    /** The pitch at [t] seconds into the note: the ornament's curve and the vibrato (sinte.curva_adorno and render). */
    private fun pitchAt(t: Double): Double {
        var f = baseHz
        when (ornament) {
            Ornament.SLIDE -> {
                val x = 1.0 - min(1.0, t * 1000.0 / ornamentMs)
                f = baseHz * DetMath.exp2(ornamentSemitones * (x * x) / 12.0)
            }
            Ornament.MORDENT -> if (t >= 0.025 && t < 0.06) f = baseHz * DetMath.exp2(ornamentSemitones / 12.0)
            Ornament.TRILL -> {
                if (t < ornamentMs / 1000.0 && (t * ornamentHz * 2).toInt() % 2 == 1) f = baseHz * DetMath.exp2(ornamentSemitones / 12.0)
            }
        }
        if (!hasVibrato || t <= vibratoDelay) return f
        val ramp = min(1.0, (t - vibratoDelay) / 0.15)
        return f * Chip.cents(vibratoCents * ramp * DetMath.sin(2.0 * DetMath.PI * vibratoHz * (t - vibratoDelay)))
    }

    /** The envelope of a note held [length] samples, phase by phase, with the values the prototype reaches. */
    private fun planEnvelope(length: Int) {
        held = length
        attack = Chip.ms(envelope.attackMs)
        attackEnd = if (attack > 0) min(attack, held) else 0
        afterAttack = if (attack > 0) attackEnd.toDouble() / attack else 1.0
        var rest = held - attackEnd
        decayEnd = attackEnd
        decayFactor = 1.0
        afterDecay = afterAttack
        val sustain = envelope.sustainLevel
        if (rest > 0 && decayMs > 0 && sustain < 1.0 && afterAttack > sustain) {
            decayFactor = DetMath.exp(-1000.0 / (decayMs * Chip.RATE))
            val steps = ceil(DetMath.ln(sustain / afterAttack) / DetMath.ln(decayFactor)).toInt()
            val nd = min(rest, max(1, steps))
            afterDecay = afterAttack * DetMath.pow(decayFactor, nd.toDouble())
            decayEnd = attackEnd + nd
            rest -= nd
        }
        sustainFactor = 1.0
        atRelease = afterDecay
        if (rest > 0 && sustainMs > 0) {
            sustainFactor = DetMath.exp(-1000.0 / (sustainMs * Chip.RATE))
            atRelease = afterDecay * DetMath.pow(sustainFactor, rest.toDouble())
        }
        release = max(1, round(atRelease * Chip.ms(envelope.releaseMs)).toInt())
        total = held + release
        level = if (attackEnd > 0) 0.0 else afterAttack
    }

    /**
     * The section this note was written in is cut short at [limit] sixteenths: as the prototype clips the note to the
     * section's end, its held length becomes (limit − position) sixteenths times the gate, from now on at the latest.
     */
    fun cutSection(sectionId: Int, limit: Int, gate: Double) {
        if (!active || section != sectionId || writtenPos + writtenSteps <= limit) return
        val newHeld = max(1, ((limit - writtenPos) * semi * gate).toInt())
        if (newHeld >= held) return
        if (index >= held) return
        reHold(max(newHeld, index))
    }

    /** Changes how long the note is held, keeping where its envelope is now. */
    private fun reHold(newHeld: Int) {
        val now = index
        val current = level
        planEnvelope(newHeld)
        index = now
        level = current
        cutChecked = false
        cutAt = -1
    }

    private fun play(out: DoubleArray, offset: Int, count: Int) {
        for (k in 0 until count) {
            if (!active) {
                out[offset + k] = 0.0
                continue
            }
            val i = index
            val e = envelopeAt(i)
            val x = if (noise) nextNoise() else nextSample(i)
            var y = x * e
            if (cutAt >= 0 && i >= cutAt - cutLength) y *= (cutAt - 1 - i).toDouble() / cutLength
            out[offset + k] = y * gain
            index = i + 1
            if (index >= total || (cutAt >= 0 && index >= cutAt)) active = false
        }
    }

    private fun envelopeAt(i: Int): Double {
        if (i < attackEnd) {
            level = (i + 1).toDouble() / attack
            if (i == attackEnd - 1) level = afterAttack
            return (i + 1).toDouble() / attack
        }
        if (i < decayEnd) {
            level = if (i == attackEnd) afterAttack * decayFactor else level * decayFactor
            if (i == decayEnd - 1) level = afterDecay
            return if (i == decayEnd - 1) afterDecay else level
        }
        if (i < held) {
            if (sustainFactor == 1.0) {
                level = afterDecay
                return afterDecay
            }
            level = if (i == decayEnd) afterDecay * sustainFactor else level * sustainFactor
            if (i == held - 1) level = atRelease
            return level
        }
        if (i < total) return atRelease * (1.0 - (i - held + 1).toDouble() / release)
        return 0.0
    }

    private fun nextSample(i: Int): Double {
        if (moving && i % Chip.PITCH_BLOCK == 0) step = stepFor(pitchAt(i.toDouble() / Chip.RATE))
        if (silent) return 0.0
        val at = pos ushr 12
        val w = ((pos and 0xFFF) ushr 4) * 4
        val g = Chip.gauss
        val value = data[at] * g[w] + data[at + 1] * g[w + 1] + data[at + 2] * g[w + 2] + data[at + 3] * g[w + 3]
        pos += step
        if (pos >= end) {
            if (loop == 0) silent = true else pos -= loop
        }
        return value
    }

    private fun nextNoise(): Double {
        noiseCount += noiseStep
        while (noiseCount >= 1.0) {
            noiseCount -= 1.0
            lfsr = Chip.nextNoise(lfsr)
            noiseValue = Chip.noiseLevel(lfsr)
        }
        return noiseValue
    }

    private companion object {
        val EMPTY = FloatArray(4)
    }
}
