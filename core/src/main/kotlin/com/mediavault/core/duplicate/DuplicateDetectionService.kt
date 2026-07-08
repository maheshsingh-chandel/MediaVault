package com.mediavault.core.duplicate

interface DuplicateDetectionService {
    suspend fun hashPending(limit: Int = 100): Int
}
