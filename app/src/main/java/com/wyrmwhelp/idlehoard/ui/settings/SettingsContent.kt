package com.wyrmwhelp.idlehoard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wyrmwhelp.idlehoard.BuildConfig
import com.wyrmwhelp.idlehoard.domain.model.LeaderboardEntry
import com.wyrmwhelp.idlehoard.domain.model.LeaderboardPeriod
import com.wyrmwhelp.idlehoard.domain.model.UNIVERSAL_STEWARD_AD_THRESHOLD
import com.wyrmwhelp.idlehoard.domain.model.isValidUsername
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.leaderboard.LeaderboardContent
import java.time.Duration
import java.time.Instant

/**
 * The "Settings" section's real content — two tabs (v0.42.0): "Account"
 * (the original single-scroll content: sign up/in/out, gating IAP
 * visibility elsewhere — see `ShopContent`'s `isSignedIn` param — plus an
 * inline leaderboard-username field, v0.39.0/v0.39.1, see `AccountCard`'s
 * own doc for why that's an inline field here rather than a separate
 * pop-up; a cloud-sync card, see `SyncCard`'s own doc; a one-line
 * `UniversalStewardStatusLine` glance at progress toward the account-wide
 * Universal Steward (`domain/model/UniversalSteward.kt`) — the real
 * progress card with its own fill bar lives on the Stewards screen, where
 * earning it actually matters; and a version footer) and "Leaderboard"
 * (just `LeaderboardContent` — see that file —
 * moved here from its own `FloatingMenu` section per explicit request,
 * since it doesn't have its own sign art yet and the user plans to make
 * one later; "Leaderboard" no longer appears in `floatingMenuItems` at
 * all). Pure display plus callbacks — reads ViewModel state passed in by
 * `MainActivity`'s `WyrmWhelpApp` and forwards actions through
 * [onSignUp]/[onSignIn]/[onSignOut]/[onSyncNow]/[onSelectLeaderboardPeriod]
 * rather than taking `GameViewModel` itself, same pattern as
 * `StewardsContent`/`ShopContent`.
 *
 * There's no separate `AuthViewModel` — this account/sync state all lives on
 * `GameViewModel` (see its class doc for why).
 *
 * The version footer reads [BuildConfig.VERSION_NAME] directly rather than
 * being threaded in as a parameter — unlike everything else on this screen
 * it's a compile-time constant, not live ViewModel state, so there's
 * nothing for `GameViewModel`/`MainActivity` to own or pass down.
 */
@Composable
fun SettingsContent(
    userEmail: String?,
    pendingVerificationEmail: String?,
    isAuthActionInProgress: Boolean,
    authMessage: String?,
    username: String?,
    isUsernameActionInProgress: Boolean,
    usernameMessage: String?,
    onSubmitUsername: (String) -> Unit,
    onDismissUsernameMessage: () -> Unit,
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
    isAccountActionInProgress: Boolean,
    accountActionMessage: String?,
    onResetAccount: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDismissAccountActionMessage: () -> Unit,
    leaderboardPeriod: LeaderboardPeriod,
    leaderboardEntries: List<LeaderboardEntry>,
    currentUserLeaderboardEntry: LeaderboardEntry?,
    isLeaderboardLoading: Boolean,
    leaderboardError: String?,
    onSelectLeaderboardPeriod: (LeaderboardPeriod) -> Unit,
    adsWatchedTowardUniversalSteward: Int,
    hasUniversalSteward: Boolean,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    var selectedTab by remember { mutableStateOf(SettingsTab.ACCOUNT) }

    Column(modifier = modifier.fillMaxSize()) {
        SettingsTabRow(selected = selectedTab, onSelect = { selectedTab = it }, palette = palette)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                SettingsTab.ACCOUNT -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        AccountCard(
                            userEmail = userEmail,
                            pendingVerificationEmail = pendingVerificationEmail,
                            isAuthActionInProgress = isAuthActionInProgress,
                            username = username,
                            isUsernameActionInProgress = isUsernameActionInProgress,
                            usernameMessage = usernameMessage,
                            onSubmitUsername = onSubmitUsername,
                            onDismissUsernameMessage = onDismissUsernameMessage,
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
                            isSignedIn = userEmail != null,
                            isSyncing = isSyncing,
                            lastSyncedAt = lastSyncedAt,
                            onSyncNow = onSyncNow,
                            palette = palette,
                        )
                    }
                    item {
                        UniversalStewardStatusLine(
                            adsWatched = adsWatchedTowardUniversalSteward,
                            unlocked = hasUniversalSteward,
                            palette = palette,
                        )
                    }
                    item {
                        DangerZoneCard(
                            canDeleteAccount = userEmail != null,
                            isActionInProgress = isAccountActionInProgress,
                            onResetAccount = onResetAccount,
                            onDeleteAccount = onDeleteAccount,
                            palette = palette,
                        )
                    }
                    accountActionMessage?.let { message ->
                        item { AuthMessageCard(message = message, onDismiss = onDismissAccountActionMessage, palette = palette) }
                    }
                    item { VersionFooter(palette = palette) }
                }
                SettingsTab.LEADERBOARD -> LeaderboardContent(
                    isSignedIn = userEmail != null,
                    period = leaderboardPeriod,
                    entries = leaderboardEntries,
                    currentUserEntry = currentUserLeaderboardEntry,
                    isLoading = isLeaderboardLoading,
                    errorMessage = leaderboardError,
                    onSelectPeriod = onSelectLeaderboardPeriod,
                    palette = palette,
                )
            }
        }
    }
}

