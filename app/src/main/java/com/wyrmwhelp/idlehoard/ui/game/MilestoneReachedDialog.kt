package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.wyrmwhelp.idlehoard.domain.model.MilestoneAnnouncement
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.GlowingGoldText
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

/**
 * The milestone-reached pop-up — same cozy-fantasy chrome as
 * [WelcomeBackDialog] (plain `Dialog`, parchment-gradient card with a carved
 * wood border, `open_chest` art standing in for "a reward was just opened",
 * [GlowingGoldText] as the focal number, a `WoodenButton` to dismiss)
 * reused here for consistency rather than inventing a second dialog look.
 * [announcement] names the rung actually crossed (see
 * `GameStateExtensions.milestonesCrossed`) — [MilestoneAnnouncement.lairName]
 * is either a specific lair's name or the literal "Everything" for the
 * global ladder, matching `UnlocksContent`'s own grouping labels, and the
 * multiplier is worded "x Speed" to match that screen's existing copy too
 * (both describe the same `MILESTONE_STEPS` bonus, so the wording needs to
 * stay in sync between the two rather than drift).
 */
@Composable
fun MilestoneReachedDialog(
    announcement: MilestoneAnnouncement,
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
                painter = painterResource(R.drawable.open_chest),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Milestone Reached!",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${announcement.lairName} — x${announcement.threshold}",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            GlowingGoldText(text = "${GoldFormat.format(announcement.multiplier)}x Speed", colors = palette)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (announcement.isGlobal) "for every lair" else "for this lair",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    color = palette.ink.copy(alpha = 0.75f),
                ),
            )
            Spacer(Modifier.height(20.dp))
            WoodenButton(text = "Nice!", onClick = onDismiss, colors = palette)
        }
    }
}
