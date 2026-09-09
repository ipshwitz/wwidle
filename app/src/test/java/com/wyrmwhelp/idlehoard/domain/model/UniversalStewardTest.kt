package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalStewardTest {

    @Test
    fun `not earned below the threshold`() {
        val state = GameState(totalAdsWatched = UNIVERSAL_STEWARD_AD_THRESHOLD - 1)

        assertFalse(state.hasUniversalSteward())
    }

    @Test
    fun `earned exactly at the threshold and beyond`() {
        assertTrue(GameState(totalAdsWatched = UNIVERSAL_STEWARD_AD_THRESHOLD).hasUniversalSteward())
        assertTrue(GameState(totalAdsWatched = UNIVERSAL_STEWARD_AD_THRESHOLD + 50).hasUniversalSteward())
    }

    @Test
    fun `a lair with a real Steward is managed regardless of the Universal Steward`() {
        val owned = OwnedLair(lairId = "kobold_warren", count = 1, hasSteward = true)
        val state = GameState(totalAdsWatched = 0)

        assertTrue(state.isLairManaged(owned))
    }

    @Test
    fun `an owned lair with no real Steward is managed once the Universal Steward is earned`() {
        val owned = OwnedLair(lairId = "kobold_warren", count = 1, hasSteward = false)
        val state = GameState(totalAdsWatched = UNIVERSAL_STEWARD_AD_THRESHOLD)

        assertTrue(state.isLairManaged(owned))
    }

    @Test
    fun `an owned lair with no real Steward is not managed before the Universal Steward is earned`() {
        val owned = OwnedLair(lairId = "kobold_warren", count = 1, hasSteward = false)
        val state = GameState(totalAdsWatched = 0)

        assertFalse(state.isLairManaged(owned))
    }

    @Test
    fun `the Universal Steward does nothing for a lair with zero units owned`() {
        val owned = OwnedLair(lairId = "kobold_warren", count = 0, hasSteward = false)
        val state = GameState(totalAdsWatched = UNIVERSAL_STEWARD_AD_THRESHOLD)

        assertFalse(state.isLairManaged(owned))
    }

    @Test
    fun `progress is clamped at the threshold rather than climbing past 100`() {
        assertEquals(37, GameState(totalAdsWatched = 37).adsWatchedTowardUniversalSteward())
        assertEquals(UNIVERSAL_STEWARD_AD_THRESHOLD, GameState(totalAdsWatched = UNIVERSAL_STEWARD_AD_THRESHOLD).adsWatchedTowardUniversalSteward())
        assertEquals(
            UNIVERSAL_STEWARD_AD_THRESHOLD,
            GameState(totalAdsWatched = UNIVERSAL_STEWARD_AD_THRESHOLD + 250).adsWatchedTowardUniversalSteward(),
        )
    }
}
