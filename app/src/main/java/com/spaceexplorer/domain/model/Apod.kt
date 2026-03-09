package com.spaceexplorer.domain.model

data class Apod(
    val date: String,
    val title: String,
    val explanation: String,
    val url: String,
    val hdUrl: String?,
    val mediaType: String,
    val copyright: String?,
    val thumbnailUrl: String?
) {
    val isVideo: Boolean get() = mediaType == "video"
    val displayUrl: String get() = if (isVideo) thumbnailUrl ?: url else url
}
