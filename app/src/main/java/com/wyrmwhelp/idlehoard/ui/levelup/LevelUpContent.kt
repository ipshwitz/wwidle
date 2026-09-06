package com.wyrmwhelp.idlehoard.ui.levelup

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wyrmwhelp.idlehoard.domain.model.gemIncomeMultiplier
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.GlowingGoldText
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

/**
 * The "Level Up" section's real content: Level Up is this game's prestige
 * mechanic (see `domain/model/LevelUp.kt`) — reset the current run for a
 * fresh batch of Gems, a *temporary* income-multiplier head start that
 * replaces whatever Gems were already held rather than adding to them.
 * Pure display plus one callback — [onLevelUp] is only called
 * after the player confirms in [LevelUpConfirmDialog] below, and only ever
 * from an enabled button, so it doesn't need to re-check [gemsEarnable]
 * itself (mirroring `ShopContent`'s `onBuy` callbacks). [gemsEarnable] is
 * `GameState.gemsEarnedFromLevelUp()`, computed live by the caller
 * (`MainActivity`'s `WyrmWhelpApp`) from the *current* run — it updates on
 * its own as gold/lairs change while this section sits open, no polling
 * needed here, same pattern as `ShopContent`'s `WatchAdRow` cooldown label.
 */
@Composable
fun LevelUpContent(
    gems: Long,
    gemEfficiencyLevel: Int,
    gemsEarnable: Long,
    onLevelUp: () -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    var showConfirm by remember { mutableStateOf(false) }
    val canLevelUp = gemsEarnable > 0

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { IntroCard(palette = palette) }
        item { GemsBalanceCard(gems = gems, gemEfficiencyLevel = gemEfficiencyLevel, palette = palette) }
        item {
            LevelUpCard(
                gemsEarnable = gemsEarnable,
                canLevelUp = canLevelUp,
                onClick = { showConfirm = true },
                palette = palette,
            )
        }
    }

    if (showConfirm) {
        LevelUpConfirmDialog(
            gemsEarnable = gemsEarnable,
            onConfirm = {
                showConfirm = false
                onLevelUp()
            },
            onCancel = { showConfirm = false },
            palette = palette,
        )
    }
}

/** A translucent parchment card matching `LairCard`/`ShopContent`/`StewardsContent`'s base treatment. */
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
            text = "Level Up",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        Text(
            text = "Reset your Gold and every owned lair back to the start for a fresh batch of Gems — a " +
                "big but temporary income boost for your next run, replacing any Gems you're currently " +
                "holding rather than adding to them. Platinum Pieces and anything bought with it carry over.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.ink.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun GemsBalanceCard(gems: Long, gemEfficiencyLevel: Int, palette: FantasyPalette, modifier: Modifier = Modifier) {
    ParchmentCard(palette = palette, modifier = modifier, borderColor = palette.gemDeep.copy(alpha = 0.8f)) {
        GlowingGoldText(
            text = "${GoldFormat.format(gems.toDouble())} gems",
            colors = palette,
            style = MaterialTheme.typography.titleLarge,
            glowBright = palette.gemBright,
            glowDeep = palette.gemDeep,
        )
        Spacer(Modifier.height(2.dp))
        val bonusPercent = (gemIncomeMultiplier(gems, gemEfficiencyLevel) - 1.0) * 100.0
        Text(
            text = "+${GoldFormat.format(bonusPercent)}% income from every lair, until your next Level Up",
            style = MaterialTheme.typography.bodySmall,
            color = palette.ink.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun LevelUpCard(
    gemsEarnable: Long,
    canLevelUp: Boolean,
    onClick: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    ParchmentCard(palette = palette, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Level Up now",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = if (canLevelUp) {
                        "Get a fresh batch of ${GoldFormat.format(gemsEarnable.toDouble())} Gems, replacing any you're holding now."
                    } else {
                        "Keep earning Gold — Level Up unlocks again once you've earned enough more."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.7f),
                )
            }
            WoodenButton(
                text = "Level Up",
                onClick = onClick,
                enabled = canLevelUp,
                colors = palette,
            )
        }
    }
}

/**
 * A confirmation step before the irreversible reset actually happens —
 * purely local UI state in [LevelUpContent] (not driven by `GameViewModel`),
 * unlike [com.wyrmwhelp.idlehoard.ui.game.LevelUpRewardDialog], which *is*
 * ViewModel-driven since it needs to appear after the reset regardless of
 * whether this section is still open. Same parchment/wood chrome as every
 * other dialog in the app, but with two `WoodenButton`s side by side instead
 * of one, since backing out needs to be just as easy as confirming.
 */
@Composable
private fun LevelUpConfirmDialog(
    gemsEarnable: Long,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    palette: FantasyPalette,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 260.dp, max = 340.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(palette.parchmentShade, palette.parchment)))
                .border(2.dp, palette.woodDark, RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Level Up now?",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "You'll get a fresh batch of ${GoldFormat.format(gemsEarnable.toDouble())} Gems, " +
                    "replacing any you're holding now — but your Gold and every owned lair will reset.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    color = palette.ink.copy(alpha = 0.8f),
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WoodenButton(text = "Cancel", onClick = onCancel, colors = palette)
                WoodenButton(text = "Level Up!", onClick = onConfirm, colors = palette)
            }
        }
    }
}
