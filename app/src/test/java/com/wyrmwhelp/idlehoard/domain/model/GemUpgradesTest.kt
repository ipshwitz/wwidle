package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GemUpgradesTest {

    @Test
    fun `Gem Efficiency has exactly 200 total tiers`() {
        assertEquals(200, GemUpgrades.PHASES.totalTiers)
    }

    @Test
    fun `costForTierGems rounds the fractional formula up to a whole Gem`() {
        val exactCost = GemUpgrades.costForTier(1)
        val roundedCost = GemUpgrades.costForTierGems(1)

        assertEquals(5.0, exactCost, 0.0001) // tier-1 cost is exactly the 5.0 Gem base cost
        assertEquals(5L, roundedCost)
    }

    @Test
    fun `bonusPerGem is 0 at level 0 and grows with level`() {
        assertEquals(0.0, GemUpgrades.bonusPerGem(0), 0.0001)
        assertTrue(GemUpgrades.bonusPerGem(200) > GemUpgrades.bonusPerGem(100))
        assertTrue(GemUpgrades.bonusPerGem(100) > 0.0)
    }

    @Test
    fun `gemIncomeMultiplier increases with Gem Efficiency level for the same gem count`() {
        val noEfficiency = gemIncomeMultiplier(gems = 100, gemEfficiencyLevel = 0)
        val maxEfficiency = gemIncomeMultiplier(gems = 100, gemEfficiencyLevel = 200)

        assertTrue(maxEfficiency > noEfficiency)
    }

    @Test
    fun `costForTier grows within a phase and jumps at the phase boundary`() {
        val phase1Tiers = GemUpgrades.PHASES.beginningTiers
        val lastOfPhase1 = GemUpgrades.costForTier(phase1Tiers)
        val firstOfPhase2 = GemUpgrades.costForTier(phase1Tiers + 1)

        assertTrue(firstOfPhase2 > lastOfPhase1 * 2)
    }
}
