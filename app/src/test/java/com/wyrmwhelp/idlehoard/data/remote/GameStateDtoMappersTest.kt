package com.wyrmwhelp.idlehoard.data.remote

import com.wyrmwhelp.idlehoard.domain.model.ActiveTemporaryBoost
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.domain.model.TemporaryBoostCategory
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class GameStateDtoMappersTest {

    @Test
    fun `round-tripping a GameState through the DTO preserves every field`() {
        val original = GameState(
            goldPieces = 12_345.5,
            platinumPieces = 42.0,
            gems = 7L,
            lifetimeGoldEarned = 987_654.0,
            everythingProfitUpgradeLevel = 12,
            everythingSpeedUpgradeLevel = 8,
            gemEfficiencyLevel = 15,
            lairs = mapOf(
                "kobold_warren" to OwnedLair(
                    lairId = "kobold_warren",
                    count = 3,
                    hasSteward = true,
                    cycleProgressSeconds = 1.5,
                    isLoading = true,
                    completedLoads = 4,
                    profitUpgradeLevel = 6,
                    speedUpgradeLevel = 9,
                ),
            ),
            offlineCapHours = 6.0,
            lastSavedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
            totalLevelUps = 2,
            permanentSpeedBoost2xLevel = 3,
            permanentSpeedBoost5xLevel = 1,
            permanentSpeedBoost10xLevel = 0,
            permanentProfitBoost15xLevel = 5,
            permanentProfitBoost2xLevel = 2,
            permanentProfitBoost5xLevel = 0,
            permanentGemBoost15xLevel = 1,
            permanentGemBoost2xLevel = 0,
            permanentGemBoost5xLevel = 4,
            activeTemporaryBoosts = listOf(
                ActiveTemporaryBoost(
                    category = TemporaryBoostCategory.SPEED,
                    multiplier = 50.0,
                    expiresAt = Instant.now().plusSeconds(300).truncatedTo(ChronoUnit.MILLIS),
                ),
            ),
            lastPlatinumAdWatchedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
            speedBoostAdWatchTimestamps = listOf(
                Instant.now().minusSeconds(3600).truncatedTo(ChronoUnit.MILLIS),
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
            ),
        )

        val restored = original.toDto().toDomain()

        assertEquals(original, restored)
    }

    @Test
    fun `a cloud save saved before boosts existed decodes with boost levels defaulting to 0`() {
        val legacyJson = """
            {
                "gold_pieces": 25.0,
                "platinum_pieces": 0.0,
                "lairs": {},
                "offline_cap_hours": 4.0,
                "last_saved_at_epoch_millis": 0
            }
        """.trimIndent()

        val decoded = Json.decodeFromString(GameStateDto.serializer(), legacyJson)

        assertEquals(0, decoded.permanentSpeedBoost2xLevel)
        assertEquals(0, decoded.permanentSpeedBoost5xLevel)
        assertEquals(0, decoded.permanentSpeedBoost10xLevel)
        assertEquals(0, decoded.permanentProfitBoost15xLevel)
        assertEquals(0, decoded.permanentProfitBoost2xLevel)
        assertEquals(0, decoded.permanentProfitBoost5xLevel)
        assertEquals(0, decoded.permanentGemBoost15xLevel)
        assertEquals(0, decoded.permanentGemBoost2xLevel)
        assertEquals(0, decoded.permanentGemBoost5xLevel)
        assertEquals(emptyList<Any>(), decoded.activeTemporaryBoosts)
        assertEquals(null, decoded.lastPlatinumAdWatchedAtEpochMillis)
        assertEquals(emptyList<Long>(), decoded.speedBoostAdWatchTimestampsEpochMillis)
        assertEquals(0L, decoded.gems)
        assertEquals(0, decoded.totalLevelUps)
        assertEquals(0.0, decoded.lifetimeGoldEarned, 0.0001)
        assertEquals(0, decoded.everythingProfitUpgradeLevel)
        assertEquals(0, decoded.everythingSpeedUpgradeLevel)
        assertEquals(0, decoded.gemEfficiencyLevel)
    }

    @Test
    fun `the DTO round-trips through actual JSON encoding, not just in memory`() {
        val original = GameState(
            goldPieces = 25.0,
            lastSavedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
        )

        val json = Json.encodeToString(original.toDto())
        val decoded = Json.decodeFromString(GameStateDto.serializer(), json)

        assertEquals(original, decoded.toDomain())
    }
}
