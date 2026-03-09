package com.spaceexplorer.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spaceexplorer.data.local.dao.ApodDao
import com.spaceexplorer.data.local.entity.ApodEntity

@Database(
    entities = [ApodEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun apodDao(): ApodDao
}
