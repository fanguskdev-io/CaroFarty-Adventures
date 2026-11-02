package com.fanguskdev.carofarty.storage

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate

internal actual fun today(): Int {
    val now = NSCalendar.currentCalendar.components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, NSDate())
    return (now.year * 10_000 + now.month * 100 + now.day).toInt()
}
