package com.wyrmwhelp.idlehoard.ui.game

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wyrmwhelp.idlehoard.ads.AdManager
import com.wyrmwhelp.idlehoard.ads.RewardedPlacement
import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import com.wyrmwhelp.idlehoard.domain.engine.GameEngine
import com.wyrmwhelp.idlehoard.domain.engine.OfflineEarnings
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.PLATINUM_AD_REWARD_PP
import com.wyrmwhelp.idlehoard.domain.model.mergeGameStates
import com.wyrmwhelp.idlehoard.domain.model.platinumAdCooldownRemaining
import com.wyrmwhelp.idlehoard.domain.model.TimeSkipOption
import com.wyrmwhelp.idlehoard.ui.format.DurationFormat
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat
import com.wyrmwhelp.idlehoard.domain.repository.AuthRepository
import com.wyrmwhelp.idlehoard.domain.repository.CloudSaveRepository
import com.wyrmwhelp.idlehoard.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
 * [watchAdForPlatinum].
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameEngine: GameEngine,
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository,
    private val cloudSaveRepository: CloudSaveRepository,
    private val adManager: AdManager,
) : ViewModel() {

    val gameState: StateFlow<GameState> = gameEngine.state

    val lairs: List<CreatureLair> = CreatureLairCatalog.lairs

    private val _welcomeBackEarnings = MutableStateFlow<OfflineEarnings?>(null)
    val welcomeBackEarnings: StateFlow<OfflineEarnings?> = _welcomeBackEarnings.asStateFlow()

    // Only one rewarded-ad watch is allowed per Welcome Back pop-up —
    // reset whenever a new one appears (see dismissWelcomeBack).
    private val _isOfflineEarningsDoubled = MutableStateFlow(false)
    val isOfflineEarningsDoubled: StateFlow<Boolean> = _isOfflineEarningsDoubled.asStateFlow()

    private val _adUnavailableMessage = MutableStateFlow<String?>(null)
    val adUnavailableMessage: StateFlow<String?> = _adUnavailableMessage.asStateFlow()

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

    init {
        viewModelScope.launch {
            val local = gameRepository.loadGameState()

            // Cloud sync is best-effort: a network hiccup or Supabase outage should
            // never block local play, so every step here degrades to local-only.
            val userId = runCatching { authRepository.ensureSignedIn() }
                .onFailure { Log.w(TAG, "Sign-in failed, continuing offline", it) }
                .getOrNull()
            currentUserId = userId
            _userEmail.value = userId?.let { authRepository.currentUserEmail() }

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
            launch { runAutosaveLoop() }
            runCloudSyncLoop()
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

    fun claimLair(lairId: String) {
        val lair = CreatureLairCatalog.get(lairId)
        val current = gameEngine.state.value
        val owned = current.ownedLair(lairId)
        val quantity = _buyQuantity.value.resolve(lair, owned.count, current.goldPieces).coerceAtLeast(1)
        gameEngine.purchaseLairs(lairId, quantity)
    }

    fun hireSteward(lairId: String) {
        gameEngine.hireSteward(lairId)
    }

    fun plunderLair(lairId: String) {
        gameEngine.plunderLair(lairId)
    }

    fun purchaseSpeedBoost() {
        gameEngine.purchaseSpeedBoost()
    }

    fun purchaseProfitBoost() {
        gameEngine.purchaseProfitBoost()
    }

    fun purchaseTimeSkip(option: TimeSkipOption) {
        gameEngine.purchaseTimeSkip(option)
    }

    private companion object {
        const val TAG = "GameViewModel"
        const val AUTOSAVE_INTERVAL_MS = 30_000L
        const val CLOUD_SYNC_INTERVAL_MS = 5 * 60_000L
    }
}
