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
import com.wyrmwhelp.idlehoard.domain.model.SPEED_BOOST_AD_DURATION
import com.wyrmwhelp.idlehoard.domain.model.SPEED_BOOST_AD_MULTIPLIER
import com.wyrmwhelp.idlehoard.domain.model.mergeGameStates
import com.wyrmwhelp.idlehoard.domain.model.milestonesCrossed
import com.wyrmwhelp.idlehoard.domain.model.platinumAdCooldownRemaining
import com.wyrmwhelp.idlehoard.domain.model.speedBoostAdCooldownRemaining
import com.wyrmwhelp.idlehoard.domain.model.PermanentBoostTier
import com.wyrmwhelp.idlehoard.domain.model.TemporaryBoostOption
import com.wyrmwhelp.idlehoard.domain.model.TimeSkipOption
import com.wyrmwhelp.idlehoard.domain.model.UpgradeCategory
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
 * [watchAdForPlatinum] — plus the Shop's real-money Platinum Pieces
 * packs, [buyPlatinumPack], via `BillingManager`.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameEngine: GameEngine,
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository,
    private val cloudSaveRepository: CloudSaveRepository,
    private val adManager: AdManager,
    private val billingManager: BillingManager,
) : ViewModel() {

    val gameState: StateFlow<GameState> = gameEngine.state

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
                _speedBoostAdMessage.value = if (gameEngine.grantSpeedBoostAdReward()) {
                    "${GoldFormat.format(SPEED_BOOST_AD_MULTIPLIER)}x Speed active for ${DurationFormat.format(SPEED_BOOST_AD_DURATION)}!"
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
        val quantity = _buyQuantity.value.resolve(lair, owned.count, current.goldPieces).coerceAtLeast(1)
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

    private companion object {
        const val TAG = "GameViewModel"
        const val AUTOSAVE_INTERVAL_MS = 30_000L
        const val CLOUD_SYNC_INTERVAL_MS = 5 * 60_000L
    }
}
