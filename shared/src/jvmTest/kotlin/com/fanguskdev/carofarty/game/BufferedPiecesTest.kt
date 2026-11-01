package com.fanguskdev.carofarty.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BufferedPiecesTest {
    private val config = GameConfig()

    private fun describe(piece: Piece) =
        piece.grounds.map { Triple(it.start, it.end, it.top) } to piece.items.map { Triple(it.kind, it.x, it.y) }

    @Test
    fun theStartIsReadyAtOnceAndTheRestComeInTheSameOrderAsWithoutABackground() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val buffered = BufferedPieces(CourseGenerator(config, seed = 9), scope)
        val direct = DirectPieces(CourseGenerator(config, seed = 9))

        assertNotNull(buffered.nextOrNull(), "the start is there from the first frame")
        direct.nextOrNull()
        repeat(50) {
            var piece = buffered.nextOrNull()
            while (piece == null) {
                delay(1)
                piece = buffered.nextOrNull()
            }
            assertEquals(describe(direct.nextOrNull()), describe(piece))
        }
        buffered.close()
        scope.cancel()
    }

    @Test
    fun aRunWithPiecesMadeInTheBackgroundIsTheSameRun() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val buffered = GameRun(config, Scenario.Nieve, seed = 5, pieces = BufferedPieces(CourseGenerator(config, 5), scope))
        val direct = GameRun(config, Scenario.Nieve, seed = 5)
        delay(200)

        val pilots = listOf(Pilot(buffered), Pilot(direct))
        repeat(60 * 60) {
            pilots.forEach { it.step() }
            if (buffered.course.end - buffered.caro.x < GameRun.AHEAD) delay(1)
        }

        assertEquals(direct.meters, buffered.meters, 0.5)
        assertEquals(direct.points, buffered.points)
        scope.cancel()
    }
}
