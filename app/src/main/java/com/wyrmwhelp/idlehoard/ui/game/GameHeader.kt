package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

/**
 * The game screen's top bar: an avatar slot on the left, the player's
 * currencies stacked in the middle, and the bulk-purchase quantity selector
 * on the right — replaces the old plain "gp" title bar.
 *
 * Layout is a rough sketch the user drew in chat:
 * ```
 * | Avatar | Total Gold Coins        | Qty Selector |
 * |        | coins earned per sec    |              |
 * |________| Premium Coins           |______________|
 * ```
 * "Premium Coins" is [platinumPieces] (`GameState.platinumPieces`, called
 * Platinum Pieces elsewhere per 5E flavor — kept that name here rather than
 * introducing a second label for the same currency).
 */
@Composable
fun GameHeader(
    goldPieces: Double,
    goldPerSecond: Double,
    platinumPieces: Double,
    buyQuantity: BuyQuantity,
    onCycleBuyQuantity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AvatarPlaceholder()

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${GoldFormat.format(goldPieces)} gp",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${GoldFormat.format(goldPerSecond)} gp/sec",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "${GoldFormat.format(platinumPieces)} pp",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        BuyQuantitySelector(quantity = buyQuantity, onClick = onCycleBuyQuantity)
    }
}

/**
 * Stand-in for the not-yet-built avatar system ("a handful of pre-created
 * avatar images they can choose from"). Plain circle, no art — same
 * placeholder-until-art-exists pattern as `FloatingMenu`'s Settings item.
 */
@Composable
private fun AvatarPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(Color.White.copy(alpha = 0.25f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/**
 * Small cyclable box for the bulk-purchase quantity — tapping advances
 * through [BuyQuantity] in order, wrapping back to x1. Plain box for now
 * ("will use an image at some point, but for now use a small box").
 */
@Composable
private fun BuyQuantitySelector(quantity: BuyQuantity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = modifier.size(width = 52.dp, height = 36.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = quantity.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
