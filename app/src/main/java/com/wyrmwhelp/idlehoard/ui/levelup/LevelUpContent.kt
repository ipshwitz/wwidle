package com.wyrmwhelp.idlehoard.ui.levelup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wyrmwhelp.idlehoard.R
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
 *
 * **Progress bar (v0.35.0)** — added per explicit feedback that a
 * brand-new player logging in had no visible sense of how close they
 * were to their first Level Up, just a disabled button and vague "keep
 * earning" text; that risked reading as the feature being broken or
 * stalled rather than just early. [rawGemsProgress]/[minGemsRequired]
 * (`GameState.rawGemsFromLevelUpFormula()`/`minGemsForLevelUp()`) feed
 * [LevelUpCard]'s bar — deliberately a *linear* fill toward the gem
 * minimum (not the underlying gold formula, which is square-root-scaled
 * and would visually crawl in the same discouraging way even while
 * real progress is being made) — hidden once eligible, since the button
 * itself is the "you're done" signal at that point.
 *
 * **Fixes in v0.36.0, per explicit follow-up feedback:**
 * - **Real "eligible" gate.** `gemsEarnable > 0` alone let a player Level
 *   Up over and over with no new progress between taps — Gems replace
 *   rather than accumulate, so a repeat tap right after a Level Up would
 *   still report the same already-cleared-the-minimum batch and happily
 *   wipe the fresh run's Gold/lairs for a batch no bigger than what was
 *   already held. `canLevelUp` here is now `gemsEarnable > gems` (mirrors
 *   `GameState.canLevelUp()` — see `domain/model/LevelUp.kt`), so a
 *   repeat Level Up only unlocks again once genuinely new lifetime
 *   earnings have pushed the formula's result past what's already
 *   banked. [LevelUpCard]'s progress-bar target follows the same rule:
 *   once at least one Gem is already held, the bar tracks toward
 *   `gems + 1` (whichever is larger, that or [minGemsRequired]) instead
 *   of the flat minimum, so it doesn't render full while the button
 *   stays disabled.
 * - **`gems.png` art** (real transparent background, 754x754 — see
 *   Assets in CLAUDE.md) — used by [GemsBalanceCard] next to the Gems
 *   total (mirroring `GameHeader`'s `coin.png` treatment) and by the new
 *   [EarningCounterCard].
 * - **A dedicated "currently earning" counter** ([EarningCounterCard]) —
 *   shows [rawGemsProgress] live, separate from the balance card and the
 *   progress bar (which only appears while blocked): the point is to let
 *   a player watch this number visibly climb over a session, especially
 *   once income is high enough (after a handful of Level Ups) that it
 *   moves noticeably instead of sitting still for a long time.
 * - **Closing this section on a successful Level Up** is handled by the
 *   caller, not here — see `MainActivity`'s `WyrmWhelpApp`, which watches
 *   `GameViewModel.levelUpReward` and clears `openSection` the moment a
 *   Level Up actually goes through, so the player lands back on the main
 *   game screen (where [com.wyrmwhelp.idlehoard.ui.game.LevelUpRewardDialog]
 *   then pops up) instead of staying parked on this now-reset section.
 */
@Composable
fun LevelUpContent(
    gems: Long,
    gemEfficiencyLevel: Int,
    gemsEarnable: Long,
    rawGemsProgress: Long,
    minGemsRequired: Long,
    onLevelUp: () -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    var showConfirm by remember { mutableStateOf(false) }
    // Must beat what's already held, not just clear the minimum — see this
    // file's class doc's "Fixes in v0.36.0" paragraph and
    // `GameState.canLevelUp()` for why gemsEarnable > 0 alone isn't enough.
    val canLevelUp = gemsEarnable > gems
    // Once at least one Gem is already banked, the bar should track toward
    // beating that amount, not the (already-cleared) flat minimum — otherwise
    // it would render full while the button stays correctly disabled.
    val progressTarget = maxOf(minGemsRequired, gems + 1)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { IntroCard(palette = palette) }
        item { GemsBalanceCard(gems = gems, gemEfficiencyLevel = gemEfficiencyLevel, palette = palette) }
        item { EarningCounterCard(rawGemsProgress = rawGemsProgress, palette = palette) }
        item {
            LevelUpCard(
                gemsEarnable = gemsEarnable,
                rawGemsProgress = rawGemsProgress,
                progressTarget = progressTarget,
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(
                painter = painterResource(R.drawable.gems),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            GlowingGoldText(
                text = "${GoldFormat.format(gems.toDouble())} gems",
                colors = palette,
                style = MaterialTheme.typography.titleLarge,
                glowBright = palette.gemBright,
                glowDeep = palette.gemDeep,
            )
        }
        Spacer(Modifier.height(2.dp))
        val bonusPercent = (gemIncomeMultiplier(gems, gemEfficiencyLevel) - 1.0) * 100.0
        Text(
            text = "+${GoldFormat.format(bonusPercent)}% income from every lair, until your next Level Up",
            style = MaterialTheme.typography.bodySmall,
            color = palette.ink.copy(alpha = 0.7f),
        )
    }
}

/**
 * A live counter of [rawGemsProgress] — the Gem batch a Level Up would
 * grant *right now*, unclamped by [com.wyrmwhelp.idlehoard.domain.model.minGemsForLevelUp]
 * (see `domain/model/LevelUp.kt`'s `rawGemsFromLevelUpFormula`) — shown
 * separately from [GemsBalanceCard] (the currently-*held* batch) and the
 * progress bar (which only appears while blocked). Recomposes every tick
 * alongside the rest of this screen's `GameState`-derived params, so it
 * visibly climbs on its own with no polling here; barely moves early on,
 * but becomes noticeable once a handful of Level Ups have raised income
 * enough for lifetime earnings to climb quickly.
 */
@Composable
private fun EarningCounterCard(rawGemsProgress: Long, palette: FantasyPalette, modifier: Modifier = Modifier) {
    ParchmentCard(palette = palette, modifier = modifier, borderColor = palette.gemDeep.copy(alpha = 0.5f)) {
        Text(
            text = "Currently earning",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(
                painter = painterResource(R.drawable.gems),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "${GoldFormat.format(rawGemsProgress.toDouble())} gems, if you Leveled Up right now",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.gemDeep,
            )
        }
    }
}

@Composable
private fun LevelUpCard(
    gemsEarnable: Long,
    rawGemsProgress: Long,
    progressTarget: Long,
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
                        "Keep earning Gold — Level Up unlocks once your lifetime earnings are worth enough Gems."
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
        if (!canLevelUp) {
            Spacer(Modifier.height(10.dp))
            LevelUpProgressBar(current = rawGemsProgress, target = progressTarget, palette = palette)
        }
    }
}

/**
 * A linear progress bar toward [target] (the current
 * `GameState.minGemsForLevelUp()`) — deliberately linear against the raw
 * Gem-count formula result rather than the underlying square-root-scaled
 * gold, so the fill actually reads as steady progress instead of crawling
 * near the end the way a gold-denominated bar would. See
 * [LevelUpContent]'s "Progress bar" doc for why this exists at all.
 */
@Composable
private fun LevelUpProgressBar(current: Long, target: Long, palette: FantasyPalette, modifier: Modifier = Modifier) {
    val fraction = if (target <= 0L) 1f else (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "levelUpProgress")

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(palette.woodDark.copy(alpha = 0.3f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Brush.horizontalGradient(listOf(palette.gemDeep, palette.gemBright))),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${GoldFormat.format(current.toDouble())} / ${GoldFormat.format(target.toDouble())} Gems to Level Up",
            style = MaterialTheme.typography.bodySmall,
            color = palette.ink.copy(alpha = 0.7f),
        )
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
