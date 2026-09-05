package com.wyrmwhelp.idlehoard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.wyrmwhelp.idlehoard.ui.common.ComingSoonScreen
import com.wyrmwhelp.idlehoard.ui.game.GameScreen
import com.wyrmwhelp.idlehoard.ui.game.GameViewModel
import com.wyrmwhelp.idlehoard.ui.menu.FloatingMenu
import com.wyrmwhelp.idlehoard.ui.navigation.ComingSoonRoute
import com.wyrmwhelp.idlehoard.ui.navigation.GameRoute
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

@Composable
private fun WyrmWhelpApp(gameViewModel: GameViewModel) {
    val navController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = GameRoute) {
            composable<GameRoute> {
                GameScreen(viewModel = gameViewModel, modifier = Modifier.fillMaxSize())
            }
            composable<ComingSoonRoute> { backStackEntry ->
                val route: ComingSoonRoute = backStackEntry.toRoute()
                ComingSoonScreen(title = route.title, modifier = Modifier.fillMaxSize())
            }
        }

        FloatingMenu(
            onItemSelected = { label ->
                navController.navigate(ComingSoonRoute(title = label)) {
                    launchSingleTop = true
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
