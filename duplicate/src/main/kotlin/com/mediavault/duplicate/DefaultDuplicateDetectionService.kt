package com.mediavault.duplicate

import com.mediavault.core.duplicate.DuplicateDetectionService
import com.mediavault.core.repository.MediaFileRepository
import java.nio.file.Path
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultDuplicateDetectionService(
    private val repository: MediaFileRepository,
    private val hasher: Sha256Hasher = Sha256Hasher(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DuplicateDetectionService {
    override suspend fun hashPending(limit: Int): Int = withContext(dispatcher) {
        repository.filesNeedingHash(limit).count { mediaFile ->
            val hash = runCatching { hasher.hash(Path.of(mediaFile.path)) }.getOrNull()
            hash != null && repository.updateHash(
                id = mediaFile.id,
                sha256 = hash,
                size = mediaFile.size,
                modifiedDate = mediaFile.modifiedDate,
            )
        }
    }
}
