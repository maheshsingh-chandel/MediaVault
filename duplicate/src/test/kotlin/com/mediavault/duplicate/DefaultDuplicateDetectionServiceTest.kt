package com.mediavault.duplicate

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaStatistics
import com.mediavault.core.model.MediaType
import com.mediavault.core.repository.DuplicateGroup
import com.mediavault.core.repository.MediaFileQuery
import com.mediavault.core.repository.MediaFileRepository
import java.time.Instant
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultDuplicateDetectionServiceTest {
    @Test
    fun hashesOnlyPendingFilesAndStoresHashInputs() = runTest {
        val file = createTempFile("mediavault-duplicate", ".txt")
        file.writeText("same")
        val repository = PendingHashRepository(
            mediaFile(path = file.toString()),
            mediaFile(path = "missing.txt", sha256 = "already-hashed"),
        )
        val service = DefaultDuplicateDetectionService(repository)

        assertEquals(1, service.hashPending(limit = 10))

        val updated = repository.files.first()
        assertEquals("0967115f2813a3541eaef77de9d9d5773f1c0c04314b0bbfe4ff3b3b1c55b5d5", updated.sha256)
        assertEquals(updated.size, updated.hashedSize)
        assertEquals(updated.modifiedDate, updated.hashedModifiedDate)
    }

    private fun mediaFile(
        path: String,
        sha256: String? = null,
    ): MediaFile = MediaFile(
        id = if (sha256 == null) 1 else 2,
        path = path,
        filename = path.substringAfterLast('\\').substringAfterLast('/'),
        extension = "txt",
        mediaType = MediaType.OTHER,
        size = 4,
        createdDate = Instant.parse("2024-01-01T00:00:00Z"),
        modifiedDate = Instant.parse("2024-01-01T00:00:00Z"),
        indexedAt = Instant.parse("2024-01-01T00:00:00Z"),
        sha256 = sha256,
        hashedSize = if (sha256 == null) null else 4,
        hashedModifiedDate = if (sha256 == null) null else Instant.parse("2024-01-01T00:00:00Z"),
    )
}

private class PendingHashRepository(
    vararg mediaFiles: MediaFile,
) : MediaFileRepository {
    val files = mediaFiles.toMutableList()

    override fun count(): Long = files.size.toLong()
    override fun count(query: MediaFileQuery): Long = files.size.toLong()
    override fun countByType(mediaType: MediaType): Long = files.count { it.mediaType == mediaType }.toLong()
    override fun getStatistics(): MediaStatistics = MediaStatistics(count(), 0, 0, 0)
    override fun findById(id: Long): MediaFile? = files.firstOrNull { it.id == id }
    override fun findByPath(path: String): MediaFile? = files.firstOrNull { it.path == path }
    override fun list(limit: Int, offset: Long): List<MediaFile> = files.drop(offset.toInt()).take(limit)
    override fun search(query: MediaFileQuery): List<MediaFile> = list(query.limit, query.offset)
    override fun indexedParentDirectories(): List<String> = emptyList()
    override fun filesNeedingHash(limit: Int): List<MediaFile> = files
        .filter { it.sha256 == null || it.hashedSize != it.size || it.hashedModifiedDate != it.modifiedDate }
        .take(limit)
    override fun duplicateGroups(limit: Int, offset: Long): List<DuplicateGroup> = emptyList()
    override fun save(mediaFile: MediaFile): Long = mediaFile.id
    override fun saveOrIgnore(mediaFile: MediaFile): Boolean = false
    override fun upsert(mediaFile: MediaFile): Boolean = false
    override fun deleteByPath(path: String): Boolean = false
    override fun updateMetadata(id: Long, metadataJson: String): Boolean = false
    override fun updateHash(id: Long, sha256: String, size: Long, modifiedDate: Instant): Boolean {
        val index = files.indexOfFirst { it.id == id }
        if (index < 0) return false
        files[index] = files[index].copy(
            sha256 = sha256,
            hashedSize = size,
            hashedModifiedDate = modifiedDate,
        )
        return true
    }
}
