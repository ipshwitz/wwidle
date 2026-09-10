package com.wyrmwhelp.idlehoard.domain.model

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementTest {

    @Test
    fun `every achievement id is unique`() {
        val ids = Achievements.ALL.map { it.achievement.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `catalog has exactly 3 per-lair tiers for every lair, plus the global achievements`() {
        val perLairCount = Achievements.ALL.count { it.group == AchievementGroup.PER_LAIR }
        assertEquals(CreatureLairCatalog.lairs.size * 3, perLairCount)
        assertEquals(68, Achievements.ALL.size)
    }

    @Test
    fun `a brand-new save has completed no achievements and no bonus`() {
        val state = GameState()
        assertTrue(state.completedAchievementIds().isEmpty())
        assertEquals(0.0, state.achievementBonusPercent(), 0.0001)
        assertEquals(1.0, state.achievementIncomeMultiplier(), 0.0001)
    }

    @Test
    fun `a per-lair ownership achievement completes off highestLairCounts, not the live owned count`() {
        val lairId = CreatureLairCatalog.lairs.first().id
        val state = GameState(highestLairCounts = mapOf(lairId to 100))
        assertTrue(state.completedAchievementIds().contains("own_${lairId}_100"))
        assertFalse(state.completedAchievementIds().contains("own_${lairId}_1000"))
    }

    @Test
    fun `net worth and prestige achievements complete at their thresholds`() {
        val state = GameState(lifetimeGoldEarned = 1_000_000.0, totalLevelUps = 5)
        val ids = state.completedAchievementIds()
        assertTrue("net_worth_1000000" in ids)
        assertFalse("net_worth_1000000000" in ids)
        assertTrue("prestige_1" in ids)
        assertTrue("prestige_5" in ids)
        assertFalse("prestige_10" in ids)
    }

    @Test
    fun `Full Staff and Claimed Them All require every lair in the catalog, not just one`() {
        val oneLair = CreatureLairCatalog.lairs.first().id
        val partial = GameState(
            everHiredStewardForLairs = setOf(oneLair),
            highestLairCounts = mapOf(oneLair to 1),
        )
        assertTrue("steward_first" in partial.completedAchievementIds())
        assertFalse("steward_all" in partial.completedAchievementIds())
        assertFalse("claimed_all_lairs" in partial.completedAchievementIds())

        val allLairIds = CreatureLairCatalog.lairs.map { it.id }.toSet()
        val complete = GameState(
            everHiredStewardForLairs = allLairIds,
            highestLairCounts = allLairIds.associateWith { 1 },
        )
        assertTrue("steward_all" in complete.completedAchievementIds())
        assertTrue("claimed_all_lairs" in complete.completedAchievementIds())
    }

    @Test
    fun `achievementBonusPercent sums every completed achievement's bonus, and achievementIncomeMultiplier is 1 plus that as a fraction`() {
        val lairId = CreatureLairCatalog.lairs.first().id
        val state = GameState(highestLairCounts = mapOf(lairId to 100), totalLevelUps = 1)
        val expectedBonus = Achievements.ALL.first { it.achievement.id == "own_${lairId}_100" }.achievement.bonusPercent +
            Achievements.ALL.first { it.achievement.id == "prestige_1" }.achievement.bonusPercent
        assertEquals(expectedBonus, state.achievementBonusPercent(), 0.0001)
        assertEquals(1.0 + expectedBonus / 100.0, state.achievementIncomeMultiplier(), 0.0001)
    }

    @Test
    fun `everything_maxed requires both Everything lines maxed, not just one`() {
        val onlyProfit = GameState(everMaxedEverythingProfit = true)
        assertFalse("everything_maxed" in onlyProfit.completedAchievementIds())

        val both = GameState(everMaxedEverythingProfit = true, everMaxedEverythingSpeed = true)
        assertTrue("everything_maxed" in both.completedAchievementIds())
    }
}
