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
 * PP amounts deliberately give only a *mild*, monotonically increasing
 * bonus at higher price points — 0%/10%/20%/30%/40% over the $0.99 tier's
 * linear rate of the money spent, and $49.99 is capped at 7,000 pp on
 * purpose, per explicit instruction that the top tier shouldn't hand over
 * "a year's worth of PP they'll never use." Permanent boost tiers' own
 * escalating cost (`Boosts.kt`'s `costForPermanentBoostPurchase`) already
 * makes any finite PP amount unable to max those out — but flat-cost
 * consumables (Time Skips, temporary boosts) have no such built-in
 * ceiling, so keeping the packs themselves modest is what actually
 * prevents a single purchase from trivializing the Platinum economy.
 * First-pass, not playtested, same as every other tuning number in this
 * game.
 */
data class PlatinumPurchaseOption(
    val productId: String,
    val priceUsd: Double,
    val platinumPieces: Long,
)

/** Every Platinum Pieces pack the Shop sells, cheapest first. */
val PLATINUM_PURCHASE_OPTIONS: List<PlatinumPurchaseOption> = listOf(
    PlatinumPurchaseOption(productId = "pp_pack_small", priceUsd = 0.99, platinumPieces = 100),
    PlatinumPurchaseOption(productId = "pp_pack_medium", priceUsd = 4.99, platinumPieces = 550),
    PlatinumPurchaseOption(productId = "pp_pack_large", priceUsd = 9.99, platinumPieces = 1_200),
    PlatinumPurchaseOption(productId = "pp_pack_huge", priceUsd = 19.99, platinumPieces = 2_600),
    PlatinumPurchaseOption(productId = "pp_pack_mega", priceUsd = 49.99, platinumPieces = 7_000),
)
