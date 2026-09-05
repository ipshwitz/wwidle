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
    fun `individualMilestoneMultiplier is 1x below the first threshold`() {
        for (owned in listOf(0, 1, 10, 24)) {
            assertEquals(1.0, lair.individualMilestoneMultiplier(owned), 0.0001)
        }
    }

    @Test
    fun `individualMilestoneMultiplier doubles at each of the first six rungs, compounding`() {
        assertEquals(2.0, lair.individualMilestoneMultiplier(25), 0.0001)
        assertEquals(4.0, lair.individualMilestoneMultiplier(50), 0.0001)
        assertEquals(8.0, lair.individualMilestoneMultiplier(100), 0.0001)
        assertEquals(16.0, lair.individualMilestoneMultiplier(200), 0.0001)
        assertEquals(32.0, lair.individualMilestoneMultiplier(300), 0.0001)
        assertEquals(64.0, lair.individualMilestoneMultiplier(400), 0.0001)
    }

    @Test
    fun `individualMilestoneMultiplier applies the later flat-rate rungs on top`() {
        assertEquals(64.0 * 4.0, lair.individualMilestoneMultiplier(500), 0.0001)
        assertEquals(64.0 * 4.0 * 5.0, lair.individualMilestoneMultiplier(1_000), 0.0001)
        assertEquals(64.0 * 4.0 * 5.0 * 6.0, lair.individualMilestoneMultiplier(5_000), 0.0001)
        assertEquals(64.0 * 4.0 * 5.0 * 6.0 * 7.0, lair.individualMilestoneMultiplier(10_000), 0.0001)
    }

    @Test
    fun `incomePerCycle applies both the individual and global multiplier`() {
        val unitsOwned = 25
        val globalMultiplier = 3.0

        val income = lair.incomePerCycle(unitsOwned, globalMultiplier)

        assertEquals(lair.baseIncomeGp * unitsOwned * 2.0 * globalMultiplier, income, 0.0001)
    }

    @Test
    fun `incomePerCycle defaults to no global bonus`() {
        assertEquals(lair.baseIncomeGp, lair.incomePerCycle(1), 0.0001)
    }
}
