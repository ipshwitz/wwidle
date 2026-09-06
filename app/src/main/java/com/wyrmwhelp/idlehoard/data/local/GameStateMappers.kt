package com.wyrmwhelp.idlehoard.data.local

import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import java.time.Instant

fun GameState.toEntities(): Pair<GameStateEntity, List<OwnedLairEntity>> {
    val stateEntity = GameStateEntity(
        goldPieces = goldPieces,
        platinumPieces = platinumPieces,
        gems = gems,
        lifetimeGoldEarned = lifetimeGoldEarned,
        offlineCapHours = offlineCapHours,
        lastSavedAtEpochMillis = lastSavedAt.toEpochMilli(),
        totalLevelUps = totalLevelUps,
        speedBoostLevel = speedBoostLevel,
        profitBoostLevel = profitBoostLevel,
        lastPlatinumAdWatchedAtEpochMillis = lastPlatinumAdWatchedAt?.toEpochMilli(),
    )
    val lairEntities = lairs.values.map { it.toEntity() }
    return stateEntity to lairEntities
}

private fun OwnedLair.toEntity(): OwnedLairEntity = OwnedLairEntity(
    lairId = lairId,
    count = count,
    hasSteward = hasSteward,
    cycleProgressSeconds = cycleProgressSeconds,
    isLoading = isLoading,
    completedLoads = completedLoads,
)

fun GameStateEntity.toDomain(lairEntities: List<OwnedLairEntity>): GameState = GameState(
    goldPieces = goldPieces,
    platinumPieces = platinumPieces,
    gems = gems,
    lifetimeGoldEarned = lifetimeGoldEarned,
    lairs = lairEntities.associate { it.lairId to it.toDomain() },
    offlineCapHours = offlineCapHours,
    lastSavedAt = Instant.ofEpochMilli(lastSavedAtEpochMillis),
    totalLevelUps = totalLevelUps,
    speedBoostLevel = speedBoostLevel,
    profitBoostLevel = profitBoostLevel,
    lastPlatinumAdWatchedAt = lastPlatinumAdWatchedAtEpochMillis?.let { Instant.ofEpochMilli(it) },
)

private fun OwnedLairEntity.toDomain(): OwnedLair = OwnedLair(
    lairId = lairId,
    count = count,
    hasSteward = hasSteward,
    cycleProgressSeconds = cycleProgressSeconds,
    isLoading = isLoading,
    completedLoads = completedLoads,
)
