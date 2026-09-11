package com.wyrmwhelp.idlehoard.ui.stewards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.wyrmwhelp.idlehoard.domain.model.StewardEfficiency
import com.wyrmwhelp.idlehoard.domain.model.UNIVERSAL_STEWARD_AD_THRESHOLD
import com.wyrmwhelp.idlehoard.domain.model.adsWatchedTowardUniversalSteward
import com.wyrmwhelp.idlehoard.domain.model.hasUniversalSteward
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
 * A [UniversalStewardCard] always leads the list — before the intro card,
 * before any per-lair row — showing progress toward, or the unlocked state
 * of, the account-wide Universal Steward (`domain/model/UniversalSteward.kt`).
 * Once earned, every owned lair's row shows the same "Steward Hired" badge a
 * real per-lair hire would, with no Hire button — there's no reason to spend
 * gold on a redundant hire once every owned lair already auto-collects.
 *
 * Below that, only lists lairs the player actually owns (hiring a Steward
 * for a lair with zero units doesn't mean anything); a save with nothing
 * owned yet shows a short placeholder instead of the whole list. Each
 * owned lair's [StewardRow] also inlines that lair's own Steward
 * Efficiency line (`domain/model/StewardEfficiency.kt`) once its Steward
 * is actually hired — moved here from the Upgrades screen in v0.44.1, per
 * explicit request, so it can be bought right alongside the Steward it
 * upgrades instead of on a separate screen. Pure display plus callbacks —
 * reads [state]/[lairs] passed in by the caller (`MainActivity`'s
 * `WyrmWhelpApp`, which already has the `GameViewModel`) and forwards
 * purchases through [onHireSteward]/[onBuyStewardEfficiency] rather than
 * calling the ViewModel itself.
 */
@Composable
fun StewardsContent(
    lairs: List<CreatureLair>,
    state: GameState,
    onHireSteward: (String) -> Unit,
    onBuyStewardEfficiency: (String) -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    val ownedLairs = lairs.filter { state.ownedLair(it.id).count > 0 }
    val hasUniversalSteward = state.hasUniversalSteward()

    if (ownedLairs.isEmpty()) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            UniversalStewardCard(
                adsWatched = state.adsWatchedTowardUniversalSteward(),
                unlocked = hasUniversalSteward,
                palette = palette,
            )
            Text(
                text = "Claim a lair first — a Steward can only be hired for a lair you already own.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            UniversalStewardCard(
                adsWatched = state.adsWatchedTowardUniversalSteward(),
                unlocked = hasUniversalSteward,
                palette = palette,
            )
        }
        item {
            IntroCard(palette = palette)
            Spacer(Modifier.height(4.dp))
        }
        items(ownedLairs, key = { it.id }) { lair ->
            StewardRow(
                lair = lair,
                owned = state.ownedLair(lair.id),
                goldPieces = state.goldPieces,
                hasUniversalSteward = hasUniversalSteward,
                onHire = { onHireSteward(lair.id) },
                onBuyStewardEfficiency = { onBuyStewardEfficiency(lair.id) },
                palette = palette,
            )
        }
    }
}

/**
 * Progress toward, or the earned state of, the account-wide Universal
 * Steward — see `domain/model/UniversalSteward.kt`. Always the first thing
 * shown in this section, unlocked or not, since it's account-wide rather
 * than tied to any one lair the way the rows below it are.
 */
