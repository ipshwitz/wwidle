package com.wyrmwhelp.idlehoard.domain.engine

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.PLATINUM_AD_COOLDOWN
import com.wyrmwhelp.idlehoard.domain.model.PLATINUM_AD_REWARD_PP
import com.wyrmwhelp.idlehoard.domain.model.TIME_SKIP_OPTIONS
import com.wyrmwhelp.idlehoard.domain.model.gemIncomeMultiplier
import com.wyrmwhelp.idlehoard.domain.model.gemsEarnedFromLevelUp
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
    fun `an untapped unmanaged lair sits idle and earns nothing no matter how long it ticks`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(GameState(goldPieces = lair.baseCostGp, lairs = emptyMap()))
        engine.purchaseLair("kobold_warren")

        // Advance far past several production cycles' worth of time in one tick.
        engine.tick(lair.baseProductionSeconds * 5)

        val owned = engine.state.value.ownedLair("kobold_warren")
        assertFalse(owned.isLoading)
        assertEquals(0.0, owned.cycleProgressSeconds, 0.0001)
        assertEquals(0, owned.completedLoads)
        assertEquals(0.0, engine.state.value.goldPieces, 0.0001)
    }

    @Test
    fun `lairProgress reports 0 for an idle unmanaged lair regardless of how long it ticks`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(GameState(goldPieces = lair.baseCostGp, lairs = emptyMap()))
        engine.purchaseLair("kobold_warren")

        engine.tick(lair.baseProductionSeconds * 5)

        assertEquals(0f, engine.lairProgress.value["kobold_warren"])
    }

    @Test
    fun `lairProgress tracks partial progress through a started load`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(GameState(goldPieces = lair.baseCostGp, lairs = emptyMap()))
        engine.purchaseLair("kobold_warren")
        engine.startLairLoad("kobold_warren")

        engine.tick(lair.baseProductionSeconds / 2.0)

        assertEquals(0.5f, engine.lairProgress.value.getValue("kobold_warren"), 0.01f)
    }

    @Test
    fun `lairProgress resets to 0 the moment a started load completes`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(GameState(goldPieces = lair.baseCostGp, lairs = emptyMap()))
        engine.purchaseLair("kobold_warren")
        engine.startLairLoad("kobold_warren")

        engine.tick(lair.baseProductionSeconds)

        assertEquals(0f, engine.lairProgress.value["kobold_warren"])
    }

    @Test
    fun `lairProgress reports a flat 1f once a lair's cycle is too fast to sample meaningfully`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        // 1.05^84 ~= 60.85x, pushing kobold_warren's 0.6s base cycle under
        // GameEngine.PROGRESS_SOLID_THRESHOLD_SECONDS (3 ticks, ~99ms).
        val speedLevel = 84
        engine.loadState(
            GameState(goldPieces = lair.baseCostGp, lairs = emptyMap(), speedBoostLevel = speedLevel),
        )
        engine.purchaseLair("kobold_warren")
        engine.startLairLoad("kobold_warren")

        // A tiny tick: nowhere near enough to finish even one lightning-fast
        // cycle, but the progress bar should already read solid rather than
        // some jittery fractional remainder.
        engine.tick(0.0001)

        assertEquals(1f, engine.lairProgress.value["kobold_warren"])
    }

    @Test
    fun `lairProgress also tracks a Steward-managed lair's current cycle`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(
            GameState(goldPieces = lair.baseCostGp + lair.stewardCostGp, lairs = emptyMap()),
        )
        engine.purchaseLair("kobold_warren")
        engine.hireSteward("kobold_warren")

        engine.tick(lair.baseProductionSeconds / 2.0)

        assertEquals(0.5f, engine.lairProgress.value.getValue("kobold_warren"), 0.01f)
    }

    @Test
    fun `starting a lair's load and letting it finish auto-collects and marks a completion`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(GameState(goldPieces = lair.baseCostGp, lairs = emptyMap()))
        engine.purchaseLair("kobold_warren")

        val started = engine.startLairLoad("kobold_warren")
        assertTrue(started)
        engine.tick(lair.baseProductionSeconds)

        val owned = engine.state.value.ownedLair("kobold_warren")
        assertEquals(lair.incomePerCycle(1), engine.state.value.goldPieces, 0.0001)
        assertFalse(owned.isLoading)
        assertEquals(1, owned.completedLoads)
    }

    @Test
    fun `starting a lair's load twice in a row is a no-op the second time`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(GameState(goldPieces = lair.baseCostGp, lairs = emptyMap()))
        engine.purchaseLair("kobold_warren")

        assertTrue(engine.startLairLoad("kobold_warren"))
        assertFalse(engine.startLairLoad("kobold_warren"))
    }

    @Test
    fun `starting a load does nothing for a Steward-managed lair`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(GameState(goldPieces = lair.baseCostGp + lair.stewardCostGp, lairs = emptyMap()))
        engine.purchaseLair("kobold_warren")
        engine.hireSteward("kobold_warren")

        assertFalse(engine.startLairLoad("kobold_warren"))
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
        assertFalse(engine.state.value.ownedLair("kobold_warren").isLoading)
        // Steward cycles collect silently and never touch this counter.
        assertEquals(0, engine.state.value.ownedLair("kobold_warren").completedLoads)
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
        engine.startLairLoad("kobold_warren")

        // A full base-cycle's worth of ticking isn't enough time anymore once
        // the effective cycle is shorter, so the started load should already
        // have completed and auto-collected.
        engine.tick(lair.baseProductionSeconds / speedBoostMultiplier(1))

        assertEquals(1, engine.state.value.ownedLair("kobold_warren").completedLoads)
        assertFalse(engine.state.value.ownedLair("kobold_warren").isLoading)
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
    fun `profit boost increases a completed load's income`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(
            GameState(goldPieces = lair.baseCostGp, platinumPieces = profitBoostCost(0), lairs = emptyMap()),
        )
        engine.purchaseLair("kobold_warren")
        engine.purchaseProfitBoost()
        engine.startLairLoad("kobold_warren")
        engine.tick(lair.baseProductionSeconds)

        assertEquals(
            lair.incomePerCycle(1, profitBoostMultiplier = profitBoostMultiplier(1)),
            engine.state.value.goldPieces,
            0.0001,
        )
    }

    @Test
    fun `time skip deducts platinum and instantly grants its production`() {
        val timeSkip = TIME_SKIP_OPTIONS.last()
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(
            GameState(
                goldPieces = lair.baseCostGp + lair.stewardCostGp,
                platinumPieces = timeSkip.costPp,
                lairs = emptyMap(),
            ),
        )
        engine.purchaseLair("kobold_warren")
        engine.hireSteward("kobold_warren")
        val goldBefore = engine.state.value.goldPieces

        val bought = engine.purchaseTimeSkip(timeSkip)

        assertTrue(bought)
        assertEquals(0.0, engine.state.value.platinumPieces, 0.0001)
        val expectedCycles = Math.floor(timeSkip.seconds / lair.baseProductionSeconds)
        assertEquals(
            goldBefore + expectedCycles * lair.incomePerCycle(1),
            engine.state.value.goldPieces,
            0.01,
        )
    }

    @Test
    fun `time skip fails when platinum is insufficient`() {
        val timeSkip = TIME_SKIP_OPTIONS.first()
        engine.loadState(GameState(platinumPieces = timeSkip.costPp - 0.01))

        val bought = engine.purchaseTimeSkip(timeSkip)

        assertFalse(bought)
        assertEquals(timeSkip.costPp - 0.01, engine.state.value.platinumPieces, 0.0001)
    }

    @Test
    fun `a load faster than the confetti threshold still pays out but doesn't mark a completion`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        // 1.05^84 ~= 60.85x, pushing kobold_warren's 0.6s base cycle under the
        // 10ms confetti threshold (see GameEngine.MIN_CONFETTI_PRODUCTION_SECONDS).
        val speedLevel = 84
        engine.loadState(
            GameState(goldPieces = lair.baseCostGp, lairs = emptyMap(), speedBoostLevel = speedLevel),
        )
        engine.purchaseLair("kobold_warren")
        val productionSeconds = lair.effectiveProductionSeconds(speedBoostMultiplier = speedBoostMultiplier(speedLevel))
        assertTrue(productionSeconds < 0.01)

        engine.startLairLoad("kobold_warren")
        engine.tick(productionSeconds)

        val owned = engine.state.value.ownedLair("kobold_warren")
        assertEquals(lair.incomePerCycle(1), engine.state.value.goldPieces, 0.0001)
        assertFalse(owned.isLoading)
        assertEquals(0, owned.completedLoads)
    }

    @Test
    fun `time skip credits an idle, untapped, unmanaged lair despite it not currently loading`() {
        val timeSkip = TIME_SKIP_OPTIONS.last()
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(
            GameState(
                goldPieces = lair.baseCostGp,
                platinumPieces = timeSkip.costPp,
                lairs = emptyMap(),
            ),
        )
        engine.purchaseLair("kobold_warren")
        val goldBefore = engine.state.value.goldPieces

        val bought = engine.purchaseTimeSkip(timeSkip)

        assertTrue(bought)
        val expectedCycles = Math.floor(timeSkip.seconds / lair.baseProductionSeconds)
        assertEquals(
            goldBefore + expectedCycles * lair.incomePerCycle(1),
            engine.state.value.goldPieces,
            0.01,
        )
        // Time Skip is a bonus on top of the tap cycle, not a substitute for it.
        assertFalse(engine.state.value.ownedLair("kobold_warren").isLoading)
    }

    @Test
    fun `grantGold adds a flat amount to gold pieces`() {
        engine.loadState(GameState(goldPieces = 100.0))

        engine.grantGold(50.0)

        assertEquals(150.0, engine.state.value.goldPieces, 0.0001)
    }

    @Test
    fun `grantGold ignores a non-positive amount`() {
        engine.loadState(GameState(goldPieces = 100.0))

        engine.grantGold(0.0)
        engine.grantGold(-10.0)

        assertEquals(100.0, engine.state.value.goldPieces, 0.0001)
    }

    @Test
    fun `grantPlatinumAdReward grants Platinum and stamps the watch time when never watched`() {
        engine.loadState(GameState(platinumPieces = 0.0, lastPlatinumAdWatchedAt = null))
        val now = Instant.now()

        val granted = engine.grantPlatinumAdReward(now)

        assertTrue(granted)
        assertEquals(PLATINUM_AD_REWARD_PP, engine.state.value.platinumPieces, 0.0001)
        assertEquals(now, engine.state.value.lastPlatinumAdWatchedAt)
    }

    @Test
    fun `grantPlatinumAdReward fails and grants nothing while still on cooldown`() {
        val watchedAt = Instant.now()
        engine.loadState(GameState(platinumPieces = 0.0, lastPlatinumAdWatchedAt = watchedAt))

        val granted = engine.grantPlatinumAdReward(watchedAt.plusSeconds(60))

        assertFalse(granted)
        assertEquals(0.0, engine.state.value.platinumPieces, 0.0001)
        assertEquals(watchedAt, engine.state.value.lastPlatinumAdWatchedAt)
    }

    @Test
    fun `grantPlatinumAdReward succeeds again once the cooldown fully elapses`() {
        val watchedAt = Instant.now()
        engine.loadState(GameState(platinumPieces = 0.0, lastPlatinumAdWatchedAt = watchedAt))
        val nextWatch = watchedAt.plus(PLATINUM_AD_COOLDOWN)

        val granted = engine.grantPlatinumAdReward(nextWatch)

        assertTrue(granted)
        assertEquals(PLATINUM_AD_REWARD_PP, engine.state.value.platinumPieces, 0.0001)
        assertEquals(nextWatch, engine.state.value.lastPlatinumAdWatchedAt)
    }

    @Test
    fun `performLevelUp does nothing and earns no gems when net worth is too low`() {
        engine.loadState(GameState())

        val gemsEarned = engine.performLevelUp()

        assertEquals(0L, gemsEarned)
        assertEquals(0L, engine.state.value.gems)
        assertEquals(0, engine.state.value.totalLevelUps)
        assertEquals(1, engine.state.value.ownedLair("kobold_warren").count)
    }

    @Test
    fun `performLevelUp resets gold and lairs but earns gems and increments totalLevelUps`() {
        val rich = GameState(goldPieces = 16_000_000.0, lairs = emptyMap())
        val expectedGems = rich.gemsEarnedFromLevelUp()
        engine.loadState(rich)

        val gemsEarned = engine.performLevelUp()

        assertEquals(expectedGems, gemsEarned)
        assertTrue(gemsEarned > 0L)
        assertEquals(0.0, engine.state.value.goldPieces, 0.0001)
        assertEquals(1, engine.state.value.ownedLair("kobold_warren").count)
        assertEquals(gemsEarned, engine.state.value.gems)
        assertEquals(1, engine.state.value.totalLevelUps)
    }

    @Test
    fun `performLevelUp carries over platinum, boosts, offline cap, and the ad cooldown`() {
        val watchedAt = Instant.now()
        engine.loadState(
            GameState(
                goldPieces = 16_000_000.0,
                lairs = emptyMap(),
                platinumPieces = 42.0,
                speedBoostLevel = 3,
                profitBoostLevel = 5,
                offlineCapHours = 8.0,
                lastPlatinumAdWatchedAt = watchedAt,
            ),
        )

        engine.performLevelUp()

        assertEquals(42.0, engine.state.value.platinumPieces, 0.0001)
        assertEquals(3, engine.state.value.speedBoostLevel)
        assertEquals(5, engine.state.value.profitBoostLevel)
        assertEquals(8.0, engine.state.value.offlineCapHours, 0.0001)
        assertEquals(watchedAt, engine.state.value.lastPlatinumAdWatchedAt)
    }

    @Test
    fun `performLevelUp accumulates gems across repeated Level Ups`() {
        engine.loadState(GameState(goldPieces = 16_000_000.0, lairs = emptyMap(), gems = 10L))

        val gemsEarned = engine.performLevelUp()

        assertEquals(10L + gemsEarned, engine.state.value.gems)
    }

    @Test
    fun `gems earned from a past Level Up permanently boost income`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(GameState(goldPieces = lair.baseCostGp, lairs = emptyMap(), gems = 10L))
        engine.purchaseLair("kobold_warren")
        engine.startLairLoad("kobold_warren")

        engine.tick(lair.baseProductionSeconds)

        assertEquals(
            lair.incomePerCycle(1, gemBonusMultiplier = gemIncomeMultiplier(10L)),
            engine.state.value.goldPieces,
            0.0001,
        )
    }
}
