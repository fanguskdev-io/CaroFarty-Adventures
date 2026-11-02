package com.fanguskdev.carofarty.audio.music

/**
 * Notes in sixteenths, column by column so that composing allocates nothing: position, length, MIDI pitch (or
 * semitones over a drum's own pitch), velocity, instrument (an index of the bank), chord quality (−1: none) and
 * ornament. [flag] marks the notes of Caro's motif, which harmonizing leaves alone; [offset] and [spread] are the
 * humanization drawn for each note (samples of timing and the velocity factor).
 */
internal class NoteList(val capacity: Int) {
    val pos = IntArray(capacity)
    val dur = IntArray(capacity)
    val pitch = IntArray(capacity)
    val vel = DoubleArray(capacity)
    val instrument = IntArray(capacity)
    val quality = IntArray(capacity)
    val ornament = IntArray(capacity)
    val ornamentSemitones = DoubleArray(capacity)
    val ornamentHz = DoubleArray(capacity)
    val ornamentMs = DoubleArray(capacity)
    val flag = BooleanArray(capacity)
    val offset = DoubleArray(capacity)
    val spread = DoubleArray(capacity)
    var size = 0
        private set

    fun clear() {
        size = 0
    }

    /** Adds a note without ornament and returns its index. */
    fun add(
        pos: Int,
        dur: Int,
        pitch: Int,
        vel: Double = 0.0,
        instrument: Int = -1,
        quality: Int = -1,
    ): Int {
        check(size < capacity) { "more than $capacity notes" }
        val i = size++
        this.pos[i] = pos
        this.dur[i] = dur
        this.pitch[i] = pitch
        this.vel[i] = vel
        this.instrument[i] = instrument
        this.quality[i] = quality
        ornament[i] = Ornament.NONE
        ornamentSemitones[i] = 0.0
        ornamentHz[i] = 0.0
        ornamentMs[i] = 0.0
        flag[i] = false
        offset[i] = 0.0
        spread[i] = 1.0
        return i
    }

    fun setOrnament(
        i: Int,
        kind: Int,
        semitones: Double = 0.0,
        hz: Double = 0.0,
        ms: Double = 0.0,
    ) {
        ornament[i] = kind
        ornamentSemitones[i] = semitones
        ornamentHz[i] = hz
        ornamentMs[i] = ms
    }

    fun setOrnament(i: Int, o: Ornament?) {
        if (o == null) setOrnament(i, Ornament.NONE) else setOrnament(i, o.kind, o.semitones, o.hz, o.ms)
    }

    /** Appends note [i] of [from] and returns its new index. */
    fun append(from: NoteList, i: Int): Int {
        check(size < capacity) { "more than $capacity notes" }
        val j = size++
        copyNote(from, i, this, j)
        return j
    }

    fun copyInto(other: NoteList) {
        other.clear()
        for (i in 0 until size) other.append(this, i)
    }

    fun removeAt(i: Int) {
        for (j in i until size - 1) copyNote(this, j + 1, this, j)
        size--
    }

    /** A stable sort by position, then length, then pitch (Python's order of (pos, dur, note) tuples). */
    fun sortByPositionLengthPitch() {
        for (i in 1 until size) {
            var j = i
            while (j > 0 && compare(j - 1, j, byLength = true) > 0) {
                swap(j - 1, j)
                j--
            }
        }
    }

    /** A stable sort by position, then pitch. */
    fun sortByPositionPitch() {
        for (i in 1 until size) {
            var j = i
            while (j > 0 && compare(j - 1, j, byLength = false) > 0) {
                swap(j - 1, j)
                j--
            }
        }
    }

    private fun compare(a: Int, b: Int, byLength: Boolean): Int {
        if (pos[a] != pos[b]) return pos[a].compareTo(pos[b])
        if (byLength && dur[a] != dur[b]) return dur[a].compareTo(dur[b])
        return pitch[a].compareTo(pitch[b])
    }

    fun swap(a: Int, b: Int) {
        if (a == b) return
        swapInt(pos, a, b)
        swapInt(dur, a, b)
        swapInt(pitch, a, b)
        swapDouble(vel, a, b)
        swapInt(instrument, a, b)
        swapInt(quality, a, b)
        swapInt(ornament, a, b)
        swapDouble(ornamentSemitones, a, b)
        swapDouble(ornamentHz, a, b)
        swapDouble(ornamentMs, a, b)
        val f = flag[a]
        flag[a] = flag[b]
        flag[b] = f
        swapDouble(offset, a, b)
        swapDouble(spread, a, b)
    }

    private companion object {
        fun swapInt(x: IntArray, a: Int, b: Int) {
            val t = x[a]
            x[a] = x[b]
            x[b] = t
        }

        fun swapDouble(x: DoubleArray, a: Int, b: Int) {
            val t = x[a]
            x[a] = x[b]
            x[b] = t
        }

        fun copyNote(
            from: NoteList,
            i: Int,
            to: NoteList,
            j: Int,
        ) {
            to.pos[j] = from.pos[i]
            to.dur[j] = from.dur[i]
            to.pitch[j] = from.pitch[i]
            to.vel[j] = from.vel[i]
            to.instrument[j] = from.instrument[i]
            to.quality[j] = from.quality[i]
            to.ornament[j] = from.ornament[i]
            to.ornamentSemitones[j] = from.ornamentSemitones[i]
            to.ornamentHz[j] = from.ornamentHz[i]
            to.ornamentMs[j] = from.ornamentMs[i]
            to.flag[j] = from.flag[i]
            to.offset[j] = from.offset[i]
            to.spread[j] = from.spread[i]
        }
    }
}
