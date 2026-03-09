package com.spaceexplorer.fake

import com.spaceexplorer.data.local.dao.ApodDao
import com.spaceexplorer.data.local.entity.ApodEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeApodDao : ApodDao {

    private val _favorites = MutableStateFlow<List<ApodEntity>>(emptyList())

    var insertCallCount = 0
    var deleteByDateCallCount = 0
    var lastInsertedEntity: ApodEntity? = null
    var lastDeletedDate: String? = null

    override fun getAllFavorites(): Flow<List<ApodEntity>> = _favorites

    override suspend fun getFavoriteByDate(date: String): ApodEntity? =
        _favorites.value.find { it.date == date }

    override fun isFavorite(date: String): Flow<Boolean> =
        _favorites.map { list -> list.any { it.date == date } }

    override suspend fun insertFavorite(apod: ApodEntity) {
        insertCallCount++
        lastInsertedEntity = apod
        _favorites.value = _favorites.value
            .filterNot { it.date == apod.date } + apod
    }

    override suspend fun deleteFavorite(apod: ApodEntity) {
        _favorites.value = _favorites.value.filterNot { it.date == apod.date }
    }

    override suspend fun deleteFavoriteByDate(date: String) {
        deleteByDateCallCount++
        lastDeletedDate = date
        _favorites.value = _favorites.value.filterNot { it.date == date }
    }

    fun seed(vararg entities: ApodEntity) {
        _favorites.value = entities.toList()
    }
}

fun testEntity(
    date: String = "2024-01-15",
    title: String = "Andromeda Galaxy",
    explanation: String = "A test explanation.",
    url: String = "https://apod.nasa.gov/apod/image/test.jpg",
    hdUrl: String? = null,
    mediaType: String = "image",
    copyright: String? = null
) = ApodEntity(
    date = date,
    title = title,
    explanation = explanation,
    url = url,
    hdUrl = hdUrl,
    mediaType = mediaType,
    copyright = copyright
)
