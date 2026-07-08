package com.mediavault.thumbnail

import com.mediavault.core.environment.AppPaths
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest

class ThumbnailCache(
    private val directory: Path = defaultThumbnailDirectory(),
) {
    fun pathFor(sourcePath: String): Path = directory.resolve("${sourcePath.sha256()}.jpg")

    companion object {
        fun defaultThumbnailDirectory(): Path = AppPaths.thumbnailDirectory
    }
}

private fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(StandardCharsets.UTF_8))
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}
