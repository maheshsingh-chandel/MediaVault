package com.mediavault.core.monitor

import kotlinx.coroutines.flow.StateFlow

interface FileSystemMonitor {
    val state: StateFlow<FileSystemMonitorState>

    fun start()
    fun refreshWatchedDirectories()
    fun close()
}
