package com.mediavault.core.repository

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaStatistics
import com.mediavault.core.model.MediaType

interface MediaFileRepository {
    fun count(): Long
    fun count(query: MediaFileQuery): Long
    fun countByType(mediaType: MediaType): Long
    fun getStatistics(): MediaStatistics
    fun findById(id: Long): MediaFile?
    fun list(limit: Int = 100, offset: Long = 0): List<MediaFile>
    fun search(query: MediaFileQuery): List<MediaFile>
    fun save(mediaFile: MediaFile): Long
    fun saveOrIgnore(mediaFile: MediaFile): Boolean
    fun updateMetadata(id: Long, metadataJson: String): Boolean
}

data class MediaFileQuery(
    val searchText: String = "",
    val sort: MediaFileSort = MediaFileSort.MODIFIED_DATE,
    val direction: SortDirection = SortDirection.DESCENDING,
    val limit: Int = 100,
    val offset: Long = 0,
)

enum class MediaFileSort {
    MODIFIED_DATE,
    SIZE,
    TYPE,
}

enum class SortDirection {
    ASCENDING,
    DESCENDING,
}
