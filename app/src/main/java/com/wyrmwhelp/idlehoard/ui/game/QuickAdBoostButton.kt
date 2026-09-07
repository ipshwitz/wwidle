package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wyrmwhelp.idlehoard.R
import com.wyrmwhelp.idlehoard.domain.model.INCOME_BOOST_AD_DURATION
import com.wyrmwhelp.idlehoard.domain.model.INCOME_BOOST_AD_MAX_SLOTS
import com.wyrmwhelp.idlehoard.domain.model.INCOME_BOOST_AD_MULTIPLIER
import com.wyrmwhelp.idlehoard.domain.model.SPEED_BOOST_AD_DURATION
import com.wyrmwhelp.idlehoard.domain.model.SPEED_BOOST_AD_MAX_SLOTS
import com.wyrmwhelp.idlehoard.domain.model.SPEED_BOOST_AD_MULTIPLIER
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.format.DurationFormat
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat
import java.time.Duration

/**
 * A persistent quick-access "watch an ad" control fixed in the main game
 * screen's corner (`GameScreen`, bottom-end). Tapping it opens
 * [AdBoostPopup] (v0.34.0), which offers *both* rewarded-ad bonuses side
 * by side — a temporary Speed boost and a temporary Income boost — rather
 * than watching one specific ad directly, since this button is now the
 * *only* place either is offered (both were previously duplicated into the
 * Shop's Temporary tab; per explicit request they were pulled out of there
 * entirely so there's exactly one place to find them). Each option calls
 * straight through to `GameViewModel.watchAdForSpeedBoost`/
 * `watchAdForIncomeBoost` — this composable holds no ad-reward state of its
 * own beyond whether the popup itself is open.
 *
 * Uses the real `media_play.png` art, sized and bottom-aligned to exactly
 * match `FloatingMenu`'s chest toggle (a 72.dp touch target around a
 * 64.dp image, 24.dp up from the screen bottom) so the two read as a
 * matched pair of round icon buttons — `GameScreen` applies that matching
 * bottom padding via this composable's [modifier].
 *
 * The button's own [SlotBadge] shows the *combined* available slots across
 * both ad types (out of [SPEED_BOOST_AD_MAX_SLOTS] + [INCOME_BOOST_AD_MAX_SLOTS]
 * = 8) — the popup itself is where the exact per-type breakdown lives, so
 * this stays a simple "there's something to watch" count rather than two
 * separate badges stacked on one icon. The play icon dims when nothing at
 * all is available, same as before, but stays tappable regardless so a tap
 * during a dry spell still opens the popup and shows each option's own
 * cooldown instead of doing nothing.
 */
@Composable
fun QuickAdBoostButton(
    speedSlots: Int,
    speedCooldownRemaining: Duration,
    speedMessage: String?,
    onWatchSpeedAd: () -> Unit,
    onDismissSpeedMessage: () -> Unit,
    incomeSlots: Int,
    incomeCooldownRemaining: Duration,
    incomeMessage: String?,
    onWatchIncomeAd: () -> Unit,
    onDismissIncomeMessage: () -> Unit,
    modifier: Modifier = Modifier,
    colors: FantasyPalette = FantasyPalette.Default,
) {
    var showPopup by remember { mutableStateOf(false) }
    val totalSlots = speedSlots + incomeSlots

    Box(
        modifier = modifier.size(72.dp).clickable { showPopup = true },
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.TopEnd) {
            Image(
                painter = painterResource(R.drawable.media_play),
                contentDescription = "Watch an ad for a bonus",
                modifier = Modifier
                    .size(64.dp)
                    .alpha(if (totalSlots > 0) 1f else 0.55f),
            )
            if (totalSlots > 0) {
                SlotBadge(
                    count = totalSlots,
                    colors = colors,
                    modifier = Modifier.offset(x = 4.dp, y = (-4).dp),
                )
            }
        }
    }

    if (showPopup) {
        AdBoostPopup(
            speedSlots = speedSlots,
            speedCooldownRemaining = speedCooldownRemaining,
            speedMessage = speedMessage,
            onWatchSpeedAd = onWatchSpeedAd,
            onDismissSpeedMessage = onDismissSpeedMessage,
            incomeSlots = incomeSlots,
            incomeCooldownRemaining = incomeCooldownRemaining,
            incomeMessage = incomeMessage,
            onWatchIncomeAd = onWatchIncomeAd,
            onDismissIncomeMessage = onDismissIncomeMessage,
            onDismiss = { showPopup = false },
            colors = colors,
        )
    }
}

