package com.wyrmwhelp.idlehoard

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyrmwhelp.idlehoard.domain.model.activeTemporaryBoostsRemaining
import com.wyrmwhelp.idlehoard.domain.model.availableSpeedBoostAdSlots
import com.wyrmwhelp.idlehoard.domain.model.gemsEarnedFromLevelUp
import com.wyrmwhelp.idlehoard.domain.model.permanentBoostLevel
import com.wyrmwhelp.idlehoard.domain.model.platinumAdCooldownRemaining
import com.wyrmwhelp.idlehoard.domain.model.speedBoostAdCooldownRemaining
import com.wyrmwhelp.idlehoard.ui.common.ComingSoonPlaceholder
import com.wyrmwhelp.idlehoard.ui.common.SectionOverlayCard
import com.wyrmwhelp.idlehoard.ui.game.GameScreen
import com.wyrmwhelp.idlehoard.ui.game.GameViewModel
import com.wyrmwhelp.idlehoard.ui.helpsocial.HelpSocialContent
import com.wyrmwhelp.idlehoard.ui.levelup.LevelUpContent
import com.wyrmwhelp.idlehoard.ui.menu.FloatingMenu
import com.wyrmwhelp.idlehoard.ui.settings.SettingsContent
import com.wyrmwhelp.idlehoard.ui.shop.ShopContent
import com.wyrmwhelp.idlehoard.ui.stewards.StewardsContent
import com.wyrmwhelp.idlehoard.ui.theme.WyrmWhelpIdleHoardTheme
import com.wyrmwhelp.idlehoard.ui.unlocks.UnlocksContent
import com.wyrmwhelp.idlehoard.ui.upgrades.UpgradesContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WyrmWhelpIdleHoardTheme {
                WyrmWhelpApp(gameViewModel = gameViewModel)
            }
        }
    }
}

/**
 * The game screen is always mounted underneath everything else — menu
 * sections are cards that slide up over it (see [SectionOverlayCard]) rather
 * than separate destinations you navigate away to, so the game is never
 * actually left.
 */
