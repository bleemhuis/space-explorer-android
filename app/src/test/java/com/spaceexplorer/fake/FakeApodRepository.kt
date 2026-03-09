package com.spaceexplorer.fake

import com.spaceexplorer.domain.model.Apod
import com.spaceexplorer.domain.repository.ApodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeApodRepository : ApodRepository {

    val storedFavorites = mutableListOf<Apod>()

    var apodResult: Result<Apod> = Result.success(testApod())
    var apodRangeResult: Result<List<Apod>> = Result.success(emptyList())

    var getApodCalled = false
    var lastGetApodDate: String? = null
    var lastRangeStart: String? = null
    var lastRangeEnd: String? = null

    var addFavoriteCallCount = 0
    var removeFavoriteCallCount = 0
    var lastRemovedDate: String? = null

    override suspend fun getApod(date: String?): Result<Apod> {
        getApodCalled = true
        lastGetApodDate = date
        return apodResult
    }

    override suspend fun getApodRange(startDate: String, endDate: String): Result<List<Apod>> {
        lastRangeStart = startDate
        lastRangeEnd = endDate
        return apodRangeResult
    }

    override fun getFavorites(): Flow<List<Apod>> = flowOf(storedFavorites.toList())

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

fun testApod(
    date: String = "2024-01-15",
    title: String = "Andromeda Galaxy",
    explanation: String = "A test explanation.",
    url: String = "https://apod.nasa.gov/apod/image/test.jpg",
    hdUrl: String? = null,
    mediaType: String = "image",
    copyright: String? = null,
    thumbnailUrl: String? = null
) = Apod(
    date = date,
    title = title,
    explanation = explanation,
    url = url,
    hdUrl = hdUrl,
    mediaType = mediaType,
    copyright = copyright,
    thumbnailUrl = thumbnailUrl
)
