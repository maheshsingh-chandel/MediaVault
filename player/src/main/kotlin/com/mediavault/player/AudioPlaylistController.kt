package com.mediavault.player

import com.mediavault.core.model.MediaFile
import kotlin.random.Random

class AudioPlaylistController(
    playlist: List<MediaFile> = emptyList(),
    private val random: Random = Random.Default,
) {
    var playlist: List<MediaFile> = playlist
        private set
    var currentIndex: Int = playlist.indexOfFirst { it.mediaType.name == "AUDIO" }.coerceAtLeast(0)
        private set
    var shuffleEnabled: Boolean = false
        private set
    var repeatMode: RepeatMode = RepeatMode.OFF
        private set

    val current: MediaFile?
        get() = playlist.getOrNull(currentIndex)

    fun setPlaylist(files: List<MediaFile>, start: MediaFile? = null) {
        playlist = files
        currentIndex = start?.let { selected -> files.indexOfFirst { it.path == selected.path } }
            ?.takeIf { it >= 0 }
            ?: 0
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
    }

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun next(): MediaFile? {
        if (playlist.isEmpty()) return null
        if (repeatMode == RepeatMode.ONE) return current

        currentIndex = if (shuffleEnabled && playlist.size > 1) {
            random.nextInt(playlist.size)
        } else if (currentIndex < playlist.lastIndex) {
            currentIndex + 1
        } else if (repeatMode == RepeatMode.ALL) {
            0
        } else {
            currentIndex
        }
        return current
    }

    fun previous(): MediaFile? {
        if (playlist.isEmpty()) return null
        if (repeatMode == RepeatMode.ONE) return current

        currentIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else if (repeatMode == RepeatMode.ALL) {
            playlist.lastIndex
        } else {
            currentIndex
        }
        return current
    }
}
