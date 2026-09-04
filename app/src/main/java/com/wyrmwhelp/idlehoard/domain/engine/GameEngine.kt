package com.wyrmwhelp.idlehoard.domain.engine

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
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
    fun purchaseLair(lairId: String): Boolean {
        val lair = CreatureLairCatalog.get(lairId)
        var purchased = false
        _state.update { current ->
            val owned = current.ownedLair(lairId)
            val cost = lair.costForNextUnit(owned.count)
            if (current.goldPieces < cost) {
                current
            } else {
                purchased = true
                current.copy(
                    goldPieces = current.goldPieces - cost,
                    lairs = current.lairs + (lairId to owned.copy(count = owned.count + 1)),
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
                current.copy(
                    goldPieces = current.goldPieces + lair.incomePerCycle(owned.count),
                    lairs = current.lairs + (
                        lairId to owned.copy(cycleProgressSeconds = 0.0, isReadyToCollect = false)
                        ),
                )
            }
        }
        return collected
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
        var goldEarned = 0.0
        val updatedLairs = state.lairs.mapValues { (lairId, owned) ->
            val (next, earned) = advanceLair(lairId, owned, deltaSeconds)
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
     */
    private fun advanceLair(
        lairId: String,
        owned: OwnedLair,
        deltaSeconds: Double,
    ): Pair<OwnedLair, Double> {
        if (owned.count <= 0) return owned to 0.0
        if (owned.isReadyToCollect && !owned.hasSteward) return owned to 0.0

        val lair = CreatureLairCatalog.get(lairId)
        val progress = owned.cycleProgressSeconds + deltaSeconds

        if (!owned.hasSteward) {
            return if (progress >= lair.baseProductionSeconds) {
                owned.copy(
                    cycleProgressSeconds = lair.baseProductionSeconds,
                    isReadyToCollect = true,
                ) to 0.0
            } else {
                owned.copy(cycleProgressSeconds = progress) to 0.0
            }
        }

        var remaining = progress
        var earned = 0.0
        while (remaining >= lair.baseProductionSeconds) {
            remaining -= lair.baseProductionSeconds
            earned += lair.incomePerCycle(owned.count)
        }
        return owned.copy(cycleProgressSeconds = remaining) to earned
    }

    private companion object {
        const val TICK_INTERVAL_MS = 200L
    }
}
