package com.fanguskdev.carofarty.storage

import java.util.Calendar

internal actual fun today(): Int {
    val now = Calendar.getInstance()
    return now.get(Calendar.YEAR) * 10_000 + (now.get(Calendar.MONTH) + 1) * 100 + now.get(Calendar.DAY_OF_MONTH)
}
