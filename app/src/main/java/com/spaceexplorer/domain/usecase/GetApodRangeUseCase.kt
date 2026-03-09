package com.spaceexplorer.domain.usecase

import com.spaceexplorer.domain.model.Apod
import com.spaceexplorer.domain.repository.ApodRepository
import javax.inject.Inject

class GetApodRangeUseCase @Inject constructor(
    private val repository: ApodRepository
) {
    suspend operator fun invoke(startDate: String, endDate: String): Result<List<Apod>> =
        repository.getApodRange(startDate, endDate)
}
