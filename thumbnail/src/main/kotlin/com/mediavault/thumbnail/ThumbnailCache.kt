package com.mediavault.thumbnail

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.Path

class ThumbnailCache(
    private val directory: Path = defaultThumbnailDirectory(),
) {
    fun pathFor(sourcePath: String): Path = directory.resolve("${sourcePath.sha256()}.jpg")

    companion object {
        fun defaultThumbnailDirectory(): Path {
            val appData = System.getenv("APPDATA")
                ?: Path(System.getProperty("user.home"), "AppData", "Roaming").toString()
            return Path(appData, "MediaVault", "thumbnails")
        }
    }
}

private fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(StandardCharsets.UTF_8))
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}
