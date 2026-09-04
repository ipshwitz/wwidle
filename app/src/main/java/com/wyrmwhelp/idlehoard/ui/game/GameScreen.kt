package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val welcomeBack by viewModel.welcomeBackEarnings.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "${GoldFormat.format(state.goldPieces)} gp",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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

    welcomeBack?.let { earnings ->
        WelcomeBackDialog(earnings = earnings, onDismiss = viewModel::dismissWelcomeBack)
    }
}
