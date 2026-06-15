package com.mediavault.metadata

import com.mediavault.core.model.MediaFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

internal val metadataJson = Json {
    prettyPrint = true
    explicitNulls = false
}

internal fun baseMetadata(
    mediaFile: MediaFile,
    builder: JsonObjectBuilderScope.() -> Unit,
): String = metadataJson.encodeToString(
    JsonObject.serializer(),
    buildJsonObject {
        put("type", mediaFile.mediaType.name)
        put("path", mediaFile.path)
        put("filename", mediaFile.filename)
        put("extractedAt", Instant.now().toString())
        JsonObjectBuilderScope(this).builder()
    },
)

internal fun unsupportedMetadata(mediaFile: MediaFile, reason: String): String = baseMetadata(mediaFile) {
    put("status", "UNSUPPORTED")
    put("message", reason)
}

internal fun failedMetadata(mediaFile: MediaFile, reason: String): String = baseMetadata(mediaFile) {
    put("status", "FAILED")
    put("message", reason)
}

internal class JsonObjectBuilderScope(
    private val builder: kotlinx.serialization.json.JsonObjectBuilder,
) {
    fun put(key: String, value: String?) {
        if (!value.isNullOrBlank()) builder.put(key, value)
    }

    fun put(key: String, value: Number?) {
        if (value != null) builder.put(key, JsonPrimitive(value))
    }

    fun put(key: String, value: Boolean?) {
        if (value != null) builder.put(key, value)
    }

    fun put(key: String, value: JsonElement?) {
        if (value != null) builder.put(key, value)
    }
}
