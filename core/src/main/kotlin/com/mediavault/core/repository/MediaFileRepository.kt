package com.mediavault.core.repository

import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaStatistics
import com.mediavault.core.model.MediaType

interface MediaFileRepository {
    fun count(): Long
    fun countByType(mediaType: MediaType): Long
    fun getStatistics(): MediaStatistics
    fun list(limit: Int = 100, offset: Long = 0): List<MediaFile>
    fun save(mediaFile: MediaFile): Long
}
