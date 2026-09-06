package com.wyrmwhelp.idlehoard.data.remote

import com.wyrmwhelp.idlehoard.domain.model.ActiveTemporaryBoost
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.domain.model.TemporaryBoostCategory
import java.time.Instant

fun GameState.toDto(): GameStateDto = GameStateDto(
    goldPieces = goldPieces,
    platinumPieces = platinumPieces,
    gems = gems,
    lifetimeGoldEarned = lifetimeGoldEarned,
    everythingProfitUpgradeLevel = everythingProfitUpgradeLevel,
    everythingSpeedUpgradeLevel = everythingSpeedUpgradeLevel,
    gemEfficiencyLevel = gemEfficiencyLevel,
    lairs = lairs.mapValues { (_, owned) -> owned.toDto() },
    offlineCapHours = offlineCapHours,
    lastSavedAtEpochMillis = lastSavedAt.toEpochMilli(),
    totalLevelUps = totalLevelUps,
    permanentSpeedBoost2xLevel = permanentSpeedBoost2xLevel,
    permanentSpeedBoost5xLevel = permanentSpeedBoost5xLevel,
    permanentSpeedBoost10xLevel = permanentSpeedBoost10xLevel,
    permanentProfitBoost15xLevel = permanentProfitBoost15xLevel,
    permanentProfitBoost2xLevel = permanentProfitBoost2xLevel,
    permanentProfitBoost5xLevel = permanentProfitBoost5xLevel,
    permanentGemBoost15xLevel = permanentGemBoost15xLevel,
    permanentGemBoost2xLevel = permanentGemBoost2xLevel,
    permanentGemBoost5xLevel = permanentGemBoost5xLevel,
    activeTemporaryBoosts = activeTemporaryBoosts.map { it.toDto() },
    lastPlatinumAdWatchedAtEpochMillis = lastPlatinumAdWatchedAt?.toEpochMilli(),
)

private fun OwnedLair.toDto(): OwnedLairDto = OwnedLairDto(
    count = count,
    hasSteward = hasSteward,
    cycleProgressSeconds = cycleProgressSeconds,
    isLoading = isLoading,
    completedLoads = completedLoads,
    profitUpgradeLevel = profitUpgradeLevel,
    speedUpgradeLevel = speedUpgradeLevel,
)

private fun ActiveTemporaryBoost.toDto(): ActiveTemporaryBoostDto = ActiveTemporaryBoostDto(
    category = category.name,
    multiplier = multiplier,
    expiresAtEpochMillis = expiresAt.toEpochMilli(),
)

fun GameStateDto.toDomain(): GameState = GameState(
    goldPieces = goldPieces,
    platinumPieces = platinumPieces,
    gems = gems,
    lifetimeGoldEarned = lifetimeGoldEarned,
    everythingProfitUpgradeLevel = everythingProfitUpgradeLevel,
    everythingSpeedUpgradeLevel = everythingSpeedUpgradeLevel,
    gemEfficiencyLevel = gemEfficiencyLevel,
    lairs = lairs.mapValues { (lairId, dto) -> dto.toDomain(lairId) },
    offlineCapHours = offlineCapHours,
    lastSavedAt = Instant.ofEpochMilli(lastSavedAtEpochMillis),
    totalLevelUps = totalLevelUps,
    permanentSpeedBoost2xLevel = permanentSpeedBoost2xLevel,
    permanentSpeedBoost5xLevel = permanentSpeedBoost5xLevel,
    permanentSpeedBoost10xLevel = permanentSpeedBoost10xLevel,
    permanentProfitBoost15xLevel = permanentProfitBoost15xLevel,
    permanentProfitBoost2xLevel = permanentProfitBoost2xLevel,
    permanentProfitBoost5xLevel = permanentProfitBoost5xLevel,
    permanentGemBoost15xLevel = permanentGemBoost15xLevel,
    permanentGemBoost2xLevel = permanentGemBoost2xLevel,
    permanentGemBoost5xLevel = permanentGemBoost5xLevel,
    activeTemporaryBoosts = activeTemporaryBoosts.map { it.toDomain() },
    lastPlatinumAdWatchedAt = lastPlatinumAdWatchedAtEpochMillis?.let { Instant.ofEpochMilli(it) },
)

private fun OwnedLairDto.toDomain(lairId: String): OwnedLair = OwnedLair(
    lairId = lairId,
    count = count,
    hasSteward = hasSteward,
    cycleProgressSeconds = cycleProgressSeconds,
    isLoading = isLoading,
    completedLoads = completedLoads,
    profitUpgradeLevel = profitUpgradeLevel,
    speedUpgradeLevel = speedUpgradeLevel,
)

private fun ActiveTemporaryBoostDto.toDomain(): ActiveTemporaryBoost = ActiveTemporaryBoost(
    category = TemporaryBoostCategory.valueOf(category),
    multiplier = multiplier,
    expiresAt = Instant.ofEpochMilli(expiresAtEpochMillis),
)
