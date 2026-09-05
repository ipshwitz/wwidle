package com.wyrmwhelp.idlehoard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyrmwhelp.idlehoard.ui.common.ComingSoonPlaceholder
import com.wyrmwhelp.idlehoard.ui.common.SectionOverlayCard
import com.wyrmwhelp.idlehoard.ui.game.GameScreen
import com.wyrmwhelp.idlehoard.ui.game.GameViewModel
import com.wyrmwhelp.idlehoard.ui.menu.FloatingMenu
import com.wyrmwhelp.idlehoard.ui.settings.SettingsContent
import com.wyrmwhelp.idlehoard.ui.shop.ShopContent
import com.wyrmwhelp.idlehoard.ui.stewards.StewardsContent
import com.wyrmwhelp.idlehoard.ui.theme.WyrmWhelpIdleHoardTheme
import com.wyrmwhelp.idlehoard.ui.unlocks.UnlocksContent
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
    val isAuthActionInProgress by gameViewModel.isAuthActionInProgress.collectAsStateWithLifecycle()
    val authMessage by gameViewModel.authMessage.collectAsStateWithLifecycle()
    val isSyncing by gameViewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncedAt by gameViewModel.lastSyncedAt.collectAsStateWithLifecycle()

    BackHandler(enabled = openSection != null) { openSection = null }

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
                            speedBoostLevel = gameState.speedBoostLevel,
                            profitBoostLevel = gameState.profitBoostLevel,
                            isSignedIn = userEmail != null,
                            onBuySpeedBoost = gameViewModel::purchaseSpeedBoost,
                            onBuyProfitBoost = gameViewModel::purchaseProfitBoost,
                            onBuyTimeSkip = gameViewModel::purchaseTimeSkip,
                        )
                    }
                }
                "Settings" -> {
                    {
                        SettingsContent(
                            userEmail = userEmail,
                            isAuthActionInProgress = isAuthActionInProgress,
                            authMessage = authMessage,
                            isSyncing = isSyncing,
                            lastSyncedAt = lastSyncedAt,
                            onSignUp = gameViewModel::signUp,
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
