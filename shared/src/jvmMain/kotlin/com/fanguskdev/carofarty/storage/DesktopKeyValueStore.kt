package com.fanguskdev.carofarty.storage

import java.util.prefs.Preferences

/**
 * What the game remembers on the desktop, in the user's Java preferences. Every write goes to disk at once: the game only
 * writes at the end of a run, and a desktop window can be closed in ways that skip the preferences' own sync.
 */
class DesktopKeyValueStore : KeyValueStore {
    private val preferences = Preferences.userRoot().node("com/fanguskdev/carofarty")

    override fun getInt(key: String, default: Int): Int = preferences.getInt(key, default)

    override fun putInt(key: String, value: Int) {
        preferences.putInt(key, value)
        preferences.flush()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean = preferences.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.putBoolean(key, value)
        preferences.flush()
    }
}
