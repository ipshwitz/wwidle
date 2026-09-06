package com.wyrmwhelp.idlehoard.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoostsTest {

    @Test
    fun `costForPermanentBoostPurchase grows by the tier's own growth rate per repeat purchase`() {
        val tier = PERMANENT_SPEED_TIERS[0] // 2x, basePp 5.0, growth 1.6
        assertEquals(5.0, costForPermanentBoostPurchase(tier, currentLevel = 0), 0.0001)
        assertEquals(8.0, costForPermanentBoostPurchase(tier, currentLevel = 1), 0.0001)
        assertEquals(12.8, costForPermanentBoostPurchase(tier, currentLevel = 2), 0.0001)
    }

    @Test
    fun `permanentSpeedMultiplier is 1x with nothing bought`() {
        assertEquals(1.0, GameState().permanentSpeedMultiplier(), 0.0001)
    }

    @Test
    fun `permanentSpeedMultiplier compounds a single tier bought repeatedly`() {
        // Buying the 5x Speed tier three times contributes 5^3 = 125x from
        // that tier alone (the explicit "3 5x speeds stacking" design).
        val state = GameState().withPermanentBoostLevel(PERMANENT_SPEED_TIERS[1], 3)
        assertEquals(125.0, state.permanentSpeedMultiplier(), 0.0001)
    }

    @Test
    fun `permanentSpeedMultiplier multiplies every tier together`() {
        val state = GameState()
            .withPermanentBoostLevel(PERMANENT_SPEED_TIERS[0], 1) // 2x
            .withPermanentBoostLevel(PERMANENT_SPEED_TIERS[1], 1) // 5x
            .withPermanentBoostLevel(PERMANENT_SPEED_TIERS[2], 1) // 10x
        assertEquals(2.0 * 5.0 * 10.0, state.permanentSpeedMultiplier(), 0.0001)
    }

    @Test
    fun `permanentProfitMultiplier compounds the same way as speed`() {
        val state = GameState().withPermanentBoostLevel(PERMANENT_PROFIT_TIERS[2], 2) // 5x bought twice
        assertEquals(25.0, state.permanentProfitMultiplier(), 0.0001)
    }

    @Test
    fun `permanentGemPercentMultiplier compounds the same way as speed and profit`() {
        val state = GameState().withPermanentBoostLevel(PERMANENT_GEM_TIERS[1], 2) // 2x bought twice
        assertEquals(4.0, state.permanentGemPercentMultiplier(), 0.0001)
    }

    @Test
    fun `permanentBoostLevel and withPermanentBoostLevel round-trip for every known tier`() {
        for (tier in ALL_PERMANENT_BOOST_TIERS) {
            val state = GameState().withPermanentBoostLevel(tier, 4)
            assertEquals(4, state.permanentBoostLevel(tier))
        }
    }

    @Test
    fun `multiplierFor combines every not-yet-expired boost in a category, ignoring other categories`() {
        val now = Instant.now()
        val boosts = listOf(
            ActiveTemporaryBoost(TemporaryBoostCategory.SPEED, 50.0, now.plusSeconds(60)),
            ActiveTemporaryBoost(TemporaryBoostCategory.SPEED, 100.0, now.plusSeconds(60)),
            ActiveTemporaryBoost(TemporaryBoostCategory.PROFIT, 15.0, now.plusSeconds(60)),
        )

        assertEquals(5_000.0, boosts.multiplierFor(TemporaryBoostCategory.SPEED, now), 0.0001)
        assertEquals(15.0, boosts.multiplierFor(TemporaryBoostCategory.PROFIT, now), 0.0001)
    }

    @Test
    fun `multiplierFor excludes an expired boost`() {
        val now = Instant.now()
        val boosts = listOf(ActiveTemporaryBoost(TemporaryBoostCategory.SPEED, 50.0, now.minusSeconds(1)))

        assertEquals(1.0, boosts.multiplierFor(TemporaryBoostCategory.SPEED, now), 0.0001)
    }

    @Test
    fun `platinumSpeedMultiplier combines permanent tiers with active temporary boosts`() {
        val now = Instant.now()
        val state = GameState(
            permanentSpeedBoost2xLevel = 1,
            activeTemporaryBoosts = listOf(ActiveTemporaryBoost(TemporaryBoostCategory.SPEED, 50.0, now.plusSeconds(60))),
        )

        assertEquals(100.0, state.platinumSpeedMultiplier(now), 0.0001)
    }

    @Test
    fun `platinumProfitMultiplier combines permanent tiers with active temporary boosts`() {
        val now = Instant.now()
        val state = GameState(
            permanentProfitBoost2xLevel = 1,
            activeTemporaryBoosts = listOf(ActiveTemporaryBoost(TemporaryBoostCategory.PROFIT, 15.0, now.plusSeconds(60))),
        )

        assertEquals(30.0, state.platinumProfitMultiplier(now), 0.0001)
    }

    @Test
    fun `activeTemporaryBoostsRemaining drops expired entries and sorts soonest-first`() {
        val now = Instant.now()
        val state = GameState(
            activeTemporaryBoosts = listOf(
                ActiveTemporaryBoost(TemporaryBoostCategory.SPEED, 50.0, now.plusSeconds(120)),
                ActiveTemporaryBoost(TemporaryBoostCategory.PROFIT, 15.0, now.plusSeconds(30)),
                ActiveTemporaryBoost(TemporaryBoostCategory.SPEED, 100.0, now.minusSeconds(1)),
            ),
        )

        val remaining = state.activeTemporaryBoostsRemaining(now)

        assertEquals(2, remaining.size)
        assertEquals(15.0, remaining[0].first.multiplier, 0.0001)
        assertEquals(50.0, remaining[1].first.multiplier, 0.0001)
    }

    @Test
    fun `time skip options are ordered cheapest first`() {
        assertEquals(6, TIME_SKIP_OPTIONS.size)
        assertEquals(TimeSkipOption(costPp = 2.0, seconds = 300.0), TIME_SKIP_OPTIONS[0])
        assertEquals(TimeSkipOption(costPp = 1_000.0, seconds = 604_800.0), TIME_SKIP_OPTIONS.last())
        assertTrue(TIME_SKIP_OPTIONS.zipWithNext().all { (a, b) -> a.costPp < b.costPp })
    }
}
