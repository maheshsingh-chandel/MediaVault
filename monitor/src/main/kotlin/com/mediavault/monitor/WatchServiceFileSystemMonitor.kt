package com.mediavault.monitor

import com.mediavault.core.model.MediaFile
import com.mediavault.core.monitor.FileSystemMonitor
import com.mediavault.core.monitor.FileSystemMonitorState
import com.mediavault.core.repository.MediaFileRepository
import java.io.IOException
import java.nio.file.ClosedWatchServiceException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WatchServiceFileSystemMonitor(
    private val repository: MediaFileRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher),
    private val watchServiceFactory: () -> WatchService = {
        Path.of(System.getProperty("user.home")).fileSystem.newWatchService()
    },
) : FileSystemMonitor {
    private val _state = MutableStateFlow(FileSystemMonitorState())
    override val state: StateFlow<FileSystemMonitorState> = _state

    private val watchedKeys = mutableMapOf<WatchKey, Path>()
    private val watchedDirectories = mutableSetOf<Path>()
    private var watchService: WatchService? = null

    override fun start() {
        if (_state.value.isRunning) return

        scope.launch {
            val service = runCatching { watchServiceFactory() }
                .getOrElse {
                    recordFailure("Unable to start filesystem monitor: ${it.message}")
                    return@launch
                }
            watchService = service
            _state.update { it.copy(isRunning = true, message = "Filesystem monitor running") }
            refreshWatchedDirectories()
            watchLoop(service)
        }
    }

    override fun refreshWatchedDirectories() {
        scope.launch {
            repository.indexedParentDirectories()
                .asSequence()
                .map { Path.of(it).toAbsolutePath().normalize() }
                .forEach(::registerDirectory)
        }
    }

    override fun close() {
        runCatching { watchService?.close() }
        scope.cancel()
        _state.update { it.copy(isRunning = false, message = "Filesystem monitor stopped") }
    }

    private fun watchLoop(service: WatchService) {
        while (_state.value.isRunning) {
            val key = try {
                service.take()
            } catch (_: ClosedWatchServiceException) {
                break
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }

            val directory = watchedKeys[key]
            if (directory == null) {
                key.reset()
                continue
            }

            key.pollEvents().forEach { event ->
                handleEvent(directory, event)
            }

            if (!key.reset()) {
                watchedKeys.remove(key)
                watchedDirectories.remove(directory)
                _state.update {
                    it.copy(
                        watchedDirectories = watchedDirectories.size,
                        message = "Stopped watching unavailable directory: $directory",
                    )
                }
            }
        }

        _state.update { it.copy(isRunning = false, message = "Filesystem monitor stopped") }
    }

    private fun handleEvent(directory: Path, event: WatchEvent<*>) {
        if (event.kind() == OVERFLOW) {
            recordFailure("Filesystem event overflow")
            return
        }

        val context = event.context() as? Path ?: return
        val path = directory.resolve(context).toAbsolutePath().normalize()

        when (event.kind()) {
            ENTRY_CREATE -> handleCreate(path)
            ENTRY_MODIFY -> handleModify(path)
            ENTRY_DELETE -> handleDelete(path)
        }
    }

    private fun handleCreate(path: Path) {
        if (Files.isDirectory(path)) {
            registerDirectory(path)
            return
        }

        val stored = upsertPath(path)
        if (stored) {
            _state.update {
                it.copy(
                    created = it.created + 1,
                    changeVersion = it.changeVersion + 1,
                    message = "Indexed created file: $path",
                )
            }
        }
    }

    private fun handleModify(path: Path) {
        val stored = upsertPath(path)
        if (stored) {
            _state.update {
                it.copy(
                    modified = it.modified + 1,
                    changeVersion = it.changeVersion + 1,
                    message = "Updated modified file: $path",
                )
            }
        }
    }

    private fun handleDelete(path: Path) {
        val deleted = runCatching { repository.deleteByPath(path.toString()) }.getOrDefault(false)
        if (deleted) {
            _state.update {
                it.copy(
                    deleted = it.deleted + 1,
                    changeVersion = it.changeVersion + 1,
                    message = "Removed deleted file: $path",
                )
            }
        }
    }

    private fun upsertPath(path: Path): Boolean {
        val mediaType = MonitorMediaTypeDetector.detect(path) ?: return false
        val attributes = runCatching { Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes::class.java) }
            .getOrElse {
                recordFailure("Unable to read file attributes: $path")
                return false
            }
        if (!attributes.isRegularFile) return false

        val mediaFile = MediaFile(
            path = path.toString(),
            filename = path.fileName?.toString() ?: path.toString(),
            extension = MonitorMediaTypeDetector.extension(path),
            mediaType = mediaType,
            size = attributes.size(),
            createdDate = attributes.creationTime().toInstant(),
            modifiedDate = attributes.lastModifiedTime().toInstant(),
            indexedAt = Instant.now(),
        )

        return runCatching { repository.upsert(mediaFile) }
            .getOrElse {
                recordFailure("Unable to update database for $path")
                false
            }
    }

    private fun registerDirectory(directory: Path) {
        if (directory in watchedDirectories || !Files.isDirectory(directory)) return

        runCatching {
            val key = directory.register(watchService ?: return, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
            watchedKeys[key] = directory
            watchedDirectories.add(directory)
            _state.update {
                it.copy(
                    watchedDirectories = watchedDirectories.size,
                    message = "Watching $directory",
                )
            }
        }.onFailure {
            recordFailure("Unable to watch directory: $directory")
        }
    }

    private fun recordFailure(message: String) {
        _state.update {
            it.copy(
                failures = it.failures + 1,
                message = message,
            )
        }
    }
}
