package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelUpTest {

    @Test
    fun `gemsEarnedFromLevelUp is 0 for a brand-new save`() {
        val state = GameState()

        assertEquals(0L, state.gemsEarnedFromLevelUp())
    }

    @Test
    fun `gemsEarnedFromLevelUp follows AdCap's Angel formula, 150 times sqrt of lifetime earnings over 10^15`() {
        // 150 * sqrt(1e15 / 1e15) = 150.
        val state150 = GameState(lifetimeGoldEarned = 1_000_000_000_000_000.0)
        assertEquals(150L, state150.gemsEarnedFromLevelUp())

        // 150 * sqrt(4e15 / 1e15) = 150 * 2 = 300.
        val state300 = GameState(lifetimeGoldEarned = 4_000_000_000_000_000.0)
        assertEquals(300L, state300.gemsEarnedFromLevelUp())
    }

    @Test
    fun `gemsEarnedFromLevelUp floors instead of rounding`() {
        // 150 * sqrt(0.99e15 / 1e15) ~= 149.25, should floor to 149, not 150.
        val state = GameState(lifetimeGoldEarned = 990_000_000_000_000.0)

        assertEquals(149L, state.gemsEarnedFromLevelUp())
    }

    @Test
    fun `gemsEarnedFromLevelUp only grants the gap above gems already earned`() {
        // The target for this much lifetime earning is 150 gems; having
        // already earned 100 of them (from a prior Level Up) should only
        // grant the remaining 50, not another fresh 150.
        val state = GameState(lifetimeGoldEarned = 1_000_000_000_000_000.0, totalGemsEarned = 100L)

        assertEquals(50L, state.gemsEarnedFromLevelUp())
    }

    @Test
    fun `gemsEarnedFromLevelUp is 0 (not negative) once totalGemsEarned has caught up to the target`() {
        // This is the actual "reach further before you can Level Up again"
        // gate: leveling up without any new lifetime earnings in between
        // grants nothing the second time.
        val state = GameState(lifetimeGoldEarned = 1_000_000_000_000_000.0, totalGemsEarned = 150L)

        assertEquals(0L, state.gemsEarnedFromLevelUp())
    }

    @Test
    fun `gemIncomeMultiplier is 1x with no gems`() {
        assertEquals(1.0, gemIncomeMultiplier(0), 0.0001)
    }

    @Test
    fun `gemIncomeMultiplier adds a flat 2 percent per gem, additively not compounding`() {
        assertEquals(1.02, gemIncomeMultiplier(1), 0.0001)
        assertEquals(1.20, gemIncomeMultiplier(10), 0.0001)
        assertEquals(3.00, gemIncomeMultiplier(100), 0.0001)
    }
}
