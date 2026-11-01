package com.fanguskdev.carofarty.storage

import com.fanguskdev.carofarty.game.Scenario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordsTest {
    @Test
    fun theFirstRunSetsBothRecordsAndEachIsBeatenOnItsOwn() {
        val records = Records(MemoryStore())
        val first = records.submit(Scenario.Bosque, meters = 120, maxPoints = 14)
        assertTrue(first.newMeters && first.newPoints)

        val second = records.submit(Scenario.Bosque, meters = 80, maxPoints = 20)
        assertFalse(second.newMeters)
        assertTrue(second.newPoints)
        assertTrue(second.newRecord)
        assertEquals(120, second.bestMeters)
        assertEquals(20, second.bestPoints)
    }

    @Test
    fun aTieIsNoNewRecord() {
        val records = Records(MemoryStore())
        records.submit(Scenario.Bosque, meters = 120, maxPoints = 14)
        assertFalse(records.submit(Scenario.Bosque, meters = 120, maxPoints = 14).newRecord)
    }

    @Test
    fun eachScenarioKeepsItsOwnAndTheyStayInTheStore() {
        val store = MemoryStore()
        Records(store).submit(Scenario.Nieve, meters = 300, maxPoints = 30)
        val again = Records(store)
        assertEquals(300, again.meters(Scenario.Nieve))
        assertEquals(30, again.points(Scenario.Nieve))
        assertEquals(0, again.meters(Scenario.Bosque))
        assertEquals(0, again.points(Scenario.Desierto))
    }
}
