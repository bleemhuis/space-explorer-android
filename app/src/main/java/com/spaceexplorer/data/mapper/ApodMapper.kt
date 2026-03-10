package com.spaceexplorer.data.mapper

import com.spaceexplorer.data.local.entity.ApodCacheEntity
import com.spaceexplorer.data.local.entity.ApodEntity
import com.spaceexplorer.data.remote.dto.ApodDto
import com.spaceexplorer.domain.model.Apod

internal fun ApodDto.toDomain(): Apod = Apod(
    date = date,
    title = title,
    explanation = explanation,
    url = url,
    hdUrl = hdUrl,
    mediaType = mediaType,
    copyright = copyright,
    thumbnailUrl = thumbnailUrl
)

internal fun ApodEntity.toDomain(): Apod = Apod(
    date = date,
    title = title,
    explanation = explanation,
    url = url,
    hdUrl = hdUrl,
    mediaType = mediaType,
    copyright = copyright,
    thumbnailUrl = thumbnailUrl
)

internal fun Apod.toEntity(): ApodEntity = ApodEntity(
    date = date,
    title = title,
    explanation = explanation,
    url = url,
    hdUrl = hdUrl,
    mediaType = mediaType,
    copyright = copyright,
    thumbnailUrl = thumbnailUrl
)

internal fun ApodCacheEntity.toDomain(): Apod = Apod(
    date = date,
    title = title,
    explanation = explanation,
    url = url,
    hdUrl = hdUrl,
    mediaType = mediaType,
    copyright = copyright,
    thumbnailUrl = thumbnailUrl
)

internal fun Apod.toCacheEntity(): ApodCacheEntity = ApodCacheEntity(
    date = date,
    title = title,
    explanation = explanation,
    url = url,
    hdUrl = hdUrl,
    mediaType = mediaType,
    copyright = copyright,
    thumbnailUrl = thumbnailUrl
)
