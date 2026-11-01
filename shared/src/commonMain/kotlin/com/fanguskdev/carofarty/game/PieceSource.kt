package com.fanguskdev.carofarty.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Where a run gets the next pieces of its course (GEN-03). */
interface PieceSource {
    /** The next piece, or null if none is ready yet. */
    fun nextOrNull(): Piece?

    /** Stops whatever is generating ahead: the run is over. */
    fun close() = Unit
}

/** Generates each piece when it is asked for: for the tests and the tools, where nothing is drawn meanwhile. */
class DirectPieces(private val generator: CourseGenerator) : PieceSource {
    override fun nextOrNull(): Piece = generator.next()
}

/**
 * Generates the pieces off the main thread, ahead of the run, and keeps up to [capacity] of them ready (GEN-03): trying
 * every jump of a piece costs a few milliseconds on a phone, which a frame cannot afford. The pieces come out in the same
 * order as from [DirectPieces], so the same seed still gives the same course. The flat start is made at once, so a run
 * always has ground under her feet from the first frame.
 */
class BufferedPieces(private val generator: CourseGenerator, scope: CoroutineScope, capacity: Int = 8) : PieceSource {
    private var start: Piece? = generator.next()
    private val ready = Channel<Piece>(capacity)
    private val producer: Job =
        scope.launch(Dispatchers.Default) {
            while (isActive) ready.send(generator.next())
        }

    override fun nextOrNull(): Piece? {
        start?.let {
            start = null
            return it
        }
        return ready.tryReceive().getOrNull()
    }

    override fun close() {
        producer.cancel()
        ready.close()
    }
}
