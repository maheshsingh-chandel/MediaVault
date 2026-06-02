package com.mediavault.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MediaStatisticsTest {
    @Test
    fun emptyStatisticsStartAtZero() {
        assertEquals(0, MediaStatistics.Empty.totalFiles)
        assertEquals(0, MediaStatistics.Empty.images)
        assertEquals(0, MediaStatistics.Empty.videos)
        assertEquals(0, MediaStatistics.Empty.audio)
    }
}
