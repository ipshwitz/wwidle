package com.wyrmwhelp.idlehoard.domain.engine

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import com.wyrmwhelp.idlehoard.domain.model.ActiveTemporaryBoost
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.domain.model.PERMANENT_PROFIT_TIERS
import com.wyrmwhelp.idlehoard.domain.model.PERMANENT_SPEED_TIERS
import com.wyrmwhelp.idlehoard.domain.model.PLATINUM_AD_COOLDOWN
import com.wyrmwhelp.idlehoard.domain.model.PLATINUM_AD_REWARD_PP
import com.wyrmwhelp.idlehoard.domain.model.SPEED_BOOST_AD_COOLDOWN
import com.wyrmwhelp.idlehoard.domain.model.SPEED_BOOST_AD_DURATION
import com.wyrmwhelp.idlehoard.domain.model.SPEED_BOOST_AD_MAX_SLOTS
import com.wyrmwhelp.idlehoard.domain.model.SPEED_BOOST_AD_MULTIPLIER
import com.wyrmwhelp.idlehoard.domain.model.TEMPORARY_BOOST_OPTIONS
import com.wyrmwhelp.idlehoard.domain.model.TIME_SKIP_OPTIONS
import com.wyrmwhelp.idlehoard.domain.model.TemporaryBoostCategory
import com.wyrmwhelp.idlehoard.domain.model.GemUpgrades
import com.wyrmwhelp.idlehoard.domain.model.GpUpgrades
import com.wyrmwhelp.idlehoard.domain.model.UpgradeCategory
import com.wyrmwhelp.idlehoard.domain.model.costForPermanentBoostPurchase
import com.wyrmwhelp.idlehoard.domain.model.gemIncomeMultiplier
import com.wyrmwhelp.idlehoard.domain.model.gemsEarnedFromLevelUp
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
        // 100x (the 10x permanent Speed tier bought twice, compounding)
        // pushes kobold_warren's 0.6s base cycle under
        // GameEngine.PROGRESS_SOLID_THRESHOLD_SECONDS (10ms).
        engine.loadState(
            GameState(goldPieces = lair.baseCostGp, lairs = emptyMap(), permanentSpeedBoost10xLevel = 2),
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
    fun `purchasing a permanent boost deducts platinum and increments that tier's own level`() {
        val tier = PERMANENT_SPEED_TIERS[0]
        engine.loadState(GameState(platinumPieces = costForPermanentBoostPurchase(tier, 0)))

        val bought = engine.purchasePermanentBoost(tier)

        assertTrue(bought)
        assertEquals(0.0, engine.state.value.platinumPieces, 0.0001)
        assertEquals(1, engine.state.value.permanentSpeedBoost2xLevel)
    }

    @Test
    fun `permanent boost purchase fails when platinum is insufficient`() {
        val tier = PERMANENT_SPEED_TIERS[0]
        engine.loadState(GameState(platinumPieces = costForPermanentBoostPurchase(tier, 0) - 0.01))

        val bought = engine.purchasePermanentBoost(tier)

        assertFalse(bought)
        assertEquals(0, engine.state.value.permanentSpeedBoost2xLevel)
    }

    @Test
    fun `buying the same permanent boost tier again costs more, per its own growth rate`() {
        val tier = PERMANENT_SPEED_TIERS[0]
        engine.loadState(GameState(platinumPieces = 1_000.0))
        engine.purchasePermanentBoost(tier)
        val platinumAfterFirst = engine.state.value.platinumPieces

        engine.purchasePermanentBoost(tier)

        val secondCost = platinumAfterFirst - engine.state.value.platinumPieces
        assertEquals(costForPermanentBoostPurchase(tier, 1), secondCost, 0.0001)
        assertEquals(2, engine.state.value.permanentSpeedBoost2xLevel)
    }

    @Test
    fun `permanent boost purchases never fail for being at a max level, only affordability`() {
        val tier = PERMANENT_SPEED_TIERS[0]
        engine.loadState(GameState(platinumPieces = Double.MAX_VALUE / 2, permanentSpeedBoost2xLevel = 500))

        val bought = engine.purchasePermanentBoost(tier)

        assertTrue(bought)
        assertEquals(501, engine.state.value.permanentSpeedBoost2xLevel)
    }

    @Test
    fun `speed boost shortens a lair's effective cycle time`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        val tier = PERMANENT_SPEED_TIERS[0]
        engine.loadState(
            GameState(goldPieces = lair.baseCostGp, platinumPieces = costForPermanentBoostPurchase(tier, 0), lairs = emptyMap()),
        )
        engine.purchaseLair("kobold_warren")
        engine.purchasePermanentBoost(tier)
        engine.startLairLoad("kobold_warren")

        // A full base-cycle's worth of ticking isn't enough time anymore once
        // the effective cycle is shorter, so the started load should already
        // have completed and auto-collected.
        engine.tick(lair.baseProductionSeconds / tier.multiplier)

        assertEquals(1, engine.state.value.ownedLair("kobold_warren").completedLoads)
        assertFalse(engine.state.value.ownedLair("kobold_warren").isLoading)
    }

    @Test
    fun `profit boost increases a completed load's income`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        val tier = PERMANENT_PROFIT_TIERS[0]
        engine.loadState(
            GameState(goldPieces = lair.baseCostGp, platinumPieces = costForPermanentBoostPurchase(tier, 0), lairs = emptyMap()),
        )
        engine.purchaseLair("kobold_warren")
        engine.purchasePermanentBoost(tier)
        engine.startLairLoad("kobold_warren")
        engine.tick(lair.baseProductionSeconds)

        assertEquals(
            lair.incomePerCycle(1, profitBoostMultiplier = tier.multiplier),
            engine.state.value.goldPieces,
            0.0001,
        )
    }

    @Test
    fun `purchasing a temporary boost deducts platinum and starts it running for its duration`() {
        val option = TEMPORARY_BOOST_OPTIONS.first { it.category == TemporaryBoostCategory.SPEED }
        engine.loadState(GameState(platinumPieces = option.costPp))
        val now = Instant.now()

        val bought = engine.purchaseTemporaryBoost(option, now)

        assertTrue(bought)
        assertEquals(0.0, engine.state.value.platinumPieces, 0.0001)
        val active = engine.state.value.activeTemporaryBoosts.single()
        assertEquals(option.category, active.category)
        assertEquals(option.multiplier, active.multiplier, 0.0001)
        assertEquals(now.plusSeconds(option.durationSeconds), active.expiresAt)
    }

    @Test
    fun `temporary boost purchase fails when platinum is insufficient`() {
        val option = TEMPORARY_BOOST_OPTIONS.first()
        engine.loadState(GameState(platinumPieces = option.costPp - 0.01))

        val bought = engine.purchaseTemporaryBoost(option)

        assertFalse(bought)
        assertTrue(engine.state.value.activeTemporaryBoosts.isEmpty())
    }

    @Test
    fun `buying a second temporary boost of the same category stacks multiplicatively while both run`() {
        val speedOptions = TEMPORARY_BOOST_OPTIONS.filter { it.category == TemporaryBoostCategory.SPEED }
        val lair = CreatureLairCatalog.get("kobold_warren")
        val now = Instant.now()
        engine.loadState(
            GameState(
                goldPieces = lair.baseCostGp,
                platinumPieces = speedOptions.sumOf { it.costPp },
                lairs = emptyMap(),
            ),
        )
        engine.purchaseLair("kobold_warren")
        speedOptions.forEach { engine.purchaseTemporaryBoost(it, now) }
        engine.startLairLoad("kobold_warren")

        // 50x * 100x = 5000x combined — comfortably past the confetti
        // threshold, so this checks completion via isLoading/gold instead of
        // OwnedLair.completedLoads (which this speed correctly skips).
        val expectedMultiplier = speedOptions.fold(1.0) { acc, option -> acc * option.multiplier }
        engine.tick(lair.baseProductionSeconds / expectedMultiplier, now.plusSeconds(1))

        assertFalse(engine.state.value.ownedLair("kobold_warren").isLoading)
        assertEquals(lair.incomePerCycle(1), engine.state.value.goldPieces, 0.0001)
    }

    @Test
    fun `an expired temporary boost is pruned and stops contributing once ticked past its expiry`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        val option = TEMPORARY_BOOST_OPTIONS.first { it.category == TemporaryBoostCategory.SPEED }
        val now = Instant.now()
        engine.loadState(
            GameState(
                goldPieces = lair.baseCostGp,
                lairs = emptyMap(),
                activeTemporaryBoosts = listOf(ActiveTemporaryBoost(option.category, option.multiplier, now.plusSeconds(5))),
            ),
        )
        engine.purchaseLair("kobold_warren")

        engine.tick(1.0, now.plusSeconds(10))

        assertTrue(engine.state.value.activeTemporaryBoosts.isEmpty())
    }

    @Test
    fun `permanent Gem percent boost multiplies the per-Gem income bonus`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(
            GameState(
                goldPieces = lair.baseCostGp,
                lairs = emptyMap(),
                gems = 10L,
                permanentGemBoost2xLevel = 1,
            ),
        )
        engine.purchaseLair("kobold_warren")
        engine.startLairLoad("kobold_warren")

        engine.tick(lair.baseProductionSeconds)

        assertEquals(
            lair.incomePerCycle(1, gemBonusMultiplier = gemIncomeMultiplier(10L, platinumGemPercentMultiplier = 2.0)),
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
        // 100x (the 10x permanent Speed tier bought twice) pushes
        // kobold_warren's 0.6s base cycle under the 10ms confetti threshold
        // (see GameEngine.MIN_CONFETTI_PRODUCTION_SECONDS).
        engine.loadState(
            GameState(goldPieces = lair.baseCostGp, lairs = emptyMap(), permanentSpeedBoost10xLevel = 2),
        )
        engine.purchaseLair("kobold_warren")
        val productionSeconds = lair.effectiveProductionSeconds(speedBoostMultiplier = 100.0)
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
    fun `grantGold also accumulates into lifetimeGoldEarned`() {
        engine.loadState(GameState(goldPieces = 100.0, lifetimeGoldEarned = 500.0))

        engine.grantGold(50.0)

        assertEquals(550.0, engine.state.value.lifetimeGoldEarned, 0.0001)
    }

    @Test
    fun `grantPlatinum adds a flat amount of platinum pieces`() {
        engine.loadState(GameState(platinumPieces = 10.0))

        engine.grantPlatinum(550L)

        assertEquals(560.0, engine.state.value.platinumPieces, 0.0001)
    }

    @Test
    fun `grantPlatinum ignores a non-positive amount`() {
        engine.loadState(GameState(platinumPieces = 10.0))

        engine.grantPlatinum(0L)
        engine.grantPlatinum(-5L)

        assertEquals(10.0, engine.state.value.platinumPieces, 0.0001)
    }

    @Test
    fun `lifetimeGoldEarned accumulates from production but never decreases from spending`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        engine.loadState(GameState(goldPieces = lair.baseCostGp, lairs = emptyMap()))
        engine.purchaseLair("kobold_warren")
        assertEquals(0.0, engine.state.value.lifetimeGoldEarned, 0.0001)

        engine.startLairLoad("kobold_warren")
        engine.tick(lair.baseProductionSeconds)
        val earned = lair.incomePerCycle(1)
        assertEquals(earned, engine.state.value.lifetimeGoldEarned, 0.0001)

        // Spending it on a second Kobold Warren shouldn't undo the lifetime tally.
        engine.purchaseLair("kobold_warren")
        assertEquals(earned, engine.state.value.lifetimeGoldEarned, 0.0001)
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
    fun `grantSpeedBoostAdReward starts a fresh 2x Speed boost and stamps a watch when never watched`() {
        engine.loadState(GameState())
        val now = Instant.now()

        val granted = engine.grantSpeedBoostAdReward(now)

        assertTrue(granted)
        assertEquals(listOf(now), engine.state.value.speedBoostAdWatchTimestamps)
        val active = engine.state.value.activeTemporaryBoosts.single()
        assertEquals(TemporaryBoostCategory.SPEED, active.category)
        assertEquals(SPEED_BOOST_AD_MULTIPLIER, active.multiplier, 0.0001)
        assertEquals(now.plus(SPEED_BOOST_AD_DURATION), active.expiresAt)
    }

    @Test
    fun `grantSpeedBoostAdReward can be granted up to four times before running out of slots`() {
        engine.loadState(GameState())
        val now = Instant.now()

        repeat(SPEED_BOOST_AD_MAX_SLOTS) { assertTrue(engine.grantSpeedBoostAdReward(now)) }
        val fifthGranted = engine.grantSpeedBoostAdReward(now)

        assertFalse(fifthGranted)
        assertEquals(SPEED_BOOST_AD_MAX_SLOTS, engine.state.value.speedBoostAdWatchTimestamps.size)
        assertEquals(SPEED_BOOST_AD_MAX_SLOTS, engine.state.value.activeTemporaryBoosts.size)
    }

    @Test
    fun `grantSpeedBoostAdReward succeeds again once one slot's own cooldown fully elapses`() {
        val watchedAt = Instant.now()
        engine.loadState(GameState(speedBoostAdWatchTimestamps = List(SPEED_BOOST_AD_MAX_SLOTS) { watchedAt }))
        val nextWatch = watchedAt.plus(SPEED_BOOST_AD_COOLDOWN)

        val granted = engine.grantSpeedBoostAdReward(nextWatch)

        assertTrue(granted)
    }

    @Test
    fun `performLevelUp does nothing and earns no gems from a brand-new save`() {
        engine.loadState(GameState())

        val gemsEarned = engine.performLevelUp()

        assertEquals(0L, gemsEarned)
        assertEquals(0L, engine.state.value.gems)
        assertEquals(0, engine.state.value.totalLevelUps)
        assertEquals(1, engine.state.value.ownedLair("kobold_warren").count)
    }

    @Test
    fun `performLevelUp resets gold and lairs but grants a gem batch and increments totalLevelUps`() {
        // 150 * sqrt(1e15 / 1e15) = 150 gems.
        val rich = GameState(lifetimeGoldEarned = 1_000_000_000_000_000.0, lairs = emptyMap())
        engine.loadState(rich)

        val gemsEarned = engine.performLevelUp()

        assertEquals(150L, gemsEarned)
        assertEquals(0.0, engine.state.value.goldPieces, 0.0001)
        assertEquals(1, engine.state.value.ownedLair("kobold_warren").count)
        assertEquals(150L, engine.state.value.gems)
        assertEquals(1, engine.state.value.totalLevelUps)
    }

    @Test
    fun `performLevelUp carries over platinum, permanent and temporary boosts, offline cap, the ad cooldown, and lifetime earnings`() {
        val watchedAt = Instant.now()
        val activeBoost = ActiveTemporaryBoost(TemporaryBoostCategory.SPEED, 50.0, watchedAt.plusSeconds(300))
        engine.loadState(
            GameState(
                lifetimeGoldEarned = 1_000_000_000_000_000.0,
                lairs = emptyMap(),
                platinumPieces = 42.0,
                permanentSpeedBoost2xLevel = 3,
                permanentProfitBoost2xLevel = 5,
                permanentGemBoost15xLevel = 2,
                activeTemporaryBoosts = listOf(activeBoost),
                offlineCapHours = 8.0,
                lastPlatinumAdWatchedAt = watchedAt,
                speedBoostAdWatchTimestamps = listOf(watchedAt),
            ),
        )

        engine.performLevelUp()

        assertEquals(42.0, engine.state.value.platinumPieces, 0.0001)
        assertEquals(3, engine.state.value.permanentSpeedBoost2xLevel)
        assertEquals(5, engine.state.value.permanentProfitBoost2xLevel)
        assertEquals(2, engine.state.value.permanentGemBoost15xLevel)
        assertEquals(listOf(activeBoost), engine.state.value.activeTemporaryBoosts)
        assertEquals(8.0, engine.state.value.offlineCapHours, 0.0001)
        assertEquals(watchedAt, engine.state.value.lastPlatinumAdWatchedAt)
        assertEquals(listOf(watchedAt), engine.state.value.speedBoostAdWatchTimestamps)
        assertEquals(1_000_000_000_000_000.0, engine.state.value.lifetimeGoldEarned, 0.0001)
    }

    @Test
    fun `performLevelUp replaces the old gem batch rather than accumulating`() {
        // Gems are temporary (see LevelUp.kt) — a new batch (150, from this
        // lifetime earnings) overwrites whatever was already held (10),
        // rather than adding to it.
        engine.loadState(GameState(lifetimeGoldEarned = 1_000_000_000_000_000.0, lairs = emptyMap(), gems = 10L, totalLevelUps = 1))

        val gemsEarned = engine.performLevelUp()

        assertEquals(150L, gemsEarned)
        assertEquals(150L, engine.state.value.gems)
    }

    @Test
    fun `performLevelUp grants the same batch again if lifetime earnings haven't grown`() {
        // Unlike a typical accumulating prestige currency, repeating a
        // Level Up with no new lifetime earnings isn't blocked — it just
        // regrants the identical batch (replacing, not adding), so nothing
        // is gained or lost by doing it again.
        engine.loadState(GameState(lifetimeGoldEarned = 1_000_000_000_000_000.0, lairs = emptyMap()))
        val firstGemsEarned = engine.performLevelUp()
        assertEquals(150L, firstGemsEarned)

        val secondGemsEarned = engine.performLevelUp()

        assertEquals(150L, secondGemsEarned)
        assertEquals(150L, engine.state.value.gems)
        assertEquals(2, engine.state.value.totalLevelUps)
    }

    @Test
    fun `performLevelUp's batch grows as lifetime earnings grow, replacing the smaller old one`() {
        engine.loadState(GameState(lifetimeGoldEarned = 1_000_000_000_000_000.0, lairs = emptyMap()))
        engine.performLevelUp()
        assertEquals(150L, engine.state.value.gems)

        // Growing lifetime earnings 4x (150 * sqrt(4) = 300) replaces the
        // old 150-gem batch with a fresh 300-gem one, not 150 + 300.
        engine.loadState(engine.state.value.copy(lifetimeGoldEarned = 4_000_000_000_000_000.0))
        val secondGemsEarned = engine.performLevelUp()

        assertEquals(300L, secondGemsEarned)
        assertEquals(300L, engine.state.value.gems)
        assertEquals(2, engine.state.value.totalLevelUps)
    }

    @Test
    fun `gems currently held boost income`() {
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

    @Test
    fun `purchaseGpLairUpgrade deducts gold and increments that lair's own level`() {
        val cost = GpUpgrades.costForLairTier("kobold_warren", UpgradeCategory.PROFIT, 1)
        engine.loadState(GameState(goldPieces = cost, lairs = mapOf("kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1))))

        val bought = engine.purchaseGpLairUpgrade("kobold_warren", UpgradeCategory.PROFIT)

        assertTrue(bought)
        assertEquals(0.0, engine.state.value.goldPieces, 0.0001)
        assertEquals(1, engine.state.value.ownedLair("kobold_warren").profitUpgradeLevel)
        assertEquals(0, engine.state.value.ownedLair("kobold_warren").speedUpgradeLevel)
    }

    @Test
    fun `purchaseGpLairUpgrade fails for a lair that isn't owned`() {
        engine.loadState(GameState(goldPieces = 1_000_000_000.0, lairs = emptyMap()))

        val bought = engine.purchaseGpLairUpgrade("goblin_camp", UpgradeCategory.PROFIT)

        assertFalse(bought)
    }

    @Test
    fun `purchaseGpLairUpgrade fails once a line is already at its max tier`() {
        val maxLevel = GpUpgrades.LAIR_LINE_PHASES.totalTiers
        engine.loadState(
            GameState(
                goldPieces = Double.MAX_VALUE / 2,
                lairs = mapOf(
                    "kobold_warren" to OwnedLair(
                        lairId = "kobold_warren",
                        count = 1,
                        profitUpgradeLevel = maxLevel,
                    ),
                ),
            ),
        )

        val bought = engine.purchaseGpLairUpgrade("kobold_warren", UpgradeCategory.PROFIT)

        assertFalse(bought)
        assertEquals(maxLevel, engine.state.value.ownedLair("kobold_warren").profitUpgradeLevel)
    }

    @Test
    fun `purchaseGpLairUpgrade fails when gold is insufficient`() {
        val cost = GpUpgrades.costForLairTier("kobold_warren", UpgradeCategory.SPEED, 1)
        engine.loadState(
            GameState(
                goldPieces = cost - 0.01,
                lairs = mapOf("kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1)),
            ),
        )

        val bought = engine.purchaseGpLairUpgrade("kobold_warren", UpgradeCategory.SPEED)

        assertFalse(bought)
        assertEquals(0, engine.state.value.ownedLair("kobold_warren").speedUpgradeLevel)
    }

    @Test
    fun `a lair's Profit upgrade level boosts only that lair's income`() {
        val lair = CreatureLairCatalog.get("kobold_warren")
        val cost = GpUpgrades.costForLairTier("kobold_warren", UpgradeCategory.PROFIT, 1)
        engine.loadState(
            GameState(goldPieces = lair.baseCostGp + cost, lairs = emptyMap()),
        )
        engine.purchaseLair("kobold_warren")
        engine.purchaseGpLairUpgrade("kobold_warren", UpgradeCategory.PROFIT)
        engine.startLairLoad("kobold_warren")

        engine.tick(lair.baseProductionSeconds)

        assertEquals(
            lair.incomePerCycle(1, upgradeProfitMultiplier = GpUpgrades.lairProfitMultiplier(1)),
            engine.state.value.goldPieces,
            0.0001,
        )
    }

    @Test
    fun `purchaseGpEverythingUpgrade deducts gold and increments the account-wide level`() {
        val cost = GpUpgrades.costForEverythingTier(UpgradeCategory.SPEED, 1)
        engine.loadState(GameState(goldPieces = cost))

        val bought = engine.purchaseGpEverythingUpgrade(UpgradeCategory.SPEED)

        assertTrue(bought)
        assertEquals(0.0, engine.state.value.goldPieces, 0.0001)
        assertEquals(1, engine.state.value.everythingSpeedUpgradeLevel)
        assertEquals(0, engine.state.value.everythingProfitUpgradeLevel)
    }

    @Test
    fun `purchaseGpEverythingUpgrade fails once already at max tier`() {
        val maxLevel = GpUpgrades.EVERYTHING_PROFIT_PHASES.totalTiers
        engine.loadState(GameState(goldPieces = Double.MAX_VALUE / 2, everythingProfitUpgradeLevel = maxLevel))

        val bought = engine.purchaseGpEverythingUpgrade(UpgradeCategory.PROFIT)

        assertFalse(bought)
        assertEquals(maxLevel, engine.state.value.everythingProfitUpgradeLevel)
    }

    @Test
    fun `purchaseGemEfficiencyUpgrade deducts gems and increments the level`() {
        val cost = GemUpgrades.costForTierGems(1)
        engine.loadState(GameState(gems = cost))

        val bought = engine.purchaseGemEfficiencyUpgrade()

        assertTrue(bought)
        assertEquals(0L, engine.state.value.gems)
        assertEquals(1, engine.state.value.gemEfficiencyLevel)
    }

    @Test
    fun `purchaseGemEfficiencyUpgrade fails when gems are insufficient`() {
        val cost = GemUpgrades.costForTierGems(1)
        engine.loadState(GameState(gems = cost - 1))

        val bought = engine.purchaseGemEfficiencyUpgrade()

        assertFalse(bought)
        assertEquals(0, engine.state.value.gemEfficiencyLevel)
    }

    @Test
    fun `purchaseGemEfficiencyUpgrade fails once already at max tier`() {
        val maxLevel = GemUpgrades.PHASES.totalTiers
        engine.loadState(GameState(gems = Long.MAX_VALUE / 2, gemEfficiencyLevel = maxLevel))

        val bought = engine.purchaseGemEfficiencyUpgrade()

        assertFalse(bought)
        assertEquals(maxLevel, engine.state.value.gemEfficiencyLevel)
    }

    @Test
    fun `performLevelUp resets every Gold Pieces and Gem upgrade level`() {
        engine.loadState(
            GameState(
                lifetimeGoldEarned = 1_000_000_000_000_000.0,
                lairs = mapOf(
                    "kobold_warren" to OwnedLair(
                        lairId = "kobold_warren",
                        count = 1,
                        profitUpgradeLevel = 3,
                        speedUpgradeLevel = 2,
                    ),
                ),
                everythingProfitUpgradeLevel = 5,
                everythingSpeedUpgradeLevel = 4,
                gemEfficiencyLevel = 10,
            ),
        )

        engine.performLevelUp()

        assertEquals(0, engine.state.value.everythingProfitUpgradeLevel)
        assertEquals(0, engine.state.value.everythingSpeedUpgradeLevel)
        assertEquals(0, engine.state.value.gemEfficiencyLevel)
        assertEquals(0, engine.state.value.ownedLair("kobold_warren").profitUpgradeLevel)
        assertEquals(0, engine.state.value.ownedLair("kobold_warren").speedUpgradeLevel)
    }
}
