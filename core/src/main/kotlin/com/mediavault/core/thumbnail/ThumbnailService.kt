package com.mediavault.core.thumbnail

import com.mediavault.core.model.MediaFile
import java.nio.file.Path
import kotlinx.coroutines.flow.StateFlow

interface ThumbnailService {
    val states: StateFlow<Map<String, ThumbnailState>>

    fun thumbnailPath(mediaFile: MediaFile): Path
    fun request(mediaFile: MediaFile)
    fun close()
}
