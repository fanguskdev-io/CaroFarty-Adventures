package com.fanguskdev.carofarty.storage

/** Today where the phone is, as yyyymmdd: the day of a run in the records. */
internal expect fun today(): Int

/** A day of [today] as the records show it (GameMaster's mockup), 2 NOV 2026, or a hyphen if it is not known. */
internal fun dayText(day: Int): String {
    val month = day / 100 % 100
    if (day <= 0 || month !in 1..MONTHS.size) return "-"
    return "${day % 100} ${MONTHS[month - 1]} ${day / 10_000}"
}

/** The months as the records show them, in three letters. */
private val MONTHS = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
