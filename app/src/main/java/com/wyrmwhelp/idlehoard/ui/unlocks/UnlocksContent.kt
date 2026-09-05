package com.wyrmwhelp.idlehoard.ui.unlocks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.MILESTONE_STEPS
import com.wyrmwhelp.idlehoard.domain.model.MilestoneStep
import com.wyrmwhelp.idlehoard.domain.model.MilestoneType
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

private const val CARDS_PER_ROW = 4

/**
 * The "Unlocks" section's real content: grouped by lair (plus an
 * "Everything" group for the global ladder — see
 * `GameState.globalSpeedMilestoneMultiplier`/`globalIncomeMilestoneMultiplier`),
 * each showing a 4-cards-per-row grid of the milestone rungs actually
 * reached for that lair — not a compressed "current bonus" summary. Owning
 * 50 Kobold Warrens shows *two* cards under "Kobold Warren" (the 25 rung and
 * the 50 rung), each a compact "x25" / "2x Speed" pair rather than a
 * sentence — the label is "Speed" or "Income" per [MilestoneStep.type],
 * since rungs at or above 500 are Income, not Speed (see `Milestone.kt`).
 * Nothing here is a preview of milestones still ahead; a rung simply
 * doesn't appear until it's actually been crossed. A lair with nothing
 * unlocked yet doesn't get a group at all. Pure display — reads
 * [state]/[lairs] passed in by the caller (`MainActivity`'s `WyrmWhelpApp`,
 * which already has the `GameViewModel`), no ViewModel reference of its
 * own.
 */
@Composable
fun UnlocksContent(
    lairs: List<CreatureLair>,
    state: GameState,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    val everythingRungs = if (lairs.isEmpty()) {
        emptyList()
    } else {
        val minOwned = lairs.minOf { state.ownedLair(it.id).count }
        MILESTONE_STEPS.filter { minOwned >= it.threshold }
    }
    val lairSections = lairs.mapNotNull { lair ->
        val owned = state.ownedLair(lair.id).count
        val rungs = MILESTONE_STEPS.filter { owned >= it.threshold }
        if (rungs.isEmpty()) null else lair to rungs
    }

    if (everythingRungs.isEmpty() && lairSections.isEmpty()) {
        Text(
            text = "No milestones unlocked yet — keep growing your hoard!",
            style = MaterialTheme.typography.bodyLarge,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (everythingRungs.isNotEmpty()) {
            item { GroupHeader(title = "Everything", palette = palette) }
            everythingRungs.chunked(CARDS_PER_ROW).forEach { row ->
                item { UnlockCardRow(rungs = row, palette = palette) }
            }
        }
        lairSections.forEach { (lair, rungs) ->
            item { GroupHeader(title = lair.name, palette = palette) }
            rungs.chunked(CARDS_PER_ROW).forEach { row ->
                item { UnlockCardRow(rungs = row, palette = palette) }
            }
        }
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

/**
 * One row of up to [CARDS_PER_ROW] unlock cards. A short final row (fewer
 * than [CARDS_PER_ROW] rungs) is padded with invisible equal-weight spacers
 * rather than letting its real cards stretch to fill the row, so every card
 * — first row or last — is the same size.
 */
@Composable
private fun UnlockCardRow(rungs: List<MilestoneStep>, palette: FantasyPalette, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        rungs.forEach { rung ->
            UnlockCard(rung = rung, palette = palette, modifier = Modifier.weight(1f))
        }
        repeat(CARDS_PER_ROW - rungs.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/** One reached milestone rung, reduced to its two load-bearing numbers: the ownership count and the bonus it grants. */
@Composable
private fun UnlockCard(rung: MilestoneStep, palette: FantasyPalette, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(palette.parchmentShade.copy(alpha = 0.8f), palette.parchment.copy(alpha = 0.8f)),
                ),
            )
            .border(1.5.dp, palette.goldDeep.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "x${rung.threshold}",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        Spacer(Modifier.height(2.dp))
        val bonusLabel = if (rung.type == MilestoneType.SPEED) "Speed" else "Income"
        Text(
            text = "${GoldFormat.format(rung.multiplier)}x $bonusLabel",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = palette.goldDeep,
        )
    }
}
