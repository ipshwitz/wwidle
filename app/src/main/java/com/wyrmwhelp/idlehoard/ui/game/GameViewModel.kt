package com.wyrmwhelp.idlehoard.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import com.wyrmwhelp.idlehoard.domain.engine.GameEngine
import com.wyrmwhelp.idlehoard.domain.engine.OfflineEarnings
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameEngine: GameEngine,
    private val gameRepository: GameRepository,
) : ViewModel() {

    val gameState: StateFlow<GameState> = gameEngine.state

    val lairs: List<CreatureLair> = CreatureLairCatalog.lairs

    private val _welcomeBackEarnings = MutableStateFlow<OfflineEarnings?>(null)
    val welcomeBackEarnings: StateFlow<OfflineEarnings?> = _welcomeBackEarnings.asStateFlow()

    init {
        viewModelScope.launch {
            gameRepository.loadGameState()?.let { saved -> gameEngine.loadState(saved) }

            val earnings = gameEngine.applyOfflineEarnings()
            if (earnings.goldEarned > 0.0) {
                _welcomeBackEarnings.value = earnings
            }
            gameEngine.start()
            runAutosaveLoop()
        }
    }

    private suspend fun CoroutineScope.runAutosaveLoop() {
        while (isActive) {
            delay(AUTOSAVE_INTERVAL_MS)
            gameRepository.saveGameState(gameEngine.state.value)
        }
    }

    fun dismissWelcomeBack() {
        _welcomeBackEarnings.value = null
    }

    fun claimLair(lairId: String) {
        gameEngine.purchaseLair(lairId)
    }

    fun hireSteward(lairId: String) {
        gameEngine.hireSteward(lairId)
    }

    fun plunderLair(lairId: String) {
        gameEngine.plunderLair(lairId)
    }

    private companion object {
        const val AUTOSAVE_INTERVAL_MS = 30_000L
    }
}
