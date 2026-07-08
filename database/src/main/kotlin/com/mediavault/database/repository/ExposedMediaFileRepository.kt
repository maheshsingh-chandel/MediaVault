package com.mediavault.database.repository

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaStatistics
import com.mediavault.core.model.MediaType
import com.mediavault.core.repository.DuplicateGroup
import com.mediavault.core.repository.MediaFileQuery
import com.mediavault.core.repository.MediaFileRepository
import com.mediavault.core.repository.MediaFileSort
import com.mediavault.core.repository.SortDirection
import com.mediavault.database.table.MediaFilesTable
import java.time.Instant
import java.nio.file.Path
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere

class ExposedMediaFileRepository(
    private val database: Database,
) : MediaFileRepository {
    override fun count(): Long = transaction(database) {
        MediaFilesTable.selectAll().count()
    }

    override fun count(query: MediaFileQuery): Long = transaction(database) {
        MediaFilesTable
            .selectAll()
            .applySearch(query.searchText)
            .count()
    }

    override fun countByType(mediaType: MediaType): Long = transaction(database) {
        MediaFilesTable
            .selectAll()
            .where { MediaFilesTable.mediaType eq mediaType.name }
            .count()
    }

    override fun getStatistics(): MediaStatistics = MediaStatistics(
        totalFiles = count(),
        images = countByType(MediaType.IMAGE),
        videos = countByType(MediaType.VIDEO),
        audio = countByType(MediaType.AUDIO),
    )

    override fun findById(id: Long): MediaFile? = transaction(database) {
        MediaFilesTable
            .selectAll()
            .where { MediaFilesTable.id eq id }
            .limit(1)
            .map(::toMediaFile)
            .firstOrNull()
    }

    override fun findByPath(path: String): MediaFile? = transaction(database) {
        MediaFilesTable
            .selectAll()
            .where { MediaFilesTable.path eq path }
            .limit(1)
            .map(::toMediaFile)
            .firstOrNull()
    }

    override fun list(limit: Int, offset: Long): List<MediaFile> = transaction(database) {
        MediaFilesTable
            .selectAll()
            .limit(limit)
            .offset(offset)
            .map(::toMediaFile)
    }

    override fun search(query: MediaFileQuery): List<MediaFile> = transaction(database) {
        MediaFilesTable
            .selectAll()
            .applySearch(query.searchText)
            .orderBy(query.sort.column to query.direction.sortOrder)
            .limit(query.limit.coerceIn(1, MAX_PAGE_SIZE))
            .offset(query.offset.coerceAtLeast(0))
            .map(::toMediaFile)
    }

    override fun indexedParentDirectories(): List<String> = transaction(database) {
        MediaFilesTable
            .selectAll()
            .mapNotNull { row -> Path.of(row[MediaFilesTable.path]).parent?.toString() }
            .distinct()
    }

    override fun filesNeedingHash(limit: Int): List<MediaFile> = transaction(database) {
        MediaFilesTable
            .selectAll()
            .orderBy(MediaFilesTable.indexedAt to SortOrder.DESC)
            .limit(limit.coerceIn(1, MAX_PAGE_SIZE))
            .map(::toMediaFile)
            .filter { mediaFile ->
                mediaFile.sha256 == null ||
                    mediaFile.hashedSize != mediaFile.size ||
                    mediaFile.hashedModifiedDate != mediaFile.modifiedDate
            }
    }

    override fun duplicateGroups(limit: Int, offset: Long): List<DuplicateGroup> = transaction(database) {
        MediaFilesTable
            .selectAll()
            .orderBy(MediaFilesTable.sha256 to SortOrder.ASC)
            .limit(DUPLICATE_SCAN_WINDOW)
            .map(::toMediaFile)
            .asSequence()
            .filter { !it.sha256.isNullOrBlank() }
            .groupBy { it.sha256.orEmpty() }
            .filterValues { it.size > 1 }
            .values
            .drop(offset.coerceAtLeast(0).toInt())
            .take(limit.coerceIn(1, MAX_PAGE_SIZE))
            .mapNotNull { files ->
                val sortedFiles = files.sortedBy { it.modifiedDate }
                val original = sortedFiles.firstOrNull() ?: return@mapNotNull null
                DuplicateGroup(
                    sha256 = original.sha256.orEmpty(),
                    original = original,
                    copies = sortedFiles.drop(1),
                )
            }
    }

    override fun save(mediaFile: MediaFile): Long = transaction(database) {
        MediaFilesTable.insert { row ->
            row[path] = mediaFile.path
            row[filename] = mediaFile.filename
            row[extension] = mediaFile.extension
            row[mediaType] = mediaFile.mediaType.name
            row[size] = mediaFile.size
            row[createdDate] = mediaFile.createdDate.toEpochMilli()
            row[modifiedDate] = mediaFile.modifiedDate.toEpochMilli()
            row[indexedAt] = mediaFile.indexedAt.toEpochMilli()
            row[metadataJson] = mediaFile.metadataJson
            row[sha256] = mediaFile.sha256
            row[hashedSize] = mediaFile.hashedSize
            row[hashedModifiedDate] = mediaFile.hashedModifiedDate?.toEpochMilli()
        }[MediaFilesTable.id]
    }

    override fun saveOrIgnore(mediaFile: MediaFile): Boolean = transaction(database) {
        val exists = MediaFilesTable
            .selectAll()
            .where { MediaFilesTable.path eq mediaFile.path }
            .limit(1)
            .any()

        if (exists) {
            false
        } else {
            MediaFilesTable.insert { row ->
                row[path] = mediaFile.path
                row[filename] = mediaFile.filename
                row[extension] = mediaFile.extension
                row[mediaType] = mediaFile.mediaType.name
                row[size] = mediaFile.size
                row[createdDate] = mediaFile.createdDate.toEpochMilli()
                row[modifiedDate] = mediaFile.modifiedDate.toEpochMilli()
                row[indexedAt] = mediaFile.indexedAt.toEpochMilli()
                row[metadataJson] = mediaFile.metadataJson
                row[sha256] = mediaFile.sha256
                row[hashedSize] = mediaFile.hashedSize
                row[hashedModifiedDate] = mediaFile.hashedModifiedDate?.toEpochMilli()
            }
            true
        }
    }

    override fun upsert(mediaFile: MediaFile): Boolean = transaction(database) {
        val existing = findByPath(mediaFile.path)
        if (existing == null) {
            MediaFilesTable.insert { row ->
                row[path] = mediaFile.path
                row[filename] = mediaFile.filename
                row[extension] = mediaFile.extension
                row[mediaType] = mediaFile.mediaType.name
                row[size] = mediaFile.size
                row[createdDate] = mediaFile.createdDate.toEpochMilli()
                row[modifiedDate] = mediaFile.modifiedDate.toEpochMilli()
                row[indexedAt] = mediaFile.indexedAt.toEpochMilli()
                row[metadataJson] = mediaFile.metadataJson
                row[sha256] = mediaFile.sha256
                row[hashedSize] = mediaFile.hashedSize
                row[hashedModifiedDate] = mediaFile.hashedModifiedDate?.toEpochMilli()
            }
            true
        } else {
            val fileStateChanged = existing.size != mediaFile.size || existing.modifiedDate != mediaFile.modifiedDate
            MediaFilesTable.update({ MediaFilesTable.id eq existing.id }) { row ->
                row[filename] = mediaFile.filename
                row[extension] = mediaFile.extension
                row[mediaType] = mediaFile.mediaType.name
                row[size] = mediaFile.size
                row[createdDate] = mediaFile.createdDate.toEpochMilli()
                row[modifiedDate] = mediaFile.modifiedDate.toEpochMilli()
                row[indexedAt] = mediaFile.indexedAt.toEpochMilli()
                row[metadataJson] = null
                if (fileStateChanged) {
                    row[sha256] = null
                    row[hashedSize] = null
                    row[hashedModifiedDate] = null
                }
            } > 0
        }
    }

    override fun deleteByPath(path: String): Boolean = transaction(database) {
        MediaFilesTable.deleteWhere { MediaFilesTable.path eq path } > 0
    }

    override fun updateMetadata(id: Long, metadataJson: String): Boolean = transaction(database) {
        MediaFilesTable.update({ MediaFilesTable.id eq id }) { row ->
            row[MediaFilesTable.metadataJson] = metadataJson
        } > 0
    }

    override fun updateHash(id: Long, sha256: String, size: Long, modifiedDate: Instant): Boolean = transaction(database) {
        MediaFilesTable.update({ MediaFilesTable.id eq id }) { row ->
            row[MediaFilesTable.sha256] = sha256
            row[hashedSize] = size
            row[hashedModifiedDate] = modifiedDate.toEpochMilli()
        } > 0
    }

    private fun toMediaFile(row: ResultRow): MediaFile = MediaFile(
        id = row[MediaFilesTable.id],
        path = row[MediaFilesTable.path],
        filename = row[MediaFilesTable.filename],
        extension = row[MediaFilesTable.extension],
        mediaType = MediaType.valueOf(row[MediaFilesTable.mediaType]),
        size = row[MediaFilesTable.size],
        createdDate = Instant.ofEpochMilli(row[MediaFilesTable.createdDate]),
        modifiedDate = Instant.ofEpochMilli(row[MediaFilesTable.modifiedDate]),
        indexedAt = Instant.ofEpochMilli(row[MediaFilesTable.indexedAt]),
        metadataJson = row[MediaFilesTable.metadataJson],
        sha256 = row[MediaFilesTable.sha256],
        hashedSize = row[MediaFilesTable.hashedSize],
        hashedModifiedDate = row[MediaFilesTable.hashedModifiedDate]?.let(Instant::ofEpochMilli),
    )

    private fun org.jetbrains.exposed.v1.jdbc.Query.applySearch(searchText: String) = apply {
        val trimmedSearch = searchText.trim()
        if (trimmedSearch.isNotEmpty()) {
            where { MediaFilesTable.filename like "%${trimmedSearch.escapeLike()}%" }
        }
    }

    private fun String.escapeLike(): String = replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    private val MediaFileSort.column
        get() = when (this) {
            MediaFileSort.MODIFIED_DATE -> MediaFilesTable.modifiedDate
            MediaFileSort.SIZE -> MediaFilesTable.size
            MediaFileSort.TYPE -> MediaFilesTable.mediaType
        }

    private val SortDirection.sortOrder
        get() = when (this) {
            SortDirection.ASCENDING -> SortOrder.ASC
            SortDirection.DESCENDING -> SortOrder.DESC
        }

    private companion object {
        const val MAX_PAGE_SIZE = 500
        const val DUPLICATE_SCAN_WINDOW = 20_000
    }
}
