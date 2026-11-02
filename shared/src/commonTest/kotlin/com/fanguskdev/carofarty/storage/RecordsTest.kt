package com.fanguskdev.carofarty.storage

import com.fanguskdev.carofarty.game.Scenario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordsTest {
    @Test
    fun theTenBestRunsOfAScenarioByMetresWithTheirPointsAndDay() {
        val records = Records(MemoryStore())
        val first = records.submit(Scenario.Bosque, meters = 120, maxPoints = 14, day = 20261102)
        assertEquals(1, first.place)
        assertTrue(first.newRecord, "the first run of a scenario is its record")

        val second = records.submit(Scenario.Bosque, meters = 80, maxPoints = 20, day = 20261103)
        assertEquals(2, second.place)
        assertFalse(second.newRecord)
        assertEquals(120, second.bestMeters)

        val third = records.submit(Scenario.Bosque, meters = 300, maxPoints = 9, day = 20261104)
        assertTrue(third.newRecord)
        assertEquals(300, third.bestMeters)
        assertEquals(
            listOf(RecordRun(300, 9, 20261104), RecordRun(120, 14, 20261102), RecordRun(80, 20, 20261103)),
            records.best(Scenario.Bosque),
        )
    }

    @Test
    fun onlyFiveAreKeptAndABetterRunPushesTheLastOneOut() {
        val records = Records(MemoryStore())
        for (meters in 10..50 step 10) records.submit(Scenario.Nieve, meters, maxPoints = 1)
        assertEquals(5, records.best(Scenario.Nieve).size)
        assertEquals(0, records.submit(Scenario.Nieve, meters = 5, maxPoints = 1).place, "too short")
        assertEquals(5, records.submit(Scenario.Nieve, meters = 15, maxPoints = 1).place, "the last place")
        assertEquals(listOf(50, 40, 30, 20, 15), records.best(Scenario.Nieve).map { it.meters })
        assertEquals(2, records.submit(Scenario.Nieve, meters = 45, maxPoints = 1).place)
        assertEquals(listOf(50, 45, 40, 30, 20), records.best(Scenario.Nieve).map { it.meters }, "the 15 goes out")
    }

    @Test
    fun aTieGoesAfterTheOlderRunAndIsNoNewRecord() {
        val records = Records(MemoryStore())
        records.submit(Scenario.Bosque, meters = 120, maxPoints = 14)
        val tie = records.submit(Scenario.Bosque, meters = 120, maxPoints = 30)
        assertEquals(2, tie.place)
        assertFalse(tie.newRecord)
    }

    @Test
    fun aRunOfNoMetresDoesNotGetIn() {
        assertEquals(0, Records(MemoryStore()).submit(Scenario.Desierto, meters = 0, maxPoints = 10).place)
    }

    @Test
    fun eachScenarioKeepsItsOwnAndTheyStayInTheStore() {
        val store = MemoryStore()
        Records(store).submit(Scenario.Nieve, meters = 300, maxPoints = 30, day = 20261102)
        val again = Records(store)
        assertEquals(listOf(RecordRun(300, 30, 20261102)), again.best(Scenario.Nieve))
        assertEquals(300, again.meters(Scenario.Nieve))
        assertTrue(again.best(Scenario.Bosque).isEmpty())
    }

    @Test
    fun theRecordsOfTheFirstVersionsComeInAsARunWithoutADay() {
        val store = MemoryStore()
        store.putInt("record_bosque_meters", 496)
        store.putInt("record_bosque_points", 83)
        val records = Records(store)
        assertEquals(listOf(RecordRun(496, 83, 0)), records.best(Scenario.Bosque))
        records.submit(Scenario.Bosque, meters = 500, maxPoints = 10, day = 20261102)
        assertEquals(listOf(RecordRun(500, 10, 20261102), RecordRun(496, 83, 0)), records.best(Scenario.Bosque))
    }

    @Test
    fun aDayAsSpanishWritesIt() {
        assertEquals("02/11/2026", dayText(20261102))
        assertEquals("-", dayText(0))
        assertTrue(today() > 20_000_000, "today is a day of this century")
    }
}
