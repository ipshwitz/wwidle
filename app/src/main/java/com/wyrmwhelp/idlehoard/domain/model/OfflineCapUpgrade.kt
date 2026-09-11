package com.wyrmwhelp.idlehoard.domain.model

/**
 * One purchasable step of [GameState.offlineCapHours]'s Platinum-bought
 * upgrade ladder (Shop → Permanent tab) — a flat, one-time raise to the cap
 * itself, unlike [PermanentBoostTier] (repeatedly purchasable, compounding
 * exponentially with each copy). [hours] is the *new* cap this tier sets
 * outright, not an amount added to the current one.
 */
data class OfflineCapTier(val hours: Double, val costPp: Double)

/**
 * The full ladder: 4h (the default every save starts with, not itself a
 * tier here) → 8h → 12h. Deliberately capped well short of a full day for
 * now — a further tier (e.g. 24h) was considered and explicitly held back
 * as "too high right now" until the economy's been played more at these
 * lower caps. Tiers must be bought in order (see
 * [GameState.nextOfflineCapTier]) — there's no way to skip straight to
 * 12h. First-pass placeholder pricing, not playtested, same as everywhere
 * else in the economy.
 */
val OFFLINE_CAP_TIERS: List<OfflineCapTier> = listOf(
    OfflineCapTier(hours = 8.0, costPp = 30.0),
    OfflineCapTier(hours = 12.0, costPp = 100.0),
)

/**
 * The next tier still ahead of [GameState.offlineCapHours], or null once
 * every [OFFLINE_CAP_TIERS] entry has been bought. Since each tier sets the
 * cap outright (never skipped, always bought in order — see
 * [OFFLINE_CAP_TIERS]'s doc), the first entry whose [OfflineCapTier.hours]
 * exceeds the current cap is always exactly the one purchasable next.
 */
fun GameState.nextOfflineCapTier(): OfflineCapTier? = OFFLINE_CAP_TIERS.firstOrNull { it.hours > offlineCapHours }
