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
import com.wyrmwhelp.idlehoard.domain.model.UNIVERSAL_STEWARD_AD_THRESHOLD
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton

/**
 * The one-time pop-up shown the moment the Universal Steward unlocks (see
 * `domain/model/UniversalSteward.kt`, `GameViewModel.universalStewardUnlocked`) —
 * same cozy-fantasy chrome as `WelcomeBackDialog`/`MilestoneReachedDialog`/
 * `LevelUpRewardDialog` (plain `Dialog`, parchment-gradient card, carved
 * wood border, a `WoodenButton` to dismiss), reused rather than a fourth
 * dialog look. Uses `open_chest` art like `MilestoneReachedDialog` — there's
 * no bespoke Universal Steward art yet, and "something permanent was just
 * unlocked" is exactly what that art already reads as elsewhere.
 */
@Composable
fun UniversalStewardRewardDialog(
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
                text = "Universal Steward!",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "$UNIVERSAL_STEWARD_AD_THRESHOLD ads watched — earned it!",
                style = MaterialTheme.typography.bodyMedium.copy(color = palette.goldDeep),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Every lair you own now collects on its own, forever — even through a Level Up.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    color = palette.ink.copy(alpha = 0.8f),
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            WoodenButton(text = "Excellent!", onClick = onDismiss, colors = palette)
        }
    }
}
