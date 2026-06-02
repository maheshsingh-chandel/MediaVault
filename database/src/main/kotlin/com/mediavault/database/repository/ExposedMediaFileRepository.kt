package com.mediavault.database.repository

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaStatistics
import com.mediavault.core.model.MediaType
import com.mediavault.core.repository.MediaFileRepository
import com.mediavault.database.table.MediaFilesTable
import java.time.Instant
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedMediaFileRepository(
    private val database: Database,
) : MediaFileRepository {
    override fun count(): Long = transaction(database) {
        MediaFilesTable.selectAll().count()
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

    override fun list(limit: Int, offset: Long): List<MediaFile> = transaction(database) {
        MediaFilesTable
            .selectAll()
            .limit(limit)
            .offset(offset)
            .map { row ->
                MediaFile(
                    id = row[MediaFilesTable.id],
                    path = row[MediaFilesTable.path],
                    filename = row[MediaFilesTable.filename],
                    extension = row[MediaFilesTable.extension],
                    mediaType = MediaType.valueOf(row[MediaFilesTable.mediaType]),
                    size = row[MediaFilesTable.size],
                    createdDate = Instant.ofEpochMilli(row[MediaFilesTable.createdDate]),
                    modifiedDate = Instant.ofEpochMilli(row[MediaFilesTable.modifiedDate]),
                    indexedAt = Instant.ofEpochMilli(row[MediaFilesTable.indexedAt]),
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
        }[MediaFilesTable.id]
    }
}
