package com.wyrmwhelp.idlehoard.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import com.wyrmwhelp.idlehoard.domain.model.AchievementGroup
import com.wyrmwhelp.idlehoard.domain.model.Achievements
import com.wyrmwhelp.idlehoard.domain.model.CategorizedAchievement
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.achievementBonusPercent
import com.wyrmwhelp.idlehoard.domain.model.completedAchievementIds
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette

/**
 * The "Achievements" section's real content — a checklist of every
 * permanent, one-time [com.wyrmwhelp.idlehoard.domain.model.Achievement]
 * in the game (`domain/model/Achievement.kt`), grouped by
 * [AchievementGroup] (with the large [AchievementGroup.PER_LAIR] group
 * further broken down by lair, same "sub-group a big flat list"
 * convention `UnlocksContent.kt` already uses). Nothing here is ever
 * purchased — an achievement's row just flips from locked/muted to
 * complete/gold the moment `Achievement.isCompleted` reads true against
 * the live [GameState], same read-only "checklist" spirit as
 * `UnlocksContent`, not the buy-a-tier interaction `UpgradesContent`/
 * `StewardsContent` use. Pure display — reads [state] passed in by the
 * caller (`MainActivity`'s `WyrmWhelpApp`), no ViewModel reference of its
 * own.
 *
 * The summary card up top surfaces the one number every other screen's
 * income math actually reads: [achievementBonusPercent] — the permanent,
 * account-wide "Achievement Bonus" percentage completed achievements
 * contribute to every lair's income, forever (see
 * `GameState.achievementIncomeMultiplier` and
 * `CreatureLair.incomePerCycle`'s `achievementBonusMultiplier` param) —
 * so the player can see at a glance both *how much* it's worth right now
 * and *how many* achievements are still open.
 */
@Composable
fun AchievementsContent(
    state: GameState,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    val completedIds = state.completedAchievementIds()
    val bonusPercent = state.achievementBonusPercent()
    val totalCount = Achievements.ALL.size

    val perLairByLair: Map<String, List<CategorizedAchievement>> = Achievements.ALL
        .filter { it.group == AchievementGroup.PER_LAIR }
        .groupBy { it.achievement.id.removePrefix("own_").substringBeforeLast('_') }
    val nonLairGroups = listOf(
        AchievementGroup.PRESTIGE,
        AchievementGroup.NET_WORTH,
        AchievementGroup.STEWARDS,
        AchievementGroup.UPGRADES,
        AchievementGroup.PLATINUM,
        AchievementGroup.MISC,
    )

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SummaryCard(
                bonusPercent = bonusPercent,
                completedCount = completedIds.size,
                totalCount = totalCount,
                palette = palette,
            )
        }
        nonLairGroups.forEach { group ->
            val items = Achievements.ALL.filter { it.group == group }
            if (items.isNotEmpty()) {
                item { GroupHeader(title = group.label, palette = palette) }
                items.forEach { item ->
                    item {
                        AchievementRow(
                            item = item,
                            completed = item.achievement.id in completedIds,
                            palette = palette,
                        )
                    }
                }
            }
        }
        item { GroupHeader(title = AchievementGroup.PER_LAIR.label, palette = palette) }
        CreatureLairCatalog.lairs.forEach { lair ->
            val items = perLairByLair[lair.id].orEmpty()
            if (items.isNotEmpty()) {
                item { SubGroupHeader(title = lair.name, palette = palette) }
                items.forEach { item ->
                    item {
                        AchievementRow(
                            item = item,
                            completed = item.achievement.id in completedIds,
                            palette = palette,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(bonusPercent: Double, completedCount: Int, totalCount: Int, palette: FantasyPalette, modifier: Modifier = Modifier) {
    ParchmentCard(palette = palette, modifier = modifier, borderColor = palette.goldDeep.copy(alpha = 0.8f)) {
        Text(
            text = "Achievement Bonus: +${formatPercent(bonusPercent)}%",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        Text(
            text = "$completedCount / $totalCount completed — a permanent boost to every lair's income, forever, unlike any Gold or Gem upgrade.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.ink.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun GroupHeader(title: String, palette: FantasyPalette, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.padding(top = 6.dp, bottom = 2.dp),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Serif, color = palette.ink),
    )
}

@Composable
private fun SubGroupHeader(title: String, palette: FantasyPalette, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge.copy(color = palette.ink.copy(alpha = 0.85f)),
    )
}

/** One achievement — locked (muted, no border tint) or complete (gold border, full ink, gold bonus badge). Never tappable — nothing here is ever purchased. */
@Composable
private fun AchievementRow(item: CategorizedAchievement, completed: Boolean, palette: FantasyPalette, modifier: Modifier = Modifier) {
    val achievement = item.achievement
    val contentAlpha = if (completed) 1f else 0.55f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(palette.parchmentShade.copy(alpha = 0.8f), palette.parchment.copy(alpha = 0.8f)),
                ),
            )
            .border(
                1.5.dp,
                if (completed) palette.goldDeep.copy(alpha = 0.7f) else palette.woodDark.copy(alpha = 0.25f),
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = achievement.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium.copy(color = palette.ink.copy(alpha = contentAlpha)),
            )
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = contentAlpha * 0.7f),
            )
        }
        Spacer(Modifier.height(0.dp))
        Text(
            text = "+${formatPercent(achievement.bonusPercent)}%",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            color = if (completed) palette.goldDeep else palette.ink.copy(alpha = 0.35f),
        )
    }
}

/** A translucent parchment card matching `LairCard`/`StewardsContent`/`ShopContent`'s base treatment — duplicated per this project's established per-file-duplication convention for small private UI helpers. */
@Composable
private fun ParchmentCard(
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
    borderColor: Color = palette.woodDark.copy(alpha = 0.5f),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(palette.parchmentShade.copy(alpha = 0.8f), palette.parchment.copy(alpha = 0.8f)),
                ),
            )
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(12.dp),
        content = content,
    )
}

/** "0.25", "0.5", "1", "5" — trims a whole-number percent to no decimal, keeps a fractional one as-is rather than forcing "1.00"/"0.25000...". */
private fun formatPercent(value: Double): String =
    if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        "%.2f".format(value).trimEnd('0').trimEnd('.')
    }
