package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameStateExtensionsTest {

    @Test
    fun `merge returns whichever side is non-null when the other is missing`() {
        val state = GameState(goldPieces = 100.0)

        assertEquals(state, mergeGameStates(local = state, cloud = null))
        assertEquals(state, mergeGameStates(local = null, cloud = state))
        assertNull(mergeGameStates(local = null, cloud = null))
    }

    @Test
    fun `merge picks the higher net-worth side when prestige counts match`() {
        val poorer = GameState(goldPieces = 100.0)
        val richer = GameState(goldPieces = 100_000.0)

        assertEquals(richer, mergeGameStates(local = poorer, cloud = richer))
        assertEquals(richer, mergeGameStates(local = richer, cloud = poorer))
    }

    @Test
    fun `merge picks the higher totalMolts side even if its net worth is lower`() {
        val freshPrestige = GameState(goldPieces = 10.0, totalMolts = 2)
        val richButUnprestiged = GameState(goldPieces = 1_000_000.0, totalMolts = 0)

        assertEquals(freshPrestige, mergeGameStates(local = richButUnprestiged, cloud = freshPrestige))
        assertEquals(freshPrestige, mergeGameStates(local = freshPrestige, cloud = richButUnprestiged))
    }

    @Test
    fun `net worth counts owned lairs as what they cost to claim from scratch`() {
        val lairId = "kobold_warren"
        val owned = OwnedLair(lairId = lairId, count = 3)
        val withLairs = GameState(goldPieces = 0.0, lairs = mapOf(lairId to owned))
        val goldOnly = GameState(goldPieces = withLairs.estimatedNetWorth(), lairs = emptyMap())

        // Same net worth either way — neither should "win" over the other.
        assertEquals(goldOnly.estimatedNetWorth(), withLairs.estimatedNetWorth(), 0.0001)
    }

    @Test
    fun `globalMilestoneMultiplier is held back by whichever lair owns the least`() {
        val lairs = listOf(testLair("a"), testLair("b"))
        val state = GameState(
            lairs = mapOf(
                "a" to OwnedLair(lairId = "a", count = 100),
                "b" to OwnedLair(lairId = "b", count = 10),
            ),
        )

        // "b" hasn't reached the first (25) rung yet, so no bonus applies
        // no matter how far ahead "a" is.
        assertEquals(1.0, state.globalMilestoneMultiplier(lairs), 0.0001)
    }

    @Test
    fun `globalMilestoneMultiplier advances once every lair has caught up`() {
        val lairs = listOf(testLair("a"), testLair("b"))
        val state = GameState(
            lairs = mapOf(
                "a" to OwnedLair(lairId = "a", count = 100),
                "b" to OwnedLair(lairId = "b", count = 25),
            ),
        )

        assertEquals(2.0, state.globalMilestoneMultiplier(lairs), 0.0001)
    }

    private fun testLair(id: String) = CreatureLair(
        id = id,
        name = id,
        monster = "Test",
        challengeRating = "1",
        flavorText = "",
        tier = 0,
        baseCostGp = 10.0,
        costGrowthRate = 1.1,
        baseIncomeGp = 1.0,
        baseProductionSeconds = 1.0,
        stewardCostGp = 100.0,
    )
}
