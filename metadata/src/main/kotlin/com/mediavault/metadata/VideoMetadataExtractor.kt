package com.mediavault.metadata

import com.mediavault.core.model.MediaFile
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class VideoMetadataExtractor(
    private val ffprobeExecutable: String = "ffprobe",
) : MetadataExtractor {
    override fun extract(mediaFile: MediaFile): String {
        val output = runFfprobe(mediaFile)
            ?: return failedMetadata(mediaFile, "ffprobe is unavailable or could not read this video")
        val root = runCatching { Json.parseToJsonElement(output).jsonObject }
            .getOrElse { return failedMetadata(mediaFile, it.message ?: "Unable to parse ffprobe output") }
        val videoStream = root["streams"]
            ?.jsonArray
            ?.mapNotNull { it.jsonObject }
            ?.firstOrNull { it["codec_type"]?.jsonPrimitive?.content == "video" }
        val format = root["format"]?.jsonObject

        return baseMetadata(mediaFile) {
            put("status", "READY")
            put(
                "video",
                buildJsonObject {
                    videoStream?.string("codec_name")?.let { put("codec", it) }
                    videoStream?.int("width")?.let { put("width", it) }
                    videoStream?.int("height")?.let { put("height", it) }
                    format?.double("duration")?.let { put("durationSeconds", it) }
                    val bitrate = videoStream?.long("bit_rate") ?: format?.long("bit_rate")
                    bitrate?.let { put("bitrate", it) }
                },
            )
        }
    }

    private fun runFfprobe(mediaFile: MediaFile): String? {
        val process = runCatching {
            ProcessBuilder(
                ffprobeExecutable,
                "-v",
                "quiet",
                "-print_format",
                "json",
                "-show_format",
                "-show_streams",
                mediaFile.path,
            )
                .redirectErrorStream(true)
                .start()
        }.getOrElse {
            return null
        }

        val finished = process.waitFor(30, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return null
        }

        val output = process.inputStream.bufferedReader().use { it.readText() }
        return output.takeIf { process.exitValue() == 0 && it.isNotBlank() }
    }

    private fun JsonObject.string(key: String): String? = get(key)?.jsonPrimitive?.content

    private fun JsonObject.int(key: String): Int? = string(key)?.toIntOrNull()

    private fun JsonObject.long(key: String): Long? = string(key)?.toLongOrNull()

    private fun JsonObject.double(key: String): Double? = get(key)?.jsonPrimitive?.doubleOrNull
}
