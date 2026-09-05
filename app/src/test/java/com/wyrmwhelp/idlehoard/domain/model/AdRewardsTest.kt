package com.wyrmwhelp.idlehoard.domain.model

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRewardsTest {

    @Test
    fun `never watched is immediately available`() {
        val state = GameState(lastPlatinumAdWatchedAt = null)

        assertTrue(state.canWatchPlatinumAd())
        assertEquals(Duration.ZERO, state.platinumAdCooldownRemaining())
    }

    @Test
    fun `just watched is not available and reports the full cooldown remaining`() {
        val now = Instant.now()
        val state = GameState(lastPlatinumAdWatchedAt = now)

        assertFalse(state.canWatchPlatinumAd(now))
        assertEquals(PLATINUM_AD_COOLDOWN, state.platinumAdCooldownRemaining(now))
    }

    @Test
    fun `available again once the cooldown fully elapses`() {
        val watchedAt = Instant.now()
        val exactlyElapsed = watchedAt.plus(PLATINUM_AD_COOLDOWN)

        assertTrue(GameState(lastPlatinumAdWatchedAt = watchedAt).canWatchPlatinumAd(exactlyElapsed))
    }

    @Test
    fun `still on cooldown one minute before it elapses`() {
        val watchedAt = Instant.now()
        val almostElapsed = watchedAt.plus(PLATINUM_AD_COOLDOWN).minusSeconds(60)
        val state = GameState(lastPlatinumAdWatchedAt = watchedAt)

        assertFalse(state.canWatchPlatinumAd(almostElapsed))
        assertEquals(Duration.ofMinutes(1), state.platinumAdCooldownRemaining(almostElapsed))
    }
}
