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
        assertEquals(PlatinumPurchaseOption("pp_pack_small", 0.99, 4), PLATINUM_PURCHASE_OPTIONS[0])
        assertEquals(PlatinumPurchaseOption("pp_pack_medium", 2.99, 15), PLATINUM_PURCHASE_OPTIONS[1])
        assertEquals(PlatinumPurchaseOption("pp_pack_large", 4.99, 30), PLATINUM_PURCHASE_OPTIONS[2])
        assertEquals(PlatinumPurchaseOption("pp_pack_huge", 6.99, 55), PLATINUM_PURCHASE_OPTIONS[3])
        assertEquals(PlatinumPurchaseOption("pp_pack_mega", 9.99, 100), PLATINUM_PURCHASE_OPTIONS[4])
    }

    @Test
    fun `the entry tier is a deliberately stingy teaser, not a real dent in anything`() {
        // Explicit instruction: $0.99 should feel like a teaser, not a
        // real currency purchase — cheap enough that it can't even afford
        // the cheapest permanent boost tier's first copy (basePp 5.0).
        val cheapestPermanentTier = ALL_PERMANENT_BOOST_TIERS.minBy { it.basePp }
        val entryTierPp = PLATINUM_PURCHASE_OPTIONS.first().platinumPieces

        assertTrue(entryTierPp < cheapestPermanentTier.basePp)
    }

    @Test
    fun `pp per dollar climbs a real amount from entry to top tier, not a flat rate`() {
        val ratesPerDollar = PLATINUM_PURCHASE_OPTIONS.map { it.platinumPieces / it.priceUsd }

        // Monotonically better value per dollar at higher tiers...
        assertTrue(ratesPerDollar.zipWithNext().all { (a, b) -> b >= a })
        // ...and this time deliberately a real jump (roughly 2-3x), not
        // the earlier "mild bonus" curve — the point is Platinum itself
        // should feel scarce, with the top pack as the one real purchase.
        val ratio = ratesPerDollar.last() / ratesPerDollar.first()
        assertTrue("expected roughly a 2-3x better rate at the top, got ${ratio}x", ratio in 2.0..3.0)
    }

    @Test
    fun `the top tier still can't buy more than a couple of the priciest permanent boost tier`() {
        // The 10x Speed / 5x Gem % tiers share the steepest cost curve
        // (basePp 60, costGrowthRate 1.8).
        val steepTier = PermanentBoostTier(PermanentBoostCategory.SPEED, multiplier = 10.0, basePp = 60.0, costGrowthRate = 1.8)
        val topTierPp = PLATINUM_PURCHASE_OPTIONS.last().platinumPieces

        var level = 0
        var spent = 0.0
        while (spent + costForPermanentBoostPurchase(steepTier, level) <= topTierPp) {
            spent += costForPermanentBoostPurchase(steepTier, level)
            level++
        }

        assertTrue("expected at most 2 repeat copies, got $level", level <= 2)
    }
}
