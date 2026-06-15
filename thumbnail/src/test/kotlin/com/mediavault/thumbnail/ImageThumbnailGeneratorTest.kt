package com.mediavault.thumbnail

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaType
import java.awt.Color
import java.awt.image.BufferedImage
import java.time.Instant
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImageThumbnailGeneratorTest {
    @Test
    fun createsJpegThumbnailForImageFile() {
        val directory = createTempDirectory("mediavault-image-thumb")
        val source = directory.resolve("source.png")
        val output = directory.resolve("thumb.jpg")
        val image = BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.color = Color.RED
        graphics.fillRect(0, 0, 200, 100)
        graphics.dispose()
        ImageIO.write(image, "png", source.toFile())

        val generated = ImageThumbnailGenerator(maxSize = 64).generate(
            mediaFile(source.toString()),
            output,
        )

        assertTrue(generated)
        assertNotNull(ImageIO.read(output.toFile()))
    }

    private fun mediaFile(path: String): MediaFile = MediaFile(
        path = path,
        filename = "source.png",
        extension = "png",
        mediaType = MediaType.IMAGE,
        size = 100,
        createdDate = Instant.parse("2024-01-01T00:00:00Z"),
        modifiedDate = Instant.parse("2024-01-01T00:00:00Z"),
        indexedAt = Instant.parse("2024-01-01T00:00:00Z"),
    )
}
