package com.spaceexplorer.fake

import com.spaceexplorer.domain.model.Apod
import com.spaceexplorer.domain.repository.ApodRepository

class FakeApodRepository : ApodRepository {

    var apodResult: Result<Apod> = Result.success(testApod())
    var apodRangeResult: Result<List<Apod>> = Result.success(emptyList())

    var getApodCalled = false
    var lastGetApodDate: String? = null
    var lastRangeStart: String? = null
    var lastRangeEnd: String? = null

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
