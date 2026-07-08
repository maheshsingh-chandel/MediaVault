package com.mediavault.duplicate

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class Sha256Hasher {
    fun hash(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val DEFAULT_BUFFER_SIZE = 1024 * 1024
    }
}
