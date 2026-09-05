package com.wyrmwhelp.idlehoard.ui.unlocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.globalMilestoneMultiplier
import com.wyrmwhelp.idlehoard.domain.model.milestoneMultiplierFor
import com.wyrmwhelp.idlehoard.domain.model.nextMilestoneThreshold
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

/**
 * The "Unlocks" section's real content: the "Everything" milestone status
 * up top, then every lair's own milestone progress. Pure display — reads
 * [state]/[lairs] passed in by the caller (`MainActivity`'s `WyrmWhelpApp`,
 * which already has the `GameViewModel`), no ViewModel reference of its own.
 */
@Composable
fun UnlocksContent(lairs: List<CreatureLair>, state: GameState, modifier: Modifier = Modifier) {
    val globalMultiplier = state.globalMilestoneMultiplier(lairs)
    val weakestLair = lairs.minByOrNull { state.ownedLair(it.id).count }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            EverythingMilestoneCard(
                multiplier = globalMultiplier,
                weakestLairName = weakestLair?.name,
                weakestLairOwned = weakestLair?.let { state.ownedLair(it.id).count } ?: 0,
            )
            Spacer(Modifier.height(4.dp))
        }
        items(lairs, key = { it.id }) { lair ->
            LairMilestoneRow(lair = lair, ownedCount = state.ownedLair(lair.id).count)
        }
    }
}

/**
 * The "Everything" bonus: the same milestone ladder as each lair's own
 * bonus, but keyed on whichever lair has the *fewest* owned — every lair has
 * to catch up before this advances, not just the player's favorite.
 */
@Composable
private fun EverythingMilestoneCard(
    multiplier: Double,
    weakestLairName: String?,
    weakestLairOwned: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Everything", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Every lair contributes once it reaches the same milestones as below — " +
                    "the bonus applies to every lair's production.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(text = "Current bonus: x${GoldFormat.format(multiplier)}", fontWeight = FontWeight.Bold)
            if (weakestLairName != null) {
                val next = nextMilestoneThreshold(weakestLairOwned)
                Text(
                    text = if (next != null) {
                        "Held back by $weakestLairName ($weakestLairOwned owned) — needs $next"
                    } else {
                        "Every lair has reached every milestone!"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** One lair's own milestone progress: its current bonus and how far to the next rung. */
@Composable
private fun LairMilestoneRow(lair: CreatureLair, ownedCount: Int, modifier: Modifier = Modifier) {
    val multiplier = milestoneMultiplierFor(ownedCount)
    val next = nextMilestoneThreshold(ownedCount)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = lair.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(text = "Owned: $ownedCount", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Bonus: x${GoldFormat.format(multiplier)}", fontWeight = FontWeight.Bold)
                Text(
                    text = if (next != null) {
                        "Next at $next (${next - ownedCount} more)"
                    } else {
                        "Every milestone reached"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
