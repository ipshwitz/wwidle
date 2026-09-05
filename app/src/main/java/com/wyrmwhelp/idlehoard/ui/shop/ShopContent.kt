package com.wyrmwhelp.idlehoard.ui.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.R
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

/**
 * The "Shop" section's real content: the player's current Platinum Pieces
 * balance, then the two ways to earn more — watching a rewarded ad or
 * buying them outright (IAP) — both shown as disabled `WoodenButton`s with a
 * "Coming soon" note, since neither ads nor billing are wired up yet. No
 * actual shop items (what Platinum will eventually buy) are listed here —
 * none have been designed yet; this is the entry point and balance display,
 * not a stocked store. Pure display — takes `platinumPieces` passed in by
 * `MainActivity`'s `WyrmWhelpApp` (which already holds the `GameViewModel`
 * reference) rather than taking a ViewModel itself.
 */
@Composable
fun ShopContent(platinumPieces: Double, modifier: Modifier = Modifier, palette: FantasyPalette = FantasyPalette.Default) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { BalanceCard(platinumPieces = platinumPieces, palette = palette) }
        item {
            EarnMethodRow(
                title = "Watch an Ad",
                description = "Earn a few free Platinum Pieces by watching a short video.",
                palette = palette,
            )
        }
        item {
            EarnMethodRow(
                title = "Buy Platinum Pieces",
                description = "Purchase Platinum Pieces with real money.",
                palette = palette,
            )
        }
    }
}

/** A translucent parchment card matching `LairCard`/`StewardsContent`/`UnlocksContent`'s base treatment. */
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
private fun BalanceCard(platinumPieces: Double, palette: FantasyPalette, modifier: Modifier = Modifier) {
    ParchmentCard(palette = palette, modifier = modifier, borderColor = palette.goldDeep.copy(alpha = 0.8f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(
                painter = painterResource(R.drawable.coin),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Column {
                Text(
                    text = "${GoldFormat.format(platinumPieces)} pp",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = "Platinum Pieces",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/** One way to earn Platinum Pieces — currently always disabled, since neither ads nor billing exist yet. */
@Composable
private fun EarnMethodRow(title: String, description: String, palette: FantasyPalette, modifier: Modifier = Modifier) {
    ParchmentCard(palette = palette, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.7f),
                )
            }
            WoodenButton(text = "Soon", onClick = {}, enabled = false, colors = palette)
        }
    }
}
