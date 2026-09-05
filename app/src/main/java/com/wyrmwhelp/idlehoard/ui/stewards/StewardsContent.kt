package com.wyrmwhelp.idlehoard.ui.stewards

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
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat
import com.wyrmwhelp.idlehoard.ui.game.rarityColor

/**
 * The "Stewards" section's real content, styled with the same cozy-fantasy
 * chrome as `LairCard`/`GameHeader` ([FantasyPalette] parchment cards and a
 * `WoodenButton`) rather than the plain Material look `UnlocksContent` still
 * has — this is the reference for what that screen should probably move to
 * as well.
 *
 * Only lists lairs the player actually owns (hiring a Steward for a lair
 * with zero units doesn't mean anything); a save with nothing owned yet
 * shows a short placeholder instead. Pure display plus one callback — reads
 * [state]/[lairs] passed in by the caller (`MainActivity`'s `WyrmWhelpApp`,
 * which already has the `GameViewModel`) and forwards hires through
 * [onHireSteward] rather than calling the ViewModel itself.
 */
@Composable
fun StewardsContent(
    lairs: List<CreatureLair>,
    state: GameState,
    onHireSteward: (String) -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    val ownedLairs = lairs.filter { state.ownedLair(it.id).count > 0 }

    if (ownedLairs.isEmpty()) {
        Text(
            text = "Claim a lair first — a Steward can only be hired for a lair you already own.",
            style = MaterialTheme.typography.bodyLarge,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            IntroCard(palette = palette)
            Spacer(Modifier.height(4.dp))
        }
        items(ownedLairs, key = { it.id }) { lair ->
            StewardRow(
                lair = lair,
                owned = state.ownedLair(lair.id),
                goldPieces = state.goldPieces,
                onHire = { onHireSteward(lair.id) },
                palette = palette,
            )
        }
    }
}

/** A translucent parchment card matching `LairCard`'s base treatment, not a Material `Surface`. */
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

@Composable
private fun IntroCard(palette: FantasyPalette, modifier: Modifier = Modifier) {
    ParchmentCard(palette = palette, modifier = modifier) {
        Text(
            text = "Stewards",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        Text(
            text = "A Steward automatically collects a lair's completed cycles — online or offline — " +
                "so you never have to tap it again.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.ink.copy(alpha = 0.8f),
        )
    }
}

/** One owned lair's Steward status: hired (a simple badge) or a `WoodenButton` to hire one. */
@Composable
private fun StewardRow(
    lair: CreatureLair,
    owned: OwnedLair,
    goldPieces: Double,
    onHire: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    val rarity = rarityColor(lair.tier)
    ParchmentCard(palette = palette, modifier = modifier, borderColor = rarity.copy(alpha = 0.7f)) {
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
                    text = "Owned: ${owned.count}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.7f),
                )
            }
            if (owned.hasSteward) {
                Text(
                    text = "Steward Hired",
                    fontWeight = FontWeight.Bold,
                    color = palette.goldDeep,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                WoodenButton(
                    text = "Hire — ${GoldFormat.format(lair.stewardCostGp)} gp",
                    onClick = onHire,
                    enabled = goldPieces >= lair.stewardCostGp,
                    colors = palette,
                )
            }
        }
    }
}
