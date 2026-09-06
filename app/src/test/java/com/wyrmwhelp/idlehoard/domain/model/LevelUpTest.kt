package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelUpTest {

    @Test
    fun `gemsEarnedFromLevelUp is 0 for a brand-new save`() {
        val state = GameState()

        // A single starting Kobold Warren's net worth is nowhere near the
        // divisor needed to earn even one Gem yet.
        assertEquals(0L, state.gemsEarnedFromLevelUp())
    }

    @Test
    fun `gemsEarnedFromLevelUp follows the square-root net-worth curve`() {
        val oneGem = GameState(goldPieces = 1_000_000.0, lairs = emptyMap())
        val fourGems = GameState(goldPieces = 16_000_000.0, lairs = emptyMap())

        assertEquals(1L, oneGem.gemsEarnedFromLevelUp())
        assertEquals(4L, fourGems.gemsEarnedFromLevelUp())
    }

    @Test
    fun `gemsEarnedFromLevelUp floors instead of rounding`() {
        // Net worth just short of the next whole Gem's threshold.
        val state = GameState(goldPieces = 3_999_999.0, lairs = emptyMap())

        assertEquals(1L, state.gemsEarnedFromLevelUp())
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
