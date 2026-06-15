package com.mediavault.metadata

import com.mediavault.core.model.MediaFile
import java.io.File
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey

class AudioMetadataExtractor : MetadataExtractor {
    override fun extract(mediaFile: MediaFile): String {
        val audioFile = runCatching { AudioFileIO.read(File(mediaFile.path)) }
            .getOrElse { return failedMetadata(mediaFile, it.message ?: "Unable to read audio metadata") }
        val tag = audioFile.tag

        return baseMetadata(mediaFile) {
            put("status", "READY")
            put(
                "audio",
                buildJsonObject {
                    tag?.getFirst(FieldKey.ARTIST)?.takeIf { it.isNotBlank() }?.let { put("artist", it) }
                    tag?.getFirst(FieldKey.ALBUM)?.takeIf { it.isNotBlank() }?.let { put("album", it) }
                    tag?.getFirst(FieldKey.GENRE)?.takeIf { it.isNotBlank() }?.let { put("genre", it) }
                    put("durationSeconds", audioFile.audioHeader.trackLength)
                },
            )
        }
    }
}