/**
 * The popup itself — same parchment-scroll `Dialog` chrome as
 * `WelcomeBackDialog`/`MilestoneReachedDialog` (plain `Dialog`, not
 * `AlertDialog`, `usePlatformDefaultWidth = false`), with one
 * [AdBoostOptionRow] per ad type stacked in a `Column`. Fronted once, at
 * the top, with the same `tv.png` "scrying TV" art `WelcomeBackDialog`
 * uses for its own ad prompt — `media_play.png` stays the main screen's
 * own trigger-button icon; the two [AdBoostOptionRow]s below stay
 * text-only rather than repeating the TV a second and third time.
 */
@Composable
private fun AdBoostPopup(
    speedSlots: Int,
    speedCooldownRemaining: Duration,
    speedMessage: String?,
    onWatchSpeedAd: () -> Unit,
    onDismissSpeedMessage: () -> Unit,
    incomeSlots: Int,
    incomeCooldownRemaining: Duration,
    incomeMessage: String?,
    onWatchIncomeAd: () -> Unit,
    onDismissIncomeMessage: () -> Unit,
    onDismiss: () -> Unit,
    colors: FantasyPalette,
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
                .background(Brush.verticalGradient(listOf(colors.parchmentShade, colors.parchment)))
                .border(2.dp, colors.woodDark, RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.tv),
                contentDescription = null,
                modifier = Modifier.size(140.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Watch an Ad",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, color = colors.ink),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Pick a bonus to watch for.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    color = colors.ink.copy(alpha = 0.75f),
                ),
            )
            Spacer(Modifier.height(16.dp))

            AdBoostOptionRow(
                title = "${GoldFormat.format(SPEED_BOOST_AD_MULTIPLIER)}x Speed — ${DurationFormat.format(SPEED_BOOST_AD_DURATION)}",
                description = "Stacks with itself — up to $SPEED_BOOST_AD_MAX_SLOTS at once " +
                    "($speedSlots/$SPEED_BOOST_AD_MAX_SLOTS available now, each slot free again 24h after its own watch).",
                cooldownRemaining = speedCooldownRemaining,
                onWatchAd = onWatchSpeedAd,
                colors = colors,
            )
            speedMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                AdBoostMessage(text = message, onDismiss = onDismissSpeedMessage, colors = colors)
            }

            Spacer(Modifier.height(12.dp))

            AdBoostOptionRow(
                title = "${GoldFormat.format(INCOME_BOOST_AD_MULTIPLIER)}x Income — ${DurationFormat.format(INCOME_BOOST_AD_DURATION)}",
                description = "Stacks with itself — up to $INCOME_BOOST_AD_MAX_SLOTS at once " +
                    "($incomeSlots/$INCOME_BOOST_AD_MAX_SLOTS available now, each slot free again 24h after its own watch).",
                cooldownRemaining = incomeCooldownRemaining,
                onWatchAd = onWatchIncomeAd,
                colors = colors,
            )
            incomeMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                AdBoostMessage(text = message, onDismiss = onDismissIncomeMessage, colors = colors)
            }

            Spacer(Modifier.height(16.dp))
            WoodenButton(text = "Close", onClick = onDismiss, colors = colors)
        }
    }
}

/** One ad-watch option's card — title, stacking description, and a Watch/cooldown button. */
@Composable
private fun AdBoostOptionRow(
    title: String,
    description: String,
    cooldownRemaining: Duration,
    onWatchAd: () -> Unit,
    colors: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    val available = cooldownRemaining.isZero
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(colors.woodLight.copy(alpha = 0.18f), colors.woodDark.copy(alpha = 0.1f))))
            .border(1.dp, colors.woodDark.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, color = colors.ink),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.ink.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(10.dp))
        WoodenButton(
            text = if (available) "Watch" else "In ${DurationFormat.format(cooldownRemaining)}",
            onClick = onWatchAd,
            enabled = available,
            colors = colors,
        )
    }
}

/** A small dismissible message line under an [AdBoostOptionRow] — same "✕" affordance used throughout the app. */
@Composable
private fun AdBoostMessage(text: String, onDismiss: () -> Unit, colors: FantasyPalette, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.goldDeep.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.ink,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = "✕",
            color = colors.ink.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onDismiss),
        )
    }
}

/** The small "N slots left" badge overlapping the play button's rim — combined count across both ad types. */
@Composable
private fun SlotBadge(count: Int, colors: FantasyPalette, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(colors.goldBright)
            .border(1.dp, colors.woodDark, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = count.toString(), color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}
