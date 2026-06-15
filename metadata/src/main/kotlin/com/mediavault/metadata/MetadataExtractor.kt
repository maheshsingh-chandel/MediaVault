package com.mediavault.metadata

import com.mediavault.core.model.MediaFile

fun interface MetadataExtractor {
    fun extract(mediaFile: MediaFile): String
}
