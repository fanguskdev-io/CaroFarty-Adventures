package com.fanguskdev.carofarty.storage

import android.content.Context

/** What the game remembers, in the app's SharedPreferences. */
class AndroidKeyValueStore(context: Context) : KeyValueStore {
    private val preferences = context.getSharedPreferences("carofarty", Context.MODE_PRIVATE)

    override fun getInt(key: String, default: Int): Int = preferences.getInt(key, default)

    override fun putInt(key: String, value: Int) = preferences.edit().putInt(key, value).apply()

    override fun getBoolean(key: String, default: Boolean): Boolean = preferences.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()
}
