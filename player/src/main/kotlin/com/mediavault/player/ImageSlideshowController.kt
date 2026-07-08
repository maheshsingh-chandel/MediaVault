package com.mediavault.player

import com.mediavault.core.model.MediaFile

class ImageSlideshowController(
    images: List<MediaFile> = emptyList(),
) {
    var images: List<MediaFile> = images
        private set
    var currentIndex: Int = 0
        private set
    var isRunning: Boolean = false
        private set

    val current: MediaFile?
        get() = images.getOrNull(currentIndex)

    fun setImages(files: List<MediaFile>, start: MediaFile? = null) {
        images = files
        currentIndex = start?.let { selected -> files.indexOfFirst { it.path == selected.path } }
            ?.takeIf { it >= 0 }
            ?: 0
    }

    fun start() {
        isRunning = true
    }

    fun stop() {
        isRunning = false
    }

    fun next(): MediaFile? {
        if (images.isEmpty()) return null
        currentIndex = if (currentIndex == images.lastIndex) 0 else currentIndex + 1
        return current
    }

    fun previous(): MediaFile? {
        if (images.isEmpty()) return null
        currentIndex = if (currentIndex == 0) images.lastIndex else currentIndex - 1
        return current
    }
}
