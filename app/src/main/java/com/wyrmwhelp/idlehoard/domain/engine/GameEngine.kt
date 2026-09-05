package com.wyrmwhelp.idlehoard.domain.engine

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.domain.model.globalMilestoneMultiplier
import com.wyrmwhelp.idlehoard.domain.model.profitBoostCost
import com.wyrmwhelp.idlehoard.domain.model.profitBoostMultiplier
import com.wyrmwhelp.idlehoard.domain.model.speedBoostCost
import com.wyrmwhelp.idlehoard.domain.model.speedBoostMultiplier
import com.wyrmwhelp.idlehoard.domain.model.TIME_SKIP_COST_PP
import com.wyrmwhelp.idlehoard.domain.model.TIME_SKIP_SECONDS
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
 * Stewards, plundering completed cycles). App-scoped singleton — outlives any
 * single screen/ViewModel, since production must keep running across
 * navigation. No persistence wiring yet; [loadState] and [applyOfflineEarnings]
 * are the seams the Room/Supabase repositories will call into once they exist.
 */
@Singleton
class GameEngine @Inject constructor() {

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state

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
    fun tick(deltaSeconds: Double) {
        if (deltaSeconds <= 0.0) return
        _state.update { advance(it, deltaSeconds) }
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
     * Manually collects a finished production cycle from [lairId] (the player
     * tapping a lair that's sitting full and waiting). Returns true if there was
     * anything to collect.
     */
    fun plunderLair(lairId: String): Boolean {
        var collected = false
        _state.update { current ->
            val owned = current.ownedLair(lairId)
            if (!owned.isReadyToCollect) {
                current
            } else {
                val lair = CreatureLairCatalog.get(lairId)
                collected = true
                val globalMultiplier = current.globalMilestoneMultiplier(CreatureLairCatalog.lairs)
                val profitMultiplier = profitBoostMultiplier(current.profitBoostLevel)
                current.copy(
                    goldPieces = current.goldPieces +
                        lair.incomePerCycle(owned.count, globalMultiplier, profitMultiplier),
                    lairs = current.lairs + (
                        lairId to owned.copy(cycleProgressSeconds = 0.0, isReadyToCollect = false)
                        ),
                )
            }
        }
        return collected
    }

    /**
     * Buys the next Speed Boost level with Platinum Pieces, permanently
     * shortening every lair's cycle time (see [speedBoostMultiplier]).
     * Returns true if bought, false if the player can't afford it.
     */
    fun purchaseSpeedBoost(): Boolean {
        var bought = false
        _state.update { current ->
            val cost = speedBoostCost(current.speedBoostLevel)
            if (current.platinumPieces < cost) {
                current
            } else {
                bought = true
                current.copy(
                    platinumPieces = current.platinumPieces - cost,
                    speedBoostLevel = current.speedBoostLevel + 1,
                )
            }
        }
        return bought
    }

    /**
     * Buys the next Profit Boost level with Platinum Pieces, permanently
     * increasing every lair's income (see [profitBoostMultiplier]).
     * Returns true if bought, false if the player can't afford it.
     */
    fun purchaseProfitBoost(): Boolean {
        var bought = false
        _state.update { current ->
            val cost = profitBoostCost(current.profitBoostLevel)
            if (current.platinumPieces < cost) {
                current
            } else {
                bought = true
                current.copy(
                    platinumPieces = current.platinumPieces - cost,
                    profitBoostLevel = current.profitBoostLevel + 1,
                )
            }
        }
        return bought
    }

    /**
     * Spends Platinum Pieces to instantly grant [TIME_SKIP_SECONDS] of
     * production, using the same [advance] logic as the live tick loop and
     * offline earnings. Returns true if bought, false if the player can't
     * afford it.
     */
    fun purchaseTimeSkip(): Boolean {
        var bought = false
        _state.update { current ->
            if (current.platinumPieces < TIME_SKIP_COST_PP) {
                current
            } else {
                bought = true
                advance(
                    current.copy(platinumPieces = current.platinumPieces - TIME_SKIP_COST_PP),
                    TIME_SKIP_SECONDS,
                )
            }
        }
        return bought
    }

    /**
     * Settles production that happened while the app was closed, based on the
     * time since [GameState.lastSavedAt], capped at [GameState.offlineCapHours].
     * Uses the same per-lair rules as the live tick loop: Steward-managed lairs
     * auto-collect every completed cycle, unmanaged lairs cap at one pending
     * cycle waiting for a tap. Call once on load, before [start].
     */
    fun applyOfflineEarnings(now: Instant = Instant.now()): OfflineEarnings {
        var earnings = OfflineEarnings(0.0, 0.0, 0.0)
        _state.update { current ->
            val elapsedSeconds = Duration.between(current.lastSavedAt, now).seconds
                .toDouble()
                .coerceAtLeast(0.0)
            val cappedSeconds = min(elapsedSeconds, current.offlineCapHours * 3600.0)

            val goldBefore = current.goldPieces
            val settled = advance(current, cappedSeconds).copy(lastSavedAt = now)
            earnings = OfflineEarnings(
                elapsedSeconds = elapsedSeconds,
                cappedSeconds = cappedSeconds,
                goldEarned = settled.goldPieces - goldBefore,
            )
            settled
        }
        return earnings
    }

    /** Pure production step: advances every owned lair and tallies Gold Pieces earned. */
    private fun advance(state: GameState, deltaSeconds: Double): GameState {
        if (deltaSeconds <= 0.0 || state.lairs.isEmpty()) return state
        val globalMultiplier = state.globalMilestoneMultiplier(CreatureLairCatalog.lairs)
        val speedMultiplier = speedBoostMultiplier(state.speedBoostLevel)
        val profitMultiplier = profitBoostMultiplier(state.profitBoostLevel)
        var goldEarned = 0.0
        val updatedLairs = state.lairs.mapValues { (lairId, owned) ->
            val (next, earned) = advanceLair(
                lairId, owned, deltaSeconds, globalMultiplier, speedMultiplier, profitMultiplier,
            )
            goldEarned += earned
            next
        }
        return state.copy(
            lairs = updatedLairs,
            goldPieces = state.goldPieces + goldEarned,
        )
    }

    /**
     * Advances a single lair's production. Steward-managed lairs run as many
     * complete cycles as fit in [deltaSeconds], auto-collecting each. Unmanaged
     * lairs progress toward one completed cycle and then sit full, waiting for
     * [plunderLair] — a second cycle never silently completes underneath the player.
     * [globalMultiplier], [speedMultiplier], and [profitMultiplier] are each
     * computed once per [advance] call (same value for every lair that tick),
     * not per lair.
     */
    private fun advanceLair(
        lairId: String,
        owned: OwnedLair,
        deltaSeconds: Double,
        globalMultiplier: Double,
        speedMultiplier: Double,
        profitMultiplier: Double,
    ): Pair<OwnedLair, Double> {
        if (owned.count <= 0) return owned to 0.0
        if (owned.isReadyToCollect && !owned.hasSteward) return owned to 0.0

        val lair = CreatureLairCatalog.get(lairId)
        val productionSeconds = lair.effectiveProductionSeconds(speedMultiplier)
        val progress = owned.cycleProgressSeconds + deltaSeconds

        if (!owned.hasSteward) {
            return if (progress >= productionSeconds) {
                owned.copy(
                    cycleProgressSeconds = productionSeconds,
                    isReadyToCollect = true,
                ) to 0.0
            } else {
                owned.copy(cycleProgressSeconds = progress) to 0.0
            }
        }

        var remaining = progress
        var earned = 0.0
        while (remaining >= productionSeconds) {
            remaining -= productionSeconds
            earned += lair.incomePerCycle(owned.count, globalMultiplier, profitMultiplier)
        }
        return owned.copy(cycleProgressSeconds = remaining) to earned
    }

    companion object {
        /**
         * How often [start] advances production. Public so the UI can match its
         * progress-fill animation duration to this and avoid visibly stepping
         * between updates — see `LairCard`.
         *
         * Deliberately short: a tick's `deltaSeconds` is measured from the
         * *previous* tick regardless of when a cycle reset (a plunder) happened
         * in between, so any reset is immediately "overshot" by up to one full
         * tick interval on the very next tick. At the old 200ms that was a third
         * of Kobold Warren's 0.6s cycle — visibly skipping the start of the fill
         * and, for fast cycles, making repeated taps look like they were
         * corrupting the animation. At 33ms the same overshoot is ~5.5% of even
         * that fastest cycle (and under 2% for every longer one) — small enough
         * to read as a clean start.
         */
        const val TICK_INTERVAL_MS = 33L
    }
}
