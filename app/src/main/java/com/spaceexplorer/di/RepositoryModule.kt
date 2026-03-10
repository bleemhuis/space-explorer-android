package com.spaceexplorer.di

import com.spaceexplorer.data.repository.ApodRepositoryImpl
import com.spaceexplorer.domain.repository.ApodRepository
import com.spaceexplorer.domain.repository.FavoriteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindApodRepository(impl: ApodRepositoryImpl): ApodRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: ApodRepositoryImpl): FavoriteRepository
}
