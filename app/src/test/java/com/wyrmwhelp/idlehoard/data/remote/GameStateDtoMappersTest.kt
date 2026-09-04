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
            scaleShards = 7L,
            lairs = mapOf(
                "kobold_warren" to OwnedLair(
                    lairId = "kobold_warren",
                    count = 3,
                    hasSteward = true,
                    cycleProgressSeconds = 1.5,
                    isReadyToCollect = false,
                ),
            ),
            offlineCapHours = 6.0,
            lastSavedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
            totalMolts = 2,
        )

        val restored = original.toDto().toDomain()

        assertEquals(original, restored)
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
