package com.wyrmwhelp.idlehoard.ui.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import com.wyrmwhelp.idlehoard.domain.engine.GameEngine
import com.wyrmwhelp.idlehoard.domain.engine.OfflineEarnings
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.mergeGameStates
import com.wyrmwhelp.idlehoard.domain.repository.AuthRepository
import com.wyrmwhelp.idlehoard.domain.repository.CloudSaveRepository
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
    private val authRepository: AuthRepository,
    private val cloudSaveRepository: CloudSaveRepository,
) : ViewModel() {

    val gameState: StateFlow<GameState> = gameEngine.state

    val lairs: List<CreatureLair> = CreatureLairCatalog.lairs

    private val _welcomeBackEarnings = MutableStateFlow<OfflineEarnings?>(null)
    val welcomeBackEarnings: StateFlow<OfflineEarnings?> = _welcomeBackEarnings.asStateFlow()

    // UI-only selection, not persisted — see BuyQuantity's doc for why it
    // doesn't affect purchase amounts yet.
    private val _buyQuantity = MutableStateFlow(BuyQuantity.X1)
    val buyQuantity: StateFlow<BuyQuantity> = _buyQuantity.asStateFlow()

    fun cycleBuyQuantity() {
        _buyQuantity.value = _buyQuantity.value.next()
    }

    init {
        viewModelScope.launch {
            val local = gameRepository.loadGameState()

            // Cloud sync is best-effort: a network hiccup or Supabase outage should
            // never block local play, so every step here degrades to local-only.
            val userId = runCatching { authRepository.ensureSignedIn() }
                .onFailure { Log.w(TAG, "Sign-in failed, continuing offline", it) }
                .getOrNull()
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
                    .onFailure { Log.w(TAG, "Cloud upload failed, continuing offline", it) }
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
        const val TAG = "GameViewModel"
        const val AUTOSAVE_INTERVAL_MS = 30_000L
    }
}
