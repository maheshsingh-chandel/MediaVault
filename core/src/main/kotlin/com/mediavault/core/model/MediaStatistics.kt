package com.mediavault.core.model

data class MediaStatistics(
    val totalFiles: Long,
    val images: Long,
    val videos: Long,
    val audio: Long,
) {
    companion object {
        val Empty = MediaStatistics(
            totalFiles = 0,
            images = 0,
            videos = 0,
            audio = 0,
        )
    }
}
