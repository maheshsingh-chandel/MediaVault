package com.mediavault.thumbnail

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaType
import java.nio.file.Path

class MediaThumbnailGenerator(
    private val imageGenerator: ThumbnailGenerator = ImageThumbnailGenerator(),
    private val videoGenerator: ThumbnailGenerator = VideoThumbnailGenerator(),
) : ThumbnailGenerator {
    override fun generate(mediaFile: MediaFile, outputPath: Path): Boolean = when (mediaFile.mediaType) {
        MediaType.IMAGE -> imageGenerator.generate(mediaFile, outputPath)
        MediaType.VIDEO -> videoGenerator.generate(mediaFile, outputPath)
        MediaType.AUDIO,
        MediaType.OTHER -> false
    }
}
