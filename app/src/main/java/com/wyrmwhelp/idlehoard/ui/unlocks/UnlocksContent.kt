package com.wyrmwhelp.idlehoard.ui.unlocks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.MILESTONE_STEPS
import com.wyrmwhelp.idlehoard.domain.model.MilestoneStep
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

/**
 * The "Unlocks" section's real content: one row per milestone rung actually
 * reached — not a compressed "current bonus" summary per lair. Owning 50
 * Kobold Warrens shows *two* rows (the 25 rung and the 50 rung), each
 * naming the specific bonus that rung grants, the same way the "Everything"
 * ladder gets one row per global rung reached rather than a single status
 * card. Nothing here is a preview of milestones still ahead; a rung simply
 * doesn't appear until it's actually been crossed. Pure display — reads
 * [state]/[lairs] passed in by the caller (`MainActivity`'s `WyrmWhelpApp`,
 * which already has the `GameViewModel`), no ViewModel reference of its own.
 */
@Composable
fun UnlocksContent(lairs: List<CreatureLair>, state: GameState, modifier: Modifier = Modifier) {
    val everythingRungs = if (lairs.isEmpty()) {
        emptyList()
    } else {
        val minOwned = lairs.minOf { state.ownedLair(it.id).count }
        MILESTONE_STEPS.filter { minOwned >= it.threshold }
    }
    val lairRungs = lairs.flatMap { lair ->
        val owned = state.ownedLair(lair.id).count
        MILESTONE_STEPS.filter { owned >= it.threshold }.map { lair to it }
    }

    if (everythingRungs.isEmpty() && lairRungs.isEmpty()) {
        Text(
            text = "No milestones unlocked yet — keep growing your hoard!",
            style = MaterialTheme.typography.bodyLarge,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(everythingRungs, key = { "everything-${it.threshold}" }) { rung ->
            EverythingUnlockRow(rung = rung)
        }
        items(lairRungs, key = { (lair, rung) -> "${lair.id}-${rung.threshold}" }) { (lair, rung) ->
            LairUnlockRow(lair = lair, rung = rung)
        }
    }
}

/** What a milestone rung's multiplier actually does, in player-facing words. */
private fun MilestoneStep.description(): String =
    if (multiplier == 2.0) "Profit Speed Doubled" else "Profit x${GoldFormat.format(multiplier)}"

/** A translucent parchment card matching `LairCard`/`StewardsContent`'s base treatment. */
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

/** One "Everything" milestone rung reached — every lair had to catch up to [rung]'s threshold for this to appear. */
@Composable
private fun EverythingUnlockRow(rung: MilestoneStep, palette: FantasyPalette = FantasyPalette.Default) {
    ParchmentCard(palette = palette, borderColor = palette.goldDeep.copy(alpha = 0.8f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Everything",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = "${rung.threshold} owned of every lair",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.7f),
                )
            }
            Text(text = rung.description(), fontWeight = FontWeight.Bold, color = palette.goldDeep)
        }
    }
}

/** One lair's own milestone rung reached. */
@Composable
private fun LairUnlockRow(lair: CreatureLair, rung: MilestoneStep, palette: FantasyPalette = FantasyPalette.Default) {
    ParchmentCard(palette = palette) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = lair.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = "${rung.threshold} owned",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.7f),
                )
            }
            Text(text = rung.description(), fontWeight = FontWeight.Bold, color = palette.goldDeep)
        }
    }
}
