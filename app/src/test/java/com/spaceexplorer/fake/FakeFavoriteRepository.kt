package com.spaceexplorer.fake

import com.spaceexplorer.domain.model.Apod
import com.spaceexplorer.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class FakeFavoriteRepository : FavoriteRepository {

    val storedFavorites = mutableListOf<Apod>()

    var addFavoriteCallCount = 0
    var removeFavoriteCallCount = 0
    var lastRemovedDate: String? = null

    /** Set to simulate a Room Flow exception in getFavorites(). */
    var flowException: Throwable? = null

    override fun getFavorites(): Flow<List<Apod>> {
        flowException?.let { ex -> return flow { throw ex } }
        return flowOf(storedFavorites.toList())
    }

    override fun isFavorite(date: String): Flow<Boolean> =
        flowOf(storedFavorites.any { it.date == date })

    override suspend fun addFavorite(apod: Apod) {
        addFavoriteCallCount++
        storedFavorites.add(apod)
    }

    override suspend fun removeFavorite(date: String) {
        removeFavoriteCallCount++
        lastRemovedDate = date
        storedFavorites.removeAll { it.date == date }
    }
}
