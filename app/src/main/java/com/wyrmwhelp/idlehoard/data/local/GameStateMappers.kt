package com.wyrmwhelp.idlehoard.data.local

import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import java.time.Instant

fun GameState.toEntities(): Pair<GameStateEntity, List<OwnedLairEntity>> {
    val stateEntity = GameStateEntity(
        goldPieces = goldPieces,
        platinumPieces = platinumPieces,
        scaleShards = scaleShards,
        offlineCapHours = offlineCapHours,
        lastSavedAtEpochMillis = lastSavedAt.toEpochMilli(),
        totalMolts = totalMolts,
        speedBoostLevel = speedBoostLevel,
        profitBoostLevel = profitBoostLevel,
    )
    val lairEntities = lairs.values.map { it.toEntity() }
    return stateEntity to lairEntities
}

private fun OwnedLair.toEntity(): OwnedLairEntity = OwnedLairEntity(
    lairId = lairId,
    count = count,
    hasSteward = hasSteward,
    cycleProgressSeconds = cycleProgressSeconds,
    isReadyToCollect = isReadyToCollect,
)

fun GameStateEntity.toDomain(lairEntities: List<OwnedLairEntity>): GameState = GameState(
    goldPieces = goldPieces,
    platinumPieces = platinumPieces,
    scaleShards = scaleShards,
    lairs = lairEntities.associate { it.lairId to it.toDomain() },
    offlineCapHours = offlineCapHours,
    lastSavedAt = Instant.ofEpochMilli(lastSavedAtEpochMillis),
    totalMolts = totalMolts,
    speedBoostLevel = speedBoostLevel,
    profitBoostLevel = profitBoostLevel,
)

private fun OwnedLairEntity.toDomain(): OwnedLair = OwnedLair(
    lairId = lairId,
    count = count,
    hasSteward = hasSteward,
    cycleProgressSeconds = cycleProgressSeconds,
    isReadyToCollect = isReadyToCollect,
)
