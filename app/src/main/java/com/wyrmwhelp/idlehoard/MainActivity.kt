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
import com.wyrmwhelp.idlehoard.ui.common.SectionOverlayCard
import com.wyrmwhelp.idlehoard.ui.game.GameScreen
import com.wyrmwhelp.idlehoard.ui.game.GameViewModel
import com.wyrmwhelp.idlehoard.ui.menu.FloatingMenu
import com.wyrmwhelp.idlehoard.ui.theme.WyrmWhelpIdleHoardTheme
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
        )
    }
}
