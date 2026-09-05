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

    @Test
    fun `milestonesCrossed reports every individual rung a bulk purchase jumps past`() {
        // "b" stays behind the whole time so it — not "a" — is always the
        // catalog-wide minimum, isolating "a"'s individual rungs from any
        // "Everything" crossing.
        val lairs = listOf(testLair("a"), testLair("b"))
        // Owned 10, buying up to 100 jumps straight past the 25 and 50 rungs too.
        val state = GameState(
            lairs = mapOf(
                "a" to OwnedLair(lairId = "a", count = 100),
                "b" to OwnedLair(lairId = "b", count = 5),
            ),
        )

        val crossed = state.milestonesCrossed(lairId = "a", previousCount = 10, catalog = lairs)

        assertEquals(listOf(25, 50, 100), crossed.map { it.threshold })
        assertEquals(listOf("a", "a", "a"), crossed.map { it.lairName })
        assertEquals(listOf(false, false, false), crossed.map { it.isGlobal })
    }

    @Test
    fun `milestonesCrossed reports nothing when the purchase doesn't reach the next rung`() {
        val lairs = listOf(testLair("a"), testLair("b"))
        val state = GameState(
            lairs = mapOf(
                "a" to OwnedLair(lairId = "a", count = 20),
                "b" to OwnedLair(lairId = "b", count = 5),
            ),
        )

        val crossed = state.milestonesCrossed(lairId = "a", previousCount = 10, catalog = lairs)

        assertEquals(emptyList<Any>(), crossed)
    }

    @Test
    fun `milestonesCrossed adds a global rung once this purchase makes it the new lowest`() {
        val lairs = listOf(testLair("a"), testLair("b"))
        // "b" was already at 25; "a" catching up to 25 makes 25 the new
        // catalog-wide minimum, so the "Everything" 25 rung fires too.
        val state = GameState(
            lairs = mapOf(
                "a" to OwnedLair(lairId = "a", count = 25),
                "b" to OwnedLair(lairId = "b", count = 25),
            ),
        )

        val crossed = state.milestonesCrossed(lairId = "a", previousCount = 10, catalog = lairs)

        assertEquals(2, crossed.size)
        assertEquals("a", crossed[0].lairName)
        assertEquals(25, crossed[0].threshold)
        assertEquals(false, crossed[0].isGlobal)
        assertEquals("Everything", crossed[1].lairName)
        assertEquals(25, crossed[1].threshold)
        assertEquals(true, crossed[1].isGlobal)
    }

    @Test
    fun `milestonesCrossed omits the global rung when another lair is still behind`() {
        val lairs = listOf(testLair("a"), testLair("b"))
        // "b" is still at 10, so the catalog-wide minimum doesn't move even
        // though "a" just reached 25 on its own.
        val state = GameState(
            lairs = mapOf(
                "a" to OwnedLair(lairId = "a", count = 25),
                "b" to OwnedLair(lairId = "b", count = 10),
            ),
        )

        val crossed = state.milestonesCrossed(lairId = "a", previousCount = 10, catalog = lairs)

        assertEquals(listOf(25), crossed.map { it.threshold })
        assertEquals(listOf(false), crossed.map { it.isGlobal })
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
