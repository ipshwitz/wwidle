package com.wyrmwhelp.idlehoard.domain.model

import java.time.Duration
import java.time.Instant
import kotlin.random.Random

/**
 * The Featured Lair mini-event: purely at random (no scaling by playtime or
 * progress — an explicit design choice), one currently-owned lair "goes
 * Featured" for a short manual-tap challenge, flashing on the main screen.
 * Every tap landed while it's Featured earns [FEATURED_LAIR_TAP_PROFIT_MULTIPLIER]
 * times that lair's own per-cycle profit, credited immediately (see
 * `GameEngine.tapFeaturedLair`) — works identically whether or not that
 * lair has a Steward, since it's a manual bonus layered on top of whatever
 * the Steward is already doing, not a substitute for it. Landing
 * [FEATURED_LAIR_TAPS_REQUIRED] taps before the window closes additionally
 * grants [FEATURED_LAIR_BONUS_PRODUCTION_SECONDS] of that lair's own
 * production, instantly, on top of everything already tapped out. Missing
 * the goal isn't a failure state — every tap's gold is already banked, so
 * there's simply no bonus, never a loss for trying. First-pass placeholder
 * numbers, not playtested, same as everywhere else in the economy.
 *
 * **Tuned in v0.49.1** — the original 20 taps/12s (per explicit feedback,
 * "the tap was too easy") averaged under 2 taps/sec, comfortably slow for
 * a real thumb. Per explicit follow-up ("a little longer... time to fill
 * up the status bar... 50 taps in 20 seconds"), both numbers moved up
 * together: the window is longer (so the progress bar reads as a real
 * fill rather than an instant blip) but the required pace is genuinely
 * higher — 50 taps in 20s averages 2.5 taps/sec, up from 1.67.
 */
const val FEATURED_LAIR_TAPS_REQUIRED = 50
const val FEATURED_LAIR_WINDOW_SECONDS = 20L
const val FEATURED_LAIR_TAP_PROFIT_MULTIPLIER = 3.0
const val FEATURED_LAIR_BONUS_PRODUCTION_SECONDS = 1800.0 // 30 minutes

/**
 * How long, at minimum/maximum, between one Featured Lair event ending
 * (success or expiry) and the next one becoming eligible to start —
 * deliberately a flat random range rather than scaled by playtime,
 * progress, or a "player level" (this game has no such stat outside
 * [GameState.totalLevelUps], which isn't what this ties to). First-pass
 * placeholder bounds, not playtested.
 */
private const val FEATURED_LAIR_MIN_INTERVAL_SECONDS = 180L // 3 minutes
private const val FEATURED_LAIR_MAX_INTERVAL_SECONDS = 480L // 8 minutes

/** A fresh random wait until the next Featured Lair event is eligible to start — see the interval bounds' doc above. */
fun randomFeaturedLairInterval(random: Random = Random.Default): Duration =
    Duration.ofSeconds(random.nextLong(FEATURED_LAIR_MIN_INTERVAL_SECONDS, FEATURED_LAIR_MAX_INTERVAL_SECONDS + 1))

/**
 * Randomly picks which owned lair goes Featured next — any lair with at
 * least one unit owned is eligible, Steward-managed or not (this is
 * meant to give a brand-new, Steward-less player something to do too, not
 * just reward lairs already left idle). [excludeLairId] (normally
 * [GameState.lastFeaturedLairId]) is avoided when there's a genuine
 * alternative, so the same lair doesn't go Featured twice in a row — but
 * with only one lair owned, that's the only real choice, so the exclusion
 * is dropped rather than returning null and stalling the event entirely.
 * Returns null only when nothing is owned yet at all.
 */
fun pickFeaturedLairId(ownedLairs: Map<String, OwnedLair>, excludeLairId: String?, random: Random = Random.Default): String? {
    val eligible = ownedLairs.values.filter { it.count > 0 }.map { it.lairId }
    if (eligible.isEmpty()) return null
    val candidates = if (eligible.size > 1) eligible.filterNot { it == excludeLairId } else eligible
    val pool = candidates.ifEmpty { eligible }
    return pool[random.nextInt(pool.size)]
}

/** The result of one [GameEngine.tapFeaturedLair] call, distinguishing a real tap from a stale one and flagging the exact tap that clears [FEATURED_LAIR_TAPS_REQUIRED] so the UI can fire its own success celebration. */
enum class FeaturedLairTapOutcome {
    /** [lairId] passed to `tapFeaturedLair` isn't the one currently Featured (or the window already closed) — a stale tap, no gold granted. */
    NOT_ACTIVE,
    /** A real tap during an active window, gold credited, goal not yet reached. */
    TAPPED,
    /** The tap that reached [FEATURED_LAIR_TAPS_REQUIRED] — gold credited, bonus granted, event ended early. */
    COMPLETED,
}
