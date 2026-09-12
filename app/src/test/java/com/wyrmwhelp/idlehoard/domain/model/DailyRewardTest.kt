package com.wyrmwhelp.idlehoard.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRewardTest {

    private val today = LocalDate.of(2026, 1, 15)

    @Test
    fun `never claimed means a claim is available`() {
        val state = GameState()
        assertTrue(state.canClaimDailyReward(today))
    }

    @Test
    fun `already claimed today means no claim is available`() {
        val state = GameState(dailyRewardLastClaimedEpochDay = today.toEpochDay())
        assertFalse(state.canClaimDailyReward(today))
    }

    @Test
    fun `claimed yesterday still leaves today claimable`() {
        val state = GameState(dailyRewardLastClaimedEpochDay = today.minusDays(1).toEpochDay())
        assertTrue(state.canClaimDailyReward(today))
    }

    @Test
    fun `the very first claim ever is day 1`() {
        val state = GameState()
        assertEquals(1, state.nextDailyRewardDay(today))
    }

    @Test
    fun `claiming the day after yesterday's claim continues the streak`() {
        val state = GameState(dailyRewardStreakDay = 6, dailyRewardLastClaimedEpochDay = today.minusDays(1).toEpochDay())
        assertEquals(7, state.nextDailyRewardDay(today))
    }

    @Test
    fun `missing a calendar day resets the streak to day 1`() {
        val state = GameState(dailyRewardStreakDay = 6, dailyRewardLastClaimedEpochDay = today.minusDays(2).toEpochDay())
        assertEquals(1, state.nextDailyRewardDay(today))
    }

    @Test
    fun `claiming twice in one day (a stale read) still reports day 1 rather than double-advancing`() {
        val state = GameState(dailyRewardStreakDay = 6, dailyRewardLastClaimedEpochDay = today.toEpochDay())
        assertEquals(1, state.nextDailyRewardDay(today))
    }

    @Test
    fun `completing the full 28-day cycle wraps back to day 1, not day 29`() {
        val state = GameState(dailyRewardStreakDay = DAILY_REWARD_CYCLE_DAYS, dailyRewardLastClaimedEpochDay = today.minusDays(1).toEpochDay())
        assertEquals(1, state.nextDailyRewardDay(today))
    }

    @Test
    fun `an ordinary day pays only a climbing percentage of current gold`() {
        val payout = computeDailyRewardPayout(day = 3, currentGold = 1_000.0, currentGems = 0L)
        assertEquals(3, payout.day)
        assertEquals(15.0, payout.goldAwarded, 0.0001) // day 3 * 0.5% = 1.5% of 1,000
        assertEquals(0L, payout.gemsAwarded)
        assertEquals(0.0, payout.platinumAwarded, 0.0)
    }

    @Test
    fun `a gem day pays gold plus the flat minimum when gems held are low`() {
        val payout = computeDailyRewardPayout(day = 7, currentGold = 1_000.0, currentGems = 500L)
        assertEquals(35.0, payout.goldAwarded, 0.0001) // day 7 * 0.5% = 3.5% of 1,000
        assertEquals(DAILY_REWARD_GEM_MINIMUM, payout.gemsAwarded) // 5% of 500 (25) is below the 100 floor
        assertEquals(0.0, payout.platinumAwarded, 0.0)
    }

    @Test
    fun `a gem day pays the percentage once it beats the flat minimum`() {
        val payout = computeDailyRewardPayout(day = 14, currentGold = 0.0, currentGems = 10_000L)
        assertEquals(500L, payout.gemsAwarded) // 5% of 10,000 beats the 100 floor
    }

    @Test
    fun `day 28 pays only platinum, nothing else, even though it's also a multiple of 7`() {
        val payout = computeDailyRewardPayout(day = DAILY_REWARD_CYCLE_DAYS, currentGold = 50_000.0, currentGems = 999_999L)
        assertEquals(0.0, payout.goldAwarded, 0.0)
        assertEquals(0L, payout.gemsAwarded)
        assertEquals(DAILY_REWARD_FINAL_PLATINUM, payout.platinumAwarded, 0.0)
    }

    @Test
    fun `days that are not 7, 14, 21, or 28 never grant gems`() {
        val payout = computeDailyRewardPayout(day = 8, currentGold = 0.0, currentGems = 1_000_000L)
        assertEquals(0L, payout.gemsAwarded)
    }

    @Test
    fun `scaledBy doubles every awarded amount`() {
        val payout = computeDailyRewardPayout(day = 7, currentGold = 1_000.0, currentGems = 10_000L).scaledBy(2.0)
        assertEquals(70.0, payout.goldAwarded, 0.0001) // 2x the plain 35.0
        assertEquals(1_000L, payout.gemsAwarded) // 2x the plain 500
    }

    @Test
    fun `scaledBy doubles the day 28 finale's platinum too`() {
        val payout = computeDailyRewardPayout(day = DAILY_REWARD_CYCLE_DAYS, currentGold = 0.0, currentGems = 0L).scaledBy(2.0)
        assertEquals(DAILY_REWARD_FINAL_PLATINUM * 2, payout.platinumAwarded, 0.0)
    }

    @Test
    fun `the auto-popup should show when a claim is available and it hasn't shown yet today`() {
        val state = GameState()
        assertTrue(state.shouldAutoShowDailyRewardPopup(today))
    }

    @Test
    fun `the auto-popup should not show again the same day once it already has, even if unclaimed`() {
        val state = GameState(dailyRewardAutoPopupShownEpochDay = today.toEpochDay())
        assertFalse(state.shouldAutoShowDailyRewardPopup(today))
    }

    @Test
    fun `the auto-popup should show again on a new day even if it showed yesterday`() {
        val state = GameState(dailyRewardAutoPopupShownEpochDay = today.minusDays(1).toEpochDay())
        assertTrue(state.shouldAutoShowDailyRewardPopup(today))
    }

    @Test
    fun `the auto-popup never shows once today's reward is already claimed, regardless of the popup flag`() {
        val state = GameState(dailyRewardLastClaimedEpochDay = today.toEpochDay())
        assertFalse(state.shouldAutoShowDailyRewardPopup(today))
    }
}
