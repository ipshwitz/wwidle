package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineCapUpgradeTest {

    @Test
    fun `a fresh save's default cap points at the first tier`() {
        val state = GameState()

        assertEquals(4.0, state.offlineCapHours, 0.0001)
        assertEquals(OFFLINE_CAP_TIERS[0], state.nextOfflineCapTier())
    }

    @Test
    fun `once the first tier is bought, the next tier is the second one`() {
        val state = GameState(offlineCapHours = OFFLINE_CAP_TIERS[0].hours)

        assertEquals(OFFLINE_CAP_TIERS[1], state.nextOfflineCapTier())
    }

    @Test
    fun `once every tier is bought, there is no next tier`() {
        val state = GameState(offlineCapHours = OFFLINE_CAP_TIERS.last().hours)

        assertNull(state.nextOfflineCapTier())
    }
}
