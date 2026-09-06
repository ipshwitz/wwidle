package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UpgradeTiersTest {

    private val phases = UpgradePhases(beginningTiers = 5, midTiers = 5, endTiers = 5)

    @Test
    fun `totalTiers sums all three phases`() {
        assertEquals(15, phases.totalTiers)
    }

    @Test
    fun `phaseOfTier reports the right phase at every boundary`() {
        assertEquals(1, phases.phaseOfTier(1))
        assertEquals(1, phases.phaseOfTier(5))
        assertEquals(2, phases.phaseOfTier(6))
        assertEquals(2, phases.phaseOfTier(10))
        assertEquals(3, phases.phaseOfTier(11))
        assertEquals(3, phases.phaseOfTier(15))
    }

    @Test
    fun `positionWithinPhase is 0-indexed from the start of each phase`() {
        assertEquals(0, phases.positionWithinPhase(1))
        assertEquals(4, phases.positionWithinPhase(5))
        assertEquals(0, phases.positionWithinPhase(6))
        assertEquals(4, phases.positionWithinPhase(10))
        assertEquals(0, phases.positionWithinPhase(11))
        assertEquals(4, phases.positionWithinPhase(15))
    }

    @Test
    fun `upgradeTierCost grows smoothly within a phase`() {
        val tier1Cost = upgradeTierCost(1, phases, baseCost = 100.0, costGrowthRate = 1.25, phaseJumpMultiplier = 8.0)
        val tier2Cost = upgradeTierCost(2, phases, baseCost = 100.0, costGrowthRate = 1.25, phaseJumpMultiplier = 8.0)

        assertEquals(100.0, tier1Cost, 0.0001)
        assertEquals(125.0, tier2Cost, 0.0001)
    }

    @Test
    fun `upgradeTierCost jumps by phaseJumpMultiplier on top of the ongoing compounding curve`() {
        val lastTierOfPhase1 = upgradeTierCost(5, phases, baseCost = 100.0, costGrowthRate = 1.25, phaseJumpMultiplier = 8.0)
        val firstTierOfPhase2 = upgradeTierCost(6, phases, baseCost = 100.0, costGrowthRate = 1.25, phaseJumpMultiplier = 8.0)
        val firstTierOfPhase3 = upgradeTierCost(11, phases, baseCost = 100.0, costGrowthRate = 1.25, phaseJumpMultiplier = 8.0)

        // Crossing a phase boundary multiplies by costGrowthRate (one more
        // compounding step, same as any other tier-to-tier step) *and* an
        // extra phaseJumpMultiplier on top — the curve never resets, it
        // just gets an additional kick exactly at the boundary.
        assertEquals(lastTierOfPhase1 * 1.25 * 8.0, firstTierOfPhase2, 0.0001)
        // tier 6 -> tier 11 is 5 tiers apart (5 more compounding steps) plus one more phase jump.
        assertEquals(firstTierOfPhase2 * Math.pow(1.25, 5.0) * 8.0, firstTierOfPhase3, 0.0001)
        assertEquals(true, firstTierOfPhase2 > lastTierOfPhase1 * 5) // comfortably more than a smooth step alone would give
    }

    @Test
    fun `upgradeTotalPercent accumulates additively within one phase`() {
        // 3 tiers into phase 1 at 2 percent each = 6 percent, no phase 2/3 contribution yet.
        val total = upgradeTotalPercent(3, phases, percentPerTierPhase1 = 2.0, percentPerTierPhase2 = 4.0, percentPerTierPhase3 = 8.0)

        assertEquals(6.0, total, 0.0001)
    }

    @Test
    fun `upgradeTotalPercent uses a higher per-tier rate once tiers spill into later phases`() {
        // 5 tiers of phase 1 (2%) + 2 tiers of phase 2 (4%) = 10 + 8 = 18.
        val total = upgradeTotalPercent(7, phases, percentPerTierPhase1 = 2.0, percentPerTierPhase2 = 4.0, percentPerTierPhase3 = 8.0)

        assertEquals(18.0, total, 0.0001)
    }

    @Test
    fun `upgradeTotalPercent at a maxed line sums every phase`() {
        // 5*2 + 5*4 + 5*8 = 10 + 20 + 40 = 70.
        val total = upgradeTotalPercent(15, phases, percentPerTierPhase1 = 2.0, percentPerTierPhase2 = 4.0, percentPerTierPhase3 = 8.0)

        assertEquals(70.0, total, 0.0001)
    }

    @Test
    fun `upgradeTotalPercent is 0 at level 0`() {
        val total = upgradeTotalPercent(0, phases, percentPerTierPhase1 = 2.0, percentPerTierPhase2 = 4.0, percentPerTierPhase3 = 8.0)

        assertEquals(0.0, total, 0.0001)
    }
}
