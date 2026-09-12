package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wyrmwhelp.idlehoard.R
import com.wyrmwhelp.idlehoard.domain.model.DAILY_REWARD_CYCLE_DAYS
import com.wyrmwhelp.idlehoard.domain.model.DailyRewardPayout
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.GlowingGoldText
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

/**
 * The Daily Reward pop-up — same cozy-fantasy `Dialog` chrome as
 * [WelcomeBackDialog]/[LevelUpRewardDialog] (plain `Dialog`, parchment
 * gradient, carved wood border, a `WoodenButton` to act), reused rather
 * than inventing a fourth dialog look. Shown automatically once per app
 * launch when a claim is available (`GameScreen`), and again any time the
 * player taps [DailyRewardButton] — in the latter case [canClaim] can be
 * false (today's reward was already claimed), which swaps the whole body
 * to a plain "come back tomorrow" notice instead of a preview/Claim button,
 * matching `QuickAdBoostButton`'s own "stays tappable regardless, just
 * shows the cooldown state" convention.
 *
 * [payout] is always [com.wyrmwhelp.idlehoard.domain.model.previewDailyRewardPayout] —
 * a pure preview computed off the live [com.wyrmwhelp.idlehoard.domain.model.GameState],
 * not the actual grant. Tapping Claim calls [onClaim] (`GameEngine.claimDailyReward`,
 * which recomputes the same math atomically off live state at that exact
 * moment) and closes immediately — the preview and the real grant can only
 * differ if a Steward-managed lair ticks a little more gold in between,
 * same negligible-edge-case shape as every other "preview now, resolve on
 * tap" flow in this app.
 *
 * A player can back out without claiming (the small "Maybe later" text) —
 * the reward isn't lost, just deferred to the next time this dialog opens
 * that same day, via [DailyRewardButton] or Settings.
 *
 * **"Watch Ad to Double"** sits alongside the plain Claim button — unlike
 * [WelcomeBackDialog], nothing is granted yet at this point, so watching
 * the ad doesn't add a second credit on top of an already-applied one; it
 * atomically claims the *doubled* payout instead
 * (`GameEngine.claimDailyReward`'s `multiplier`). No explicit "close on
 * success" wiring is needed here: once that claim lands, [canClaim] flips
 * false on the next recomposition and this same dialog naturally re-renders
 * into its own already-claimed view, same as it would after any other
 * claim.
 */
@Composable
fun DailyRewardDialog(
    canClaim: Boolean,
    payout: DailyRewardPayout,
    adUnavailableMessage: String?,
    onClaim: () -> Unit,
    onWatchAdToDouble: () -> Unit,
    onDismiss: () -> Unit,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    Dialog(
        onDismissRequest = onDismiss,
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
            Image(
                painter = painterResource(if (payout.platinumAwarded > 0.0) R.drawable.open_chest else R.drawable.coin),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Daily Reward",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Day ${payout.day} of $DAILY_REWARD_CYCLE_DAYS",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    color = palette.ink.copy(alpha = 0.75f),
                ),
            )
            Spacer(Modifier.height(14.dp))

            if (!canClaim) {
                Text(
                    text = "You've already claimed today's reward — come back tomorrow!",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(color = palette.ink),
                )
                Spacer(Modifier.height(18.dp))
                WoodenButton(text = "Close", onClick = onDismiss, colors = palette)
            } else {
                if (payout.platinumAwarded > 0.0) {
                    Text(
                        text = "You made it the whole cycle!",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium.copy(color = palette.goldDeep),
                    )
                    Spacer(Modifier.height(8.dp))
                    GlowingGoldText(text = "${GoldFormat.format(payout.platinumAwarded)} pp", colors = palette)
                } else {
                    GlowingGoldText(text = "${GoldFormat.format(payout.goldAwarded)} gp", colors = palette)
                    if (payout.gemsAwarded > 0L) {
                        Spacer(Modifier.height(6.dp))
                        GlowingGoldText(
                            text = "+ ${GoldFormat.format(payout.gemsAwarded.toDouble())} Gems!",
                            colors = palette,
                            glowBright = palette.gemBright,
                            glowDeep = palette.gemDeep,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                WoodenButton(text = "Claim", onClick = onClaim, colors = palette)
                Spacer(Modifier.height(8.dp))
                WoodenButton(text = "Watch Ad to Double", onClick = onWatchAdToDouble, colors = palette)
                adUnavailableMessage?.let { message ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic,
                            color = palette.ink.copy(alpha = 0.7f),
                        ),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Maybe later",
                    style = MaterialTheme.typography.bodySmall.copy(color = palette.ink.copy(alpha = 0.6f)),
                    modifier = Modifier.clickable(onClick = onDismiss),
                )
            }
        }
    }
}
