package com.mediavault.core.scanner

import kotlinx.coroutines.flow.StateFlow

interface MediaScanner {
    val progress: StateFlow<ScanProgress>

    suspend fun scanAllMountedDrives()
}
