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

    @Test
    fun `never watched speed boost ad has all four slots available`() {
        val state = GameState(speedBoostAdWatchTimestamps = emptyList())

        assertEquals(SPEED_BOOST_AD_MAX_SLOTS, state.availableSpeedBoostAdSlots())
        assertTrue(state.canWatchSpeedBoostAd())
        assertEquals(Duration.ZERO, state.speedBoostAdCooldownRemaining())
    }

    @Test
    fun `each watch uses up one of the four independent slots`() {
        val now = Instant.now()
        val state = GameState(speedBoostAdWatchTimestamps = listOf(now, now, now))

        assertEquals(1, state.availableSpeedBoostAdSlots(now))
        assertTrue(state.canWatchSpeedBoostAd(now))
    }

    @Test
    fun `all four slots busy blocks watching and reports the soonest slot's remaining cooldown`() {
        val now = Instant.now()
        val watches = listOf(
            now.minusSeconds(3600), // 1h ago -> 23h left
            now.minusSeconds(7200), // 2h ago -> 22h left (soonest to free up)
            now,
            now,
        )
        val state = GameState(speedBoostAdWatchTimestamps = watches)

        assertEquals(0, state.availableSpeedBoostAdSlots(now))
        assertFalse(state.canWatchSpeedBoostAd(now))
        assertEquals(Duration.ofHours(22), state.speedBoostAdCooldownRemaining(now))
    }

    @Test
    fun `a slot frees up again once its own 24-hour cooldown fully elapses`() {
        val watchedAt = Instant.now()
        val state = GameState(speedBoostAdWatchTimestamps = List(SPEED_BOOST_AD_MAX_SLOTS) { watchedAt })
        val exactlyElapsed = watchedAt.plus(SPEED_BOOST_AD_COOLDOWN)

        assertTrue(state.canWatchSpeedBoostAd(exactlyElapsed))
        assertEquals(SPEED_BOOST_AD_MAX_SLOTS, state.availableSpeedBoostAdSlots(exactlyElapsed))
    }

    @Test
    fun `an old watch outside the cooldown window doesn't count against available slots`() {
        val now = Instant.now()
        val longAgo = now.minus(SPEED_BOOST_AD_COOLDOWN).minusSeconds(60)
        val state = GameState(speedBoostAdWatchTimestamps = listOf(longAgo, longAgo, longAgo, longAgo))

        assertEquals(SPEED_BOOST_AD_MAX_SLOTS, state.availableSpeedBoostAdSlots(now))
    }

    @Test
    fun `never watched income boost ad has all four slots available`() {
        val state = GameState(incomeBoostAdWatchTimestamps = emptyList())

        assertEquals(INCOME_BOOST_AD_MAX_SLOTS, state.availableIncomeBoostAdSlots())
        assertTrue(state.canWatchIncomeBoostAd())
        assertEquals(Duration.ZERO, state.incomeBoostAdCooldownRemaining())
    }

    @Test
    fun `income boost slots are tracked independently of speed boost slots`() {
        val now = Instant.now()
        val state = GameState(
            speedBoostAdWatchTimestamps = List(SPEED_BOOST_AD_MAX_SLOTS) { now },
            incomeBoostAdWatchTimestamps = emptyList(),
        )

        assertFalse(state.canWatchSpeedBoostAd(now))
        assertTrue(state.canWatchIncomeBoostAd(now))
        assertEquals(INCOME_BOOST_AD_MAX_SLOTS, state.availableIncomeBoostAdSlots(now))
    }

    @Test
    fun `all four income boost slots busy blocks watching and reports the soonest slot's remaining cooldown`() {
        val now = Instant.now()
        val watches = listOf(
            now.minusSeconds(3600),
            now.minusSeconds(7200),
            now,
            now,
        )
        val state = GameState(incomeBoostAdWatchTimestamps = watches)

        assertEquals(0, state.availableIncomeBoostAdSlots(now))
        assertFalse(state.canWatchIncomeBoostAd(now))
        assertEquals(Duration.ofHours(22), state.incomeBoostAdCooldownRemaining(now))
    }

    @Test
    fun `an income boost slot frees up again once its own 24-hour cooldown fully elapses`() {
        val watchedAt = Instant.now()
        val state = GameState(incomeBoostAdWatchTimestamps = List(INCOME_BOOST_AD_MAX_SLOTS) { watchedAt })
        val exactlyElapsed = watchedAt.plus(INCOME_BOOST_AD_COOLDOWN)

        assertTrue(state.canWatchIncomeBoostAd(exactlyElapsed))
        assertEquals(INCOME_BOOST_AD_MAX_SLOTS, state.availableIncomeBoostAdSlots(exactlyElapsed))
    }
}
