package com.spaceexplorer.domain.usecase

import com.spaceexplorer.domain.repository.ApodRepository
import javax.inject.Inject

class RemoveFavoriteUseCase @Inject constructor(
    private val repository: ApodRepository
) {
    suspend operator fun invoke(date: String): Result<Unit> =
        runCatching { repository.removeFavorite(date) }
}
