package com.mediavault.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediavault.core.duplicate.DuplicateDetectionService
import com.mediavault.core.model.MediaFile
import com.mediavault.core.repository.DuplicateGroup
import com.mediavault.core.repository.MediaFileRepository
import java.awt.Desktop
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DuplicatesScreen(
    repository: MediaFileRepository,
    duplicateDetectionService: DuplicateDetectionService,
    refreshVersion: Long,
) {
    var groups by remember { mutableStateOf<List<DuplicateGroup>>(emptyList()) }
    var page by remember { mutableLongStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready") }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            isLoading = true
            groups = withContext(Dispatchers.IO) {
                repository.duplicateGroups(limit = PAGE_SIZE, offset = page * PAGE_SIZE)
            }
            isLoading = false
        }
    }

    LaunchedEffect(page, refreshVersion) {
        isLoading = true
        val hashed = withContext(Dispatchers.IO) {
            duplicateDetectionService.hashPending(limit = 200)
        }
        if (hashed > 0) {
            status = "Hashed $hashed pending files"
        }
        reload()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Duplicates",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val hashed = duplicateDetectionService.hashPending(limit = 200)
                        status = "Hashed $hashed pending files"
                        reload()
                    }
                },
                enabled = !isLoading,
            ) {
                Text("Hash pending")
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = groups,
                key = { it.sha256 },
            ) { group ->
                DuplicateGroupCard(group)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { if (page > 0) page -= 1 },
                enabled = page > 0 && !isLoading,
            ) {
                Text("Previous")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { page += 1 },
                enabled = groups.size == PAGE_SIZE && !isLoading,
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(group: DuplicateGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "${group.totalFiles} copies - ${group.sha256.take(16)}...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            MediaFileDuplicateRow("Original", group.original)
            group.copies.forEach { copy ->
                MediaFileDuplicateRow("Duplicate", copy)
            }
        }
    }
}

@Composable
private fun MediaFileDuplicateRow(
    label: String,
    mediaFile: MediaFile,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(90.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaFile.filename,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = mediaFile.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OutlinedButton(onClick = { openFile(mediaFile.path) }) {
            Text("Open file")
        }
        OutlinedButton(onClick = { openContainingFolder(mediaFile.path) }) {
            Text("Open folder")
        }
    }
}

private fun openFile(path: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(File(path))
        }
    }
}

private fun openContainingFolder(path: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            File(path).parentFile?.let { Desktop.getDesktop().open(it) }
        }
    }
}

private const val PAGE_SIZE = 50
