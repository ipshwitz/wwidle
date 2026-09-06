package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `gemsEarnedFromLevelUp does not subtract gems already held — the batch replaces, not accumulates`() {
        // Gems are temporary (see LevelUp.kt's class doc): the batch a
        // Level Up would grant depends only on lifetimeGoldEarned, not on
        // how many Gems are currently held or were ever earned before.
        val holdingLots = GameState(lifetimeGoldEarned = 1_000_000_000_000_000.0, gems = 10_000L, totalLevelUps = 1)
        val holdingNone = GameState(lifetimeGoldEarned = 1_000_000_000_000_000.0, gems = 0L, totalLevelUps = 1)

        assertEquals(150L, holdingLots.gemsEarnedFromLevelUp())
        assertEquals(150L, holdingNone.gemsEarnedFromLevelUp())
    }

    @Test
    fun `the very first Level Up is blocked until lifetime earnings alone are worth at least 50 gems`() {
        // 150 * sqrt(4.444e12 / 1e15) ~= 10 gems worth of lifetime earnings —
        // comfortably below the 50-gem minimum, so a completely fresh save
        // (totalLevelUps defaults to 0) can't Level Up yet.
        val notEnoughYet = GameState(lifetimeGoldEarned = 4_444_444_444_444.0)

        assertEquals(0L, notEnoughYet.gemsEarnedFromLevelUp())
    }

    @Test
    fun `recurring Level Ups use a smaller 25-gem minimum instead of the first Level Up's 50`() {
        // 150 * sqrt(2.5e13 / 1e15) ~= 23.7 gems -> floors to 23, below the
        // 25-gem recurring minimum but would also fail the first-time 50 bar.
        val tooSmall = GameState(lifetimeGoldEarned = 25_000_000_000_000.0, totalLevelUps = 1)
        assertEquals(0L, tooSmall.gemsEarnedFromLevelUp())

        // Just above the exact L for a target of 25 (1e15 * (25/150)^2 =
        // 1e15/36 ~= 27,777,777,777,777.78) — nudged up by 1 so floating-point
        // rounding can't accidentally land the computed value a hair under
        // 25.0 and floor down to 24. Would have failed the first-time 50 bar.
        val exactlyEnough = GameState(lifetimeGoldEarned = 27_777_777_777_778.0, totalLevelUps = 1)
        assertEquals(25L, exactlyEnough.gemsEarnedFromLevelUp())
    }

    @Test
    fun `a Level Up can never grant fewer gems than a previous one, since lifetime earnings only grow`() {
        val early = GameState(lifetimeGoldEarned = 1_000_000_000_000_000.0, totalLevelUps = 1)
        val later = early.copy(lifetimeGoldEarned = early.lifetimeGoldEarned * 4)

        assertTrue(later.gemsEarnedFromLevelUp() >= early.gemsEarnedFromLevelUp())
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
