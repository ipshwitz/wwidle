package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

@Composable
fun LairCard(
    lair: CreatureLair,
    owned: OwnedLair,
    goldPieces: Double,
    onClaim: () -> Unit,
    onHireSteward: () -> Unit,
    onPlunder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val claimCost = lair.costForNextUnit(owned.count)
    val canClaim = goldPieces >= claimCost
    val canHireSteward = owned.count > 0 && !owned.hasSteward && goldPieces >= lair.stewardCostGp
    val progress = if (lair.baseProductionSeconds <= 0.0) {
        0f
    } else {
        (owned.cycleProgressSeconds / lair.baseProductionSeconds).toFloat().coerceIn(0f, 1f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = owned.isReadyToCollect, onClick = onPlunder),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = lair.name, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${lair.monster} • CR ${lair.challengeRating}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(text = "Owned: ${owned.count}", style = MaterialTheme.typography.bodyMedium)
            }

            if (owned.count > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (owned.isReadyToCollect) {
                            "Ready — tap to plunder!"
                        } else if (owned.hasSteward) {
                            "The Steward is collecting automatically"
                        } else {
                            "Producing…"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "${GoldFormat.format(lair.incomePerCycle(owned.count))} gp/cycle",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                LinearProgressIndicator(
                    progress = { if (owned.isReadyToCollect) 1f else progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onClaim, enabled = canClaim) {
                    Text(
                        if (owned.count == 0) {
                            "Claim — ${GoldFormat.format(claimCost)} gp"
                        } else {
                            "+1 — ${GoldFormat.format(claimCost)} gp"
                        },
                    )
                }
                if (owned.count > 0 && !owned.hasSteward) {
                    OutlinedButton(onClick = onHireSteward, enabled = canHireSteward) {
                        Text("Steward — ${GoldFormat.format(lair.stewardCostGp)} gp")
                    }
                }
            }
        }
    }
}
