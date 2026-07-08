package com.mediavault.scanner

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaStatistics
import com.mediavault.core.model.MediaType
import com.mediavault.core.repository.DuplicateGroup
import com.mediavault.core.repository.MediaFileQuery
import com.mediavault.core.repository.MediaFileRepository
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WalkTreeMediaScannerTest {
    @Test
    fun storesSupportedMediaFilesRecursively() = runTest {
        val root = createTempDirectory("mediavault-scan")
        val nested = root.resolve("Pictures").createDirectories()
        nested.resolve("image.jpg").createFile()
        nested.resolve("clip.mkv").createFile()
        nested.resolve("notes.txt").createFile()
        val repository = FakeMediaFileRepository()
        val scanner = WalkTreeMediaScanner(
            repository = repository,
            driveProvider = StaticDriveProvider(root),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        scanner.scanAllMountedDrives()

        assertEquals(2, repository.saved.size)
        assertEquals(2, scanner.progress.value.discoveredMediaFiles)
        assertFalse(scanner.progress.value.isScanning)
    }

    @Test
    fun skipsExcludedDirectories() = runTest {
        val root = createTempDirectory("mediavault-scan")
        val windows = root.resolve("Windows").createDirectories()
        windows.resolve("secret.mp4").createFile()
        root.resolve("visible.mp3").createFile()
        val repository = FakeMediaFileRepository()
        val scanner = WalkTreeMediaScanner(
            repository = repository,
            driveProvider = StaticDriveProvider(root),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        scanner.scanAllMountedDrives()

        assertEquals(listOf("visible.mp3"), repository.saved.map { it.filename })
        assertEquals(1, scanner.progress.value.skippedDirectories)
    }

    @Test
    fun recordsFailureForMissingRootWithoutThrowing() = runTest {
        val missingRoot = Path.of("Z:/definitely/missing/mediavault/root")
        val scanner = WalkTreeMediaScanner(
            repository = FakeMediaFileRepository(),
            driveProvider = StaticDriveProvider(missingRoot),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        scanner.scanAllMountedDrives()

        assertEquals(1, scanner.progress.value.permissionFailures)
        assertFalse(scanner.progress.value.isScanning)
    }
}

private class StaticDriveProvider(
    private val root: Path,
) : MountedDriveProvider {
    override fun mountedDrives(): List<Path> = listOf(root)
}

private class FakeMediaFileRepository : MediaFileRepository {
    val saved = mutableListOf<MediaFile>()
    private val savedPaths = mutableSetOf<String>()

    override fun count(): Long = saved.size.toLong()

    override fun count(query: MediaFileQuery): Long = search(query).size.toLong()

    override fun countByType(mediaType: MediaType): Long = saved.count { it.mediaType == mediaType }.toLong()

    override fun getStatistics(): MediaStatistics = MediaStatistics(
        totalFiles = count(),
        images = countByType(MediaType.IMAGE),
        videos = countByType(MediaType.VIDEO),
        audio = countByType(MediaType.AUDIO),
    )

    override fun findById(id: Long): MediaFile? = saved.firstOrNull { it.id == id }

    override fun findByPath(path: String): MediaFile? = saved.firstOrNull { it.path == path }

    override fun list(limit: Int, offset: Long): List<MediaFile> = saved
        .drop(offset.toInt())
        .take(limit)

    override fun search(query: MediaFileQuery): List<MediaFile> = saved
        .filter { it.filename.contains(query.searchText, ignoreCase = true) }
        .drop(query.offset.toInt())
        .take(query.limit)

    override fun indexedParentDirectories(): List<String> = saved.map { java.nio.file.Path.of(it.path).parent.toString() }

    override fun filesNeedingHash(limit: Int): List<MediaFile> = saved
        .filter { it.sha256 == null || it.hashedSize != it.size || it.hashedModifiedDate != it.modifiedDate }
        .take(limit)

    override fun duplicateGroups(limit: Int, offset: Long): List<DuplicateGroup> = emptyList()

    override fun save(mediaFile: MediaFile): Long {
        saved += mediaFile
        savedPaths += mediaFile.path
        return saved.size.toLong()
    }

    override fun saveOrIgnore(mediaFile: MediaFile): Boolean {
        if (!savedPaths.add(mediaFile.path)) {
            return false
        }
        saved += mediaFile
        return true
    }

    override fun upsert(mediaFile: MediaFile): Boolean {
        val index = saved.indexOfFirst { it.path == mediaFile.path }
        if (index >= 0) {
            saved[index] = mediaFile
        } else {
            saved += mediaFile
        }
        return true
    }

    override fun deleteByPath(path: String): Boolean = saved.removeIf { it.path == path }

    override fun updateMetadata(id: Long, metadataJson: String): Boolean {
        val index = saved.indexOfFirst { it.id == id }
        if (index < 0) return false
        saved[index] = saved[index].copy(metadataJson = metadataJson)
        return true
    }

    override fun updateHash(id: Long, sha256: String, size: Long, modifiedDate: java.time.Instant): Boolean {
        val index = saved.indexOfFirst { it.id == id }
        if (index < 0) return false
        saved[index] = saved[index].copy(
            sha256 = sha256,
            hashedSize = size,
            hashedModifiedDate = modifiedDate,
        )
        return true
    }
}
