package com.mediavault.metadata

import com.mediavault.core.metadata.MetadataService
import com.mediavault.core.model.MediaFile
import com.mediavault.core.repository.MediaFileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaMetadataService(
    private val repository: MediaFileRepository,
    private val extractor: MetadataExtractor = CompositeMetadataExtractor(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MetadataService {
    override suspend fun extractAndStore(mediaFile: MediaFile): MediaFile = withContext(dispatcher) {
        val existing = repository.findById(mediaFile.id) ?: mediaFile
        if (!existing.metadataJson.isNullOrBlank()) {
            return@withContext existing
        }

        val metadataJson = extractor.extract(existing)
        repository.updateMetadata(existing.id, metadataJson)
        repository.findById(existing.id) ?: existing.copy(metadataJson = metadataJson)
    }
}
