package com.wyrmwhelp.idlehoard.domain.engine

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import com.wyrmwhelp.idlehoard.domain.model.GameState
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineTest {

    private lateinit var engine: GameEngine

    @Before
    fun setUp() {
        engine = GameEngine()
    }

    @Test
    fun `a new game starts owning one Kobold Warren and no gold`() {
        val state = GameState()

        assertEquals(0.0, state.goldPieces, 0.0001)
        assertEquals(1, state.ownedLair("kobold_warren").count)
    }

    @Test
    fun `purchasing the first unit of a lair deducts its base cost`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(GameState(goldPieces = lair.baseCostGp, lairs = emptyMap()))

        val purchased = engine.purchaseLair("kobold_warren")

        assertTrue(purchased)
        assertEquals(0.0, engine.state.value.goldPieces, 0.0001)
        assertEquals(1, engine.state.value.ownedLair("kobold_warren").count)
    }

    @Test
    fun `purchase fails when gold is insufficient`() {
        engine.loadState(GameState(goldPieces = 0.0, lairs = emptyMap()))

        val purchased = engine.purchaseLair("kobold_warren")

        assertFalse(purchased)
        assertEquals(0, engine.state.value.ownedLair("kobold_warren").count)
    }

    @Test
    fun `unmanaged lair caps at one completed cycle awaiting collection`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(GameState(goldPieces = lair.baseCostGp, lairs = emptyMap()))
        engine.purchaseLair("kobold_warren")

        // Advance far past several production cycles' worth of time in one tick.
        engine.tick(lair.baseProductionSeconds * 5)

        val owned = engine.state.value.ownedLair("kobold_warren")
        assertTrue(owned.isReadyToCollect)
        assertEquals(0.0, engine.state.value.goldPieces, 0.0001) // nothing auto-collected
    }

    @Test
    fun `plundering a ready lair grants its income and resets the cycle`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(GameState(goldPieces = lair.baseCostGp, lairs = emptyMap()))
        engine.purchaseLair("kobold_warren")
        engine.tick(lair.baseProductionSeconds)

        val collected = engine.plunderLair("kobold_warren")

        assertTrue(collected)
        assertEquals(lair.incomePerCycle(1), engine.state.value.goldPieces, 0.0001)
        assertFalse(engine.state.value.ownedLair("kobold_warren").isReadyToCollect)
    }

    @Test
    fun `steward auto-collects every completed cycle without a tap`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(
            GameState(
                goldPieces = lair.baseCostGp + lair.stewardCostGp,
                lairs = emptyMap(),
            ),
        )
        engine.purchaseLair("kobold_warren")
        engine.hireSteward("kobold_warren")

        engine.tick(lair.baseProductionSeconds * 3.5)

        val expectedGold = lair.incomePerCycle(1) * 3
        assertEquals(expectedGold, engine.state.value.goldPieces, 0.0001)
        assertFalse(engine.state.value.ownedLair("kobold_warren").isReadyToCollect)
    }

    @Test
    fun `offline earnings are capped at offlineCapHours`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        val lastSaved = Instant.now().minusSeconds(10 * 3600) // 10 hours ago
        engine.loadState(
            GameState(
                goldPieces = lair.baseCostGp + lair.stewardCostGp,
                lairs = emptyMap(),
                offlineCapHours = 1.0,
                lastSavedAt = lastSaved,
            ),
        )
        engine.purchaseLair("kobold_warren")
        engine.hireSteward("kobold_warren")

        val earnings = engine.applyOfflineEarnings(Instant.now())

        assertEquals(3600.0, earnings.cappedSeconds, 1.0)
        val expectedCycles = Math.floor(3600.0 / lair.baseProductionSeconds)
        assertEquals(expectedCycles * lair.incomePerCycle(1), earnings.goldEarned, 0.01)
    }
}
