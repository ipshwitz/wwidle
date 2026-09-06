package com.wyrmwhelp.idlehoard.domain.engine

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.domain.model.PLATINUM_AD_REWARD_PP
import com.wyrmwhelp.idlehoard.domain.model.GemUpgrades
import com.wyrmwhelp.idlehoard.domain.model.GpUpgrades
import com.wyrmwhelp.idlehoard.domain.model.UpgradeCategory
import com.wyrmwhelp.idlehoard.domain.model.ActiveTemporaryBoost
import com.wyrmwhelp.idlehoard.domain.model.PermanentBoostTier
import com.wyrmwhelp.idlehoard.domain.model.TemporaryBoostCategory
import com.wyrmwhelp.idlehoard.domain.model.TemporaryBoostOption
import com.wyrmwhelp.idlehoard.domain.model.canWatchPlatinumAd
import com.wyrmwhelp.idlehoard.domain.model.costForPermanentBoostPurchase
import com.wyrmwhelp.idlehoard.domain.model.gemIncomeMultiplier
import com.wyrmwhelp.idlehoard.domain.model.gemsEarnedFromLevelUp
import com.wyrmwhelp.idlehoard.domain.model.globalIncomeMilestoneMultiplier
import com.wyrmwhelp.idlehoard.domain.model.globalSpeedMilestoneMultiplier
import com.wyrmwhelp.idlehoard.domain.model.multiplierFor
import com.wyrmwhelp.idlehoard.domain.model.permanentBoostLevel
import com.wyrmwhelp.idlehoard.domain.model.permanentGemPercentMultiplier
import com.wyrmwhelp.idlehoard.domain.model.permanentProfitMultiplier
import com.wyrmwhelp.idlehoard.domain.model.permanentSpeedMultiplier
import com.wyrmwhelp.idlehoard.domain.model.platinumProfitMultiplier
import com.wyrmwhelp.idlehoard.domain.model.platinumSpeedMultiplier
import com.wyrmwhelp.idlehoard.domain.model.withPermanentBoostLevel
import com.wyrmwhelp.idlehoard.domain.model.TimeSkipOption
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Result of settling offline production on load, for showing a "while you were
 * away" summary to the player.
 */
data class OfflineEarnings(
    val elapsedSeconds: Double,
    val cappedSeconds: Double,
    val goldEarned: Double,
)

/**
 * Core domain engine: owns the authoritative [GameState], runs the passive
 * production tick loop, and applies player actions (claiming lairs, hiring
 * Stewards, starting lairs' production cycles, Leveling Up). App-scoped singleton —
 * outlives any single screen/ViewModel, since production must keep running
 * across navigation. No persistence wiring yet; [loadState] and
 * [applyOfflineEarnings] are the seams the Room/Supabase repositories will
 * call into once they exist.
 */
@Singleton
class GameEngine @Inject constructor() {

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state

    /**
     * Each owned lair's current cycle-fill fraction (0f–1f), recomputed once
     * per [tick] and published separately from [state] — see [computeLairProgress]
     * for why this needs to exist as its own map rather than each `LairCard`
     * deriving it from raw [OwnedLair.cycleProgressSeconds]/`effectiveProductionSeconds`
     * itself (that per-composable derivation is what used to visibly stutter
     * at high Speed multipliers).
     */
    private val _lairProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val lairProgress: StateFlow<Map<String, Float>> = _lairProgress

    /** Replaces the current state wholesale, e.g. right after a save is loaded. */
    fun loadState(newState: GameState) {
        _state.value = newState
    }

