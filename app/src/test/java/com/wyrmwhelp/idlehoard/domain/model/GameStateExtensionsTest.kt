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
}
