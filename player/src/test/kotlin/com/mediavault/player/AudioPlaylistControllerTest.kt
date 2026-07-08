package com.mediavault.player

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaType
import java.time.Instant
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioPlaylistControllerTest {
    @Test
    fun advancesThroughPlaylist() {
        val tracks = listOf(track("one.mp3"), track("two.mp3"), track("three.mp3"))
        val controller = AudioPlaylistController(tracks)

        assertEquals("one.mp3", controller.current?.filename)
        assertEquals("two.mp3", controller.next()?.filename)
        assertEquals("three.mp3", controller.next()?.filename)
        assertEquals("three.mp3", controller.next()?.filename)
    }

    @Test
    fun repeatAllWrapsToBeginning() {
        val controller = AudioPlaylistController(listOf(track("one.mp3"), track("two.mp3")))
        controller.cycleRepeatMode()

        controller.next()
        assertEquals("one.mp3", controller.next()?.filename)
    }

    @Test
    fun repeatOneKeepsCurrentTrack() {
        val controller = AudioPlaylistController(listOf(track("one.mp3"), track("two.mp3")))
        controller.cycleRepeatMode()
        controller.cycleRepeatMode()

        assertEquals(RepeatMode.ONE, controller.repeatMode)
        assertEquals("one.mp3", controller.next()?.filename)
    }

    @Test
    fun shuffleSelectsATrackFromPlaylist() {
        val tracks = listOf(track("one.mp3"), track("two.mp3"), track("three.mp3"))
        val controller = AudioPlaylistController(tracks, Random(7))
        controller.toggleShuffle()

        assertTrue(controller.next() in tracks)
    }

    private fun track(filename: String): MediaFile = MediaFile(
        path = "C:/media/$filename",
        filename = filename,
        extension = "mp3",
        mediaType = MediaType.AUDIO,
        size = 10,
        createdDate = Instant.parse("2024-01-01T00:00:00Z"),
        modifiedDate = Instant.parse("2024-01-01T00:00:00Z"),
        indexedAt = Instant.parse("2024-01-01T00:00:00Z"),
    )
}
