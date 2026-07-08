package com.mediavault.database.repository

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaType
import com.mediavault.core.repository.MediaFileQuery
import com.mediavault.core.repository.MediaFileSort
import com.mediavault.core.repository.SortDirection
import com.mediavault.database.table.MediaFilesTable
import java.time.Instant
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedMediaFileRepositoryIntegrationTest {
    @Test
    fun searchesByFilenameAndPaginatesResults() {
        val repository = createRepository()
        repository.save(mediaFile(path = "C:/media/cat-1.jpg", filename = "cat-1.jpg"))
        repository.save(mediaFile(path = "C:/media/cat-2.jpg", filename = "cat-2.jpg"))
        repository.save(mediaFile(path = "C:/media/dog.jpg", filename = "dog.jpg"))

        val query = MediaFileQuery(
            searchText = "cat",
            sort = MediaFileSort.MODIFIED_DATE,
            direction = SortDirection.ASCENDING,
            limit = 1,
            offset = 1,
        )

        assertEquals(2, repository.count(query))
        assertEquals(listOf("cat-2.jpg"), repository.search(query).map { it.filename })
    }

    @Test
    fun sortsBySizeAndTypeInSql() {
        val repository = createRepository()
        repository.save(mediaFile(path = "C:/media/video.mp4", filename = "video.mp4", mediaType = MediaType.VIDEO, size = 300))
        repository.save(mediaFile(path = "C:/media/audio.mp3", filename = "audio.mp3", mediaType = MediaType.AUDIO, size = 100))
        repository.save(mediaFile(path = "C:/media/image.jpg", filename = "image.jpg", mediaType = MediaType.IMAGE, size = 200))

        val bySize = repository.search(
            MediaFileQuery(
                sort = MediaFileSort.SIZE,
                direction = SortDirection.ASCENDING,
            ),
        )
        val byType = repository.search(
            MediaFileQuery(
                sort = MediaFileSort.TYPE,
                direction = SortDirection.ASCENDING,
            ),
        )

        assertEquals(listOf("audio.mp3", "image.jpg", "video.mp4"), bySize.map { it.filename })
        assertEquals(listOf("audio.mp3", "image.jpg", "video.mp4"), byType.map { it.filename })
    }

    @Test
    fun saveOrIgnorePreventsDuplicatePaths() {
        val repository = createRepository()
        val mediaFile = mediaFile(path = "C:/media/duplicate.jpg", filename = "duplicate.jpg")

        assertTrue(repository.saveOrIgnore(mediaFile))
        assertFalse(repository.saveOrIgnore(mediaFile.copy(filename = "renamed.jpg")))
        assertEquals(1, repository.count())
    }

    @Test
    fun updatesAndLoadsMetadataJson() {
        val repository = createRepository()
        val id = repository.save(mediaFile(path = "C:/media/metadata.jpg", filename = "metadata.jpg"))

        assertTrue(repository.updateMetadata(id, """{"image":{"width":100}}"""))

        assertEquals("""{"image":{"width":100}}""", repository.findById(id)?.metadataJson)
    }

    @Test
    fun tracksHashStateAndDuplicateGroups() {
        val repository = createRepository()
        val firstId = repository.save(mediaFile(path = "C:/media/a.jpg", filename = "a.jpg", modifiedDate = Instant.parse("2024-01-01T00:00:00Z")))
        val secondId = repository.save(mediaFile(path = "C:/media/b.jpg", filename = "b.jpg", modifiedDate = Instant.parse("2024-01-02T00:00:00Z")))
        val thirdId = repository.save(mediaFile(path = "C:/media/c.jpg", filename = "c.jpg"))

        repository.updateHash(firstId, "abc", 100, Instant.parse("2024-01-01T00:00:00Z"))
        repository.updateHash(secondId, "abc", 100, Instant.parse("2024-01-02T00:00:00Z"))
        repository.updateHash(thirdId, "xyz", 100, Instant.parse("2024-01-01T00:00:00Z"))

        val groups = repository.duplicateGroups()

        assertEquals(1, groups.size)
        assertEquals("abc", groups.first().sha256)
        assertEquals("a.jpg", groups.first().original.filename)
        assertEquals(listOf("b.jpg"), groups.first().copies.map { it.filename })
    }

    @Test
    fun upsertClearsHashWhenFileStateChanges() {
        val repository = createRepository()
        val id = repository.save(mediaFile(path = "C:/media/state.jpg", filename = "state.jpg"))
        repository.updateHash(id, "abc", 100, Instant.parse("2024-01-01T00:00:00Z"))

        repository.upsert(
            mediaFile(
                path = "C:/media/state.jpg",
                filename = "state.jpg",
                size = 200,
                modifiedDate = Instant.parse("2024-01-03T00:00:00Z"),
            ),
        )

        val updated = repository.findById(id)
        assertEquals(null, updated?.sha256)
        assertEquals(1, repository.filesNeedingHash().size)
    }

    private fun createRepository(): ExposedMediaFileRepository {
        val databaseFile = createTempFile("mediavault-repository", ".db").toFile()
        databaseFile.deleteOnExit()
        val database = Database.connect(
            url = "jdbc:sqlite:${databaseFile.absolutePath}",
            driver = "org.sqlite.JDBC",
        )
        transaction(database) {
            SchemaUtils.create(MediaFilesTable)
        }
        return ExposedMediaFileRepository(database)
    }

    private fun mediaFile(
        path: String,
        filename: String,
        mediaType: MediaType = MediaType.IMAGE,
        size: Long = 100,
        modifiedDate: Instant = Instant.parse("2024-01-01T00:00:00Z"),
    ): MediaFile = MediaFile(
        path = path,
        filename = filename,
        extension = filename.substringAfterLast('.', ""),
        mediaType = mediaType,
        size = size,
        createdDate = modifiedDate,
        modifiedDate = modifiedDate,
        indexedAt = modifiedDate,
    )
}
