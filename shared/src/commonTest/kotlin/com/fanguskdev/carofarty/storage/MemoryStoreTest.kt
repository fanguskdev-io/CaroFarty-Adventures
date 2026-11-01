package com.fanguskdev.carofarty.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MemoryStoreTest {
    @Test
    fun itGivesBackWhatWasPutAndTheDefaultForWhatWasNot() {
        val store = MemoryStore()
        store.putInt("record", 742)
        store.putBoolean("music", false)

        assertEquals(742, store.getInt("record", 0))
        assertFalse(store.getBoolean("music", true))
        assertEquals(10, store.getInt("missing", 10))
        assertTrue(store.getBoolean("missing", true))
    }

    @Test
    fun aKeyOfAnotherTypeGivesTheDefault() {
        val store = MemoryStore()
        store.putBoolean("record", true)

        assertEquals(0, store.getInt("record", 0))
    }
}
