package com.mediavault.player

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaType
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageSlideshowControllerTest {
    @Test
    fun navigatesImagesCircularly() {
        val images = listOf(image("one.jpg"), image("two.jpg"))
        val controller = ImageSlideshowController(images)

        assertEquals("one.jpg", controller.current?.filename)
        assertEquals("two.jpg", controller.next()?.filename)
        assertEquals("one.jpg", controller.next()?.filename)
        assertEquals("two.jpg", controller.previous()?.filename)
    }

    @Test
    fun tracksRunningState() {
        val controller = ImageSlideshowController()

        controller.start()
        assertTrue(controller.isRunning)

        controller.stop()
        assertFalse(controller.isRunning)
    }

    private fun image(filename: String): MediaFile = MediaFile(
        path = "C:/media/$filename",
        filename = filename,
        extension = "jpg",
        mediaType = MediaType.IMAGE,
        size = 10,
        createdDate = Instant.parse("2024-01-01T00:00:00Z"),
        modifiedDate = Instant.parse("2024-01-01T00:00:00Z"),
        indexedAt = Instant.parse("2024-01-01T00:00:00Z"),
    )
}
