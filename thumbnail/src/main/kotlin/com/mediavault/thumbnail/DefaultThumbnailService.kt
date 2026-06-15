package com.mediavault.thumbnail

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaType
import com.mediavault.core.thumbnail.ThumbnailService
import com.mediavault.core.thumbnail.ThumbnailState
import com.mediavault.core.thumbnail.ThumbnailStatus
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DefaultThumbnailService(
    private val cache: ThumbnailCache = ThumbnailCache(),
    private val generator: ThumbnailGenerator = MediaThumbnailGenerator(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher),
) : ThumbnailService {
    private val queue = Channel<MediaFile>(capacity = Channel.UNLIMITED)
    private val queuedPaths = mutableSetOf<String>()
    private val _states = MutableStateFlow<Map<String, ThumbnailState>>(emptyMap())
    override val states: StateFlow<Map<String, ThumbnailState>> = _states

    init {
        repeat(WORKER_COUNT) {
            scope.launch {
                for (mediaFile in queue) {
                    generate(mediaFile)
                }
            }
        }
    }

    override fun thumbnailPath(mediaFile: MediaFile): Path = cache.pathFor(mediaFile.path)

    override fun request(mediaFile: MediaFile) {
        val outputPath = thumbnailPath(mediaFile)
        if (outputPath.exists() && outputPath.isRegularFile()) {
            updateState(mediaFile.path, ThumbnailState(ThumbnailStatus.READY, outputPath))
            return
        }

        if (mediaFile.mediaType != MediaType.IMAGE && mediaFile.mediaType != MediaType.VIDEO) {
            updateState(mediaFile.path, ThumbnailState(ThumbnailStatus.UNSUPPORTED))
            return
        }

        synchronized(queuedPaths) {
            if (!queuedPaths.add(mediaFile.path)) {
                return
            }
        }

        updateState(mediaFile.path, ThumbnailState(ThumbnailStatus.QUEUED, outputPath))
        queue.trySend(mediaFile)
    }

    override fun close() {
        queue.close()
        scope.cancel()
    }

    private suspend fun generate(mediaFile: MediaFile) {
        val outputPath = thumbnailPath(mediaFile)
        updateState(mediaFile.path, ThumbnailState(ThumbnailStatus.GENERATING, outputPath))

        val generated = withContext(dispatcher) {
            runCatching {
                outputPath.parent?.createDirectories()
                if (outputPath.exists() && outputPath.isRegularFile()) {
                    true
                } else {
                    generator.generate(mediaFile, outputPath)
                }
            }.getOrDefault(false)
        }

        synchronized(queuedPaths) {
            queuedPaths.remove(mediaFile.path)
        }

        updateState(
            mediaFile.path,
            if (generated) {
                ThumbnailState(ThumbnailStatus.READY, outputPath)
            } else {
                ThumbnailState(ThumbnailStatus.FAILED, outputPath, "Unable to generate thumbnail")
            },
        )
    }

    private fun updateState(sourcePath: String, state: ThumbnailState) {
        _states.update { current -> current + (sourcePath to state) }
    }

    private companion object {
        const val WORKER_COUNT = 2
    }
}
