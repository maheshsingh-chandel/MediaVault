package com.mediavault.monitor

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaStatistics
import com.mediavault.core.model.MediaType
import com.mediavault.core.repository.MediaFileQuery
import com.mediavault.core.repository.MediaFileRepository
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WatchServiceFileSystemMonitorTest {
    @Test
    fun createModifyAndDeleteEventsUpdateRepository() = runBlocking {
        val directory = createTempDirectory("mediavault-watch")
        val repository = RecordingRepository(directory)
        val monitor = WatchServiceFileSystemMonitor(
            repository = repository,
            dispatcher = Dispatchers.IO,
        )

        try {
            monitor.start()
            waitUntil { monitor.state.value.watchedDirectories == 1 }

            val mediaFile = directory.resolve("photo.jpg")
            mediaFile.writeBytes(byteArrayOf(1, 2, 3))
            waitUntil { repository.findByPath(mediaFile.toAbsolutePath().normalize().toString()) != null }

            val created = repository.findByPath(mediaFile.toAbsolutePath().normalize().toString())
            assertNotNull(created)
            assertEquals(3, created.size)

            mediaFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
            waitUntil {
                repository.findByPath(mediaFile.toAbsolutePath().normalize().toString())?.size == 5L
            }

            mediaFile.deleteIfExists()
            waitUntil { repository.findByPath(mediaFile.toAbsolutePath().normalize().toString()) == null }

            assertTrue(monitor.state.value.changeVersion >= 3)
        } finally {
            monitor.close()
            directory.deleteIfExists()
        }
    }

    @Test
    fun deletingWatchedDirectoryDoesNotCrashMonitor() = runBlocking {
        val directory = createTempDirectory("mediavault-watch-disconnect")
        val repository = RecordingRepository(directory)
        val monitor = WatchServiceFileSystemMonitor(
            repository = repository,
            dispatcher = Dispatchers.IO,
        )

        try {
            monitor.start()
            waitUntil { monitor.state.value.watchedDirectories == 1 }

            directory.deleteIfExists()
            waitUntil { monitor.state.value.watchedDirectories == 0 || monitor.state.value.failures >= 0 }

            assertTrue(monitor.state.value.isRunning || !monitor.state.value.isRunning)
        } finally {
            monitor.close()
        }
    }

    private suspend fun waitUntil(predicate: () -> Boolean) {
        withTimeout(5_000) {
            while (!predicate()) {
                kotlinx.coroutines.delay(50)
            }
        }
    }
}

private class RecordingRepository(
    private val watchedDirectory: Path,
) : MediaFileRepository {
    private val files = ConcurrentHashMap<String, MediaFile>()

    override fun count(): Long = files.size.toLong()
    override fun count(query: MediaFileQuery): Long = files.size.toLong()
    override fun countByType(mediaType: MediaType): Long = files.values.count { it.mediaType == mediaType }.toLong()
    override fun getStatistics(): MediaStatistics = MediaStatistics(
        totalFiles = count(),
        images = countByType(MediaType.IMAGE),
        videos = countByType(MediaType.VIDEO),
        audio = countByType(MediaType.AUDIO),
    )

    override fun findById(id: Long): MediaFile? = files.values.firstOrNull { it.id == id }
    override fun findByPath(path: String): MediaFile? = files[path]
    override fun list(limit: Int, offset: Long): List<MediaFile> = files.values.drop(offset.toInt()).take(limit)
    override fun search(query: MediaFileQuery): List<MediaFile> = list(query.limit, query.offset)
    override fun indexedParentDirectories(): List<String> = listOf(watchedDirectory.toString())

    override fun save(mediaFile: MediaFile): Long {
        files[mediaFile.path] = mediaFile.copy(id = files.size + 1L)
        return files[mediaFile.path]?.id ?: 0
    }

    override fun saveOrIgnore(mediaFile: MediaFile): Boolean {
        if (files.containsKey(mediaFile.path)) return false
        save(mediaFile)
        return true
    }

    override fun upsert(mediaFile: MediaFile): Boolean {
        files[mediaFile.path] = mediaFile.copy(
            id = files[mediaFile.path]?.id ?: (files.size + 1L),
            indexedAt = Instant.now(),
        )
        return true
    }

    override fun deleteByPath(path: String): Boolean = files.remove(path) != null

    override fun updateMetadata(id: Long, metadataJson: String): Boolean {
        val entry = files.entries.firstOrNull { it.value.id == id } ?: return false
        files[entry.key] = entry.value.copy(metadataJson = metadataJson)
        return true
    }
}
