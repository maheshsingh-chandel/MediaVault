package com.mediavault.database.table

import org.jetbrains.exposed.v1.core.Table

object MediaFilesTable : Table("media_files") {
    val id = long("id").autoIncrement()
    val path = varchar("path", 4096).uniqueIndex("idx_media_files_path_unique")
    val filename = varchar("filename", 512).index("idx_media_files_filename")
    val extension = varchar("extension", 64)
    val mediaType = varchar("media_type", 32).index("idx_media_files_media_type")
    val size = long("size").index("idx_media_files_size")
    val createdDate = long("created_date")
    val modifiedDate = long("modified_date").index("idx_media_files_modified_date")
    val indexedAt = long("indexed_at")
    val metadataJson = text("metadata_json").nullable()
    val sha256 = varchar("sha256", 64).nullable().index("idx_media_files_sha256")
    val hashedSize = long("hashed_size").nullable()
    val hashedModifiedDate = long("hashed_modified_date").nullable()

    override val primaryKey = PrimaryKey(id)
}
