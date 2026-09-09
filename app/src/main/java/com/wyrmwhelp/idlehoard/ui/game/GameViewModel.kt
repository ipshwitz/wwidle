package com.wyrmwhelp.idlehoard.ui.game

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wyrmwhelp.idlehoard.ads.AdManager
import com.wyrmwhelp.idlehoard.ads.RewardedPlacement
import com.wyrmwhelp.idlehoard.billing.BillingManager
import com.wyrmwhelp.idlehoard.billing.PlatinumPurchaseResult
import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import com.wyrmwhelp.idlehoard.domain.engine.GameEngine
import com.wyrmwhelp.idlehoard.domain.engine.OfflineEarnings
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.MilestoneAnnouncement
import com.wyrmwhelp.idlehoard.domain.model.PLATINUM_AD_REWARD_PP
import com.wyrmwhelp.idlehoard.domain.model.SPEED_BOOST_AD_MULTIPLIER
import com.wyrmwhelp.idlehoard.domain.model.INCOME_BOOST_AD_MULTIPLIER
import com.wyrmwhelp.idlehoard.domain.model.mergeGameStates
import com.wyrmwhelp.idlehoard.domain.model.milestonesCrossed
import com.wyrmwhelp.idlehoard.domain.model.platinumAdCooldownRemaining
import com.wyrmwhelp.idlehoard.domain.model.speedBoostAdCooldownRemaining
import com.wyrmwhelp.idlehoard.domain.model.incomeBoostAdCooldownRemaining
import com.wyrmwhelp.idlehoard.domain.model.PermanentBoostTier
import com.wyrmwhelp.idlehoard.domain.model.TemporaryBoostOption
import com.wyrmwhelp.idlehoard.domain.model.TimeSkipOption
import com.wyrmwhelp.idlehoard.domain.model.UpgradeCategory
import com.wyrmwhelp.idlehoard.domain.model.StewardEfficiency
import com.wyrmwhelp.idlehoard.domain.model.isValidUsername
import com.wyrmwhelp.idlehoard.ui.format.DurationFormat
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat
import com.wyrmwhelp.idlehoard.domain.repository.AuthRepository
import com.wyrmwhelp.idlehoard.domain.repository.CloudSaveRepository
import com.wyrmwhelp.idlehoard.domain.repository.GameRepository
import com.wyrmwhelp.idlehoard.domain.repository.LeaderboardRepository
import com.wyrmwhelp.idlehoard.domain.model.LeaderboardEntry
import com.wyrmwhelp.idlehoard.domain.model.LeaderboardPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.exceptions.RestException
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Presentation-layer wrapper around [GameEngine]. [GameEngine] itself is an
 * app-scoped singleton that keeps running independent of any screen, so this
 * ViewModel only starts it and applies offline earnings once on first
 * creation — it never stops the engine in `onCleared`.
 *
 * Also owns the account/sync side of the Settings screen (there's no
 * separate `AuthViewModel` — the two are tightly coupled, since signing in
 * or out directly changes which cloud row this save syncs to) —
 * [userEmail]/[signUp]/[signIn]/[signOut]/[syncNow] below — and both
 * rewarded-ad placements, [watchAdToDoubleOfflineEarnings] and
 * [watchAdForPlatinum] — plus the Shop's real-money Platinum Pieces
 * packs, [buyPlatinumPack], via `BillingManager`.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameEngine: GameEngine,
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository,
    private val cloudSaveRepository: CloudSaveRepository,
    private val leaderboardRepository: LeaderboardRepository,
    private val adManager: AdManager,
    private val billingManager: BillingManager,
) : ViewModel() {

    val gameState: StateFlow<GameState> = gameEngine.state

    // True until the init sequence below (local load, sign-in, cloud merge,
    // offline earnings) has settled — drives `MainActivity`'s `LoadingScreen`.
    // Flips false right after `gameEngine.start()`, before the two infinite
    // sync loops are launched (`runCloudSyncLoop()` in particular runs
    // un-launched on this same coroutine, so anything after it would never
    // execute).
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Each owned lair's current fill fraction — see `GameEngine.lairProgress` for why this is separate from [gameState]. */
    val lairProgress: StateFlow<Map<String, Float>> = gameEngine.lairProgress

    val lairs: List<CreatureLair> = CreatureLairCatalog.lairs

    private val _welcomeBackEarnings = MutableStateFlow<OfflineEarnings?>(null)
    val welcomeBackEarnings: StateFlow<OfflineEarnings?> = _welcomeBackEarnings.asStateFlow()

    // Only one rewarded-ad watch is allowed per Welcome Back pop-up —
    // reset whenever a new one appears (see dismissWelcomeBack).
    private val _isOfflineEarningsDoubled = MutableStateFlow(false)
    val isOfflineEarningsDoubled: StateFlow<Boolean> = _isOfflineEarningsDoubled.asStateFlow()

    private val _adUnavailableMessage = MutableStateFlow<String?>(null)
    val adUnavailableMessage: StateFlow<String?> = _adUnavailableMessage.asStateFlow()

    private val _milestoneAnnouncement = MutableStateFlow<MilestoneAnnouncement?>(null)
    val milestoneAnnouncement: StateFlow<MilestoneAnnouncement?> = _milestoneAnnouncement.asStateFlow()

    // Gems earned by the most recent Level Up, shown once via
    // LevelUpRewardDialog then cleared — null the rest of the time, same
    // one-shot-pop-up shape as _milestoneAnnouncement above.
    private val _levelUpReward = MutableStateFlow<Long?>(null)
    val levelUpReward: StateFlow<Long?> = _levelUpReward.asStateFlow()

    // One-shot pop-up flag for the Universal Steward unlock (see
    // `domain/model/UniversalSteward.kt`) — flips true the instant
    // `GameEngine.recordAdWatched()` reports the 100th ad crossed, cleared
    // by `dismissUniversalStewardUnlocked` once `GameScreen` shows the
    // reward dialog. A plain Boolean (not the reward count itself) since
    // there's nothing variable to display — it's the same unlock every time.
    private val _universalStewardUnlocked = MutableStateFlow(false)
    val universalStewardUnlocked: StateFlow<Boolean> = _universalStewardUnlocked.asStateFlow()

    /** Every rewarded-ad `onRewardEarned` callback calls this once, regardless of placement — see `GameEngine.recordAdWatched`'s doc for why. */
    private fun recordAdWatched() {
        if (gameEngine.recordAdWatched()) {
            _universalStewardUnlocked.value = true
        }
    }

    // A single big purchase (e.g. buying MAX) can cross several rungs at
    // once — held here and drained one at a time via
    // dismissMilestoneAnnouncement rather than bundled into one pop-up.
    private val pendingMilestoneAnnouncements = ArrayDeque<MilestoneAnnouncement>()

    // Not persisted — resets to X1 each launch, same as most idle games'
    // buy-quantity selector.
    private val _buyQuantity = MutableStateFlow(BuyQuantity.X1)
    val buyQuantity: StateFlow<BuyQuantity> = _buyQuantity.asStateFlow()

    fun cycleBuyQuantity() {
        _buyQuantity.value = _buyQuantity.value.next()
    }

    // The cloud identity currently syncing this save — null only in the
    // brief window before the very first ensureSignedIn() resolves, or if it
    // failed outright (see the resilience note in init below).
    private var currentUserId: String? = null

    // Null means a guest (anonymous) session, or a permanent one still
    // pending email confirmation — see AuthRepository.currentUserEmail's doc.
    // This is what gates IAP visibility in the Shop.
    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _isAuthActionInProgress = MutableStateFlow(false)
    val isAuthActionInProgress: StateFlow<Boolean> = _isAuthActionInProgress.asStateFlow()

    // Result text from the last sign-up/sign-in/sign-out attempt — could be
    // an error ("Wrong password") or a neutral notice ("Check your email to
    // confirm"), Settings doesn't need to distinguish the two for v1.
    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    // Non-null while a signUp is waiting on the emailed verification code —
    // drives Settings into the code-entry step instead of the sign-up form.
    private val _pendingVerificationEmail = MutableStateFlow<String?>(null)
    val pendingVerificationEmail: StateFlow<String?> = _pendingVerificationEmail.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncedAt = MutableStateFlow<Instant?>(null)
    val lastSyncedAt: StateFlow<Instant?> = _lastSyncedAt.asStateFlow()

    // The signed-in player's leaderboard username (`profiles` table) — null
    // for guests, and null for a signed-in player who hasn't set one yet.
    // Edited inline in Settings' Account card (SettingsContent.kt) rather
    // than through a separate pop-up — there's no "needs a username" flag
    // driving an unprompted dialog; the field just always shows whatever
    // this currently holds, and only Settings ever renders it (gated to
    // signed-in players there, same as the rest of the Account card).
    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _isUsernameActionInProgress = MutableStateFlow(false)
    val isUsernameActionInProgress: StateFlow<Boolean> = _isUsernameActionInProgress.asStateFlow()

    private val _usernameMessage = MutableStateFlow<String?>(null)
    val usernameMessage: StateFlow<String?> = _usernameMessage.asStateFlow()

    // The Leaderboard menu section's state — see `loadLeaderboard`. Guests
    // are excluded from the boards entirely (only players with a `profiles`
    // username ever appear — see SQL/004_create_leaderboards.sql), but can
    // still view them; there's just never a [currentUserLeaderboardEntry]
    // for one. Rankings are precomputed hourly server-side, not live, so
    // this is a plain fetch-on-open, not something the tick loop touches.
    private val _leaderboardPeriod = MutableStateFlow(LeaderboardPeriod.WEEKLY)
    val leaderboardPeriod: StateFlow<LeaderboardPeriod> = _leaderboardPeriod.asStateFlow()

    private val _leaderboardEntries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _leaderboardEntries.asStateFlow()

    private val _currentUserLeaderboardEntry = MutableStateFlow<LeaderboardEntry?>(null)
    val currentUserLeaderboardEntry: StateFlow<LeaderboardEntry?> = _currentUserLeaderboardEntry.asStateFlow()

    private val _isLeaderboardLoading = MutableStateFlow(false)
    val isLeaderboardLoading: StateFlow<Boolean> = _isLeaderboardLoading.asStateFlow()

    private val _leaderboardError = MutableStateFlow<String?>(null)
    val leaderboardError: StateFlow<String?> = _leaderboardError.asStateFlow()

    /**
     * Fetches [period]'s top entries plus the current player's own (if
     * signed in) — called once when the Leaderboard section opens
     * (`MainActivity`'s `LaunchedEffect(openSection)`) and again on every
     * tab switch. `leaderboard_rankings` has a public-read RLS policy, so
     * the top list itself is fetched for guests too (a guest just never
     * gets a [currentUserLeaderboardEntry], since there's no account for
     * `fetchCurrentUserEntry` to look up) — only skipping the *whole*
     * fetch for guests would be wrong here, unlike `refreshUsernameState`'s
     * genuinely-nothing-to-fetch shortcut, since the board itself has
     * nothing to do with whether the viewer is signed in.
     */
    fun loadLeaderboard(period: LeaderboardPeriod = _leaderboardPeriod.value) {
        _leaderboardPeriod.value = period
        viewModelScope.launch {
            _isLeaderboardLoading.value = true
            _leaderboardError.value = null
            runCatching {
                val top = leaderboardRepository.fetchTop(period)
                val own = if (_userEmail.value != null) leaderboardRepository.fetchCurrentUserEntry(period) else null
                top to own
            }
                .onSuccess { (top, own) ->
                    _leaderboardEntries.value = top
                    _currentUserLeaderboardEntry.value = own
                }
                .onFailure { e ->
                    Log.w(TAG, "Loading the leaderboard failed", e)
                    _leaderboardError.value = "Couldn't load the leaderboard — try again shortly."
                }
            _isLeaderboardLoading.value = false
        }
    }

    init {
        viewModelScope.launch {
            val loadStartedAtMs = System.currentTimeMillis()
            val local = gameRepository.loadGameState()

            // Cloud sync is best-effort: a network hiccup or Supabase outage should
            // never block local play, so every step here degrades to local-only.
            val userId = runCatching { authRepository.ensureSignedIn() }
                .onFailure { Log.w(TAG, "Sign-in failed, continuing offline", it) }
                .getOrNull()
            currentUserId = userId
            _userEmail.value = userId?.let { authRepository.currentUserEmail() }
            refreshUsernameState()

            val cloud = userId?.let { id ->
                runCatching { cloudSaveRepository.downloadSave(id) }
                    .onFailure { Log.w(TAG, "Cloud download failed, continuing offline", it) }
                    .getOrNull()
            }

            mergeGameStates(local, cloud)?.let { gameEngine.loadState(it) }

            val earnings = gameEngine.applyOfflineEarnings()
            if (earnings.goldEarned > 0.0) {
                _welcomeBackEarnings.value = earnings
            }

            val settled = gameEngine.state.value
            gameRepository.saveGameState(settled)
            if (userId != null) {
                runCatching { cloudSaveRepository.uploadSave(userId, settled) }
                    .onSuccess { _lastSyncedAt.value = Instant.now() }
                    .onFailure { Log.w(TAG, "Cloud upload failed, continuing offline", it) }
            }

            gameEngine.start()

            // Purely cosmetic — see MIN_LOADING_SCREEN_DURATION_MS's doc.
            val elapsedMs = System.currentTimeMillis() - loadStartedAtMs
            if (elapsedMs < MIN_LOADING_SCREEN_DURATION_MS) {
                delay(MIN_LOADING_SCREEN_DURATION_MS - elapsedMs)
            }

            _isLoading.value = false
            launch { runAutosaveLoop() }
            runCloudSyncLoop()
        }
        viewModelScope.launch { runPlatinumPurchaseEventLoop() }
    }

    /**
     * Credits Platinum Pieces the moment a Shop purchase actually completes
     * (see `BillingManager.purchaseEvents`) — a separate coroutine from the
     * main load sequence above since a purchase can complete at any point
     * in the session, not just during initial load.
     */
    private suspend fun runPlatinumPurchaseEventLoop() {
        billingManager.purchaseEvents.collect { result ->
            _platinumPurchaseMessage.value = when (result) {
                is PlatinumPurchaseResult.Granted -> {
                    gameEngine.grantPlatinum(result.platinumPieces)
                    "Purchased ${GoldFormat.format(result.platinumPieces.toDouble())} Platinum Pieces!"
                }
                is PlatinumPurchaseResult.Failed -> result.message
            }
        }
    }

    private suspend fun CoroutineScope.runAutosaveLoop() {
        while (isActive) {
            delay(AUTOSAVE_INTERVAL_MS)
            gameRepository.saveGameState(gameEngine.state.value)
        }
    }

    private suspend fun CoroutineScope.runCloudSyncLoop() {
        while (isActive) {
            delay(CLOUD_SYNC_INTERVAL_MS)
            syncToCloud()
        }
    }

    /** Uploads the live game state to [currentUserId]'s cloud row, if any. Best-effort. */
    private suspend fun syncToCloud() {
        val userId = currentUserId ?: return
        _isSyncing.value = true
        val state = gameEngine.state.value
        runCatching {
            gameRepository.saveGameState(state)
            cloudSaveRepository.uploadSave(userId, state)
        }
            .onSuccess { _lastSyncedAt.value = Instant.now() }
            .onFailure { Log.w(TAG, "Cloud sync failed", it) }
        _isSyncing.value = false
    }

    /** The Settings screen's manual "Sync Now" button. */
    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch { syncToCloud() }
    }

    /**
     * Starts upgrading the current guest session to a permanent account,
     * keeping the same save (no merge needed — see [AuthRepository.signUp]).
     * If the Supabase project requires email confirmation (the expected/
     * recommended setup — see CLAUDE.md's Auth section, this is what makes
     * the code-verification step below an actual anti-bot gate rather than
     * a formality), this only *starts* the upgrade: [pendingVerificationEmail]
     * is set and the caller must follow up with [verifySignUpCode]. If
     * confirmation is disabled project-side, the upgrade is already
     * complete by the time this returns — detected via [currentUserEmail]
     * already being non-null — and no code step is needed at all.
     */
    fun signUp(email: String, password: String) {
        if (_isAuthActionInProgress.value) return
        viewModelScope.launch {
            _isAuthActionInProgress.value = true
            _authMessage.value = null
            runCatching { authRepository.signUp(email, password) }
                .onSuccess { userId ->
                    currentUserId = userId
                    val confirmedEmail = authRepository.currentUserEmail()
                    if (confirmedEmail != null) {
                        _userEmail.value = confirmedEmail
                        _authMessage.value = "Account created!"
                        refreshUsernameState()
                        syncToCloud()
                    } else {
                        _pendingVerificationEmail.value = email
                        _authMessage.value =
                            "We emailed a verification code to $email — enter it below to finish creating your account."
                    }
                }
                .onFailure { e ->
                    Log.w(TAG, "Sign up failed", e)
                    _authMessage.value = e.message?.takeIf { it.isNotBlank() } ?: "Sign up failed."
                }
            _isAuthActionInProgress.value = false
        }
    }

    /** Completes a [signUp] upgrade with the code Supabase emailed to [pendingVerificationEmail]. */
    fun verifySignUpCode(code: String) {
        val email = _pendingVerificationEmail.value ?: return
        if (_isAuthActionInProgress.value) return
        viewModelScope.launch {
            _isAuthActionInProgress.value = true
            _authMessage.value = null
            runCatching { authRepository.verifySignUpCode(email, code) }
                .onSuccess { userId ->
                    currentUserId = userId
                    _userEmail.value = authRepository.currentUserEmail()
                    _pendingVerificationEmail.value = null
                    _authMessage.value = "Account verified!"
                    refreshUsernameState()
                    syncToCloud()
                }
                .onFailure { e ->
                    Log.w(TAG, "Sign up code verification failed", e)
                    _authMessage.value =
                        e.message?.takeIf { it.isNotBlank() } ?: "That code didn't work — check it and try again."
                }
            _isAuthActionInProgress.value = false
        }
    }

    /** Re-sends the verification code for a [signUp] upgrade still pending [verifySignUpCode]. */
    fun resendSignUpCode() {
        val email = _pendingVerificationEmail.value ?: return
        if (_isAuthActionInProgress.value) return
        viewModelScope.launch {
            _isAuthActionInProgress.value = true
            _authMessage.value = null
            runCatching { authRepository.resendSignUpCode(email) }
                .onSuccess { _authMessage.value = "Sent a new code to $email." }
                .onFailure { e ->
                    Log.w(TAG, "Resend sign up code failed", e)
                    _authMessage.value = e.message?.takeIf { it.isNotBlank() } ?: "Couldn't resend the code."
                }
            _isAuthActionInProgress.value = false
        }
    }

    /** Backs out of a pending [signUp] verification (e.g. the player wants to redo the form). */
    fun cancelSignUpVerification() {
        _pendingVerificationEmail.value = null
        _authMessage.value = null
    }

    /**
     * Switches to a different, already-existing permanent account. Its
     * user id differs from the current session's, so the local save and
     * that account's cloud save are reconciled via [mergeGameStates] —
     * same logic used when merging local vs. cloud on launch.
     */
    fun signIn(email: String, password: String) {
        if (_isAuthActionInProgress.value) return
        viewModelScope.launch {
            _isAuthActionInProgress.value = true
            _authMessage.value = null
            runCatching { authRepository.signIn(email, password) }
                .onSuccess { userId ->
                    currentUserId = userId
                    _userEmail.value = authRepository.currentUserEmail()
                    refreshUsernameState()

                    val cloud = runCatching { cloudSaveRepository.downloadSave(userId) }.getOrNull()
                    val merged = mergeGameStates(gameEngine.state.value, cloud) ?: gameEngine.state.value
                    gameEngine.loadState(merged)
                    gameRepository.saveGameState(merged)
                    runCatching { cloudSaveRepository.uploadSave(userId, merged) }
                        .onSuccess { _lastSyncedAt.value = Instant.now() }

                    _authMessage.value = "Signed in!"
                }
                .onFailure { e ->
                    Log.w(TAG, "Sign in failed", e)
                    _authMessage.value = e.message?.takeIf { it.isNotBlank() } ?: "Sign in failed."
                }
            _isAuthActionInProgress.value = false
        }
    }

    /**
     * Signs out entirely, then immediately re-establishes a fresh guest
     * session — local play always continues regardless of cloud identity
     * (see CLAUDE.md's Auth section). The old account's cloud row is synced
     * one last time first so nothing played under it is lost.
     */
    fun signOut() {
        if (_isAuthActionInProgress.value) return
        viewModelScope.launch {
            _isAuthActionInProgress.value = true
            _authMessage.value = null
            syncToCloud()
            runCatching { authRepository.signOut() }
                .onFailure { Log.w(TAG, "Sign out failed", it) }
            runCatching { authRepository.ensureSignedIn() }
                .onSuccess { userId ->
                    currentUserId = userId
                    _userEmail.value = authRepository.currentUserEmail()
                    refreshUsernameState()
                }
                .onFailure { e ->
                    Log.w(TAG, "Re-establishing guest session after sign out failed", e)
                    _authMessage.value = e.message?.takeIf { it.isNotBlank() } ?: "Sign out failed."
                }
            _isAuthActionInProgress.value = false
        }
    }

    fun dismissAuthMessage() {
        _authMessage.value = null
    }

    /**
     * Refreshes [username] to match the current session — called after
     * every point [userEmail] changes. A guest (null email) just clears it
     * without a network call, since guests are never shown the username
     * field at all (see `SettingsContent`'s `AccountCard`). Also covers a
     * pre-existing account signing in from before this feature shipped,
     * since it's checked fresh on every sign-in/sign-up, not just once at
     * account creation.
     */
    private suspend fun refreshUsernameState() {
        if (_userEmail.value == null) {
            _username.value = null
            return
        }
        _username.value = runCatching { authRepository.currentUsername() }
            .onFailure { Log.w(TAG, "Fetching username failed", it) }
            .getOrNull()
    }

    /**
     * Sets (or changes) the signed-in player's leaderboard username — see
     * `SettingsContent`'s `AccountCard` for the inline field this backs.
     * Re-validated here (not just trusting the UI's own gate) since a
     * caller could pass anything.
     */
    fun submitUsername(username: String) {
        if (_isUsernameActionInProgress.value) return
        if (!isValidUsername(username)) {
            _usernameMessage.value = "Usernames are 3-20 letters, numbers, or underscores."
            return
        }
        viewModelScope.launch {
            _isUsernameActionInProgress.value = true
            _usernameMessage.value = null
            runCatching { authRepository.setUsername(username) }
                .onSuccess { _username.value = username }
                .onFailure { e ->
                    Log.w(TAG, "Setting username failed", e)
                    _usernameMessage.value = usernameErrorMessage(e)
                }
            _isUsernameActionInProgress.value = false
        }
    }

    fun dismissUsernameMessage() {
        _usernameMessage.value = null
    }

    /**
     * Deliberately reads [RestException.statusCode]/[RestException.error]
     * rather than pattern-matching `e.message` — caught live, not
     * hypothetically: `RestException.message` (see its source) bundles the
     * *full* request diagnostics (URL, headers — including this session's
     * own bearer token) after the clean one-line [RestException.error], and
     * this call's own `Prefer: resolution=merge-duplicates` upsert header
     * means that dump always contains the literal word "duplicate"
     * regardless of what actually went wrong — an earlier
     * `message.contains("duplicate")` check flagged an unrelated "table
     * doesn't exist yet" error as "username already taken" during testing.
     * PostgREST returns [HTTP_CONFLICT] specifically for a unique-constraint
     * violation (our `profiles_username_lower_idx` index) — checking that
     * directly is both correct and avoids message-text guessing entirely.
     * Falling back to [RestException.error] (never `.message`) for any
     * other REST failure for the same reason: `.message` would leak that
     * header dump — including the auth token — straight into the dialog.
     */
    private fun usernameErrorMessage(e: Throwable): String {
        if (e is RestException) {
            if (e.statusCode == HTTP_CONFLICT) {
                return "That username is already taken — try another."
            }
            return e.error
        }
        return e.message?.takeIf { it.isNotBlank() } ?: "Couldn't save that username — try again."
    }

    fun dismissWelcomeBack() {
        _welcomeBackEarnings.value = null
        _isOfflineEarningsDoubled.value = false
        _adUnavailableMessage.value = null
    }

    /**
     * The Welcome Back dialog's "Watch Ad to Double" button. Grants a
     * second, identical [GameEngine.grantGold] credit on top of the offline
     * earnings already applied (see `GameEngine.applyOfflineEarnings`) once
     * the player watches the rewarded ad to completion — the reward, not
     * the act of tapping the button, is what pays out.
     */
    fun watchAdToDoubleOfflineEarnings(activity: Activity) {
        val earnings = _welcomeBackEarnings.value ?: return
        if (_isOfflineEarningsDoubled.value) return
        _adUnavailableMessage.value = null
        adManager.showAd(
            placement = RewardedPlacement.OFFLINE_EARNINGS_DOUBLE,
            activity = activity,
            onRewardEarned = {
                recordAdWatched()
                gameEngine.grantGold(earnings.goldEarned)
                _welcomeBackEarnings.value = earnings.copy(goldEarned = earnings.goldEarned * 2)
                _isOfflineEarningsDoubled.value = true
            },
            onUnavailable = {
                _adUnavailableMessage.value = "Ad isn't ready yet — try again in a moment."
            },
        )
    }

    fun dismissAdUnavailableMessage() {
        _adUnavailableMessage.value = null
    }

    // Result text from the last Shop ad-watch attempt — an error/cooldown
    // notice or an "Earned 2 pp!" confirmation; Shop doesn't need to
    // distinguish the two for v1, same as authMessage above.
    private val _platinumAdMessage = MutableStateFlow<String?>(null)
    val platinumAdMessage: StateFlow<String?> = _platinumAdMessage.asStateFlow()

    /**
     * The Shop's "Watch an Ad" button — earns [PLATINUM_AD_REWARD_PP]
     * Platinum Pieces once every [com.wyrmwhelp.idlehoard.domain.model.PLATINUM_AD_COOLDOWN],
     * gated by [GameState.lastPlatinumAdWatchedAt] rather than anything
     * ad-network-side, so the cooldown survives across sessions. Checks the
     * cooldown up front (so a tap while it's active never even asks
     * `AdManager` for an ad) and again in [GameEngine.grantPlatinumAdReward]
     * itself (so a race between two rapid taps can't double-grant).
     */
    fun watchAdForPlatinum(activity: Activity) {
        val state = gameEngine.state.value
        val cooldownRemaining = state.platinumAdCooldownRemaining()
        if (!cooldownRemaining.isZero) {
            _platinumAdMessage.value = "Come back in ${DurationFormat.format(cooldownRemaining)} to watch again."
            return
        }
        _platinumAdMessage.value = null
        adManager.showAd(
            placement = RewardedPlacement.SHOP_PLATINUM,
            activity = activity,
            onRewardEarned = {
                recordAdWatched()
                _platinumAdMessage.value = if (gameEngine.grantPlatinumAdReward()) {
                    "Earned ${GoldFormat.format(PLATINUM_AD_REWARD_PP)} pp!"
                } else {
                    "Come back later to watch again."
                }
            },
            onUnavailable = {
                _platinumAdMessage.value = "Ad isn't ready yet — try again in a moment."
            },
        )
    }

    fun dismissPlatinumAdMessage() {
        _platinumAdMessage.value = null
    }

    // Result text from the last Shop Speed-boost ad-watch attempt — same
    // one-message shape as _platinumAdMessage above.
    private val _speedBoostAdMessage = MutableStateFlow<String?>(null)
    val speedBoostAdMessage: StateFlow<String?> = _speedBoostAdMessage.asStateFlow()

    /**
     * The Shop's ad-watch Speed-boost button — grants a temporary 2x Speed
     * boost (see `domain/model/AdRewards.kt`'s `SPEED_BOOST_AD_MULTIPLIER`/
     * `SPEED_BOOST_AD_DURATION`) as long as one of the four independent
     * daily slots is free, gated by [GameState.speedBoostAdWatchTimestamps]
     * rather than anything ad-network-side, so the cooldowns survive across
     * sessions. Checks up front (so a tap while all slots are busy never
     * even asks `AdManager` for an ad) and again in
     * [GameEngine.grantSpeedBoostAdReward] itself (so a race between two
     * rapid taps can't double-grant), same shape as [watchAdForPlatinum].
     */
    fun watchAdForSpeedBoost(activity: Activity) {
        val state = gameEngine.state.value
        val cooldownRemaining = state.speedBoostAdCooldownRemaining()
        if (!cooldownRemaining.isZero) {
            _speedBoostAdMessage.value = "Come back in ${DurationFormat.format(cooldownRemaining)} for another slot."
            return
        }
        _speedBoostAdMessage.value = null
        adManager.showAd(
            placement = RewardedPlacement.SHOP_SPEED_BOOST,
            activity = activity,
            onRewardEarned = {
                recordAdWatched()
                _speedBoostAdMessage.value = if (gameEngine.grantSpeedBoostAdReward()) {
                    "Reward earned! ${GoldFormat.format(SPEED_BOOST_AD_MULTIPLIER)}x Speed stacked in — see the live countdown below."
                } else {
                    "Come back later to watch again."
                }
            },
            onUnavailable = {
                _speedBoostAdMessage.value = "Ad isn't ready yet — try again in a moment."
            },
        )
    }

    fun dismissSpeedBoostAdMessage() {
        _speedBoostAdMessage.value = null
    }

    // Result text from the last Income-boost ad-watch attempt — same shape as _speedBoostAdMessage above.
    private val _incomeBoostAdMessage = MutableStateFlow<String?>(null)
    val incomeBoostAdMessage: StateFlow<String?> = _incomeBoostAdMessage.asStateFlow()

    /** Same as [watchAdForSpeedBoost] but for the Income-boost ad-watch reward — see `domain/model/AdRewards.kt`. */
    fun watchAdForIncomeBoost(activity: Activity) {
        val state = gameEngine.state.value
        val cooldownRemaining = state.incomeBoostAdCooldownRemaining()
        if (!cooldownRemaining.isZero) {
            _incomeBoostAdMessage.value = "Come back in ${DurationFormat.format(cooldownRemaining)} for another slot."
            return
        }
        _incomeBoostAdMessage.value = null
        adManager.showAd(
            placement = RewardedPlacement.AD_BOOST_INCOME,
            activity = activity,
            onRewardEarned = {
                recordAdWatched()
                _incomeBoostAdMessage.value = if (gameEngine.grantIncomeBoostAdReward()) {
                    "Reward earned! ${GoldFormat.format(INCOME_BOOST_AD_MULTIPLIER)}x Income stacked in — see the live countdown below."
                } else {
                    "Come back later to watch again."
                }
            },
            onUnavailable = {
                _incomeBoostAdMessage.value = "Ad isn't ready yet — try again in a moment."
            },
        )
    }

    fun dismissIncomeBoostAdMessage() {
        _incomeBoostAdMessage.value = null
    }

    /** Play Store's own formatted price per product id (e.g. "$4.99") — see `BillingManager.formattedPrices`. Empty until Play Billing resolves them. */
    val platinumPurchasePrices: StateFlow<Map<String, String>> = billingManager.formattedPrices

    // Result text from the last Shop IAP attempt — a "Purchased 550 Platinum
    // Pieces!" confirmation or a short failure message; same one-message
    // shape as _platinumAdMessage above, set by runPlatinumPurchaseEventLoop.
    private val _platinumPurchaseMessage = MutableStateFlow<String?>(null)
    val platinumPurchaseMessage: StateFlow<String?> = _platinumPurchaseMessage.asStateFlow()

    /**
     * Starts the Play Billing connection — call once when the Shop section
     * opens (see `MainActivity`'s `WyrmWhelpApp`), not any earlier. See
     * `BillingManager`'s class doc for why this is deliberately lazy
     * rather than started at app launch like `AdManager`'s ad preloading.
     */
    fun ensureBillingConnected() {
        billingManager.connect()
    }

    /** The Shop's "Buy Platinum Pieces" row for [productId] — launches Play's own purchase sheet. See `BillingManager.launchPurchaseFlow`. */
    fun buyPlatinumPack(activity: Activity, productId: String) {
        billingManager.launchPurchaseFlow(activity, productId)
    }

    fun dismissPlatinumPurchaseMessage() {
        _platinumPurchaseMessage.value = null
    }

    /**
     * Buys this lair's current [BuyQuantity] and, if the purchase actually
     * went through, checks whether it crossed any [MilestoneAnnouncement]
     * rungs (this lair's own, or the global "Everything" one) by comparing
     * owned count before vs. after — see [milestonesCrossed]. Any newly
     * crossed rungs queue up behind [milestoneAnnouncement] for
     * `GameScreen` to pop up one at a time.
     */
    fun claimLair(lairId: String) {
        val lair = CreatureLairCatalog.get(lairId)
        val current = gameEngine.state.value
        val owned = current.ownedLair(lairId)
        val previousCount = owned.count
        val costMultiplier = StewardEfficiency.costMultiplier(owned.stewardEfficiencyLevel)
        val quantity = _buyQuantity.value.resolve(lair, owned.count, current.goldPieces, costMultiplier).coerceAtLeast(1)
        val purchased = gameEngine.purchaseLairs(lairId, quantity)
        if (purchased > 0) {
            enqueueMilestoneAnnouncements(gameEngine.state.value.milestonesCrossed(lairId, previousCount))
        }
    }

    private fun enqueueMilestoneAnnouncements(announcements: List<MilestoneAnnouncement>) {
        if (announcements.isEmpty()) return
        pendingMilestoneAnnouncements.addAll(announcements)
        if (_milestoneAnnouncement.value == null) {
            _milestoneAnnouncement.value = pendingMilestoneAnnouncements.removeFirstOrNull()
        }
    }

    /** Dismisses the currently shown milestone pop-up and reveals the next queued one, if any. */
    fun dismissMilestoneAnnouncement() {
        _milestoneAnnouncement.value = pendingMilestoneAnnouncements.removeFirstOrNull()
    }

    fun hireSteward(lairId: String) {
        gameEngine.hireSteward(lairId)
    }

    /** The avatar picker's selection callback — see `GameEngine.selectAvatar`. */
    fun selectAvatar(avatarId: String?) {
        gameEngine.selectAvatar(avatarId)
    }

    /** Dismisses the Stewards "new feature" badge — see `GameEngine.markStewardOpportunitiesSeen`. */
    fun markStewardOpportunitiesSeen() {
        gameEngine.markStewardOpportunitiesSeen()
    }

    /** Dismisses the Upgrades "new feature" badge — see `GameEngine.markUpgradeOpportunitiesSeen`. */
    fun markUpgradeOpportunitiesSeen() {
        gameEngine.markUpgradeOpportunitiesSeen()
    }

    /** The player tapping a lair to start its production cycle — see `GameEngine.startLairLoad`. */
    fun startLairLoad(lairId: String) {
        gameEngine.startLairLoad(lairId)
    }

    /** The Shop's permanent-boost tiles (2x/5x/10x Speed, 1.5x/2x/5x Profit, 1.5x/2x/5x Gem %). See `GameEngine.purchasePermanentBoost`. */
    fun purchasePermanentBoost(tier: PermanentBoostTier) {
        gameEngine.purchasePermanentBoost(tier)
    }

    /** The Shop's temporary-boost tiles (50x/100x Speed, 15x/25x Profit). See `GameEngine.purchaseTemporaryBoost`. */
    fun purchaseTemporaryBoost(option: TemporaryBoostOption) {
        gameEngine.purchaseTemporaryBoost(option)
    }

    fun purchaseTimeSkip(option: TimeSkipOption) {
        gameEngine.purchaseTimeSkip(option)
    }

    /** The Upgrades section's Gold tab — buys the next tier of one lair's own Profit/Speed line. See `GameEngine.purchaseGpLairUpgrade`. */
    fun purchaseGpLairUpgrade(lairId: String, category: UpgradeCategory) {
        gameEngine.purchaseGpLairUpgrade(lairId, category)
    }

    /** The Upgrades section's Gold tab — buys the next tier of a lair's own Steward Efficiency line. See `GameEngine.purchaseStewardEfficiencyUpgrade`. */
    fun purchaseStewardEfficiencyUpgrade(lairId: String) {
        gameEngine.purchaseStewardEfficiencyUpgrade(lairId)
    }

    /** The Upgrades section's Gold tab — buys the next tier of an "Everything" line. See `GameEngine.purchaseGpEverythingUpgrade`. */
    fun purchaseGpEverythingUpgrade(category: UpgradeCategory) {
        gameEngine.purchaseGpEverythingUpgrade(category)
    }

    /** The Upgrades section's Gems tab — buys the next Gem Efficiency tier. See `GameEngine.purchaseGemEfficiencyUpgrade`. */
    fun purchaseGemEfficiencyUpgrade() {
        gameEngine.purchaseGemEfficiencyUpgrade()
    }

    /**
     * The Level Up section's confirmed reset button — see
     * `GameEngine.performLevelUp`. Only shows [levelUpReward] when Gems were
     * actually earned; the Level Up screen itself is expected to disable its
     * button (and skip the confirmation dialog) whenever
     * `GameState.gemsEarnedFromLevelUp()` is 0, so reaching here with
     * nothing earned would mean the two disagreed rather than a normal
     * outcome — same defensive shape as `watchAdForPlatinum`.
     */
    fun performLevelUp() {
        val gemsEarned = gameEngine.performLevelUp()
        if (gemsEarned > 0) {
            _levelUpReward.value = gemsEarned
        }
    }

    /** Dismisses the Level Up reward pop-up. */
    fun dismissLevelUpReward() {
        _levelUpReward.value = null
    }

    /** Dismisses the Universal Steward unlock pop-up. */
    fun dismissUniversalStewardUnlocked() {
        _universalStewardUnlocked.value = false
    }

    private companion object {
        const val TAG = "GameViewModel"
        const val AUTOSAVE_INTERVAL_MS = 30_000L
        const val CLOUD_SYNC_INTERVAL_MS = 5 * 60_000L

        /**
         * The init load sequence (local load, sign-in, cloud merge, offline
         * earnings) can settle in well under a second on a warm launch,
         * which barely shows `LoadingScreen`'s video at all. Padding the
         * loading screen out to at least this long — see the `delay` right
         * before `_isLoading.value = false` below — is purely cosmetic,
         * not something load itself needs.
         */
        const val MIN_LOADING_SCREEN_DURATION_MS = 5_000L

        /** HTTP 409 — PostgREST's status code for a unique-constraint violation. See [usernameErrorMessage]. */
        const val HTTP_CONFLICT = 409
    }
}
