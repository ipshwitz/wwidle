package com.wyrmwhelp.idlehoard.domain.model

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpUpgradesTest {

    @Test
    fun `the 30 GP upgrade lines sum to exactly 475 total tiers`() {
        val perLairLineTiers = GpUpgrades.LAIR_LINE_PHASES.totalTiers
        val lairLineCount = CreatureLairCatalog.lairs.size * 2 // Profit + Speed per lair
        val everythingTiers = GpUpgrades.EVERYTHING_PROFIT_PHASES.totalTiers + GpUpgrades.EVERYTHING_SPEED_PHASES.totalTiers

        assertEquals(14, CreatureLairCatalog.lairs.size)
        assertEquals(475, perLairLineTiers * lairLineCount + everythingTiers)
    }

    @Test
    fun `a lair line's base cost scales with that lair's own base claim cost`() {
        val kobold = CreatureLairCatalog.get("kobold_warren")
        val dragon = CreatureLairCatalog.get("ancient_dragons_hoard")

        assertTrue(GpUpgrades.lairLineBaseCost(dragon) > GpUpgrades.lairLineBaseCost(kobold))
        assertEquals(kobold.baseCostGp * 100.0, GpUpgrades.lairLineBaseCost(kobold), 0.0001)
    }

    @Test
    fun `costForLairTier grows within a lair's line and jumps at phase boundaries`() {
        val tier1 = GpUpgrades.costForLairTier("kobold_warren", UpgradeCategory.PROFIT, 1)
        val tier5 = GpUpgrades.costForLairTier("kobold_warren", UpgradeCategory.PROFIT, 5)
        val tier6 = GpUpgrades.costForLairTier("kobold_warren", UpgradeCategory.PROFIT, 6)

        assertTrue(tier5 > tier1)
        // Crossing from phase 1's last tier into phase 2's first should jump
        // by far more than one more smooth compounding step would.
        assertTrue(tier6 > tier5 * 2)
    }

    @Test
    fun `lairProfitMultiplier is 1x at level 0 and grows with level`() {
        assertEquals(1.0, GpUpgrades.lairProfitMultiplier(0), 0.0001)
        assertTrue(GpUpgrades.lairProfitMultiplier(5) > 1.0)
        assertTrue(GpUpgrades.lairProfitMultiplier(15) > GpUpgrades.lairProfitMultiplier(5))
    }

    @Test
    fun `lairSpeedMultiplier is 1x at level 0 and grows with level`() {
        assertEquals(1.0, GpUpgrades.lairSpeedMultiplier(0), 0.0001)
        assertTrue(GpUpgrades.lairSpeedMultiplier(15) > 1.0)
    }

    @Test
    fun `everythingProfitMultiplier and everythingSpeedMultiplier are 1x at level 0`() {
        assertEquals(1.0, GpUpgrades.everythingProfitMultiplier(0), 0.0001)
        assertEquals(1.0, GpUpgrades.everythingSpeedMultiplier(0), 0.0001)
    }

    @Test
    fun `costForEverythingTier differs from a lair line's cost scale`() {
        val everythingTier1 = GpUpgrades.costForEverythingTier(UpgradeCategory.PROFIT, 1)
        val koboldLairTier1 = GpUpgrades.costForLairTier("kobold_warren", UpgradeCategory.PROFIT, 1)

        assertTrue(everythingTier1 > koboldLairTier1)
    }
}
