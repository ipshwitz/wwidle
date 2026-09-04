package com.wyrmwhelp.idlehoard.data.local

import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class GameStateMappersTest {

    @Test
    fun `round-tripping a GameState through entities preserves every field`() {
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
                "goblin_camp" to OwnedLair(
                    lairId = "goblin_camp",
                    count = 1,
                    hasSteward = false,
                    cycleProgressSeconds = 0.0,
                    isReadyToCollect = true,
                ),
            ),
            offlineCapHours = 6.0,
            // Room stores millisecond precision, so truncate before comparing.
            lastSavedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
            totalMolts = 2,
        )

        val (stateEntity, lairEntities) = original.toEntities()
        val restored = stateEntity.toDomain(lairEntities)

        assertEquals(original, restored)
    }

    @Test
    fun `a GameState with no owned lairs round-trips to an empty lairs map`() {
        val original = GameState(
            lairs = emptyMap(),
            lastSavedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
        )

        val (stateEntity, lairEntities) = original.toEntities()
        val restored = stateEntity.toDomain(lairEntities)

        assertEquals(original, restored)
        assertEquals(emptyList<OwnedLairEntity>(), lairEntities)
    }
}
