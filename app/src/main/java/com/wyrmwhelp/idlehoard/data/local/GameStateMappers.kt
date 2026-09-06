package com.wyrmwhelp.idlehoard.data.local

import com.wyrmwhelp.idlehoard.domain.model.ActiveTemporaryBoost
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.domain.model.TemporaryBoostCategory
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun GameState.toEntities(): Pair<GameStateEntity, List<OwnedLairEntity>> {
    val stateEntity = GameStateEntity(
        goldPieces = goldPieces,
        platinumPieces = platinumPieces,
        gems = gems,
        lifetimeGoldEarned = lifetimeGoldEarned,
        everythingProfitUpgradeLevel = everythingProfitUpgradeLevel,
        everythingSpeedUpgradeLevel = everythingSpeedUpgradeLevel,
        gemEfficiencyLevel = gemEfficiencyLevel,
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
        activeTemporaryBoostsJson = activeTemporaryBoosts.toJson(),
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
    profitUpgradeLevel = profitUpgradeLevel,
    speedUpgradeLevel = speedUpgradeLevel,
)

fun GameStateEntity.toDomain(lairEntities: List<OwnedLairEntity>): GameState = GameState(
    goldPieces = goldPieces,
    platinumPieces = platinumPieces,
    gems = gems,
    lifetimeGoldEarned = lifetimeGoldEarned,
    everythingProfitUpgradeLevel = everythingProfitUpgradeLevel,
    everythingSpeedUpgradeLevel = everythingSpeedUpgradeLevel,
    gemEfficiencyLevel = gemEfficiencyLevel,
    lairs = lairEntities.associate { it.lairId to it.toDomain() },
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
    activeTemporaryBoosts = activeTemporaryBoostsJson.toActiveTemporaryBoosts(),
    lastPlatinumAdWatchedAt = lastPlatinumAdWatchedAtEpochMillis?.let { Instant.ofEpochMilli(it) },
)

private fun OwnedLairEntity.toDomain(): OwnedLair = OwnedLair(
    lairId = lairId,
    count = count,
    hasSteward = hasSteward,
    cycleProgressSeconds = cycleProgressSeconds,
    isLoading = isLoading,
    completedLoads = completedLoads,
    profitUpgradeLevel = profitUpgradeLevel,
    speedUpgradeLevel = speedUpgradeLevel,
)

/** Plain JSON mirror of [ActiveTemporaryBoost] for [GameStateEntity.activeTemporaryBoostsJson]'s single-column encoding. */
@Serializable
private data class ActiveTemporaryBoostRecord(
    val category: String,
    val multiplier: Double,
    val expiresAtEpochMillis: Long,
)

private fun List<ActiveTemporaryBoost>.toJson(): String =
    Json.encodeToString(map { ActiveTemporaryBoostRecord(it.category.name, it.multiplier, it.expiresAt.toEpochMilli()) })

private fun String.toActiveTemporaryBoosts(): List<ActiveTemporaryBoost> =
    Json.decodeFromString<List<ActiveTemporaryBoostRecord>>(this).map {
        ActiveTemporaryBoost(TemporaryBoostCategory.valueOf(it.category), it.multiplier, Instant.ofEpochMilli(it.expiresAtEpochMillis))
    }