    /** Starts the passive-production tick loop. Idempotent — safe to call repeatedly. */
    fun start() {
        if (tickJob?.isActive == true) return
        tickJob = engineScope.launch {
            var lastTick = System.nanoTime()
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                val now = System.nanoTime()
                val deltaSeconds = (now - lastTick) / 1_000_000_000.0
                lastTick = now
                tick(deltaSeconds)
            }
        }
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
    }

    /** Advances all owned lairs' production by [deltaSeconds] of wall-clock time. */
    fun tick(deltaSeconds: Double, now: Instant = Instant.now()) {
        if (deltaSeconds <= 0.0) return
        _state.update { advance(it, deltaSeconds, now) }
        _lairProgress.value = computeLairProgress(_state.value, now)
    }

    /**
     * The fill fraction (0f–1f) each owned lair's `LairCard` should animate
     * toward right now. Idle unmanaged lairs (owned but not `isLoading`) are
     * pinned at 0f. Once a lair's own [CreatureLair.effectiveProductionSeconds]
     * drops under [PROGRESS_SOLID_THRESHOLD_SECONDS] — meaning it can complete
     * one or more full cycles inside a single [TICK_INTERVAL_MS] tick — the
     * raw `cycleProgressSeconds / productionSeconds` ratio stops meaning
     * anything: it's a modulo remainder being sampled at a coarser rate than
     * it changes, which is exactly what read as the fill bar "bouncing up and
     * down at random spots" instead of animating smoothly. Below that
     * threshold this reports a flat 1f instead — a heavily Speed-boosted lair
     * reads as a continuously solid bar, which is the truthful picture (it's
     * completing cycles far faster than a human can watch one fill) rather
     * than an aliased, jittery one.
     */
    private fun computeLairProgress(state: GameState, now: Instant): Map<String, Float> {
        if (state.lairs.isEmpty()) return emptyMap()
        val globalSpeedMultiplier = state.globalSpeedMilestoneMultiplier(CreatureLairCatalog.lairs)
        val speedMultiplier = state.platinumSpeedMultiplier(now)
        val everythingSpeedUpgradeMultiplier = GpUpgrades.everythingSpeedMultiplier(state.everythingSpeedUpgradeLevel)
        val progress = mutableMapOf<String, Float>()
        for ((lairId, owned) in state.lairs) {
            if (owned.count <= 0) continue
            if (!owned.hasSteward && !owned.isLoading) {
                progress[lairId] = 0f
                continue
            }
            val lair = CreatureLairCatalog.get(lairId)
            val upgradeSpeedMultiplier = GpUpgrades.lairSpeedMultiplier(owned.speedUpgradeLevel) * everythingSpeedUpgradeMultiplier
            val productionSeconds = lair.effectiveProductionSeconds(owned.count, speedMultiplier, globalSpeedMultiplier, upgradeSpeedMultiplier)
            progress[lairId] = when {
                productionSeconds < PROGRESS_SOLID_THRESHOLD_SECONDS -> 1f
                productionSeconds <= 0.0 -> 0f
                else -> (owned.cycleProgressSeconds / productionSeconds).toFloat().coerceIn(0f, 1f)
            }
        }
        return progress
    }

    /**
     * Attempts to claim the next unit of [lairId]. Returns true if the Gold Pieces
     * were spent and the unit was claimed, false if the player couldn't afford it.
     */
    fun purchaseLair(lairId: String): Boolean = purchaseLairs(lairId, quantity = 1) > 0

    /**
     * Attempts to claim [quantity] more units of [lairId] at once, atomically —
     * either the full bulk cost (see [CreatureLair.costForUnits]) is affordable
     * and all of them are bought in one state update, or none are (never a
     * partial buy that leaves the player short mid-purchase). Returns the
     * number actually purchased: either [quantity] or 0.
     */
    fun purchaseLairs(lairId: String, quantity: Int): Int {
        if (quantity <= 0) return 0
        val lair = CreatureLairCatalog.get(lairId)
        var purchased = 0
        _state.update { current ->
            val owned = current.ownedLair(lairId)
            val cost = lair.costForUnits(owned.count, quantity)
            if (current.goldPieces < cost) {
                current
            } else {
                purchased = quantity
                current.copy(
                    goldPieces = current.goldPieces - cost,
                    lairs = current.lairs + (lairId to owned.copy(count = owned.count + quantity)),
                )
            }
        }
        return purchased
    }

    /**
     * Hires a Steward for [lairId], who will auto-collect finished production
     * cycles from then on. Returns true if hired, false if already hired, the
     * lair isn't owned yet, or the player can't afford it.
     */
    fun hireSteward(lairId: String): Boolean {
        val lair = CreatureLairCatalog.get(lairId)
        var hired = false
        _state.update { current ->
            val owned = current.ownedLair(lairId)
            if (owned.count <= 0 || owned.hasSteward || current.goldPieces < lair.stewardCostGp) {
                current
            } else {
                hired = true
                current.copy(
                    goldPieces = current.goldPieces - lair.stewardCostGp,
                    lairs = current.lairs + (lairId to owned.copy(hasSteward = true)),
                )
            }
        }
        return hired
    }

    /**
     * Starts [lairId]'s production cycle (the player tapping a lair that's
     * sitting idle) — gold isn't credited here; it's credited automatically
     * once the cycle actually completes (see [advanceLair] and
     * [OwnedLair.isLoading]). Returns true if a cycle was actually started,
     * false if the lair isn't owned, has a Steward (which runs continuously
     * on its own — tapping does nothing), or is already mid-cycle.
     */
    fun startLairLoad(lairId: String): Boolean {
        var started = false
        _state.update { current ->
            val owned = current.ownedLair(lairId)
            if (owned.count <= 0 || owned.hasSteward || owned.isLoading) {
                current
            } else {
                started = true
                current.copy(
                    lairs = current.lairs + (lairId to owned.copy(isLoading = true, cycleProgressSeconds = 0.0)),
                )
            }
        }
        return started
    }

    /**
     * Buys one more copy of the permanent boost tier [tier] with Platinum
     * Pieces (see `domain/model/Boosts.kt`) — a repeatable, stackable
     * purchase, not a leveled-cap one, so this never fails on "already
     * maxed," only on affordability. Returns true if bought, false if the
     * player can't afford the next copy's cost
     * ([costForPermanentBoostPurchase]).
     */
    fun purchasePermanentBoost(tier: PermanentBoostTier): Boolean {
        var bought = false
        _state.update { current ->
            val currentLevel = current.permanentBoostLevel(tier)
            val cost = costForPermanentBoostPurchase(tier, currentLevel)
            if (current.platinumPieces < cost) {
                current
            } else {
                bought = true
                current.withPermanentBoostLevel(tier, currentLevel + 1).copy(
                    platinumPieces = current.platinumPieces - cost,
                )
            }
        }
        return bought
    }

    /**
     * Buys [option] with Platinum Pieces, instantly starting a running
     * temporary boost at [TemporaryBoostOption.multiplier] for
     * [TemporaryBoostOption.durationSeconds] — a fixed price every time,
     * unlike [purchasePermanentBoost]'s escalating cost. Buying another
     * temporary boost of the same category before this one expires stacks
     * multiplicatively with it for their overlap (see
     * [GameState.activeTemporaryBoosts]'s doc) rather than replacing it.
     * Returns true if bought, false if the player can't afford [option].
     */
    fun purchaseTemporaryBoost(option: TemporaryBoostOption, now: Instant = Instant.now()): Boolean {
        var bought = false
        _state.update { current ->
            if (current.platinumPieces < option.costPp) {
                current
            } else {
                bought = true
                current.copy(
                    platinumPieces = current.platinumPieces - option.costPp,
                    activeTemporaryBoosts = current.activeTemporaryBoosts +
                        ActiveTemporaryBoost(option.category, option.multiplier, now.plusSeconds(option.durationSeconds)),
                )
            }
        }
        return bought
    }

    /**
     * Spends Platinum Pieces to instantly grant [TimeSkipOption.seconds] of
     * production from every owned lair — see [grantInstantProduction] for why
     * this doesn't just reuse [advance]. Returns true if bought, false if the
     * player can't afford [option].
     */
    fun purchaseTimeSkip(option: TimeSkipOption, now: Instant = Instant.now()): Boolean {
        var bought = false
        _state.update { current ->
            if (current.platinumPieces < option.costPp) {
                current
            } else {
                bought = true
                grantInstantProduction(
                    current.copy(platinumPieces = current.platinumPieces - option.costPp),
                    option.seconds,
                    now,
                )
            }
        }
        return bought
    }

    /**
     * Buys the next tier of [lairId]'s own Gold Pieces [category] upgrade
     * line (`GpUpgrades.kt`) — one of the 28 per-lair lines, distinct from
     * the two account-wide "Everything" lines (see [purchaseGpEverythingUpgrade]).
     * Returns true if bought, false if [lairId] isn't owned, the line is
     * already at [GpUpgrades.LAIR_LINE_PHASES]'s max tier, or the player
     * can't afford the next tier.
     */
    fun purchaseGpLairUpgrade(lairId: String, category: UpgradeCategory): Boolean {
        var bought = false
        _state.update { current ->
            val owned = current.ownedLair(lairId)
            val currentLevel = if (category == UpgradeCategory.PROFIT) owned.profitUpgradeLevel else owned.speedUpgradeLevel
            val nextTier = currentLevel + 1
            if (owned.count <= 0 || nextTier > GpUpgrades.LAIR_LINE_PHASES.totalTiers) {
                current
            } else {
                val cost = GpUpgrades.costForLairTier(lairId, category, nextTier)
                if (current.goldPieces < cost) {
                    current
                } else {
                    bought = true
                    val updatedOwned = if (category == UpgradeCategory.PROFIT) {
                        owned.copy(profitUpgradeLevel = nextTier)
                    } else {
                        owned.copy(speedUpgradeLevel = nextTier)
                    }
                    current.copy(
                        goldPieces = current.goldPieces - cost,
                        lairs = current.lairs + (lairId to updatedOwned),
                    )
                }
            }
        }
        return bought
    }

    /**
     * Buys the next tier of the account-wide "Everything" [category]
     * upgrade line (`GpUpgrades.kt`) — affects every owned lair at once,
     * unlike [purchaseGpLairUpgrade]'s per-lair lines. Returns true if
     * bought, false if already at max tier or unaffordable.
     */
    fun purchaseGpEverythingUpgrade(category: UpgradeCategory): Boolean {
        var bought = false
        _state.update { current ->
            val currentLevel = if (category == UpgradeCategory.PROFIT) current.everythingProfitUpgradeLevel else current.everythingSpeedUpgradeLevel
            val phases = if (category == UpgradeCategory.PROFIT) GpUpgrades.EVERYTHING_PROFIT_PHASES else GpUpgrades.EVERYTHING_SPEED_PHASES
            val nextTier = currentLevel + 1
            if (nextTier > phases.totalTiers) {
                current
            } else {
                val cost = GpUpgrades.costForEverythingTier(category, nextTier)
                if (current.goldPieces < cost) {
                    current
                } else {
                    bought = true
                    if (category == UpgradeCategory.PROFIT) {
                        current.copy(goldPieces = current.goldPieces - cost, everythingProfitUpgradeLevel = nextTier)
                    } else {
                        current.copy(goldPieces = current.goldPieces - cost, everythingSpeedUpgradeLevel = nextTier)
                    }
                }
            }
        }
        return bought
    }

    /**
     * Buys the next tier of the Gem-spent "Gem Efficiency" upgrade
     * (`GemUpgrades.kt`), raising the per-Gem income bonus
     * [gemIncomeMultiplier] grants. Returns true if bought, false if
     * already at max tier or the player can't afford it in Gems.
     */
    fun purchaseGemEfficiencyUpgrade(): Boolean {
        var bought = false
        _state.update { current ->
            val nextTier = current.gemEfficiencyLevel + 1
            if (nextTier > GemUpgrades.PHASES.totalTiers) {
                current
            } else {
                val cost = GemUpgrades.costForTierGems(nextTier)
                if (current.gems < cost) {
                    current
                } else {
                    bought = true
                    current.copy(gems = current.gems - cost, gemEfficiencyLevel = nextTier)
                }
            }
        }
        return bought
    }

    /**
     * Resets the current run for a fresh Gem batch (see
     * `domain/model/LevelUp.kt`'s `gemsEarnedFromLevelUp`) — Gold Pieces
     * and every owned lair go back to the exact same starting shape a
     * brand-new [GameState] begins with (one Kobold Warren, nothing else),
     * and [GameState.gems] itself is *replaced* by the new batch, not
     * added to (Gems are a temporary head start, not an accumulating
     * currency — see `LevelUp.kt`'s class doc for why). Platinum Pieces,
     * every permanent boost tier bought with it
     * ([GameState.permanentSpeedBoost2xLevel] and its eight siblings) and
     * every currently-running [GameState.activeTemporaryBoosts] instance,
     * [GameState.offlineCapHours], the
     * ad-watch cooldown ([GameState.lastPlatinumAdWatchedAt]), and —
     * critically — [GameState.lifetimeGoldEarned] itself all carry over
     * unchanged; only the gold side of the *current run* (and the old Gem
     * batch) resets. That includes every Gold Pieces upgrade
     * (`GpUpgrades.kt`) — the per-lair ones implicitly, since [GameState.lairs]
     * resets to the starting map, and [GameState.everythingProfitUpgradeLevel]/
     * [GameState.everythingSpeedUpgradeLevel] explicitly, by simply not
     * naming them below (a fresh [GameState]'s own defaults are 0) — and
     * [GameState.gemEfficiencyLevel] (`GemUpgrades.kt`), for the same reason
     * Gems themselves reset. Returns the size of the new Gem batch; if
     * [GameState.lifetimeGoldEarned] hasn't grown enough since the last
     * Level Up to clear the applicable minimum, nothing is reset and this
     * returns 0 (checked and applied atomically inside the same [_state]
     * update, so two rapid calls can't both reset off a stale "earns
     * enough" read).
     */
    fun performLevelUp(): Long {
        var gemsEarned = 0L
        _state.update { current ->
            gemsEarned = current.gemsEarnedFromLevelUp()
            if (gemsEarned <= 0L) {
                current
            } else {
                GameState(
                    platinumPieces = current.platinumPieces,
                    gems = gemsEarned,
                    lifetimeGoldEarned = current.lifetimeGoldEarned,
                    offlineCapHours = current.offlineCapHours,
                    totalLevelUps = current.totalLevelUps + 1,
                    permanentSpeedBoost2xLevel = current.permanentSpeedBoost2xLevel,
                    permanentSpeedBoost5xLevel = current.permanentSpeedBoost5xLevel,
                    permanentSpeedBoost10xLevel = current.permanentSpeedBoost10xLevel,
                    permanentProfitBoost15xLevel = current.permanentProfitBoost15xLevel,
                    permanentProfitBoost2xLevel = current.permanentProfitBoost2xLevel,
                    permanentProfitBoost5xLevel = current.permanentProfitBoost5xLevel,
                    permanentGemBoost15xLevel = current.permanentGemBoost15xLevel,
                    permanentGemBoost2xLevel = current.permanentGemBoost2xLevel,
                    permanentGemBoost5xLevel = current.permanentGemBoost5xLevel,
                    activeTemporaryBoosts = current.activeTemporaryBoosts,
                    lastPlatinumAdWatchedAt = current.lastPlatinumAdWatchedAt,
                )
            }
        }
        _lairProgress.value = computeLairProgress(_state.value, Instant.now())
        return gemsEarned
    }

    /**
     * Settles production that happened while the app was closed, based on the
     * time since [GameState.lastSavedAt], capped at [GameState.offlineCapHours].
     * Uses the same per-lair rules as the live tick loop: Steward-managed lairs
     * keep auto-collecting every completed cycle exactly as if the app had
     * stayed open. Unmanaged lairs only continue if they were already mid-cycle
     * ([OwnedLair.isLoading]) when the app closed — completing that one cycle at
     * most, same as live play; a lair that was sitting idle (not tapped) earns
     * nothing while away, since nothing was running for it to interrupt. Call
     * once on load, before [start].
     */
    fun applyOfflineEarnings(now: Instant = Instant.now()): OfflineEarnings {
        var earnings = OfflineEarnings(0.0, 0.0, 0.0)
        _state.update { current ->
            val elapsedSeconds = Duration.between(current.lastSavedAt, now).seconds
                .toDouble()
                .coerceAtLeast(0.0)
            val cappedSeconds = min(elapsedSeconds, current.offlineCapHours * 3600.0)

            val goldBefore = current.goldPieces
            val settled = advance(current, cappedSeconds, now).copy(lastSavedAt = now)
            earnings = OfflineEarnings(
                elapsedSeconds = elapsedSeconds,
                cappedSeconds = cappedSeconds,
                goldEarned = settled.goldPieces - goldBefore,
            )
            settled
        }
        return earnings
    }

    /**
     * Directly credits [amount] Gold Pieces, bypassing the normal
     * income/milestone pipeline entirely — a flat grant for one-off bonuses
     * like a rewarded ad (see `GameViewModel.watchAdToDoubleOfflineEarnings`),
     * not anything a lair produces. Still counts toward
     * [GameState.lifetimeGoldEarned] — it's earned Gold same as anything a
     * lair produces, just not tied to a specific lair's own cycle.
     */
    fun grantGold(amount: Double) {
        if (amount <= 0.0) return
        _state.update { it.copy(goldPieces = it.goldPieces + amount, lifetimeGoldEarned = it.lifetimeGoldEarned + amount) }
    }

    /**
     * Directly credits [amount] Platinum Pieces from a completed real-money
     * IAP purchase (see `billing/BillingManager.kt`,
     * `GameViewModel`'s collector on `BillingManager.purchaseEvents`) —
     * same flat-grant shape as [grantGold], no separate pipeline to run.
     */
    fun grantPlatinum(amount: Long) {
        if (amount <= 0L) return
        _state.update { it.copy(platinumPieces = it.platinumPieces + amount) }
    }

    /**
     * Grants the Shop's "Watch an Ad" Platinum reward if its 24-hour
     * cooldown has elapsed (see `domain/model/AdRewards.kt`), stamping
     * [GameState.lastPlatinumAdWatchedAt] so the cooldown persists across
     * sessions. Returns true if granted, false if still on cooldown —
     * callers should only reach this after `AdManager.showAd` already
     * confirmed the reward was earned, so false here would mean the two
     * clocks disagreed, not a normal outcome.
     */
    fun grantPlatinumAdReward(now: Instant = Instant.now()): Boolean {
        var granted = false
        _state.update { current ->
            if (!current.canWatchPlatinumAd(now)) {
                current
            } else {
                granted = true
                current.copy(
                    platinumPieces = current.platinumPieces + PLATINUM_AD_REWARD_PP,
                    lastPlatinumAdWatchedAt = now,
                )
            }
        }
        return granted
    }

    /**
     * Pure production step: advances every owned lair and tallies Gold
     * Pieces earned. That earned amount also accumulates into
     * [GameState.lifetimeGoldEarned], which never resets — see
     * `domain/model/LevelUp.kt` for why that's what actually gates Gems.
     */
    private fun advance(state: GameState, deltaSeconds: Double, now: Instant): GameState {
        if (deltaSeconds <= 0.0 || state.lairs.isEmpty()) return state
        // Pruned once here, per tick, so a purchase's expiry doesn't linger
        // in the persisted save forever — see `GameState.activeTemporaryBoosts`.
        val activeBoosts = state.activeTemporaryBoosts.filter { it.expiresAt.isAfter(now) }
        val globalSpeedMultiplier = state.globalSpeedMilestoneMultiplier(CreatureLairCatalog.lairs)
        val globalIncomeMultiplier = state.globalIncomeMilestoneMultiplier(CreatureLairCatalog.lairs)
        val speedMultiplier = state.permanentSpeedMultiplier() * activeBoosts.multiplierFor(TemporaryBoostCategory.SPEED, now)
        val profitMultiplier = state.permanentProfitMultiplier() * activeBoosts.multiplierFor(TemporaryBoostCategory.PROFIT, now)
        val gemMultiplier = gemIncomeMultiplier(state.gems, state.gemEfficiencyLevel, state.permanentGemPercentMultiplier())
        val everythingProfitUpgradeMultiplier = GpUpgrades.everythingProfitMultiplier(state.everythingProfitUpgradeLevel)
        val everythingSpeedUpgradeMultiplier = GpUpgrades.everythingSpeedMultiplier(state.everythingSpeedUpgradeLevel)
        var goldEarned = 0.0
        val updatedLairs = state.lairs.mapValues { (lairId, owned) ->
            val (next, earned) = advanceLair(
                lairId, owned, deltaSeconds, globalSpeedMultiplier, globalIncomeMultiplier, speedMultiplier, profitMultiplier, gemMultiplier,
                everythingProfitUpgradeMultiplier, everythingSpeedUpgradeMultiplier,
            )
            goldEarned += earned
            next
        }
        return state.copy(
            lairs = updatedLairs,
            lifetimeGoldEarned = state.lifetimeGoldEarned + goldEarned,
            goldPieces = state.goldPieces + goldEarned,
            activeTemporaryBoosts = activeBoosts,
        )
    }

    /**
     * Advances a single lair's production. Steward-managed lairs run as many
     * complete cycles as fit in [deltaSeconds], auto-collecting each. Unmanaged
     * lairs only progress while [OwnedLair.isLoading] is true (started by
     * [startLairLoad]) — sitting idle otherwise, earning nothing, rather than
     * silently filling in the background. A started cycle completes at most
     * once per call (it doesn't loop for extra completions even if
     * [deltaSeconds] covers several cycles' worth of time — the player tapped
     * once, they get one load), crediting the gold immediately and returning
     * to idle; [OwnedLair.completedLoads] increments so the UI can fire a
     * coin-burst effect, but only if this cycle's own production time was at
     * least [MIN_CONFETTI_PRODUCTION_SECONDS] — a very heavily Speed-Boosted
     * lair can complete faster than that effect can read as anything but a
     * flicker, so it's skipped rather than spammed. [globalSpeedMultiplier],
     * [globalIncomeMultiplier], [speedMultiplier], [profitMultiplier],
     * [gemMultiplier], [everythingProfitUpgradeMultiplier], and
     * [everythingSpeedUpgradeMultiplier] are each computed once per
     * [advance] call (same value for every lair that tick), not per lair —
     * only this lair's own `profitUpgradeLevel`/`speedUpgradeLevel` (see
     * `GpUpgrades.kt`) vary lair to lair, so those are combined with the
     * Everything multipliers here instead.
     */
    private fun advanceLair(
        lairId: String,
        owned: OwnedLair,
        deltaSeconds: Double,
        globalSpeedMultiplier: Double,
        globalIncomeMultiplier: Double,
        speedMultiplier: Double,
        profitMultiplier: Double,
        gemMultiplier: Double,
        everythingProfitUpgradeMultiplier: Double,
        everythingSpeedUpgradeMultiplier: Double,
    ): Pair<OwnedLair, Double> {
        if (owned.count <= 0) return owned to 0.0

        val lair = CreatureLairCatalog.get(lairId)
        val upgradeProfitMultiplier = GpUpgrades.lairProfitMultiplier(owned.profitUpgradeLevel) * everythingProfitUpgradeMultiplier
        val upgradeSpeedMultiplier = GpUpgrades.lairSpeedMultiplier(owned.speedUpgradeLevel) * everythingSpeedUpgradeMultiplier
        val productionSeconds = lair.effectiveProductionSeconds(owned.count, speedMultiplier, globalSpeedMultiplier, upgradeSpeedMultiplier)

        if (!owned.hasSteward) {
            if (!owned.isLoading) return owned to 0.0
            val progress = owned.cycleProgressSeconds + deltaSeconds
            return if (progress >= productionSeconds) {
                val earned = lair.incomePerCycle(owned.count, globalIncomeMultiplier, profitMultiplier, gemMultiplier, upgradeProfitMultiplier)
                val confettiWorthy = productionSeconds >= MIN_CONFETTI_PRODUCTION_SECONDS
                owned.copy(
                    cycleProgressSeconds = 0.0,
                    isLoading = false,
                    completedLoads = if (confettiWorthy) owned.completedLoads + 1 else owned.completedLoads,
                ) to earned
            } else {
                owned.copy(cycleProgressSeconds = progress) to 0.0
            }
        }

        var remaining = owned.cycleProgressSeconds + deltaSeconds
        var earned = 0.0
        while (remaining >= productionSeconds) {
            remaining -= productionSeconds
            earned += lair.incomePerCycle(owned.count, globalIncomeMultiplier, profitMultiplier, gemMultiplier, upgradeProfitMultiplier)
        }
        return owned.copy(cycleProgressSeconds = remaining) to earned
    }

    /**
     * Credits [seconds] worth of production for every owned lair as a
     * one-off bonus — used by [purchaseTimeSkip]. Deliberately separate from
     * [advance]: a Time Skip is a deliberate, paid purchase explicitly
     * described (see `ui/shop/ShopContent.kt`) as granting production "from
     * every owned lair," so unlike the live tick loop it must apply
     * uniformly rather than silently skipping a lair that isn't currently
     * loading. Steward-managed lairs use the same looping/carry-over math
     * [advanceLair] does; unmanaged lairs get a standalone credit that
     * doesn't touch their actual [OwnedLair.isLoading]/`cycleProgressSeconds`
     * — using a Time Skip is a bonus on top of whatever the player is doing
     * with that lair's own tap cycle, not a substitute for tapping it.
     */
    private fun grantInstantProduction(state: GameState, seconds: Double, now: Instant): GameState {
        if (seconds <= 0.0 || state.lairs.isEmpty()) return state
        val globalSpeedMultiplier = state.globalSpeedMilestoneMultiplier(CreatureLairCatalog.lairs)
        val globalIncomeMultiplier = state.globalIncomeMilestoneMultiplier(CreatureLairCatalog.lairs)
        val speedMultiplier = state.platinumSpeedMultiplier(now)
        val profitMultiplier = state.platinumProfitMultiplier(now)
        val gemMultiplier = gemIncomeMultiplier(state.gems, state.gemEfficiencyLevel, state.permanentGemPercentMultiplier())
        val everythingProfitUpgradeMultiplier = GpUpgrades.everythingProfitMultiplier(state.everythingProfitUpgradeLevel)
        val everythingSpeedUpgradeMultiplier = GpUpgrades.everythingSpeedMultiplier(state.everythingSpeedUpgradeLevel)

        var goldEarned = 0.0
        val updatedLairs = state.lairs.mapValues { (lairId, owned) ->
            if (owned.count <= 0) return@mapValues owned
            val lair = CreatureLairCatalog.get(lairId)
            val upgradeProfitMultiplier = GpUpgrades.lairProfitMultiplier(owned.profitUpgradeLevel) * everythingProfitUpgradeMultiplier
            val upgradeSpeedMultiplier = GpUpgrades.lairSpeedMultiplier(owned.speedUpgradeLevel) * everythingSpeedUpgradeMultiplier
            val productionSeconds = lair.effectiveProductionSeconds(owned.count, speedMultiplier, globalSpeedMultiplier, upgradeSpeedMultiplier)
            if (owned.hasSteward) {
                var remaining = owned.cycleProgressSeconds + seconds
                var earned = 0.0
                while (remaining >= productionSeconds) {
                    remaining -= productionSeconds
                    earned += lair.incomePerCycle(owned.count, globalIncomeMultiplier, profitMultiplier, gemMultiplier, upgradeProfitMultiplier)
                }
                goldEarned += earned
                owned.copy(cycleProgressSeconds = remaining)
            } else {
                val cycles = kotlin.math.floor(seconds / productionSeconds)
                goldEarned += cycles * lair.incomePerCycle(owned.count, globalIncomeMultiplier, profitMultiplier, gemMultiplier, upgradeProfitMultiplier)
                owned
            }
        }
        return state.copy(
            lairs = updatedLairs,
            goldPieces = state.goldPieces + goldEarned,
            lifetimeGoldEarned = state.lifetimeGoldEarned + goldEarned,
        )
    }

    companion object {
        /**
         * The shortest a lair's own production time can be for its
         * completion to still fire the coin-burst effect (see [advanceLair]).
         * Below this, the burst would complete faster than it can read as
         * anything but a flicker — gold is still credited either way, only
         * the confetti is skipped.
         */
        const val MIN_CONFETTI_PRODUCTION_SECONDS = 0.01

        /**
         * How often [start] advances production, and how often [lairProgress]
         * publishes a fresh fill fraction for the UI to animate toward.
         *
         * Deliberately short: a tick's `deltaSeconds` is measured from the
         * *previous* tick regardless of when a cycle reset (a completed load
         * starting fresh, or a fresh tap) happened in between, so any reset is
         * immediately "overshot" by up to one full tick interval on the very
         * next tick. At the old 200ms that was a third of Kobold Warren's 0.6s
         * cycle — visibly skipping the start of the fill and, for fast cycles,
         * making repeated taps look like they were corrupting the animation.
         *
         * Lowered from an original 33ms to 8ms (v0.21.6) for a second reason
         * beyond overshoot: [lairProgress] can only be as smooth as how many
         * *samples* fall within one cycle, and a lair whose
         * `effectiveProductionSeconds` gets down near this interval — a
         * Kobold Warren at 38ms was the case that prompted this, comfortably
         * above [PROGRESS_SOLID_THRESHOLD_SECONDS] so it was still trying to
         * animate rather than just showing solid — was only getting roughly
         * one sample per cycle at 33ms. One sample can't distinguish "just
         * started" from "about to finish," so the bar visibly jumped around
         * rather than climbing. At 8ms the same 38ms cycle gets ~4-5 samples,
         * enough to read as a real (if fast) climb. This doesn't fix the
         * problem for arbitrarily fast cycles — there's always some speed
         * fast enough to alias against whatever this constant is — it just
         * moves the point where that stops mattering further out, matching
         * where `LairCard`'s own animation and [PROGRESS_SOLID_THRESHOLD_SECONDS]
         * are tuned for. `LairCard`'s own fill animation duration is a fixed,
         * separate constant, deliberately *not* tied to this one — see
         * [PROGRESS_SOLID_THRESHOLD_SECONDS] for why coupling the two broke
         * down once a lair's cycle got fast enough to complete inside a
         * single tick.
         */
        const val TICK_INTERVAL_MS = 8L

        /**
         * Below this, a lair's own [CreatureLair.effectiveProductionSeconds]
         * is fast enough that [computeLairProgress]'s
         * `cycleProgressSeconds / productionSeconds` ratio stops being a
         * meaningful *fill level* to show — [computeLairProgress] reports a
         * flat 1f instead, a continuously solid bar, which is the *truthful*
         * picture once cycles complete far faster than a human can watch one
         * fill.
         *
         * Deliberately the *same* value as [MIN_CONFETTI_PRODUCTION_SECONDS]
         * rather than something derived from [TICK_INTERVAL_MS] (a first
         * pass used 3x the tick interval, ~99ms) — that was calibrated to
         * the tick rate's own sampling limits, but it went solid far too
         * early: reaching just the 200-owned Speed milestone rung (16x) is
         * already enough to cross 99ms for the early lairs, well before
         * their own milestone ladder is anywhere near exhausted. The
         * confetti threshold was already calibrated to "genuinely extreme,
         * beyond ordinary milestone stacking" (its own doc: "reachable only
         * after ~84+ Speed Boost levels") — reusing it here means the bar
         * only goes solid around the same point the coin-burst effect
         * already stops bothering to fire, both expressing the same "too
         * fast to visually register a single cycle" idea. A lair maxing out
         * its own individual Speed milestones alone (400 owned, 64x) sits
         * right at this line rather than well past it — some lairs will
         * still show a real, very fast (and, below [TICK_INTERVAL_MS], not
         * perfectly smooth) animation rather than going solid until
         * additional stacking (the global "Everything" Speed bonus, or a
         * purchased Speed Boost) pushes them further — an accepted
         * trade-off for not freezing the bar prematurely.
         */
        const val PROGRESS_SOLID_THRESHOLD_SECONDS = MIN_CONFETTI_PRODUCTION_SECONDS
    }
}
