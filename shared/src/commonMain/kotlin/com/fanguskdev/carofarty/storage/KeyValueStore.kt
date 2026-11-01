package com.fanguskdev.carofarty.storage

/**
 * The little the game remembers between launches (the records and the settings), kept on the phone by each platform:
 * SharedPreferences on Android, NSUserDefaults on iOS, Preferences on the desktop. Nothing leaves the phone (ADR 0005).
 */
interface KeyValueStore {
    fun getInt(key: String, default: Int): Int

    fun putInt(key: String, value: Int)

    fun getBoolean(key: String, default: Boolean): Boolean

    fun putBoolean(key: String, value: Boolean)
}

/** A store that forgets everything when the app closes: for the tests and the screenshots. */
class MemoryStore : KeyValueStore {
    private val values = HashMap<String, Any>()

    override fun getInt(key: String, default: Int): Int = values[key] as? Int ?: default

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun getBoolean(key: String, default: Boolean): Boolean = values[key] as? Boolean ?: default

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }
}
