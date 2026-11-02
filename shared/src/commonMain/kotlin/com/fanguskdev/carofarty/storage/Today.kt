package com.fanguskdev.carofarty.storage

/** Today where the phone is, as yyyymmdd: the day of a run in the records. */
internal expect fun today(): Int

/** A day of [today] as Spanish writes it, 02/11/2026, or a hyphen if it is not known (the pixel font has no long dash). */
internal fun dayText(day: Int): String {
    if (day <= 0) return "-"
    val year = day / 10_000
    val month = day / 100 % 100
    return "${(day % 100).toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/$year"
}
