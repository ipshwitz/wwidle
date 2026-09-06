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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wyrmwhelp.idlehoard.R
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.GlowingGoldText
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

/**
 * The pop-up shown right after a successful Level Up — same cozy-fantasy
 * chrome as [WelcomeBackDialog]/[MilestoneReachedDialog] (plain `Dialog`,
 * parchment-gradient card with a carved wood border, `open_chest` art, a
 * `WoodenButton` to dismiss), reused here rather than inventing a third
 * dialog look. [GlowingGoldText]'s glow is overridden to
 * [FantasyPalette.gemBright]/[FantasyPalette.gemDeep] (amethyst, not gold)
 * since this is celebrating Gems, not Gold Pieces — the one visual
 * difference from the other two reward dialogs.
 */
@Composable
fun LevelUpRewardDialog(
    gemsEarned: Long,
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
                text = "Level Up!",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            GlowingGoldText(
                text = "+${GoldFormat.format(gemsEarned.toDouble())} Gems",
                colors = palette,
                glowBright = palette.gemBright,
                glowDeep = palette.gemDeep,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "your hoard resets, but the bonus is permanent",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    color = palette.ink.copy(alpha = 0.75f),
                ),
            )
            Spacer(Modifier.height(20.dp))
            WoodenButton(text = "Onward!", onClick = onDismiss, colors = palette)
        }
    }
}
