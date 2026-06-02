package com.mediavault.scanner

import com.mediavault.core.model.MediaFile
import com.mediavault.core.repository.MediaFileRepository
import com.mediavault.core.scanner.MediaScanner
import com.mediavault.core.scanner.ScanProgress
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class WalkTreeMediaScanner(
    private val repository: MediaFileRepository,
    private val driveProvider: MountedDriveProvider = DefaultMountedDriveProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MediaScanner {
    private val _progress = MutableStateFlow(ScanProgress())
    override val progress: StateFlow<ScanProgress> = _progress

    override suspend fun scanAllMountedDrives() {
        withContext(dispatcher) {
            val drives = driveProvider.mountedDrives()
            val startedAt = Instant.now()

            _progress.value = ScanProgress(
                isScanning = true,
                drivesTotal = drives.size,
                startedAt = startedAt,
                message = "Scanning mounted drives",
            )

            drives.forEachIndexed { index, drive ->
                updateProgress {
                    copy(
                        currentDrive = index + 1,
                        currentPath = drive.toString(),
                        message = "Scanning $drive",
                    )
                }
                scanRoot(drive)
            }

            updateProgress {
                copy(
                    isScanning = false,
                    currentPath = "",
                    completedAt = Instant.now(),
                    message = "Scan complete",
                )
            }
        }
    }

    suspend fun scanRoots(roots: List<Path>) {
        withContext(dispatcher) {
            val startedAt = Instant.now()
            _progress.value = ScanProgress(
                isScanning = true,
                drivesTotal = roots.size,
                startedAt = startedAt,
                message = "Scanning selected roots",
            )

            roots.forEachIndexed { index, root ->
                updateProgress {
                    copy(
                        currentDrive = index + 1,
                        currentPath = root.toString(),
                        message = "Scanning $root",
                    )
                }
                scanRoot(root)
            }

            updateProgress {
                copy(
                    isScanning = false,
                    currentPath = "",
                    completedAt = Instant.now(),
                    message = "Scan complete",
                )
            }
        }
    }

    private fun scanRoot(root: Path) {
        try {
            Files.walkFileTree(root, ScannerFileVisitor())
        } catch (exception: IOException) {
            recordPermissionFailure(root, exception)
        } catch (exception: SecurityException) {
            recordPermissionFailure(root, exception)
        }
    }

    private inner class ScannerFileVisitor : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (shouldExclude(dir)) {
                updateProgress {
                    copy(
                        skippedDirectories = skippedDirectories + 1,
                        currentPath = dir.toString(),
                        message = "Skipped excluded directory",
                    )
                }
                return FileVisitResult.SKIP_SUBTREE
            }

            updateProgress { copy(currentPath = dir.toString()) }
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            val mediaType = MediaTypeDetector.detect(file)
            val scannedCount = progress.value.scannedFiles + 1

            if (mediaType == null) {
                if (scannedCount % PROGRESS_UPDATE_INTERVAL == 0L) {
                    updateProgress {
                        copy(
                            scannedFiles = scannedCount,
                            currentPath = file.toString(),
                        )
                    }
                } else {
                    updateProgress { copy(scannedFiles = scannedCount) }
                }
                return FileVisitResult.CONTINUE
            }

            val mediaFile = MediaFile(
                path = file.toAbsolutePath().normalize().toString(),
                filename = file.fileName?.toString() ?: file.toString(),
                extension = MediaTypeDetector.extension(file),
                mediaType = mediaType,
                size = attrs.size(),
                createdDate = attrs.creationTime().toInstant(),
                modifiedDate = attrs.lastModifiedTime().toInstant(),
                indexedAt = Instant.now(),
            )

            val stored = runCatching { repository.saveOrIgnore(mediaFile) }.getOrDefault(false)

            updateProgress {
                copy(
                    scannedFiles = scannedFiles + 1,
                    discoveredMediaFiles = if (stored) discoveredMediaFiles + 1 else discoveredMediaFiles,
                    currentPath = file.toString(),
                    message = if (stored) "Indexed media file" else message,
                )
            }

            return FileVisitResult.CONTINUE
        }

        override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
            recordPermissionFailure(file, exc)
            return FileVisitResult.CONTINUE
        }
    }

    private fun shouldExclude(directory: Path): Boolean {
        val name = directory.fileName?.toString() ?: return false
        return excludedDirectories.any { it.equals(name, ignoreCase = true) }
    }

    private fun recordPermissionFailure(path: Path, exception: Exception) {
        updateProgress {
            copy(
                permissionFailures = permissionFailures + 1,
                currentPath = path.toString(),
                message = exception.message ?: "Unable to access path",
            )
        }
    }

    private fun updateProgress(transform: ScanProgress.() -> ScanProgress) {
        _progress.update(transform)
    }

    private companion object {
        const val PROGRESS_UPDATE_INTERVAL = 50L

        val excludedDirectories = setOf(
            "Windows",
            "Program Files",
            "Program Files (x86)",
            "ProgramData",
            "\$Recycle.Bin",
            "Recovery",
            "PerfLogs",
            "System Volume Information",
        )
    }
}
