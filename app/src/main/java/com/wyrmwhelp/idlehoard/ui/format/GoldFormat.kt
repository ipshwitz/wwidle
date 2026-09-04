package com.wyrmwhelp.idlehoard.ui.format

import kotlin.math.floor

/**
 * Formats Gold Piece amounts for display: whole numbers under 1,000, then
 * short-scale suffixes (K/M/B/T/Qa/...) beyond that. First-pass placeholder —
 * see CLAUDE.md's open questions on large-number formatting; `goldPieces` is a
 * raw Double for now, so this will need revisiting once the economy grows past
 * what a Double can represent precisely.
 */
object GoldFormat {

    private val suffixes = listOf("", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No")

    fun format(amount: Double): String {
        if (amount < 1_000.0) {
            return if (amount == floor(amount)) {
                amount.toLong().toString()
            } else {
                "%.1f".format(amount)
            }
        }
        var value = amount
        var suffixIndex = 0
        while (value >= 1_000.0 && suffixIndex < suffixes.lastIndex) {
            value /= 1_000.0
            suffixIndex++
        }
        return "%.2f%s".format(value, suffixes[suffixIndex])
    }
}
