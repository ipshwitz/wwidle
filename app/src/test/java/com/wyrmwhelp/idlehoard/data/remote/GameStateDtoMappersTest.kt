package com.wyrmwhelp.idlehoard.data.remote

import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
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
            lairs = mapOf(
                "kobold_warren" to OwnedLair(
                    lairId = "kobold_warren",
                    count = 3,
                    hasSteward = true,
                    cycleProgressSeconds = 1.5,
                    isLoading = true,
                    completedLoads = 4,
                ),
            ),
            offlineCapHours = 6.0,
            lastSavedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
            totalLevelUps = 2,
            speedBoostLevel = 3,
            profitBoostLevel = 5,
            lastPlatinumAdWatchedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
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

        assertEquals(0, decoded.speedBoostLevel)
        assertEquals(0, decoded.profitBoostLevel)
        assertEquals(null, decoded.lastPlatinumAdWatchedAtEpochMillis)
        assertEquals(0L, decoded.gems)
        assertEquals(0, decoded.totalLevelUps)
        assertEquals(0.0, decoded.lifetimeGoldEarned, 0.0001)
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
