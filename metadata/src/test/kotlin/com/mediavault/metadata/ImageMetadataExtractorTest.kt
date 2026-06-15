package com.mediavault.metadata

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaType
import java.awt.Color
import java.awt.image.BufferedImage
import java.time.Instant
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageMetadataExtractorTest {
    @Test
    fun extractsImageDimensionsAsJson() {
        val directory = createTempDirectory("mediavault-metadata-image")
        val source = directory.resolve("source.png")
        val image = BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.BLUE
        graphics.fillRect(0, 0, 320, 180)
        graphics.dispose()
        ImageIO.write(image, "png", source.toFile())

        val json = ImageMetadataExtractor().extract(mediaFile(source.toString(), MediaType.IMAGE))
        val imageJson = Json.parseToJsonElement(json).jsonObject["image"]!!.jsonObject

        assertEquals(320, imageJson["width"]!!.jsonPrimitive.int)
        assertEquals(180, imageJson["height"]!!.jsonPrimitive.int)
    }
}

internal fun mediaFile(
    path: String = "C:/media/file.jpg",
    mediaType: MediaType = MediaType.IMAGE,
    id: Long = 1,
    metadataJson: String? = null,
): MediaFile = MediaFile(
    id = id,
    path = path,
    filename = path.substringAfterLast('/').substringAfterLast('\\'),
    extension = path.substringAfterLast('.', ""),
    mediaType = mediaType,
    size = 100,
    createdDate = Instant.parse("2024-01-01T00:00:00Z"),
    modifiedDate = Instant.parse("2024-01-01T00:00:00Z"),
    indexedAt = Instant.parse("2024-01-01T00:00:00Z"),
    metadataJson = metadataJson,
)
