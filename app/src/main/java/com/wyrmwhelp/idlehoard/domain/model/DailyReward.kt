package com.wyrmwhelp.idlehoard.domain.model

import java.time.LocalDate

/**
 * The Daily Reward system — a real-world login-streak mechanic, distinct
 * from every other reward in this game in that it's keyed on actual
 * calendar days rather than playtime or purchases. Built from an explicit
 * design discussion: a 28-day cycle (four even weeks — deliberately not 30,
 * so every "every 7 days" milestone lines up exactly and the finale doesn't
 * need to special-case a 30/31-day month), plain calendar-day comparisons
 * (device local date, not a rolling 24h/48h window — "claiming at 11:58pm
 * then again at 12:01am isn't such a bonus that it's a game changer," per
 * explicit answer, so the simpler always-wins here), and a hard reset to
 * Day 1 on any missed calendar day (no grace window — a real push
 * notification/reminder system is planned before this ships for real, so a
 * player won't be missing a day silently the way they could today).
 *
 * Three reward types, all stacking independently on whichever days they
 * apply — a day is never "only" one type, except the finale:
 * - **Every day 1-27** (day 28 is its own special case — see below): a
 *   percentage of the player's *current* Gold Pieces balance —
 *   deliberately the live [GameState.goldPieces] figure the header already
 *   shows, not [GameState.lifetimeGoldEarned], since "a % of what I
 *   actually have" is what reads as intuitive, and the mild incentive to
 *   hoard gold right before claiming is harmless flavor, not something
 *   exploitable. The percentage climbs by [DAILY_REWARD_GOLD_PERCENT_PER_DAY]
 *   (0.5%) per day of the streak and never resets mid-cycle — day 7's Gems
 *   bonus doesn't interrupt the climb, it just happens on top of that
 *   day's own Gold cut.
 * - **Every [DAILY_REWARD_GEM_DAYS] (7, 14, 21 — day 28 is deliberately
 *   excluded, see below)**: a Gems bonus, `max([DAILY_REWARD_GEM_MINIMUM],
 *   [DAILY_REWARD_GEM_PERCENT_OF_HELD] of Gems currently held)` — the flat
 *   minimum matters most early (a fresh save's first Level Up grants as
 *   few as 50-150 Gems, where another flat 100 is a huge relative bump),
 *   while the percentage takes over once Gems climb into the
 *   thousands-plus range a well-progressed save's Level Ups can reach,
 *   where a flat 100 would be meaningless.
 * - **Day [DAILY_REWARD_CYCLE_DAYS] (28) — the cycle finale**: **just**
 *   [DAILY_REWARD_FINAL_PLATINUM] (20) Platinum Pieces, deliberately
 *   nothing else that day (no Gold%, no Gems, even though 28 is also a
 *   multiple of 7) — a clean, singular "you made it the whole cycle"
 *   payout, then the very next claim starts a fresh cycle back at Day 1.
 *
 * First-pass placeholder numbers throughout, not playtested, same as
 * everywhere else in the economy.
 */
const val DAILY_REWARD_CYCLE_DAYS = 28
const val DAILY_REWARD_GOLD_PERCENT_PER_DAY = 0.5
val DAILY_REWARD_GEM_DAYS: Set<Int> = setOf(7, 14, 21)
const val DAILY_REWARD_GEM_MINIMUM = 100L
const val DAILY_REWARD_GEM_PERCENT_OF_HELD = 0.05
const val DAILY_REWARD_FINAL_PLATINUM = 20.0

/** One claim's actual payout — see this file's class doc for how each field is computed. */
data class DailyRewardPayout(
    val day: Int,
    val goldAwarded: Double,
    val gemsAwarded: Long,
    val platinumAwarded: Double,
) {
    /** Scales every awarded amount by [multiplier] — used by `GameEngine.claimDailyReward`'s "Watch Ad to Double" path. */
    fun scaledBy(multiplier: Double): DailyRewardPayout = copy(
        goldAwarded = goldAwarded * multiplier,
        gemsAwarded = (gemsAwarded * multiplier).toLong(),
        platinumAwarded = platinumAwarded * multiplier,
    )
}

