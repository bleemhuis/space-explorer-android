package com.spaceexplorer.di

import android.content.Context
import androidx.room.Room
import com.spaceexplorer.data.local.dao.ApodCacheDao
import com.spaceexplorer.data.local.dao.ApodDao
import com.spaceexplorer.data.local.database.AppDatabase
import com.spaceexplorer.data.local.database.migrations.MIGRATION_1_2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "space_explorer.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideApodDao(database: AppDatabase): ApodDao = database.apodDao()

    @Provides
    fun provideApodCacheDao(database: AppDatabase): ApodCacheDao = database.apodCacheDao()
}