private enum class SettingsTab(val label: String) {
    ACCOUNT("Account"),
    LEADERBOARD("Leaderboard"),
}

@Composable
private fun SettingsTabRow(selected: SettingsTab, onSelect: (SettingsTab) -> Unit, palette: FantasyPalette, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        SettingsTab.entries.forEach { tab ->
            SettingsTabButton(
                text = tab.label,
                selected = tab == selected,
                onClick = { onSelect(tab) },
                palette = palette,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Same shape as `ShopContent.kt`'s private `ShopTabButton`/`UpgradesContent.kt`'s `UpgradeTabButton`, duplicated per this project's established per-file-duplication convention for small private UI helpers. */
@Composable
private fun SettingsTabButton(text: String, selected: Boolean, onClick: () -> Unit, palette: FantasyPalette, modifier: Modifier = Modifier) {
    val shape = CutCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (selected) listOf(palette.goldBright, palette.goldDeep) else listOf(palette.woodLight, palette.woodDark),
                ),
            )
            .border(1.dp, palette.woodDark, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) palette.ink else palette.parchment,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
        )
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

/**
 * The signed-in branch shows [UsernameField] inline — right in this card,
 * not a separate pop-up — per explicit correction to the original v0.39.0
 * design, which auto-popped a `Dialog` the instant registration completed.
 * That's gone entirely now: there's no "needs a username" trigger anymore,
 * just this field, always sitting here for a signed-in player to fill in
 * or change whenever they want.
 *
 * **A guest sees the same [username] too, since v0.50.0 — just read-only.**
 * Every account (guest included) is auto-assigned a placeholder
 * `AnonymousNNNNNN` name the instant it exists
 * (`SQL/006_auto_generate_usernames.sql`'s trigger), so guests now show up
 * on the leaderboard from session one — the guest branch below renders a
 * plain "Username: AnonymousXXXXXX" line plus a "Sign in to change your
 * username" hint, but not [UsernameField] itself, which stays gated to
 * `userEmail != null` — editing a name only makes sense once there's a
 * permanent account to attach it to; a guest's identity (and thus its
 * placeholder name) doesn't survive a reinstall regardless. This is
 * unrelated to `SyncCard`'s "Sync Now"/`ShopContent`'s "Buy Platinum
 * Pieces" gating, which stays sign-in-only for its own reasons (a
 * recoverable account, real money) — only the username's *visibility*
 * changed here, not those.
 */
@Composable
private fun AccountCard(
    userEmail: String?,
    pendingVerificationEmail: String?,
    isAuthActionInProgress: Boolean,
    username: String?,
    isUsernameActionInProgress: Boolean,
    usernameMessage: String?,
    onSubmitUsername: (String) -> Unit,
    onDismissUsernameMessage: () -> Unit,
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
            Spacer(Modifier.height(10.dp))
            UsernameField(
                username = username,
                isSubmitting = isUsernameActionInProgress,
                errorMessage = usernameMessage,
                onSubmit = onSubmitUsername,
                onDismissMessage = onDismissUsernameMessage,
                palette = palette,
            )
            Spacer(Modifier.height(10.dp))
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

        if (username != null) {
            Text(
                text = "Username: $username",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink,
            )
            Text(
                text = "Sign in to change your username.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(8.dp))
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

/**
 * The manual "Sync Now" button is gated to signed-in players
 * ([isSignedIn]) — a guest's anonymous identity still syncs automatically
 * every 5 minutes (same as everyone else, see `GameViewModel.runCloudSyncLoop`),
 * but that identity is lost on reinstall regardless, so a manual sync
 * button doesn't buy a guest anything beyond what's already happening in
 * the background — mirrors `ShopContent`'s guest-gated "Buy Platinum
 * Pieces" (same reasoning: keep a manually-triggered action tied to a
 * recoverable account).
 */
@Composable
private fun SyncCard(
    isSignedIn: Boolean,
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
                enabled = isSignedIn && !isSyncing,
                colors = palette,
            )
        }
        if (!isSignedIn) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Sign in above to sync on demand — as a guest, you're still backed up " +
                    "automatically, but that identity can't be recovered after a reinstall.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * "Danger Zone" — Account Reset (wipes game progress back to a fresh save,
 * keeping Platinum Pieces and everything bought with them — see
 * `GameEngine.resetProgress`'s doc for the exact split) and, only for a real
 * signed-in account, Account Delete (irreversible — wipes local data, the
 * cloud save, and the Supabase account itself, see
 * `AuthRepository.deleteAccount`). A guest never sees the Delete option at
 * all, per explicit design — deleting an anonymous session is meaningless
 * since reinstalling already does that; Reset is available to guest and
 * signed-in players alike, since it never touches the auth session. Both
 * actions require an explicit confirm dialog before anything happens —
 * [ResetAccountConfirmDialog]/[DeleteAccountConfirmDialog] below — matching
 * severity: Reset is a plain two-button confirm (same shape as
 * `LevelUpContent.kt`'s `LevelUpConfirmDialog`), Delete additionally
 * requires typing "DELETE" since it's permanent.
 */
@Composable
private fun DangerZoneCard(
    canDeleteAccount: Boolean,
    isActionInProgress: Boolean,
    onResetAccount: () -> Unit,
    onDeleteAccount: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ParchmentCard(palette = palette, modifier = modifier, borderColor = DANGER_COLOR.copy(alpha = 0.6f)) {
        Text(
            text = "Danger Zone",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Reset Account",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium.copy(color = palette.ink),
        )
        Text(
            text = "Wipes your Gold, lairs, Gems, Level Up count, avatar, and username back " +
                "to a fresh start. Platinum Pieces and anything bought with them are kept.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.ink.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(6.dp))
        WoodenButton(
            text = "Reset Account",
            onClick = { showResetConfirm = true },
            enabled = !isActionInProgress,
            colors = dangerPalette(palette),
        )

        if (canDeleteAccount) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Delete Account",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium.copy(color = palette.ink),
            )
            Text(
                text = "Permanently deletes your account and every bit of saved data — local " +
                    "and cloud. This cannot be undone.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(6.dp))
            WoodenButton(
                text = "Delete Account",
                onClick = { showDeleteConfirm = true },
                enabled = !isActionInProgress,
                colors = dangerPalette(palette),
            )
        }
    }

    if (showResetConfirm) {
        ResetAccountConfirmDialog(
            onConfirm = { showResetConfirm = false; onResetAccount() },
            onCancel = { showResetConfirm = false },
            palette = palette,
        )
    }
    if (showDeleteConfirm) {
        DeleteAccountConfirmDialog(
            onConfirm = { showDeleteConfirm = false; onDeleteAccount() },
            onCancel = { showDeleteConfirm = false },
            palette = palette,
        )
    }
}

/** A muted rust-red — this app has no "danger" tone in `FantasyPalette` itself, so it's kept local to this file's Danger Zone UI. */
private val DANGER_COLOR = Color(0xFF7A2626)

/** [WoodenButton] takes a whole [FantasyPalette] for its coloring, not a single accent — this retints just the wood/gold tones red for a destructive-action button, keeping the same carved-wood look everywhere else. */
private fun dangerPalette(palette: FantasyPalette): FantasyPalette = palette.copy(
    woodLight = Color(0xFFA13B3B),
    woodMid = DANGER_COLOR,
    woodDark = Color(0xFF4A1414),
    goldBright = Color(0xFFE5A5A5),
)

@Composable
private fun ResetAccountConfirmDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    palette: FantasyPalette,
) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .widthIn(min = 260.dp, max = 340.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(palette.parchmentShade, palette.parchment)))
                .border(2.dp, DANGER_COLOR, RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Reset your account?",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your Gold, every owned lair, Gems, Level Up count, avatar, and username " +
                    "will all reset to a brand-new save. Platinum Pieces and anything bought " +
                    "with them are kept.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    color = palette.ink.copy(alpha = 0.8f),
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WoodenButton(text = "Cancel", onClick = onCancel, colors = palette)
                WoodenButton(text = "Reset", onClick = onConfirm, colors = dangerPalette(palette))
            }
        }
    }
}

