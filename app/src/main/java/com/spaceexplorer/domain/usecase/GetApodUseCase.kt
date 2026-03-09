package com.spaceexplorer.domain.usecase

import com.spaceexplorer.domain.model.Apod
import com.spaceexplorer.domain.repository.ApodRepository
import javax.inject.Inject

class GetApodUseCase @Inject constructor(
    private val repository: ApodRepository
) {
    suspend operator fun invoke(date: String? = null): Result<Apod> =
        repository.getApod(date)
}
