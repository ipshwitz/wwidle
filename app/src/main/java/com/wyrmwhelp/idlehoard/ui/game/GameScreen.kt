package com.wyrmwhelp.idlehoard.ui.game

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyrmwhelp.idlehoard.domain.model.globalIncomeMilestoneMultiplier
import com.wyrmwhelp.idlehoard.domain.model.globalSpeedMilestoneMultiplier
import com.wyrmwhelp.idlehoard.domain.model.profitBoostMultiplier
import com.wyrmwhelp.idlehoard.domain.model.speedBoostMultiplier
import com.wyrmwhelp.idlehoard.ui.common.AppBackground

@Composable
fun GameScreen(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val welcomeBack by viewModel.welcomeBackEarnings.collectAsStateWithLifecycle()
    val isOfflineEarningsDoubled by viewModel.isOfflineEarningsDoubled.collectAsStateWithLifecycle()
    val adUnavailableMessage by viewModel.adUnavailableMessage.collectAsStateWithLifecycle()
    val buyQuantity by viewModel.buyQuantity.collectAsStateWithLifecycle()
    val milestoneAnnouncement by viewModel.milestoneAnnouncement.collectAsStateWithLifecycle()
    val lairProgress by viewModel.lairProgress.collectAsStateWithLifecycle()

    // The "Everything" milestone bonuses — same compounding schedule as each
    // lair's own bonus, but keyed on the lowest owned count across all of
    // them. Computed once per recomposition and threaded through, since
    // they're the same numbers for every lair this tick. Split into Speed
    // and Income since milestone rungs are one or the other, never both.
    val globalSpeedMultiplier = state.globalSpeedMilestoneMultiplier(viewModel.lairs)
    val globalIncomeMultiplier = state.globalIncomeMilestoneMultiplier(viewModel.lairs)
    val speedMultiplier = speedBoostMultiplier(state.speedBoostLevel)
    val profitMultiplier = profitBoostMultiplier(state.profitBoostLevel)

    // Total income rate from Steward-managed lairs only — the only ones
    // that actually run continuously on their own now. A lair without a
    // Steward sits idle earning nothing until tapped (see
    // `GameEngine.startLairLoad`), so including it here would overstate
    // what the player is actually earning per second while not playing.
    val goldPerSecond = viewModel.lairs.sumOf { lair ->
        val owned = state.ownedLair(lair.id)
        if (owned.count > 0 && owned.hasSteward) {
            lair.incomePerCycle(owned.count, globalIncomeMultiplier, profitMultiplier) /
                lair.effectiveProductionSeconds(owned.count, speedMultiplier, globalSpeedMultiplier)
        } else {
            0.0
        }
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
                    LairRow(
                        lair = lair,
                        owned = owned,
                        goldPieces = state.goldPieces,
                        buyQuantity = buyQuantity,
                        globalIncomeMultiplier = globalIncomeMultiplier,
                        progress = lairProgress[lair.id] ?: 0f,
                        productionSeconds = lair.effectiveProductionSeconds(owned.count, speedMultiplier, globalSpeedMultiplier),
                        onClaim = { viewModel.claimLair(lair.id) },
                        onStartLoad = { viewModel.startLairLoad(lair.id) },
                        profitBoostMultiplier = profitMultiplier,
                    )
                }
            }
        }
    }

    welcomeBack?.let { earnings ->
        val context = LocalContext.current
        WelcomeBackDialog(
            earnings = earnings,
            isDoubled = isOfflineEarningsDoubled,
            adUnavailableMessage = adUnavailableMessage,
            onWatchAd = {
                (context as? Activity)?.let { viewModel.watchAdToDoubleOfflineEarnings(it) }
            },
            onDismiss = viewModel::dismissWelcomeBack,
        )
    }

    milestoneAnnouncement?.let { announcement ->
        MilestoneReachedDialog(
            announcement = announcement,
            onDismiss = viewModel::dismissMilestoneAnnouncement,
        )
    }
}
