package com.wyrmwhelp.idlehoard.domain.model

/**
 * One purchasable Platinum Pieces pack — a real-money IAP, not something
 * earned or spent in-game like everything else in `Boosts.kt`.
 * [productId] must match an in-app product configured in the Google Play
 * Console (see CLAUDE.md's Monetization section for the dashboard-only
 * setup this depends on — there is no way to create these from code, the
 * same way an AdMob ad unit id or a Supabase table can't be). [priceUsd]
 * is a display-only fallback shown before Play Billing's own live price
 * loads (`billing/BillingManager.kt`) — the actual charge always comes
 * from Play Billing's own `ProductDetails`, never this field.
 *
 * **Deliberately a low-currency-value economy, per explicit instruction**
 * ("100pp for 0.99 seems like it devalued the worth of the currency...
 * meant to be a teaser"): the $0.99 tier is a stingy 4 pp — barely enough
 * to try a couple of the cheapest Time Skips, not a real dent in anything
 * — while the $9.99 top tier caps out at 100 pp, still modest against the
 * permanent boost tiers it funds (`Boosts.kt`'s costs run into the tens
 * and hundreds of pp per tier once repeat purchases compound). Unlike the
 * very first pass at this feature (a flatter 100-1,400 pp range, still
 * visible in git history), pp-per-dollar now climbs a real amount from
 * bottom to top (~4/$ to ~10/$, 2.5x) — the point isn't a mild, even
 * curve anymore, it's making Platinum itself feel scarce and worth
 * rationing, with the top pack as the one "real" purchase rather than an
 * obviously-better bulk deal. First-pass, not playtested, same as every
 * other tuning number in this game.
 */
data class PlatinumPurchaseOption(
    val productId: String,
    val priceUsd: Double,
    val platinumPieces: Long,
)

/** Every Platinum Pieces pack the Shop sells, cheapest first. */
val PLATINUM_PURCHASE_OPTIONS: List<PlatinumPurchaseOption> = listOf(
    PlatinumPurchaseOption(productId = "pp_pack_small", priceUsd = 0.99, platinumPieces = 4),
    PlatinumPurchaseOption(productId = "pp_pack_medium", priceUsd = 2.99, platinumPieces = 15),
    PlatinumPurchaseOption(productId = "pp_pack_large", priceUsd = 4.99, platinumPieces = 30),
    PlatinumPurchaseOption(productId = "pp_pack_huge", priceUsd = 6.99, platinumPieces = 55),
    PlatinumPurchaseOption(productId = "pp_pack_mega", priceUsd = 9.99, platinumPieces = 100),
)