@Composable
private fun WyrmWhelpApp(gameViewModel: GameViewModel) {
    var openSection by rememberSaveable { mutableStateOf<String?>(null) }
    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val userEmail by gameViewModel.userEmail.collectAsStateWithLifecycle()
    val pendingVerificationEmail by gameViewModel.pendingVerificationEmail.collectAsStateWithLifecycle()
    val isAuthActionInProgress by gameViewModel.isAuthActionInProgress.collectAsStateWithLifecycle()
    val authMessage by gameViewModel.authMessage.collectAsStateWithLifecycle()
    val isSyncing by gameViewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncedAt by gameViewModel.lastSyncedAt.collectAsStateWithLifecycle()
    val platinumAdMessage by gameViewModel.platinumAdMessage.collectAsStateWithLifecycle()
    val platinumPurchasePrices by gameViewModel.platinumPurchasePrices.collectAsStateWithLifecycle()
    val platinumPurchaseMessage by gameViewModel.platinumPurchaseMessage.collectAsStateWithLifecycle()
    val speedBoostAdMessage by gameViewModel.speedBoostAdMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(enabled = openSection != null) { openSection = null }

    // Play Billing's connection handshake is slow enough to noticeably
    // worsen cold-start time if started eagerly (see `BillingManager`'s
    // class doc) — only connect once the player actually opens the Shop.
    LaunchedEffect(openSection) {
        if (openSection == "Shop") gameViewModel.ensureBillingConnected()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GameScreen(viewModel = gameViewModel, modifier = Modifier.fillMaxSize())

        FloatingMenu(
            onItemSelected = { label -> openSection = label },
            modifier = Modifier.fillMaxSize(),
        )

        SectionOverlayCard(
            title = openSection,
            onDismiss = { openSection = null },
            modifier = Modifier.fillMaxSize(),
            content = when (openSection) {
                "Help & Social" -> {
                    {
                        HelpSocialContent(
                            onOpenLink = { url ->
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (e: ActivityNotFoundException) {
                                    Log.w("MainActivity", "No app found to handle $url", e)
                                }
                            },
                        )
                    }
                }
                "Unlocks" -> { { UnlocksContent(lairs = gameViewModel.lairs, state = gameState) } }
                "Stewards" -> {
                    {
                        StewardsContent(
                            lairs = gameViewModel.lairs,
                            state = gameState,
                            onHireSteward = gameViewModel::hireSteward,
                        )
                    }
                }
                "Shop" -> {
                    {
                        ShopContent(
                            platinumPieces = gameState.platinumPieces,
                            permanentBoostLevelFor = gameState::permanentBoostLevel,
                            activeTemporaryBoosts = gameState.activeTemporaryBoostsRemaining(),
                            isSignedIn = userEmail != null,
                            platinumAdCooldownRemaining = gameState.platinumAdCooldownRemaining(),
                            platinumAdMessage = platinumAdMessage,
                            platinumPurchasePrices = platinumPurchasePrices,
                            platinumPurchaseMessage = platinumPurchaseMessage,
                            speedBoostAdAvailableSlots = gameState.availableSpeedBoostAdSlots(),
                            speedBoostAdCooldownRemaining = gameState.speedBoostAdCooldownRemaining(),
                            speedBoostAdMessage = speedBoostAdMessage,
                            onBuyPermanentBoost = gameViewModel::purchasePermanentBoost,
                            onBuyTemporaryBoost = gameViewModel::purchaseTemporaryBoost,
                            onBuyTimeSkip = gameViewModel::purchaseTimeSkip,
                            onWatchAd = {
                                (context as? Activity)?.let { gameViewModel.watchAdForPlatinum(it) }
                            },
                            onDismissPlatinumAdMessage = gameViewModel::dismissPlatinumAdMessage,
                            onBuyPlatinumPack = { productId ->
                                (context as? Activity)?.let { gameViewModel.buyPlatinumPack(it, productId) }
                            },
                            onDismissPlatinumPurchaseMessage = gameViewModel::dismissPlatinumPurchaseMessage,
                            onWatchSpeedBoostAd = {
                                (context as? Activity)?.let { gameViewModel.watchAdForSpeedBoost(it) }
                            },
                            onDismissSpeedBoostAdMessage = gameViewModel::dismissSpeedBoostAdMessage,
                        )
                    }
                }
                "Upgrades" -> {
                    {
                        UpgradesContent(
                            lairs = gameViewModel.lairs,
                            state = gameState,
                            onBuyGpLairUpgrade = gameViewModel::purchaseGpLairUpgrade,
                            onBuyGpEverythingUpgrade = gameViewModel::purchaseGpEverythingUpgrade,
                            onBuyGemEfficiencyUpgrade = gameViewModel::purchaseGemEfficiencyUpgrade,
                        )
                    }
                }
                "Level Up" -> {
                    {
                        LevelUpContent(
                            gems = gameState.gems,
                            gemEfficiencyLevel = gameState.gemEfficiencyLevel,
                            gemsEarnable = gameState.gemsEarnedFromLevelUp(),
                            onLevelUp = gameViewModel::performLevelUp,
                        )
                    }
                }
                "Settings" -> {
                    {
                        SettingsContent(
                            userEmail = userEmail,
                            pendingVerificationEmail = pendingVerificationEmail,
                            isAuthActionInProgress = isAuthActionInProgress,
                            authMessage = authMessage,
                            isSyncing = isSyncing,
                            lastSyncedAt = lastSyncedAt,
                            onSignUp = gameViewModel::signUp,
                            onVerifySignUpCode = gameViewModel::verifySignUpCode,
                            onResendSignUpCode = gameViewModel::resendSignUpCode,
                            onCancelSignUpVerification = gameViewModel::cancelSignUpVerification,
                            onSignIn = gameViewModel::signIn,
                            onSignOut = gameViewModel::signOut,
                            onSyncNow = gameViewModel::syncNow,
                            onDismissAuthMessage = gameViewModel::dismissAuthMessage,
                        )
                    }
                }
                else -> { { ComingSoonPlaceholder() } }
            },
        )
    }
}
