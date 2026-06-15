package com.mediavault.core.thumbnail

import java.nio.file.Path

data class ThumbnailState(
    val status: ThumbnailStatus,
    val path: Path? = null,
    val message: String = "",
)

enum class ThumbnailStatus {
    QUEUED,
    GENERATING,
    READY,
    FAILED,
    UNSUPPORTED,
}
