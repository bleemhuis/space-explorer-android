package com.spaceexplorer.fake

import com.spaceexplorer.data.local.dao.ApodCacheDao
import com.spaceexplorer.data.local.entity.ApodCacheEntity

class FakeApodCacheDao : ApodCacheDao {

    private val cache = mutableMapOf<String, ApodCacheEntity>()

    var insertCallCount = 0
    var deleteOldestCallCount = 0
    var lastDeleteOldestN: Int = 0

    override suspend fun getByDate(date: String): ApodCacheEntity? = cache[date]

    override suspend fun insert(entity: ApodCacheEntity) {
        insertCallCount++
        cache[entity.date] = entity
    }

    override suspend fun count(): Int = cache.size

    override suspend fun deleteOldest(n: Int) {
        deleteOldestCallCount++
        lastDeleteOldestN = n
        cache.entries
            .sortedBy { it.value.cachedAt }
            .take(n)
            .forEach { cache.remove(it.key) }
    }

    fun seed(vararg entities: ApodCacheEntity) {
        entities.forEach { cache[it.date] = it }
    }
}

fun testCacheEntity(
    date: String = "2024-01-15",
    title: String = "Andromeda Galaxy",
    explanation: String = "A test explanation.",
    url: String = "https://apod.nasa.gov/apod/image/test.jpg",
    hdUrl: String? = null,
    mediaType: String = "image",
    copyright: String? = null,
    thumbnailUrl: String? = null,
    cachedAt: Long = System.currentTimeMillis()
) = ApodCacheEntity(
    date = date,
    title = title,
    explanation = explanation,
    url = url,
    hdUrl = hdUrl,
    mediaType = mediaType,
    copyright = copyright,
    thumbnailUrl = thumbnailUrl,
    cachedAt = cachedAt
)
