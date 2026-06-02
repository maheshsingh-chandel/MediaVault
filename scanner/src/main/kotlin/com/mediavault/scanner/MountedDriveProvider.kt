package com.mediavault.scanner

import java.nio.file.FileSystems
import java.nio.file.Path

interface MountedDriveProvider {
    fun mountedDrives(): List<Path>
}

class DefaultMountedDriveProvider : MountedDriveProvider {
    override fun mountedDrives(): List<Path> = FileSystems
        .getDefault()
        .rootDirectories
        .toList()
}
