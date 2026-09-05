package com.wyrmwhelp.idlehoard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import java.time.Duration
import java.time.Instant

/**
 * The "Settings" section's real content: an account card (sign up/in/out,
 * gating IAP visibility elsewhere — see `ShopContent`'s `isSignedIn` param)
 * and a cloud-sync card (automatic-every-5-minutes note, last-synced time,
 * manual "Sync Now"). Pure display plus callbacks — reads ViewModel state
 * passed in by `MainActivity`'s `WyrmWhelpApp` and forwards actions through
 * [onSignUp]/[onSignIn]/[onSignOut]/[onSyncNow] rather than taking
 * `GameViewModel` itself, same pattern as `StewardsContent`/`ShopContent`.
 *
 * There's no separate `AuthViewModel` — this account/sync state all lives on
 * `GameViewModel` (see its class doc for why).
 */
@Composable
fun SettingsContent(
    userEmail: String?,
    pendingVerificationEmail: String?,
    isAuthActionInProgress: Boolean,
    authMessage: String?,
    isSyncing: Boolean,
    lastSyncedAt: Instant?,
    onSignUp: (email: String, password: String) -> Unit,
    onVerifySignUpCode: (code: String) -> Unit,
    onResendSignUpCode: () -> Unit,
    onCancelSignUpVerification: () -> Unit,
    onSignIn: (email: String, password: String) -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
    onDismissAuthMessage: () -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            AccountCard(
                userEmail = userEmail,
                pendingVerificationEmail = pendingVerificationEmail,
                isAuthActionInProgress = isAuthActionInProgress,
                onSignUp = onSignUp,
                onVerifySignUpCode = onVerifySignUpCode,
                onResendSignUpCode = onResendSignUpCode,
                onCancelSignUpVerification = onCancelSignUpVerification,
                onSignIn = onSignIn,
                onSignOut = onSignOut,
                palette = palette,
            )
        }
        authMessage?.let { message ->
            item { AuthMessageCard(message = message, onDismiss = onDismissAuthMessage, palette = palette) }
        }
        item {
            SyncCard(
                isSyncing = isSyncing,
                lastSyncedAt = lastSyncedAt,
                onSyncNow = onSyncNow,
                palette = palette,
            )
        }
    }
}

/** A translucent parchment card matching `LairCard`/`StewardsContent`/`ShopContent`'s base treatment. */
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

private enum class AuthFormMode { SignUp, SignIn }

@Composable
private fun AccountCard(
    userEmail: String?,
    pendingVerificationEmail: String?,
    isAuthActionInProgress: Boolean,
    onSignUp: (String, String) -> Unit,
    onVerifySignUpCode: (String) -> Unit,
    onResendSignUpCode: () -> Unit,
    onCancelSignUpVerification: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignOut: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    ParchmentCard(palette = palette, modifier = modifier) {
        Text(
            text = "Account",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        Spacer(Modifier.height(6.dp))

        if (userEmail != null) {
            Text(
                text = "Signed in as $userEmail",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink,
            )
            Spacer(Modifier.height(8.dp))
            WoodenButton(
                text = "Sign Out",
                onClick = onSignOut,
                enabled = !isAuthActionInProgress,
                colors = palette,
            )
            return@ParchmentCard
        }

        if (pendingVerificationEmail != null) {
            VerificationCodeForm(
                email = pendingVerificationEmail,
                isSubmitting = isAuthActionInProgress,
                onVerify = onVerifySignUpCode,
                onResend = onResendSignUpCode,
                onCancel = onCancelSignUpVerification,
                palette = palette,
            )
            return@ParchmentCard
        }

        Text(
            text = "Playing as Guest. Create an account or sign in to back up your progress " +
                "across devices and unlock Platinum Piece purchases.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.ink.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(8.dp))

        var formMode by remember { mutableStateOf<AuthFormMode?>(null) }
        when (formMode) {
            null -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WoodenButton(
                    text = "Create Account",
                    onClick = { formMode = AuthFormMode.SignUp },
                    enabled = !isAuthActionInProgress,
                    colors = palette,
                )
                WoodenButton(
                    text = "Sign In",
                    onClick = { formMode = AuthFormMode.SignIn },
                    enabled = !isAuthActionInProgress,
                    colors = palette,
                )
            }
            else -> AuthForm(
                mode = formMode!!,
                isSubmitting = isAuthActionInProgress,
                onSubmit = { email, password ->
                    if (formMode == AuthFormMode.SignUp) onSignUp(email, password) else onSignIn(email, password)
                    formMode = null
                },
                onCancel = { formMode = null },
                palette = palette,
            )
        }
    }
}

/**
 * The code-entry step [signUp] transitions into once Supabase has emailed a
 * verification code — a deliberate anti-bot/anti-spam gate on account
 * creation, not just an email-ownership nicety (see `AuthRepository`'s class
 * doc). Doesn't hardcode a digit count since the code length is a Supabase
 * project setting (Authentication > Emails), not something this app
 * controls — any non-blank input is submittable.
 */
@Composable
private fun VerificationCodeForm(
    email: String,
    isSubmitting: Boolean,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onCancel: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    var code by remember { mutableStateOf("") }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "We emailed a verification code to $email — enter it below to finish creating your account.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.ink.copy(alpha = 0.8f),
        )
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Verification code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = authFieldColors(palette),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WoodenButton(
                text = "Verify",
                onClick = { onVerify(code) },
                enabled = code.isNotBlank() && !isSubmitting,
                colors = palette,
            )
            WoodenButton(
                text = "Cancel",
                onClick = onCancel,
                enabled = !isSubmitting,
                colors = palette,
            )
        }
        Text(
            text = "Resend code",
            style = MaterialTheme.typography.bodySmall,
            color = palette.woodDark,
            modifier = Modifier.clickable(enabled = !isSubmitting, onClick = onResend),
        )
    }
}