@Composable
private fun UniversalStewardCard(adsWatched: Int, unlocked: Boolean, palette: FantasyPalette, modifier: Modifier = Modifier) {
    ParchmentCard(palette = palette, modifier = modifier, borderColor = palette.goldDeep.copy(alpha = 0.7f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Universal Steward",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
            )
            if (unlocked) {
                Text(
                    text = "Active",
                    fontWeight = FontWeight.Bold,
                    color = palette.goldDeep,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (unlocked) {
            Text(
                text = "Every lair you own auto-collects on its own, forever — even through a Level Up.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.8f),
            )
        } else {
            Text(
                text = "Watch $UNIVERSAL_STEWARD_AD_THRESHOLD rewarded ads in total (any kind counts) to permanently " +
                    "auto-staff every lair you own, forever — no more per-lair Steward costs.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(6.dp))
            AdsWatchedProgressBar(adsWatched = adsWatched, palette = palette)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$adsWatched / $UNIVERSAL_STEWARD_AD_THRESHOLD ads watched",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.7f),
            )
        }
    }
}

/** A plain static fill bar — this progress only ever moves once an ad is watched, so it doesn't need `LairCard`'s live-tick animation. */
@Composable
private fun AdsWatchedProgressBar(adsWatched: Int, palette: FantasyPalette, modifier: Modifier = Modifier) {
    val fraction = (adsWatched.toFloat() / UNIVERSAL_STEWARD_AD_THRESHOLD).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(palette.woodDark.copy(alpha = 0.25f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Brush.horizontalGradient(listOf(palette.goldDeep, palette.goldBright))),
        )
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

/**
 * One owned lair's Steward status: hired (a simple badge) or a
 * `WoodenButton` to hire one. [hasUniversalSteward] short-circuits straight
 * to the same "Steward Hired" badge regardless of [OwnedLair.hasSteward] —
 * once the account-wide Universal Steward covers every owned lair, there's
 * no Hire button left to show, real or otherwise.
 *
 * Once [OwnedLair.hasSteward] is actually true (the account-wide Universal
 * Steward does *not* count — see `domain/model/StewardEfficiency.kt`'s
 * class doc for why that's deliberate), a second [StewardEfficiencyRow]
 * renders below the hire status for that same lair's own Steward
 * Efficiency line — moved here from the Upgrades screen in v0.44.1 so it
 * sits right next to the Steward it upgrades.
 *
 * A real per-lair hire also replaces the row's own title — normally
 * [CreatureLair.name] — with [OwnedLair.stewardName]
 * (`domain/model/StewardNames.kt`), pushing the lair's own name down into
 * the subtitle alongside the owned count instead; flavor only, no effect
 * on anything. The Universal-Steward-only case keeps the lair name as the
 * title, since that one never claims a specific hire to name (see that
 * file's class doc) — there's no name to promote.
 */
@Composable
private fun StewardRow(
    lair: CreatureLair,
    owned: OwnedLair,
    goldPieces: Double,
    hasUniversalSteward: Boolean,
    onHire: () -> Unit,
    onBuyStewardEfficiency: () -> Unit,
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
                    text = owned.stewardName ?: lair.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = if (owned.stewardName != null) "${lair.name} — Owned: ${owned.count}" else "Owned: ${owned.count}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.7f),
                )
            }
            if (owned.hasSteward || hasUniversalSteward) {
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
        if (owned.hasSteward) {
            Spacer(Modifier.height(8.dp))
            StewardEfficiencyRow(
                lair = lair,
                owned = owned,
                goldPieces = goldPieces,
                onBuy = onBuyStewardEfficiency,
                palette = palette,
            )
        }
    }
}

/**
 * This lair's own Steward Efficiency line (`domain/model/StewardEfficiency.kt`)
 * — only ever rendered by [StewardRow] once [OwnedLair.hasSteward] is
 * true, since that real per-lair Steward is exactly what this line
 * requires to unlock at all. Same shape as `UpgradesContent.kt`'s private
 * `UpgradeLineRow`, duplicated here per this project's established
 * per-file-duplication convention for small private UI helpers rather
 * than shared across files.
 */
@Composable
private fun StewardEfficiencyRow(
    lair: CreatureLair,
    owned: OwnedLair,
    goldPieces: Double,
    onBuy: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    val maxed = owned.stewardEfficiencyLevel >= StewardEfficiency.MAX_TIER
    val nextTier = owned.stewardEfficiencyLevel + 1
    val cost = if (!maxed) StewardEfficiency.costForTier(lair, nextTier) else 0.0
    val currentDiscountPercent = if (owned.stewardEfficiencyLevel <= 0) {
        0.0
    } else {
        StewardEfficiency.DISCOUNT_PERCENTAGES[(owned.stewardEfficiencyLevel - 1).coerceAtMost(StewardEfficiency.MAX_TIER - 1)]
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Steward Efficiency — Lv ${owned.stewardEfficiencyLevel}/${StewardEfficiency.MAX_TIER}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif, color = palette.ink),
            )
            Text(
                text = "${GoldFormat.format(currentDiscountPercent)}% off this lair's own cost",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.7f),
            )
        }
        WoodenButton(
            text = if (maxed) "Maxed" else "Buy — ${GoldFormat.format(cost)} gp",
            onClick = onBuy,
            enabled = !maxed && goldPieces >= cost,
            colors = palette,
        )
    }
}
