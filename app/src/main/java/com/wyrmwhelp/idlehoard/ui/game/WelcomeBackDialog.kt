package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.wyrmwhelp.idlehoard.domain.engine.OfflineEarnings
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat
import kotlin.math.roundToLong

@Composable
fun WelcomeBackDialog(earnings: OfflineEarnings, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Claim") }
        },
        title = { Text("While you were away…") },
        text = {
            val minutesAway = (earnings.cappedSeconds / 60.0).roundToLong()
            Text(
                "Your lairs produced ${GoldFormat.format(earnings.goldEarned)} gp " +
                    "over the last $minutesAway minute(s).",
            )
        },
    )
}