/**
 * Requires typing "DELETE" before the confirm button enables, unlike
 * [ResetAccountConfirmDialog]'s plain two-button confirm — this action is
 * permanent and removes the account entirely, so it gets the extra friction.
 */
@Composable
private fun DeleteAccountConfirmDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    palette: FantasyPalette,
) {
    var confirmText by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .widthIn(min = 260.dp, max = 340.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(palette.parchmentShade, palette.parchment)))
                .border(2.dp, DANGER_COLOR, RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Delete your account?",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "This permanently deletes your account and every bit of saved data — " +
                    "local and cloud. There is no undo.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    color = palette.ink.copy(alpha = 0.8f),
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Type DELETE to confirm:",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.7f),
            )
            OutlinedTextField(
                value = confirmText,
                onValueChange = { confirmText = it },
                singleLine = true,
                colors = authFieldColors(palette),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WoodenButton(text = "Cancel", onClick = onCancel, colors = palette)
                WoodenButton(
                    text = "Delete",
                    onClick = onConfirm,
                    enabled = confirmText == "DELETE",
                    colors = dangerPalette(palette),
                )
            }
        }
    }
}

/**
 * The leaderboard-username field itself — an inline label, text field, and
 * Save button sitting directly in `AccountCard` (v0.39.1; originally a
 * separate pop-up `Dialog` that appeared unprompted right after
 * registration, replaced per explicit correction: no more surprise
 * pop-up, and it now only exists at all inside `AccountCard`'s signed-in
 * branch, so a guest can't reach it). `text` is keyed on [username] so the
 * field re-syncs to the confirmed value after a successful save (or if a
 * different username loads in, e.g. right after sign-in) rather than
 * holding on to a stale local edit. The Save button stays disabled until
 * the typed value is both a syntactically valid username *and* actually
 * different from what's already saved — there's nothing useful to submit
 * otherwise.
 */
