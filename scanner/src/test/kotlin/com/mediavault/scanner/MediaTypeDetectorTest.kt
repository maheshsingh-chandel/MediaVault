package com.mediavault.scanner

import com.mediavault.core.model.MediaType
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaTypeDetectorTest {
    @Test
    fun detectsSupportedMediaTypesCaseInsensitively() {
        assertEquals(MediaType.IMAGE, MediaTypeDetector.detect(Path.of("photo.JPG")))
        assertEquals(MediaType.VIDEO, MediaTypeDetector.detect(Path.of("movie.Mp4")))
        assertEquals(MediaType.AUDIO, MediaTypeDetector.detect(Path.of("song.FLAC")))
    }

    @Test
    fun ignoresUnsupportedFiles() {
        assertNull(MediaTypeDetector.detect(Path.of("notes.txt")))
    }
}
