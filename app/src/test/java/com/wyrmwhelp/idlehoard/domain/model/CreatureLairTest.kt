package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CreatureLairTest {

    private val lair = CreatureLair(
        id = "test_lair",
        name = "Test Lair",
        monster = "Test Monster",
        challengeRating = "1",
        flavorText = "",
        tier = 0,
        baseCostGp = 10.0,
        costGrowthRate = 1.1,
        baseIncomeGp = 1.0,
        baseProductionSeconds = 1.0,
        stewardCostGp = 100.0,
    )

    @Test
    fun `costForUnits of 1 matches costForNextUnit`() {
        for (owned in listOf(0, 1, 5, 20)) {
            assertEquals(lair.costForNextUnit(owned), lair.costForUnits(owned, 1), 0.0001)
        }
    }

    @Test
    fun `costForUnits of 0 is free`() {
        assertEquals(0.0, lair.costForUnits(0, 0), 0.0001)
    }

    @Test
    fun `costForUnits matches the sum of costForNextUnit called in a loop`() {
        val owned = 7
        val quantity = 25
        var expected = 0.0
        for (i in 0 until quantity) {
            expected += lair.costForNextUnit(owned + i)
        }

        assertEquals(expected, lair.costForUnits(owned, quantity), expected * 0.0001)
    }

    @Test
    fun `maxAffordableUnits is 0 when even one more unit is unaffordable`() {
        val nextCost = lair.costForNextUnit(0)
        assertEquals(0, lair.maxAffordableUnits(0, nextCost - 0.01))
    }

    @Test
    fun `maxAffordableUnits matches a purchase-simulating loop`() {
        val owned = 3
        val gold = 5_000.0

        var simulated = 0
        var spent = 0.0
        while (spent + lair.costForNextUnit(owned + simulated) <= gold) {
            spent += lair.costForNextUnit(owned + simulated)
            simulated++
        }

        assertEquals(simulated, lair.maxAffordableUnits(owned, gold))
    }

    @Test
    fun `maxAffordableUnits never overspends and cannot afford one more`() {
        val owned = 10
        val gold = 123_456.0

        val n = lair.maxAffordableUnits(owned, gold)

        assertEquals(true, lair.costForUnits(owned, n) <= gold)
        assertEquals(true, lair.costForUnits(owned, n + 1) > gold)
    }

    @Test
    fun `maxAffordableUnits handles exactly affording the max`() {
        val owned = 2
        val quantity = 12
        val exactGold = lair.costForUnits(owned, quantity)

        assertEquals(quantity, lair.maxAffordableUnits(owned, exactGold))
    }

    @Test
    fun `individualSpeedMilestoneMultiplier is 1x below the first threshold`() {
        for (owned in listOf(0, 1, 10, 24)) {
            assertEquals(1.0, lair.individualSpeedMilestoneMultiplier(owned), 0.0001)
        }
    }

    @Test
    fun `individualSpeedMilestoneMultiplier doubles at each of the first six rungs, compounding`() {
        assertEquals(2.0, lair.individualSpeedMilestoneMultiplier(25), 0.0001)
        assertEquals(4.0, lair.individualSpeedMilestoneMultiplier(50), 0.0001)
        assertEquals(8.0, lair.individualSpeedMilestoneMultiplier(100), 0.0001)
        assertEquals(16.0, lair.individualSpeedMilestoneMultiplier(200), 0.0001)
        assertEquals(32.0, lair.individualSpeedMilestoneMultiplier(300), 0.0001)
        assertEquals(64.0, lair.individualSpeedMilestoneMultiplier(400), 0.0001)
    }

    @Test
    fun `individualSpeedMilestoneMultiplier stops compounding once the Speed rungs are exhausted`() {
        // 500 and up are Income rungs (see Milestone.kt), so the Speed
        // multiplier caps at the 400 rung's 64x and doesn't grow further.
        assertEquals(64.0, lair.individualSpeedMilestoneMultiplier(500), 0.0001)
        assertEquals(64.0, lair.individualSpeedMilestoneMultiplier(10_000), 0.0001)
    }

    @Test
    fun `individualIncomeMilestoneMultiplier is 1x below the first Income threshold`() {
        for (owned in listOf(0, 1, 400, 499)) {
            assertEquals(1.0, lair.individualIncomeMilestoneMultiplier(owned), 0.0001)
        }
    }

    @Test
    fun `individualIncomeMilestoneMultiplier compounds across the Income rungs only`() {
        assertEquals(4.0, lair.individualIncomeMilestoneMultiplier(500), 0.0001)
        assertEquals(4.0 * 5.0, lair.individualIncomeMilestoneMultiplier(1_000), 0.0001)
        assertEquals(4.0 * 5.0 * 6.0, lair.individualIncomeMilestoneMultiplier(5_000), 0.0001)
        assertEquals(4.0 * 5.0 * 6.0 * 7.0, lair.individualIncomeMilestoneMultiplier(10_000), 0.0001)
    }

    @Test
    fun `incomePerCycle applies both the individual and global Income multiplier`() {
        val unitsOwned = 500
        val globalIncomeMultiplier = 3.0

        val income = lair.incomePerCycle(unitsOwned, globalIncomeMultiplier)

        assertEquals(lair.baseIncomeGp * unitsOwned * 4.0 * globalIncomeMultiplier, income, 0.0001)
    }

    @Test
    fun `incomePerCycle ignores Speed-rung ownership entirely`() {
        // 25 owned crosses a Speed rung, not an Income one, so incomePerCycle
        // should see no milestone bonus at all here.
        assertEquals(lair.baseIncomeGp * 25, lair.incomePerCycle(25), 0.0001)
    }

    @Test
    fun `incomePerCycle defaults to no global bonus`() {
        assertEquals(lair.baseIncomeGp, lair.incomePerCycle(1), 0.0001)
    }

    @Test
    fun `incomePerCycle applies the profit boost multiplier on top of the others`() {
        val unitsOwned = 500
        val globalIncomeMultiplier = 3.0
        val profitBoostMultiplier = 1.5

        val income = lair.incomePerCycle(unitsOwned, globalIncomeMultiplier, profitBoostMultiplier)

        assertEquals(
            lair.baseIncomeGp * unitsOwned * 4.0 * globalIncomeMultiplier * profitBoostMultiplier,
            income,
            0.0001,
        )
    }

    @Test
    fun `incomePerCycle applies the gem bonus multiplier on top of the others`() {
        val unitsOwned = 500
        val globalIncomeMultiplier = 3.0
        val profitBoostMultiplier = 1.5
        val gemBonusMultiplier = 1.2

        val income = lair.incomePerCycle(unitsOwned, globalIncomeMultiplier, profitBoostMultiplier, gemBonusMultiplier)

        assertEquals(
            lair.baseIncomeGp * unitsOwned * 4.0 * globalIncomeMultiplier * profitBoostMultiplier * gemBonusMultiplier,
            income,
            0.0001,
        )
    }

    @Test
    fun `incomePerCycle applies the GP upgrade profit multiplier on top of the others`() {
        val unitsOwned = 500
        val globalIncomeMultiplier = 3.0
        val profitBoostMultiplier = 1.5
        val gemBonusMultiplier = 1.2
        val upgradeProfitMultiplier = 1.3

        val income = lair.incomePerCycle(
            unitsOwned, globalIncomeMultiplier, profitBoostMultiplier, gemBonusMultiplier, upgradeProfitMultiplier,
        )

        assertEquals(
            lair.baseIncomeGp * unitsOwned * 4.0 * globalIncomeMultiplier * profitBoostMultiplier * gemBonusMultiplier * upgradeProfitMultiplier,
            income,
            0.0001,
        )
    }

    @Test
    fun `effectiveProductionSeconds defaults to the base cycle time unchanged`() {
        assertEquals(lair.baseProductionSeconds, lair.effectiveProductionSeconds(), 0.0001)
    }

    @Test
    fun `effectiveProductionSeconds shrinks the cycle time as the speed boost multiplier grows`() {
        assertEquals(
            lair.baseProductionSeconds / 1.5,
            lair.effectiveProductionSeconds(speedBoostMultiplier = 1.5),
            0.0001,
        )
    }

    @Test
    fun `effectiveProductionSeconds also shrinks from this lair's own Speed milestone rungs`() {
        // 25 owned crosses a Speed rung (2x) — should shrink cycle time just
        // like the account-wide speed boost does.
        assertEquals(
            lair.baseProductionSeconds / 2.0,
            lair.effectiveProductionSeconds(unitsOwned = 25),
            0.0001,
        )
    }

    @Test
    fun `effectiveProductionSeconds ignores Income-rung ownership entirely`() {
        // Crossing from 400 to 500 owned reaches an Income rung, not a
        // Speed one, so cycle time shouldn't shrink any further than it
        // already had at 400 (the last Speed rung).
        assertEquals(
            lair.effectiveProductionSeconds(unitsOwned = 400),
            lair.effectiveProductionSeconds(unitsOwned = 500),
            0.0001,
        )
    }

    @Test
    fun `effectiveProductionSeconds compounds the global Speed milestone multiplier too`() {
        assertEquals(
            lair.baseProductionSeconds / (2.0 * 3.0),
            lair.effectiveProductionSeconds(unitsOwned = 25, globalSpeedMilestoneMultiplier = 3.0),
            0.0001,
        )
    }

    @Test
    fun `effectiveProductionSeconds also divides by the GP upgrade speed multiplier`() {
        assertEquals(
            lair.baseProductionSeconds / (2.0 * 3.0 * 1.4),
            lair.effectiveProductionSeconds(
                unitsOwned = 25,
                globalSpeedMilestoneMultiplier = 3.0,
                upgradeSpeedMultiplier = 1.4,
            ),
            0.0001,
        )
    }
}
