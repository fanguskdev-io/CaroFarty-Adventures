package com.fanguskdev.carofarty.storage

import platform.Foundation.NSUserDefaults

/** What the game remembers, in the app's NSUserDefaults. */
class IosKeyValueStore : KeyValueStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getInt(key: String, default: Int): Int =
        if (defaults.objectForKey(key) == null) default else defaults.integerForKey(key).toInt()

    override fun putInt(key: String, value: Int) = defaults.setInteger(value.toLong(), key)

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)

    override fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, key)
}
