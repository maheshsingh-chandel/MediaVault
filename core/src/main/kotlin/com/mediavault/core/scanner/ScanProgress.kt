package com.mediavault.core.scanner

import java.time.Instant

data class ScanProgress(
    val isScanning: Boolean = false,
    val drivesTotal: Int = 0,
    val currentDrive: Int = 0,
    val currentPath: String = "",
    val scannedFiles: Long = 0,
    val discoveredMediaFiles: Long = 0,
    val skippedDirectories: Long = 0,
    val permissionFailures: Long = 0,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val message: String = "Ready",
) {
    val driveLabel: String
        get() = if (drivesTotal == 0) "No drives" else "$currentDrive of $drivesTotal"
}
