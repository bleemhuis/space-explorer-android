package com.spaceexplorer.domain.usecase

import com.spaceexplorer.domain.repository.FavoriteRepository
import javax.inject.Inject

class RemoveFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRepository
) {
    suspend operator fun invoke(date: String): Result<Unit> =
        runCatching { repository.removeFavorite(date) }
}