/**
 * True once a new calendar day has turned over since
 * [GameState.dailyRewardLastClaimedEpochDay] — the sole gate on whether a
 * claim is available right now, regardless of whether the streak itself
 * would continue or reset.
 */
fun GameState.canClaimDailyReward(today: LocalDate = LocalDate.now()): Boolean =
    dailyRewardLastClaimedEpochDay != today.toEpochDay()

/**
 * True the first time the main screen loads on a new calendar day with a
 * claim available — the sole gate on the *automatic* pop-up
 * (`GameScreen`'s `LaunchedEffect`), separate from [canClaimDailyReward]
 * itself. Per explicit design: the auto-popup should interrupt at most
 * once a day, even across multiple app opens that same day — dismissing
 * or ignoring it is a real "you might miss out" moment, not something
 * that keeps re-asking. [GameState.dailyRewardAutoPopupShownEpochDay] is
 * stamped the instant the popup actually shows
 * (`GameEngine.markDailyRewardPopupShown`), independent of whether the
 * player then claims, watches an ad, or dismisses it — manually opening
 * [DailyRewardButton]/Settings afterward is unaffected either way, since
 * those never consult this function.
 */
fun GameState.shouldAutoShowDailyRewardPopup(today: LocalDate = LocalDate.now()): Boolean =
    canClaimDailyReward(today) && dailyRewardAutoPopupShownEpochDay != today.toEpochDay()

/**
 * The day-in-cycle (1-[DAILY_REWARD_CYCLE_DAYS]) that claiming right now
 * would grant — continues yesterday's streak by one, or restarts at Day 1
 * if this is the very first claim ever, a full cycle just completed
 * yesterday, or a calendar day was missed entirely (the gap since the last
 * claim is anything other than exactly one day).
 */
fun GameState.nextDailyRewardDay(today: LocalDate = LocalDate.now()): Int {
    val lastEpochDay = dailyRewardLastClaimedEpochDay ?: return 1
    val gapDays = today.toEpochDay() - lastEpochDay
    return if (gapDays == 1L && dailyRewardStreakDay < DAILY_REWARD_CYCLE_DAYS) dailyRewardStreakDay + 1 else 1
}

/** What claiming right now would actually pay out — see [nextDailyRewardDay]/[computeDailyRewardPayout]. */
fun GameState.previewDailyRewardPayout(today: LocalDate = LocalDate.now()): DailyRewardPayout =
    computeDailyRewardPayout(nextDailyRewardDay(today), goldPieces, gems)

/**
 * Pure payout math for [day] of the cycle, given the player's current Gold/
 * Gems balances — see this file's class doc for the reward shape. Kept
 * separate from [previewDailyRewardPayout] so `GameEngine.claimDailyReward`
 * can compute the *actual* grant off live state atomically, inside the
 * same `_state.update` the preview would otherwise have gone stale
 * relative to.
 */
fun computeDailyRewardPayout(day: Int, currentGold: Double, currentGems: Long): DailyRewardPayout {
    if (day == DAILY_REWARD_CYCLE_DAYS) {
        return DailyRewardPayout(day = day, goldAwarded = 0.0, gemsAwarded = 0L, platinumAwarded = DAILY_REWARD_FINAL_PLATINUM)
    }
    val goldAwarded = currentGold * (day * DAILY_REWARD_GOLD_PERCENT_PER_DAY / 100.0)
    val gemsAwarded = if (day in DAILY_REWARD_GEM_DAYS) {
        maxOf(DAILY_REWARD_GEM_MINIMUM, (currentGems * DAILY_REWARD_GEM_PERCENT_OF_HELD).toLong())
    } else {
        0L
    }
    return DailyRewardPayout(day = day, goldAwarded = goldAwarded, gemsAwarded = gemsAwarded, platinumAwarded = 0.0)
}
