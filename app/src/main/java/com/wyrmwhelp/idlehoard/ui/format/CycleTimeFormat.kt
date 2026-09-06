package com.wyrmwhelp.idlehoard.ui.format

import kotlin.math.roundToLong

/**
 * Formats a lair's actual cycle time — `CreatureLair.effectiveProductionSeconds`,
 * after Speed Boost and milestone stacking — for `LairCard`'s "gp / cycle
 * time" line. Ranges from single-digit milliseconds (a heavily Speed-boosted
 * early lair) up to multiple days (a late, unboosted high tier), so unlike
 * `DurationFormat` (which only ever deals in whole minutes/hours for ad
 * cooldowns) this needs a millisecond tier at the bottom in addition to the
 * usual d/h/m/s ladder.
 */
object CycleTimeFormat {

    fun format(seconds: Double): String {
        if (seconds < 1.0) {
            val millis = (seconds * 1_000.0).roundToLong().coerceAtLeast(0)
            return "${millis}ms"
        }
        if (seconds < 60.0) {
            return if (seconds == Math.floor(seconds)) {
                "${seconds.toLong()}s"
            } else {
                "%.1fs".format(seconds)
            }
        }

        val totalSeconds = seconds.roundToLong()
        val days = totalSeconds / 86_400
        val hours = (totalSeconds % 86_400) / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val secs = totalSeconds % 60

        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            secs > 0 -> "${minutes}m ${secs}s"
            else -> "${minutes}m"
        }
    }
}
