package com.mediavault.monitor

import com.mediavault.core.model.MediaType
import java.nio.file.Path

object MonitorMediaTypeDetector {
    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
    private val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "wmv", "webm", "m4v")
    private val audioExtensions = setOf("mp3", "flac", "wav", "aac", "ogg", "m4a")

    fun detect(path: Path): MediaType? = when (extension(path)) {
        in imageExtensions -> MediaType.IMAGE
        in videoExtensions -> MediaType.VIDEO
        in audioExtensions -> MediaType.AUDIO
        else -> null
    }

    fun extension(path: Path): String {
        val filename = path.fileName?.toString() ?: return ""
        val dotIndex = filename.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < filename.lastIndex) {
            filename.substring(dotIndex + 1).lowercase()
        } else {
            ""
        }
    }
}
