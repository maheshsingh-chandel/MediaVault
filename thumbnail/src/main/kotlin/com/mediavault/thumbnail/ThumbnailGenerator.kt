package com.mediavault.thumbnail

import com.mediavault.core.model.MediaFile
import java.nio.file.Path

interface ThumbnailGenerator {
    fun generate(mediaFile: MediaFile, outputPath: Path): Boolean
}
