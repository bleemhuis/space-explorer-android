package com.spaceexplorer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spaceexplorer.data.local.entity.ApodCacheEntity

@Dao
interface ApodCacheDao {

    @Query("SELECT * FROM apod_cache WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): ApodCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ApodCacheEntity)

    @Query("SELECT COUNT(*) FROM apod_cache")
    suspend fun count(): Int

    @Query(
        "DELETE FROM apod_cache WHERE date IN " +
        "(SELECT date FROM apod_cache ORDER BY cachedAt ASC LIMIT :n)"
    )
    suspend fun deleteOldest(n: Int)
}
