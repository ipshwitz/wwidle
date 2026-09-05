package com.wyrmwhelp.idlehoard.domain.engine

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.TIME_SKIP_COST_PP
import com.wyrmwhelp.idlehoard.domain.model.TIME_SKIP_SECONDS
import com.wyrmwhelp.idlehoard.domain.model.profitBoostCost
import com.wyrmwhelp.idlehoard.domain.model.profitBoostMultiplier
import com.wyrmwhelp.idlehoard.domain.model.speedBoostCost
import com.wyrmwhelp.idlehoard.domain.model.speedBoostMultiplier
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
    fun `purchasing multiple units at once deducts the bulk cost and adds them all`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        val bulkCost = lair.costForUnits(0, 5)
        engine.loadState(GameState(goldPieces = bulkCost, lairs = emptyMap()))

        val purchased = engine.purchaseLairs("kobold_warren", 5)

        assertEquals(5, purchased)
        assertEquals(0.0, engine.state.value.goldPieces, 0.0001)
        assertEquals(5, engine.state.value.ownedLair("kobold_warren").count)
    }

    @Test
    fun `bulk purchase is all-or-nothing when short even one unit's cost`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        val bulkCost = lair.costForUnits(0, 5)
        engine.loadState(GameState(goldPieces = bulkCost - 0.01, lairs = emptyMap()))

        val purchased = engine.purchaseLairs("kobold_warren", 5)

        assertEquals(0, purchased)
        assertEquals(0, engine.state.value.ownedLair("kobold_warren").count)
        assertEquals(bulkCost - 0.01, engine.state.value.goldPieces, 0.0001)
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

    @Test
    fun `purchasing a speed boost deducts platinum and increments the level`() {
        engine.loadState(GameState(platinumPieces = speedBoostCost(0)))

        val bought = engine.purchaseSpeedBoost()

        assertTrue(bought)
        assertEquals(0.0, engine.state.value.platinumPieces, 0.0001)
        assertEquals(1, engine.state.value.speedBoostLevel)
    }

    @Test
    fun `speed boost purchase fails when platinum is insufficient`() {
        engine.loadState(GameState(platinumPieces = speedBoostCost(0) - 0.01))

        val bought = engine.purchaseSpeedBoost()

        assertFalse(bought)
        assertEquals(0, engine.state.value.speedBoostLevel)
    }

    @Test
    fun `speed boost shortens a lair's effective cycle time`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(
            GameState(goldPieces = lair.baseCostGp, platinumPieces = speedBoostCost(0), lairs = emptyMap()),
        )
        engine.purchaseLair("kobold_warren")
        engine.purchaseSpeedBoost()

        // A full base-cycle's worth of ticking isn't enough time anymore once
        // the effective cycle is shorter, so the lair should already be ready.
        engine.tick(lair.baseProductionSeconds / speedBoostMultiplier(1))

        assertTrue(engine.state.value.ownedLair("kobold_warren").isReadyToCollect)
    }

    @Test
    fun `purchasing a profit boost deducts platinum and increments the level`() {
        engine.loadState(GameState(platinumPieces = profitBoostCost(0)))

        val bought = engine.purchaseProfitBoost()

        assertTrue(bought)
        assertEquals(0.0, engine.state.value.platinumPieces, 0.0001)
        assertEquals(1, engine.state.value.profitBoostLevel)
    }

    @Test
    fun `profit boost increases a plundered lair's income`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(
            GameState(goldPieces = lair.baseCostGp, platinumPieces = profitBoostCost(0), lairs = emptyMap()),
        )
        engine.purchaseLair("kobold_warren")
        engine.purchaseProfitBoost()
        engine.tick(lair.baseProductionSeconds)

        engine.plunderLair("kobold_warren")

        assertEquals(
            lair.incomePerCycle(1, profitBoostMultiplier = profitBoostMultiplier(1)),
            engine.state.value.goldPieces,
            0.0001,
        )
    }

    @Test
    fun `time skip deducts platinum and instantly grants an hour of production`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(
            GameState(
                goldPieces = lair.baseCostGp + lair.stewardCostGp,
                platinumPieces = TIME_SKIP_COST_PP,
                lairs = emptyMap(),
            ),
        )
        engine.purchaseLair("kobold_warren")
        engine.hireSteward("kobold_warren")
        val goldBefore = engine.state.value.goldPieces

        val bought = engine.purchaseTimeSkip()

        assertTrue(bought)
        assertEquals(0.0, engine.state.value.platinumPieces, 0.0001)
        val expectedCycles = Math.floor(TIME_SKIP_SECONDS / lair.baseProductionSeconds)
        assertEquals(
            goldBefore + expectedCycles * lair.incomePerCycle(1),
            engine.state.value.goldPieces,
            0.01,
        )
    }

    @Test
    fun `time skip fails when platinum is insufficient`() {
        engine.loadState(GameState(platinumPieces = TIME_SKIP_COST_PP - 0.01))

        val bought = engine.purchaseTimeSkip()

        assertFalse(bought)
        assertEquals(TIME_SKIP_COST_PP - 0.01, engine.state.value.platinumPieces, 0.0001)
    }
}
