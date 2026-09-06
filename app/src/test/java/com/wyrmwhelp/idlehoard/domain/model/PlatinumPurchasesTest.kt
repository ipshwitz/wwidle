package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatinumPurchasesTest {

    @Test
    fun `there are exactly five packs, cheapest first`() {
        assertEquals(5, PLATINUM_PURCHASE_OPTIONS.size)
        assertTrue(PLATINUM_PURCHASE_OPTIONS.zipWithNext().all { (a, b) -> a.priceUsd < b.priceUsd })
        assertTrue(PLATINUM_PURCHASE_OPTIONS.zipWithNext().all { (a, b) -> a.platinumPieces < b.platinumPieces })
    }

    @Test
    fun `every product id is unique`() {
        val ids = PLATINUM_PURCHASE_OPTIONS.map { it.productId }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `matches the confirmed price and PP amounts`() {
        assertEquals(PlatinumPurchaseOption("pp_pack_small", 0.99, 100), PLATINUM_PURCHASE_OPTIONS[0])
        assertEquals(PlatinumPurchaseOption("pp_pack_medium", 2.99, 330), PLATINUM_PURCHASE_OPTIONS[1])
        assertEquals(PlatinumPurchaseOption("pp_pack_large", 4.99, 600), PLATINUM_PURCHASE_OPTIONS[2])
        assertEquals(PlatinumPurchaseOption("pp_pack_huge", 6.99, 920), PLATINUM_PURCHASE_OPTIONS[3])
        assertEquals(PlatinumPurchaseOption("pp_pack_mega", 9.99, 1_400), PLATINUM_PURCHASE_OPTIONS[4])
    }

    @Test
    fun `pp per dollar increases only mildly at higher tiers, never enough for the top tier to feel unlimited`() {
        val ratesPerDollar = PLATINUM_PURCHASE_OPTIONS.map { it.platinumPieces / it.priceUsd }

        // Monotonically better value per dollar at higher tiers...
        assertTrue(ratesPerDollar.zipWithNext().all { (a, b) -> b >= a })
        // ...but the top tier is at most ~50% better per-dollar than the entry
        // tier, not an order of magnitude — the explicit "shouldn't be a
        // year's worth of PP they'll never use" constraint on the top pack.
        assertTrue(ratesPerDollar.last() / ratesPerDollar.first() < 1.5)
    }

    @Test
    fun `the top tier can't buy more than a handful of repeat copies of the priciest permanent boost tier`() {
        // The 10x Speed / 5x Gem % tiers share the steepest cost curve
        // (basePp 60, costGrowthRate 1.8) — this is the scenario that
        // originally motivated pulling the whole price range in from
        // $0.99-$49.99 to $0.99-$9.99.
        val steepTier = PermanentBoostTier(PermanentBoostCategory.SPEED, multiplier = 10.0, basePp = 60.0, costGrowthRate = 1.8)
        val topTierPp = PLATINUM_PURCHASE_OPTIONS.last().platinumPieces

        var level = 0
        var spent = 0.0
        while (spent + costForPermanentBoostPurchase(steepTier, level) <= topTierPp) {
            spent += costForPermanentBoostPurchase(steepTier, level)
            level++
        }

        assertTrue("expected at most 5 repeat copies, got $level", level <= 5)
    }
}
