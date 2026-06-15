package com.mediavault.core.model

import java.time.Instant

data class MediaFile(
    val id: Long = 0,
    val path: String,
    val filename: String,
    val extension: String,
    val mediaType: MediaType,
    val size: Long,
    val createdDate: Instant,
    val modifiedDate: Instant,
    val indexedAt: Instant,
    val metadataJson: String? = null,
)
