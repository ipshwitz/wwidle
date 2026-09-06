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
                    isLoading = false,
                    completedLoads = 4,
                    profitUpgradeLevel = 6,
                    speedUpgradeLevel = 9,
                ),
                "goblin_camp" to OwnedLair(
                    lairId = "goblin_camp",
                    count = 1,
                    hasSteward = false,
                    cycleProgressSeconds = 0.0,
                    isLoading = true,
                    completedLoads = 2,
                    profitUpgradeLevel = 0,
                    speedUpgradeLevel = 3,
                ),
            ),
            offlineCapHours = 6.0,
            // Room stores millisecond precision, so truncate before comparing.
            lastSavedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
            totalLevelUps = 2,
            speedBoostLevel = 3,
            profitBoostLevel = 5,
            lastPlatinumAdWatchedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
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
