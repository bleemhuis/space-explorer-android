package com.spaceexplorer.domain.repository

import com.spaceexplorer.domain.model.Apod

interface ApodRepository {
    suspend fun getApod(date: String? = null): Result<Apod>
    suspend fun getApodRange(startDate: String, endDate: String): Result<List<Apod>>
}
