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
 * linear rate of the money spent. The whole range was pulled in from an
 * original $0.99-$49.99 spread (100-7,000 pp) to $0.99-$9.99 (100-1,400 pp)
 * after checking it against the permanent boost tiers it's meant to fund:
 * the priciest of those (`PERMANENT_SPEED_TIERS`'s 10x tier and
 * `PERMANENT_GEM_TIERS`'s 5x tier, both `basePp = 60.0`, `costGrowthRate =
 * 1.8`) only costs 60 pp for its *first* copy, so a 7,000 pp top tier could
 * buy seven repeat copies of it in one sitting (10^7 = 10,000,000x from
 * that tier alone) — the kind of one-purchase "trivializes the whole
 * economy" outcome the packs are supposed to avoid. 1,400 pp caps that
 * same tier at four repeat copies instead. Permanent boost tiers' own
 * escalating cost (`Boosts.kt`'s `costForPermanentBoostPurchase`) still
 * does most of the real work here — no *finite* PP amount can max a tier
 * out, since the cost curve is geometric — but flat-cost consumables
 * (Time Skips, temporary boosts) have no such built-in ceiling, so keeping
 * the packs themselves modest is what actually prevents a single purchase
 * from trivializing those. First-pass, not playtested, same as every
 * other tuning number in this game.
 */
data class PlatinumPurchaseOption(
    val productId: String,
    val priceUsd: Double,
    val platinumPieces: Long,
)

/** Every Platinum Pieces pack the Shop sells, cheapest first. */
val PLATINUM_PURCHASE_OPTIONS: List<PlatinumPurchaseOption> = listOf(
    PlatinumPurchaseOption(productId = "pp_pack_small", priceUsd = 0.99, platinumPieces = 100),
    PlatinumPurchaseOption(productId = "pp_pack_medium", priceUsd = 2.99, platinumPieces = 330),
    PlatinumPurchaseOption(productId = "pp_pack_large", priceUsd = 4.99, platinumPieces = 600),
    PlatinumPurchaseOption(productId = "pp_pack_huge", priceUsd = 6.99, platinumPieces = 920),
    PlatinumPurchaseOption(productId = "pp_pack_mega", priceUsd = 9.99, platinumPieces = 1_400),
)
