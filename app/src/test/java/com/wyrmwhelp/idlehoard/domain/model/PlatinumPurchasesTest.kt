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
        assertEquals(PlatinumPurchaseOption("pp_pack_medium", 4.99, 550), PLATINUM_PURCHASE_OPTIONS[1])
        assertEquals(PlatinumPurchaseOption("pp_pack_large", 9.99, 1_200), PLATINUM_PURCHASE_OPTIONS[2])
        assertEquals(PlatinumPurchaseOption("pp_pack_huge", 19.99, 2_600), PLATINUM_PURCHASE_OPTIONS[3])
        assertEquals(PlatinumPurchaseOption("pp_pack_mega", 49.99, 7_000), PLATINUM_PURCHASE_OPTIONS[4])
    }

    @Test
    fun `pp per dollar increases only mildly at higher tiers, never enough for the top tier to feel unlimited`() {
        val ratesPerDollar = PLATINUM_PURCHASE_OPTIONS.map { it.platinumPieces / it.priceUsd }

        // Monotonically better value per dollar at higher tiers...
        assertTrue(ratesPerDollar.zipWithNext().all { (a, b) -> b >= a })
        // ...but the top tier is at most ~50% better per-dollar than the entry
        // tier, not an order of magnitude — the explicit "shouldn't be a
        // year's worth of PP they'll never use" constraint on the $49.99 pack.
        assertTrue(ratesPerDollar.last() / ratesPerDollar.first() < 1.5)
    }
}