@Composable
private fun UsernameField(
    username: String?,
    isSubmitting: Boolean,
    errorMessage: String?,
    onSubmit: (String) -> Unit,
    onDismissMessage: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    var text by remember(username) { mutableStateOf(username.orEmpty()) }
    val canSubmit = isValidUsername(text) && text != username.orEmpty() && !isSubmitting

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Leaderboard Username",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    if (errorMessage != null) onDismissMessage()
                },
                placeholder = { Text("Choose a username") },
                singleLine = true,
                colors = authFieldColors(palette),
                modifier = Modifier.weight(1f),
            )
            WoodenButton(
                text = if (isSubmitting) "Saving…" else "Save",
                onClick = { onSubmit(text.trim()) },
                enabled = canSubmit,
                colors = palette,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = errorMessage ?: "3-20 characters: letters, numbers, and underscores. Shown on the Leaderboard tab above.",
            style = MaterialTheme.typography.bodySmall,
            color = if (errorMessage != null) palette.goldDeep else palette.ink.copy(alpha = 0.6f),
        )
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

/** Unboxed, muted footer text — lower visual weight than the account/sync cards above since it isn't actionable. */
@Composable
private fun VersionFooter(palette: FantasyPalette, modifier: Modifier = Modifier) {
    Text(
        text = "Wyrm & Whelp: Idle Hoard v${BuildConfig.VERSION_NAME}",
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
        color = palette.ink.copy(alpha = 0.5f),
    )
}

/**
 * A one-line account-wide stat, unboxed like [VersionFooter] rather than a
 * full [ParchmentCard] — the real progress card with its own fill bar lives
 * on the Stewards screen (`ui/stewards/StewardsContent.kt`'s
 * `UniversalStewardCard`); this is just a quick "how am I doing" glance
 * from Settings, matching the low visual weight [VersionFooter] already
 * has here.
 */
@Composable
private fun UniversalStewardStatusLine(adsWatched: Int, unlocked: Boolean, palette: FantasyPalette, modifier: Modifier = Modifier) {
    Text(
        text = if (unlocked) {
            "Universal Steward: Active"
        } else {
            "Universal Steward: $adsWatched / $UNIVERSAL_STEWARD_AD_THRESHOLD ads watched"
        },
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
        color = if (unlocked) palette.goldDeep else palette.ink.copy(alpha = 0.6f),
    )
}