/**
 * The email/password form shared by "Create Account" and "Sign In" — same
 * fields, different submit label and [onSubmit] target. Collapses back to
 * the two buttons immediately on submit (optimistic) rather than waiting for
 * the result, since success and failure end up looking the same either way
 * ([SettingsContent]'s `authMessage` banner reports which one happened) —
 * a failed attempt just means tapping the button again to retry.
 */
@Composable
private fun AuthForm(
    mode: AuthFormMode,
    isSubmitting: Boolean,
    onSubmit: (email: String, password: String) -> Unit,
    onCancel: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val canSubmit = email.contains("@") && password.length >= 6 && !isSubmitting

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = authFieldColors(palette),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (min 6 characters)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = authFieldColors(palette),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WoodenButton(
                text = if (mode == AuthFormMode.SignUp) "Create Account" else "Sign In",
                onClick = { onSubmit(email, password) },
                enabled = canSubmit,
                colors = palette,
            )
            WoodenButton(
                text = "Cancel",
                onClick = onCancel,
                enabled = !isSubmitting,
                colors = palette,
            )
        }
    }
}

@Composable
private fun authFieldColors(palette: FantasyPalette) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = palette.ink,
    unfocusedTextColor = palette.ink,
    focusedBorderColor = palette.woodDark,
    unfocusedBorderColor = palette.woodDark.copy(alpha = 0.6f),
    focusedLabelColor = palette.woodDark,
    unfocusedLabelColor = palette.ink.copy(alpha = 0.7f),
    cursorColor = palette.woodDark,
)

@Composable
private fun AuthMessageCard(
    message: String,
    onDismiss: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    ParchmentCard(palette = palette, modifier = modifier, borderColor = palette.goldDeep.copy(alpha = 0.8f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink,
            )
            Text(
                text = "✕",
                color = palette.ink.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(onClick = onDismiss),
            )
        }
    }
}

@Composable
private fun SyncCard(
    isSyncing: Boolean,
    lastSyncedAt: Instant?,
    onSyncNow: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    ParchmentCard(palette = palette, modifier = modifier) {
        Text(
            text = "Cloud Sync",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        Text(
            text = "Your progress syncs automatically every 5 minutes.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.ink.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = lastSyncedAt?.let { "Last synced ${relativeSyncTime(it)}" } ?: "Not synced yet",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.7f),
            )
            WoodenButton(
                text = if (isSyncing) "Syncing…" else "Sync Now",
                onClick = onSyncNow,
                enabled = !isSyncing,
                colors = palette,
            )
        }
    }
}

private fun relativeSyncTime(instant: Instant): String {
    val seconds = Duration.between(instant, Instant.now()).seconds.coerceAtLeast(0)
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60} min ago"
        seconds < 86_400 -> "${seconds / 3_600} hr ago"
        else -> "${seconds / 86_400} day(s) ago"
    }
}
