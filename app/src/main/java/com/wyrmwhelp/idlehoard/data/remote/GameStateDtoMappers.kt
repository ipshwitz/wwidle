package com.wyrmwhelp.idlehoard.data.remote

import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import java.time.Instant

fun GameState.toDto(): GameStateDto = GameStateDto(
    goldPieces = goldPieces,
    platinumPieces = platinumPieces,
    scaleShards = scaleShards,
    lairs = lairs.mapValues { (_, owned) -> owned.toDto() },
    offlineCapHours = offlineCapHours,
    lastSavedAtEpochMillis = lastSavedAt.toEpochMilli(),
    totalMolts = totalMolts,
    speedBoostLevel = speedBoostLevel,
    profitBoostLevel = profitBoostLevel,
    lastPlatinumAdWatchedAtEpochMillis = lastPlatinumAdWatchedAt?.toEpochMilli(),
)

private fun OwnedLair.toDto(): OwnedLairDto = OwnedLairDto(
    count = count,
    hasSteward = hasSteward,
    cycleProgressSeconds = cycleProgressSeconds,
    isReadyToCollect = isReadyToCollect,
)

fun GameStateDto.toDomain(): GameState = GameState(
    goldPieces = goldPieces,
    platinumPieces = platinumPieces,
    scaleShards = scaleShards,
    lairs = lairs.mapValues { (lairId, dto) -> dto.toDomain(lairId) },
    offlineCapHours = offlineCapHours,
    lastSavedAt = Instant.ofEpochMilli(lastSavedAtEpochMillis),
    totalMolts = totalMolts,
    speedBoostLevel = speedBoostLevel,
    profitBoostLevel = profitBoostLevel,
    lastPlatinumAdWatchedAt = lastPlatinumAdWatchedAtEpochMillis?.let { Instant.ofEpochMilli(it) },
)

private fun OwnedLairDto.toDomain(lairId: String): OwnedLair = OwnedLair(
    lairId = lairId,
    count = count,
    hasSteward = hasSteward,
    cycleProgressSeconds = cycleProgressSeconds,
    isReadyToCollect = isReadyToCollect,
)
