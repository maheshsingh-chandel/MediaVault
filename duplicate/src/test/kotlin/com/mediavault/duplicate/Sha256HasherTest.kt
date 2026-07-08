package com.mediavault.duplicate

import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class Sha256HasherTest {
    @Test
    fun hashesFileContentWithSha256() {
        val file = createTempFile("mediavault-hash", ".txt")
        file.writeText("hello")

        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            Sha256Hasher().hash(file),
        )
    }
}
