package com.mediavault.metadata

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaStatistics
import com.mediavault.core.model.MediaType
import com.mediavault.core.repository.MediaFileQuery
import com.mediavault.core.repository.MediaFileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaMetadataServiceTest {
    @Test
    fun extractsAndStoresMetadataJson() = runTest {
        val repository = FakeMediaFileRepository(mediaFile(id = 42))
        val service = MediaMetadataService(
            repository = repository,
            extractor = MetadataExtractor { """{"status":"READY","image":{"width":10}}""" },
        )

        val updated = service.extractAndStore(repository.saved)

        assertEquals("""{"status":"READY","image":{"width":10}}""", updated.metadataJson)
        assertEquals("""{"status":"READY","image":{"width":10}}""", repository.saved.metadataJson)
    }
}

private class FakeMediaFileRepository(
    var saved: MediaFile,
) : MediaFileRepository {
    override fun count(): Long = 1
    override fun count(query: MediaFileQuery): Long = 1
    override fun countByType(mediaType: MediaType): Long = if (saved.mediaType == mediaType) 1 else 0
    override fun getStatistics(): MediaStatistics = MediaStatistics(1, 1, 0, 0)
    override fun findById(id: Long): MediaFile? = saved.takeIf { it.id == id }
    override fun list(limit: Int, offset: Long): List<MediaFile> = listOf(saved)
    override fun search(query: MediaFileQuery): List<MediaFile> = listOf(saved)
    override fun save(mediaFile: MediaFile): Long = mediaFile.id
    override fun saveOrIgnore(mediaFile: MediaFile): Boolean = false
    override fun updateMetadata(id: Long, metadataJson: String): Boolean {
        saved = saved.copy(metadataJson = metadataJson)
        return saved.id == id
    }
}
