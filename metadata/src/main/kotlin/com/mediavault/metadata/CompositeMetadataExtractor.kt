package com.mediavault.metadata

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaType

class CompositeMetadataExtractor(
    private val imageExtractor: MetadataExtractor = ImageMetadataExtractor(),
    private val videoExtractor: MetadataExtractor = VideoMetadataExtractor(),
    private val audioExtractor: MetadataExtractor = AudioMetadataExtractor(),
) : MetadataExtractor {
    override fun extract(mediaFile: MediaFile): String = when (mediaFile.mediaType) {
        MediaType.IMAGE -> imageExtractor.extract(mediaFile)
        MediaType.VIDEO -> videoExtractor.extract(mediaFile)
        MediaType.AUDIO -> audioExtractor.extract(mediaFile)
        MediaType.OTHER -> unsupportedMetadata(mediaFile, "Unsupported media type")
    }
}
