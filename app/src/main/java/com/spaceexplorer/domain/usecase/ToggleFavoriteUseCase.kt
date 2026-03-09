package com.spaceexplorer.domain.usecase

import com.spaceexplorer.domain.model.Apod
import com.spaceexplorer.domain.repository.ApodRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: ApodRepository
) {
    suspend operator fun invoke(apod: Apod): Result<Unit> = runCatching {
        if (repository.isFavorite(apod.date).first()) {
            repository.removeFavorite(apod.date)
        } else {
            repository.addFavorite(apod)
        }
    }
}
