package com.mediavault.metadata

import com.mediavault.core.model.MediaType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoMetadataExtractorTest {
    @Test
    fun returnsFailureJsonWhenFfprobeIsUnavailable() {
        val json = VideoMetadataExtractor(ffprobeExecutable = "missing-ffprobe-for-test")
            .extract(mediaFile(path = "C:/media/video.mp4", mediaType = MediaType.VIDEO))
        val root = Json.parseToJsonElement(json).jsonObject

        assertEquals("FAILED", root["status"]!!.jsonPrimitive.content)
    }
}
