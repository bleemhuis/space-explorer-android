package com.spaceexplorer.domain.usecase

import com.spaceexplorer.domain.model.Apod
import com.spaceexplorer.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoritesUseCase @Inject constructor(
    private val repository: FavoriteRepository
) {
    operator fun invoke(): Flow<List<Apod>> = repository.getFavorites()
}
