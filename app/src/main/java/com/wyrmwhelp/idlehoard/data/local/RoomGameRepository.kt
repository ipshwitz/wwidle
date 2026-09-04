package com.wyrmwhelp.idlehoard.data.local

import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.repository.GameRepository
import javax.inject.Inject

class RoomGameRepository @Inject constructor(
    private val dao: GameStateDao,
) : GameRepository {

    override suspend fun loadGameState(): GameState? {
        val stateEntity = dao.getGameState() ?: return null
        val lairEntities = dao.getOwnedLairs()
        return stateEntity.toDomain(lairEntities)
    }

    override suspend fun saveGameState(state: GameState) {
        val (stateEntity, lairEntities) = state.toEntities()
        dao.saveAll(stateEntity, lairEntities)
    }
}
