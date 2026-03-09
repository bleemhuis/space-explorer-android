package com.spaceexplorer.domain.usecase

import com.spaceexplorer.domain.repository.ApodRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveIsFavoriteUseCase @Inject constructor(
    private val repository: ApodRepository
) {
    operator fun invoke(date: String): Flow<Boolean> = repository.isFavorite(date)
}
