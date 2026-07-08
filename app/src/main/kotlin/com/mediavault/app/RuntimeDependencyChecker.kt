package com.mediavault.app

import com.mediavault.core.environment.RuntimeDependencyStatus
import java.io.File
import java.util.concurrent.TimeUnit

object RuntimeDependencyChecker {
    fun checkAll(): List<RuntimeDependencyStatus> = listOf(
        checkCommand("ffmpeg", "ffmpeg", "-version"),
        checkCommand("ffprobe", "ffprobe", "-version"),
        checkVlc(),
    )

    private fun checkVlc(): RuntimeDependencyStatus {
        val commandStatus = checkCommand("VLC", "vlc", "--version")
        if (commandStatus.available) return commandStatus

        val programFiles = listOfNotNull(
            System.getenv("ProgramFiles"),
            System.getenv("ProgramFiles(x86)"),
        )
        val installedPath = programFiles
            .map { File(it, "VideoLAN/VLC/vlc.exe") }
            .firstOrNull { it.isFile }

        return if (installedPath != null) {
            RuntimeDependencyStatus("VLC", true, installedPath.absolutePath)
        } else {
            RuntimeDependencyStatus("VLC", false, "Install VLC for video and audio playback")
        }
    }

    private fun checkCommand(
        name: String,
        command: String,
        argument: String,
    ): RuntimeDependencyStatus {
        val available = runCatching {
            val process = ProcessBuilder(command, argument)
                .redirectErrorStream(true)
                .start()
            process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0
        }.getOrDefault(false)

        return RuntimeDependencyStatus(
            name = name,
            available = available,
            details = if (available) "Available on PATH" else "Not found on PATH",
        )
    }
}
