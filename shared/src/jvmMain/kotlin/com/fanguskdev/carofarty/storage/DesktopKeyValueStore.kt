package com.fanguskdev.carofarty.storage

import java.util.prefs.Preferences

/** What the game remembers on the desktop, in the user's Java preferences. */
class DesktopKeyValueStore : KeyValueStore {
    private val preferences = Preferences.userRoot().node("com/fanguskdev/carofarty")

    override fun getInt(key: String, default: Int): Int = preferences.getInt(key, default)

    override fun putInt(key: String, value: Int) = preferences.putInt(key, value)

    override fun getBoolean(key: String, default: Boolean): Boolean = preferences.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) = preferences.putBoolean(key, value)
}
