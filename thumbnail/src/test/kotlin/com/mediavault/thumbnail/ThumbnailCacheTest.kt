package com.mediavault.thumbnail

import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class ThumbnailCacheTest {
    @Test
    fun usesSha256PathNaming() {
        val cache = ThumbnailCache(Path("C:/AppData/MediaVault/thumbnails"))

        assertEquals(
            Path("C:/AppData/MediaVault/thumbnails/32f0377b14a947d6fde3cf1f6af2d9fd7cf30ea53fb22c2a4277073ccd47d184.jpg"),
            cache.pathFor("C:/media/photo.jpg"),
        )
    }
}
