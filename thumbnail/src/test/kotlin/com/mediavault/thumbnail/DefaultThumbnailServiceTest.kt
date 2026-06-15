package com.mediavault.thumbnail

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaType
import com.mediavault.core.thumbnail.ThumbnailStatus
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultThumbnailServiceTest {
    @Test
    fun doesNotRegenerateExistingThumbnail() = runTest {
        val cacheDirectory = createTempDirectory("mediavault-thumb-cache")
        val mediaFile = mediaFile()
        val cache = ThumbnailCache(cacheDirectory)
        val existingThumbnail = cache.pathFor(mediaFile.path)
        existingThumbnail.parent.createDirectories()
        existingThumbnail.writeText("cached")
        val generator = RecordingThumbnailGenerator()
        val service = service(cache, generator)

        try {
            service.request(mediaFile)
            advanceUntilIdle()

            assertEquals(0, generator.calls)
            assertEquals(ThumbnailStatus.READY, service.states.value[mediaFile.path]?.status)
        } finally {
            service.close()
        }
    }

    @Test
    fun generatesMissingThumbnailAsynchronously() = runTest {
        val mediaFile = mediaFile()
        val generator = RecordingThumbnailGenerator()
        val service = service(ThumbnailCache(createTempDirectory("mediavault-thumb-cache")), generator)

        try {
            service.request(mediaFile)
            advanceUntilIdle()

            assertEquals(1, generator.calls)
            assertEquals(ThumbnailStatus.READY, service.states.value[mediaFile.path]?.status)
            assertTrue(service.thumbnailPath(mediaFile).toFile().exists())
        } finally {
            service.close()
        }
    }

    @Test
    fun marksAudioAsUnsupported() = runTest {
        val mediaFile = mediaFile(mediaType = MediaType.AUDIO)
        val service = service(ThumbnailCache(createTempDirectory("mediavault-thumb-cache")), RecordingThumbnailGenerator())

        try {
            service.request(mediaFile)
            advanceUntilIdle()

            assertEquals(ThumbnailStatus.UNSUPPORTED, service.states.value[mediaFile.path]?.status)
        } finally {
            service.close()
        }
    }

    private fun TestScope.service(
        cache: ThumbnailCache,
        generator: RecordingThumbnailGenerator,
    ): DefaultThumbnailService = DefaultThumbnailService(
        cache = cache,
        generator = generator,
        dispatcher = UnconfinedTestDispatcher(testScheduler),
        scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler)),
    )

    private fun mediaFile(
        mediaType: MediaType = MediaType.IMAGE,
    ): MediaFile = MediaFile(
        path = "C:/media/photo.jpg",
        filename = "photo.jpg",
        extension = "jpg",
        mediaType = mediaType,
        size = 100,
        createdDate = Instant.parse("2024-01-01T00:00:00Z"),
        modifiedDate = Instant.parse("2024-01-01T00:00:00Z"),
        indexedAt = Instant.parse("2024-01-01T00:00:00Z"),
    )
}

private class RecordingThumbnailGenerator : ThumbnailGenerator {
    var calls = 0

    override fun generate(mediaFile: MediaFile, outputPath: Path): Boolean {
        calls += 1
        outputPath.parent.createDirectories()
        outputPath.writeText("generated")
        return true
    }
}
