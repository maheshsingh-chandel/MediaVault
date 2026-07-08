package com.mediavault.core.monitor

data class FileSystemMonitorState(
    val isRunning: Boolean = false,
    val watchedDirectories: Int = 0,
    val created: Long = 0,
    val modified: Long = 0,
    val deleted: Long = 0,
    val failures: Long = 0,
    val changeVersion: Long = 0,
    val message: String = "Filesystem monitor stopped",
)
