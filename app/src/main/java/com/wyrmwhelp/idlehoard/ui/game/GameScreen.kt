package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyrmwhelp.idlehoard.ui.common.AppBackground

@Composable
fun GameScreen(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val welcomeBack by viewModel.welcomeBackEarnings.collectAsStateWithLifecycle()
    val buyQuantity by viewModel.buyQuantity.collectAsStateWithLifecycle()

    // Theoretical total income rate across owned lairs, independent of
    // whether each has a Steward — matches how idle games typically show a
    // "per sec" stat regardless of manual-tap vs. auto-collect status.
    val goldPerSecond = viewModel.lairs.sumOf { lair ->
        val owned = state.ownedLair(lair.id)
        if (owned.count > 0) lair.incomePerCycle(owned.count) / lair.baseProductionSeconds else 0.0
    }

    AppBackground(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                GameHeader(
                    state = GameHeaderState(
                        goldPieces = state.goldPieces,
                        goldPerSecond = goldPerSecond,
                        platinumPieces = state.platinumPieces,
                        buyQuantity = buyQuantity,
                    ),
                    onCycleBuyQuantity = viewModel::cycleBuyQuantity,
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(viewModel.lairs, key = { it.id }) { lair ->
                    val owned = state.ownedLair(lair.id)
                    LairCard(
                        lair = lair,
                        owned = owned,
                        goldPieces = state.goldPieces,
                        onClaim = { viewModel.claimLair(lair.id) },
                        onHireSteward = { viewModel.hireSteward(lair.id) },
                        onPlunder = { viewModel.plunderLair(lair.id) },
                    )
                }
            }
        }
    }

    welcomeBack?.let { earnings ->
        WelcomeBackDialog(earnings = earnings, onDismiss = viewModel::dismissWelcomeBack)
    }
}
