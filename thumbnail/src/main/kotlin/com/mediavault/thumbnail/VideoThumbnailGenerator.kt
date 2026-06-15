package com.mediavault.thumbnail

import com.mediavault.core.model.MediaFile
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class VideoThumbnailGenerator(
    private val ffmpegExecutable: String = "ffmpeg",
) : ThumbnailGenerator {
    override fun generate(mediaFile: MediaFile, outputPath: Path): Boolean {
        val process = runCatching {
            ProcessBuilder(
                ffmpegExecutable,
                "-y",
                "-ss",
                "00:00:10",
                "-i",
                mediaFile.path,
                "-frames:v",
                "1",
                "-vf",
                "scale=96:-1",
                outputPath.toString(),
            )
                .redirectErrorStream(true)
                .start()
        }.getOrElse {
            return false
        }

        val finished = process.waitFor(30, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return false
        }

        return process.exitValue() == 0 && outputPath.toFile().isFile
    }
}
