package com.spaceexplorer.data.remote.api

import com.spaceexplorer.data.remote.dto.ApodDto
import retrofit2.http.GET
import retrofit2.http.Query

interface NasaApiService {

    @GET("planetary/apod")
    suspend fun getApod(
        @Query("date") date: String? = null,
        @Query("thumbs") thumbs: Boolean = true
    ): ApodDto

    @GET("planetary/apod")
    suspend fun getApodRange(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String? = null,
        @Query("thumbs") thumbs: Boolean = true
    ): List<ApodDto>
}
