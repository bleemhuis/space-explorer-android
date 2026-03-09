package com.spaceexplorer.data.repository

import com.spaceexplorer.data.local.dao.ApodCacheDao
import com.spaceexplorer.data.local.dao.ApodDao
import com.spaceexplorer.data.mapper.toCacheEntity
import com.spaceexplorer.data.mapper.toDomain
import com.spaceexplorer.data.mapper.toEntity
import com.spaceexplorer.data.remote.api.NasaApiService
import com.spaceexplorer.domain.model.Apod
import com.spaceexplorer.domain.repository.ApodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

class ApodRepositoryImpl @Inject constructor(
    private val apiService: NasaApiService,
    private val dao: ApodDao,
    private val cacheDao: ApodCacheDao
) : ApodRepository {

    override suspend fun getApod(date: String?): Result<Apod> =
        withContext(Dispatchers.IO) {
            val networkResult = runCatching { apiService.getApod(date).toDomain() }
            if (networkResult.isSuccess) {
                val apod = networkResult.getOrThrow()
                cacheDao.insert(apod.toCacheEntity())
                val count = cacheDao.count()
                if (count > CACHE_MAX_SIZE) {
                    cacheDao.deleteOldest(count - CACHE_MAX_SIZE)
                }
                networkResult
            } else {
                val cacheKey = date ?: LocalDate.now(ZoneOffset.UTC).toString()
                val cached = cacheDao.getByDate(cacheKey)
                if (cached != null) {
                    Result.success(cached.toDomain())
                } else {
                    networkResult
                }
            }
        }

    override suspend fun getApodRange(startDate: String, endDate: String): Result<List<Apod>> =
        withContext(Dispatchers.IO) {
            runCatching {
                apiService.getApodRange(startDate, endDate)
                    .map { it.toDomain() }
                    .sortedByDescending { it.date }
            }
        }

    override fun getFavorites(): Flow<List<Apod>> =
        dao.getAllFavorites().map { entities -> entities.map { it.toDomain() } }

    override fun isFavorite(date: String): Flow<Boolean> =
        dao.isFavorite(date)

    override suspend fun addFavorite(apod: Apod) =
        withContext(Dispatchers.IO) { dao.insertFavorite(apod.toEntity()) }

    override suspend fun removeFavorite(date: String) =
        withContext(Dispatchers.IO) { dao.deleteFavoriteByDate(date) }

    companion object {
        const val CACHE_MAX_SIZE = 50
    }
}
