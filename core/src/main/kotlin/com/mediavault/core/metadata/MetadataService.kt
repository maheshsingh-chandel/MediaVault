package com.mediavault.core.metadata

import com.mediavault.core.model.MediaFile

interface MetadataService {
    suspend fun extractAndStore(mediaFile: MediaFile): MediaFile
}
