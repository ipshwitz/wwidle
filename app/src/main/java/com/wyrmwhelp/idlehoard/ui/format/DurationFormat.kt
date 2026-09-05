package com.wyrmwhelp.idlehoard.ui.format

import java.time.Duration

/**
 * Formats a short cooldown remainder as "3h 12m" / "12m" — used by both
 * `GameViewModel`'s Platinum-ad message text and `ShopContent`'s own
 * reactive button label, so the two can't drift apart.
 */
object DurationFormat {

    fun format(duration: Duration): String {
        val totalMinutes = duration.toMinutes().coerceAtLeast(1)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
}
