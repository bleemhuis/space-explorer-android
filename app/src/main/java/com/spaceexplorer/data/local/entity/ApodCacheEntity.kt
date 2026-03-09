package com.spaceexplorer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apod_cache")
data class ApodCacheEntity(
    @PrimaryKey val date: String,
    val title: String,
    val explanation: String,
    val url: String,
    val hdUrl: String?,
    val mediaType: String,
    val copyright: String?,
    val thumbnailUrl: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
