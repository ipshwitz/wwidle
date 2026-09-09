package com.wyrmwhelp.idlehoard.domain.model

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StewardEfficiencyTest {

    @Test
    fun `level 0 applies no discount`() {
        assertEquals(1.0, StewardEfficiency.costMultiplier(0), 0.0001)
    }

    @Test
    fun `each tier sets the discount outright, not additive with the tier before it`() {
        assertEquals(0.90, StewardEfficiency.costMultiplier(1), 0.0001) // 10% off
        assertEquals(0.80, StewardEfficiency.costMultiplier(2), 0.0001) // 20% off, not 10%+20%
        assertEquals(0.50, StewardEfficiency.costMultiplier(5), 0.0001) // 50% off
    }

    @Test
    fun `the final tier caps the discount at 99 percent, never free`() {
        assertEquals(0.01, StewardEfficiency.costMultiplier(StewardEfficiency.MAX_TIER), 0.0001)
        assertTrue(StewardEfficiency.costMultiplier(StewardEfficiency.MAX_TIER) > 0.0)
    }

    @Test
    fun `a level past the max tier clamps rather than reading past the discount table`() {
        assertEquals(
            StewardEfficiency.costMultiplier(StewardEfficiency.MAX_TIER),
            StewardEfficiency.costMultiplier(StewardEfficiency.MAX_TIER + 5),
            0.0001,
        )
    }

    @Test
    fun `tier costs escalate steeply, each tier costing 3x the last`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        val tier1 = StewardEfficiency.costForTier(lair, 1)
        val tier2 = StewardEfficiency.costForTier(lair, 2)
        assertEquals(tier1 * 3.0, tier2, 0.001)
    }

    @Test
    fun `the discount actually reduces a lair's next-unit cost`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        val fullPrice = lair.costForNextUnit(unitsOwned = 0)
        val discounted = lair.costForNextUnit(unitsOwned = 0, costMultiplier = StewardEfficiency.costMultiplier(1))
        assertEquals(fullPrice * 0.9, discounted, 0.0001)
    }
}
