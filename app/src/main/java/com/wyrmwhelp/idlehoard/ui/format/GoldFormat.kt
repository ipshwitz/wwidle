package com.wyrmwhelp.idlehoard.ui.format

import kotlin.math.floor

/**
 * Formats currency amounts (Gold Pieces, Platinum Pieces — anything that's
 * just a `Double` count) for display: whole numbers under 1,000, then
 * short-scale suffixes (K/M/B/T/Qa/...) beyond that, and letter suffixes
 * (A/B/.../Z/AA/AB/...) once the named short-scale list runs out — so the
 * economy can grow indefinitely without ever falling back to a raw digit
 * string like "2334345523343". First-pass placeholder — see CLAUDE.md's open
 * questions on large-number formatting; the underlying amount is a raw
 * `Double` for now, so this will need revisiting once the economy grows past
 * what a `Double` can represent precisely.
 */
object GoldFormat {

    private val namedSuffixes = listOf("", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc")

    fun format(amount: Double): String {
        if (amount < 1_000.0) {
            return if (amount == floor(amount)) {
                amount.toLong().toString()
            } else {
                "%.1f".format(amount)
            }
        }
        var value = amount
        var tier = 0
        while (value >= 1_000.0) {
            value /= 1_000.0
            tier++
        }
        return "%.2f%s".format(value, suffixForTier(tier))
    }

    private fun suffixForTier(tier: Int): String {
        if (tier < namedSuffixes.size) return namedSuffixes[tier]
        // Bijective base-26 (same scheme as spreadsheet columns): the first
        // tier past the named list is "A", then "B" ... "Z", "AA", "AB", ...
        var n = tier - namedSuffixes.size + 1
        val letters = StringBuilder()
        while (n > 0) {
            val remainder = (n - 1) % 26
            letters.insert(0, 'A' + remainder)
            n = (n - 1) / 26
        }
        return letters.toString()
    }
}
