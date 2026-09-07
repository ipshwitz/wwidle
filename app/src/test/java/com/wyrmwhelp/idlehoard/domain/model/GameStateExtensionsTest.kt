package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `merge picks the higher totalLevelUps side even if its net worth is lower`() {
        val freshPrestige = GameState(goldPieces = 10.0, totalLevelUps = 2)
        val richButUnprestiged = GameState(goldPieces = 1_000_000.0, totalLevelUps = 0)

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
    fun `globalSpeedMilestoneMultiplier is held back by whichever lair owns the least`() {
        val lairs = listOf(testLair("a"), testLair("b"))
        val state = GameState(
            lairs = mapOf(
                "a" to OwnedLair(lairId = "a", count = 100),
                "b" to OwnedLair(lairId = "b", count = 10),
            ),
        )

        // "b" hasn't reached the first (25) rung yet, so no bonus applies
        // no matter how far ahead "a" is.
        assertEquals(1.0, state.globalSpeedMilestoneMultiplier(lairs), 0.0001)
    }

    @Test
    fun `globalSpeedMilestoneMultiplier advances once every lair has caught up`() {
        val lairs = listOf(testLair("a"), testLair("b"))
        val state = GameState(
            lairs = mapOf(
                "a" to OwnedLair(lairId = "a", count = 100),
                "b" to OwnedLair(lairId = "b", count = 25),
            ),
        )

        assertEquals(2.0, state.globalSpeedMilestoneMultiplier(lairs), 0.0001)
    }

    @Test
    fun `globalIncomeMilestoneMultiplier only advances on Income-type rungs, held back the same way`() {
        val lairs = listOf(testLair("a"), testLair("b"))
        val heldBack = GameState(
            lairs = mapOf(
                "a" to OwnedLair(lairId = "a", count = 1_000),
                "b" to OwnedLair(lairId = "b", count = 100),
            ),
        )
        // "b" is at 100 (a Speed rung), well short of the first Income rung
        // (500), so the global Income multiplier is still 1x.
        assertEquals(1.0, heldBack.globalIncomeMilestoneMultiplier(lairs), 0.0001)

        val caughtUp = GameState(
            lairs = mapOf(
                "a" to OwnedLair(lairId = "a", count = 1_000),
                "b" to OwnedLair(lairId = "b", count = 500),
            ),
        )
        assertEquals(4.0, caughtUp.globalIncomeMilestoneMultiplier(lairs), 0.0001)
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

    @Test
    fun `an owned, unaffordable, Steward-less lair is not an opportunity yet`() {
        // kobold_warren's real stewardCostGp is 1,000 gp — availability, not
        // just visibility, is what should gate the badge.
        val state = GameState(goldPieces = 1.0, lairs = mapOf("kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1)))

        assertEquals(emptySet<String>(), state.unseenStewardOpportunities())
        assertFalse(state.hasUnseenStewardOpportunity())
    }

    @Test
    fun `an owned, affordable, Steward-less lair is an unseen opportunity`() {
        val state = GameState(goldPieces = 1_000.0, lairs = mapOf("kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1)))

        assertEquals(setOf("kobold_warren"), state.unseenStewardOpportunities())
        assertTrue(state.hasUnseenStewardOpportunity())
    }

    @Test
    fun `a hired Steward is never an opportunity, seen or not`() {
        val state = GameState(
            goldPieces = 1_000_000.0,
            lairs = mapOf("kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1, hasSteward = true)),
        )

        assertEquals(emptySet<String>(), state.unseenStewardOpportunities())
        assertFalse(state.hasUnseenStewardOpportunity())
    }

    @Test
    fun `an unowned lair is never an opportunity`() {
        val state = GameState(goldPieces = 1_000_000.0, lairs = emptyMap())

        assertFalse(state.hasUnseenStewardOpportunity())
    }

    @Test
    fun `marking opportunities seen clears the badge for exactly the currently-affordable lairs`() {
        val state = GameState(
            goldPieces = 20_000.0, // covers kobold_warren (1,000) and giant_rat_burrow (15,000)
            lairs = mapOf(
                "kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1),
                "giant_rat_burrow" to OwnedLair(lairId = "giant_rat_burrow", count = 1),
            ),
        )

        val seen = state.withStewardOpportunitiesSeen()

        assertFalse(seen.hasUnseenStewardOpportunity())
        assertEquals(setOf("kobold_warren", "giant_rat_burrow"), seen.seenStewardOpportunities)
    }

    @Test
    fun `newly affording another lair's Steward after seeing the first surfaces only the new one`() {
        val afterSeeingFirst = GameState(
            goldPieces = 1_000.0,
            lairs = mapOf(
                "kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1),
                "giant_rat_burrow" to OwnedLair(lairId = "giant_rat_burrow", count = 1),
            ),
        ).withStewardOpportunitiesSeen()
        // Not yet affordable at 1,000 gp — confirms it wasn't marked seen above.
        assertEquals(emptySet<String>(), afterSeeingFirst.unseenStewardOpportunities().minus("kobold_warren"))

        val afterEarningMore = afterSeeingFirst.copy(goldPieces = 20_000.0)

        assertEquals(setOf("giant_rat_burrow"), afterEarningMore.unseenStewardOpportunities())
    }

    @Test
    fun `hiring a Steward for an already-seen lair leaves it seen`() {
        val seen = GameState(
            goldPieces = 1_000.0,
            lairs = mapOf("kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1)),
        ).withStewardOpportunitiesSeen()

        val hired = seen.copy(
            lairs = mapOf("kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1, hasSteward = true)),
        )

        assertFalse(hired.hasUnseenStewardOpportunity())
    }

    @Test
    fun `no upgrade line is an opportunity when nothing is affordable`() {
        val state = GameState(goldPieces = 0.0, gems = 0L, lairs = mapOf("kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1)))

        assertFalse(state.hasUnseenUpgradeOpportunity())
    }

    @Test
    fun `an affordable lair upgrade tier is an unseen opportunity`() {
        val state = GameState(goldPieces = 1_000_000_000.0)

        val unseen = state.unseenUpgradeOpportunities()

        assertTrue("kobold_warren:profit" in unseen)
        assertTrue("kobold_warren:speed" in unseen)
        assertTrue(state.hasUnseenUpgradeOpportunity())
    }

    @Test
    fun `an affordable Gem Efficiency tier is an unseen opportunity`() {
        val state = GameState(gems = 1_000L)

        assertEquals(setOf("gem_efficiency"), state.unseenUpgradeOpportunities())
    }

    @Test
    fun `marking upgrade opportunities seen clears exactly the currently-affordable lines`() {
        val state = GameState(goldPieces = 1_000_000_000.0, gems = 1_000L)
        val unseenBefore = state.unseenUpgradeOpportunities()
        assertTrue(unseenBefore.isNotEmpty())

        val seen = state.withUpgradeOpportunitiesSeen()

        assertFalse(seen.hasUnseenUpgradeOpportunity())
        assertEquals(unseenBefore, seen.seenUpgradeOpportunities)
    }

    @Test
    fun `buying the seen tier of an upgrade line still leaves it seen even though a costlier tier is now unaffordable`() {
        val state = GameState(goldPieces = 1_000_000_000.0).withUpgradeOpportunitiesSeen()

        val afterSpending = state.copy(
            goldPieces = 0.0,
            lairs = state.lairs + ("kobold_warren" to state.ownedLair("kobold_warren").copy(profitUpgradeLevel = 1)),
        )

        assertFalse("kobold_warren:profit" in afterSpending.unseenUpgradeOpportunities())
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
