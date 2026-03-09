package com.spaceexplorer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spaceexplorer.data.local.entity.ApodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApodDao {

    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<ApodEntity>>

    @Query("SELECT * FROM favorites WHERE date = :date")
    suspend fun getFavoriteByDate(date: String): ApodEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE date = :date)")
    fun isFavorite(date: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(apod: ApodEntity)

    @Delete
    suspend fun deleteFavorite(apod: ApodEntity)

    @Query("DELETE FROM favorites WHERE date = :date")
    suspend fun deleteFavoriteByDate(date: String)
}
