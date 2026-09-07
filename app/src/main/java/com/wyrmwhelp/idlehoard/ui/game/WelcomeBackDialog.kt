package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wyrmwhelp.idlehoard.R
import com.wyrmwhelp.idlehoard.domain.engine.OfflineEarnings
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.GlowingGoldText
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat
import kotlin.math.roundToLong

/**
 * The "while you were away" offline-earnings pop-up, restyled to match the
 * app's cozy-fantasy chrome instead of a plain Material `AlertDialog`: a
 * parchment scroll with a carved wood border, [GlowingGoldText] for the
 * amount earned, and `WoodenButton`s instead of Material buttons. Plain
 * `Dialog` (not `AlertDialog`) since none of `AlertDialog`'s built-in
 * title/text/button slots would let this look like anything other than a
 * Material dialog — `usePlatformDefaultWidth = false` hands control of
 * sizing to the content itself.
 *
 * Split top/bottom (v0.32.0, per explicit request — "redesign... so the
 * bottom half has the new tv graphic"): the top half is the earnings
 * recap (`open_chest` art, the title, [GlowingGoldText], the away-time
 * line); the bottom half is the app's first rewarded-ad placement,
 * fronted by the new `tv.png` art (a hand-illustrated magical "scrying
 * TV," real transparent background, square — a much more legible "watch
 * something" cue than a generic button ever was) instead of reusing the
 * chest icon a second time. While [isDoubled] is false, a flavor line
 * plus a "Watch Ad to Double" button sit under the TV (only one watch is
 * allowed per pop-up — [isDoubled] flips true once the reward actually
 * lands, not just on tapping the button, since `AdManager` only calls
 * back on a completed watch); once doubled, the TV stays put (removing it
 * would make the bottom half flicker empty right as the reward lands) but
 * the button/flavor line are replaced by a short confirmation line.
 * [adUnavailableMessage] surfaces the one real failure mode worth telling
 * the player about — no ad loaded yet — rather than the button silently
 * doing nothing.
 */
@Composable
fun WelcomeBackDialog(
    earnings: OfflineEarnings,
    isDoubled: Boolean,
    adUnavailableMessage: String?,
    onWatchAd: () -> Unit,
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
            // Top half: the earnings recap.
            Image(
                painter = painterResource(R.drawable.open_chest),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "While You Were Away…",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            GlowingGoldText(text = "${GoldFormat.format(earnings.goldEarned)} gp", colors = palette)
            Spacer(Modifier.height(4.dp))
            val minutesAway = (earnings.cappedSeconds / 60.0).roundToLong()
            Text(
                text = "earned over the last $minutesAway minute(s)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    color = palette.ink.copy(alpha = 0.75f),
                ),
            )

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = palette.woodDark.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(0.7f),
            )
            Spacer(Modifier.height(14.dp))

            // Bottom half: the rewarded-ad prompt, fronted by the scrying TV.
            Image(
                painter = painterResource(R.drawable.tv),
                contentDescription = null,
                modifier = Modifier.size(140.dp),
            )
            Spacer(Modifier.height(6.dp))
            if (!isDoubled) {
                Text(
                    text = "Tune in to double your haul!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        color = palette.ink.copy(alpha = 0.8f),
                    ),
                )
                Spacer(Modifier.height(10.dp))
                WoodenButton(text = "Watch Ad to Double", onClick = onWatchAd, colors = palette)
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
                Spacer(Modifier.height(8.dp))
            } else {
                Text(
                    text = "Broadcast complete — earnings doubled!",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(color = palette.goldDeep),
                )
                Spacer(Modifier.height(14.dp))
            }
            WoodenButton(text = "Claim", onClick = onDismiss, colors = palette)
        }
    }
}
