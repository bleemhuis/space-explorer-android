package com.spaceexplorer.domain.repository

import com.spaceexplorer.domain.model.Apod
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun getFavorites(): Flow<List<Apod>>
    fun isFavorite(date: String): Flow<Boolean>
    suspend fun addFavorite(apod: Apod)
    suspend fun removeFavorite(date: String)
}
