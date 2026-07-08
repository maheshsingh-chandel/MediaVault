package com.mediavault.ui.screen

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediavault.core.metadata.MetadataService
import com.mediavault.core.model.MediaFile
import com.mediavault.core.repository.MediaFileQuery
import com.mediavault.core.repository.MediaFileRepository
import com.mediavault.core.repository.MediaFileSort
import com.mediavault.core.repository.SortDirection
import com.mediavault.core.thumbnail.ThumbnailService
import com.mediavault.core.thumbnail.ThumbnailStatus
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage

@Composable
fun LibraryScreen(
    repository: MediaFileRepository,
    thumbnailService: ThumbnailService,
    metadataService: MetadataService,
    refreshVersion: Long,
) {
    var searchText by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(MediaFileSort.MODIFIED_DATE) }
    var direction by remember { mutableStateOf(SortDirection.DESCENDING) }
    var page by remember { mutableLongStateOf(0) }
    var files by remember { mutableStateOf<List<MediaFile>>(emptyList()) }
    var total by remember { mutableLongStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<MediaFile?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        val query = MediaFileQuery(
            searchText = searchText,
            sort = sort,
            direction = direction,
            limit = PAGE_SIZE,
            offset = page * PAGE_SIZE,
        )

        scope.launch {
            isLoading = true
            val result = withContext(Dispatchers.IO) {
                repository.count(query) to repository.search(query)
            }
            total = result.first
            files = result.second
            isLoading = false
        }
    }

    LaunchedEffect(searchText, sort, direction, page, refreshVersion) {
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
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$total indexed files",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LibraryToolbar(
            searchText = searchText,
            onSearchTextChange = {
                searchText = it
                page = 0
            },
            sort = sort,
            onSortChange = {
                sort = it
                page = 0
            },
            direction = direction,
            onDirectionChange = {
                direction = it
                page = 0
            },
        )

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }

        LibraryTable(
            files = files,
            thumbnailService = thumbnailService,
            onDetails = { selectedFile = it },
            modifier = Modifier.weight(1f),
        )

        selectedFile?.let { file ->
            DetailsScreen(
                mediaFile = file,
                metadataService = metadataService,
                onMetadataLoaded = { loadedFile ->
                    selectedFile = loadedFile
                    files = files.map { if (it.id == loadedFile.id) loadedFile else it }
                },
                onClose = { selectedFile = null },
            )
        }

        PaginationControls(
            page = page,
            total = total,
            onPrevious = { if (page > 0) page -= 1 },
            onNext = { if ((page + 1) * PAGE_SIZE < total) page += 1 },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryToolbar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    sort: MediaFileSort,
    onSortChange: (MediaFileSort) -> Unit,
    direction: SortDirection,
    onDirectionChange: (SortDirection) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text("Search filename") },
        )

        SortDropdown(
            sort = sort,
            onSortChange = onSortChange,
        )

        DirectionDropdown(
            direction = direction,
            onDirectionChange = onDirectionChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortDropdown(
    sort: MediaFileSort,
    onSortChange: (MediaFileSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = sort.label,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .width(180.dp),
            readOnly = true,
            label = { Text("Sort by") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            MediaFileSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectionDropdown(
    direction: SortDirection,
    onDirectionChange: (SortDirection) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = direction.label,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .width(160.dp),
            readOnly = true,
            label = { Text("Order") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SortDirection.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onDirectionChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun LibraryTable(
    files: List<MediaFile>,
    thumbnailService: ThumbnailService,
    onDetails: (MediaFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        if (files.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No indexed media files found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Card
        }

        val listState = rememberLazyListState()
        val horizontalScrollState = rememberScrollState()

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState),
            ) {
                LibraryHeader()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .width(TABLE_WIDTH),
                    state = listState,
                ) {
                    items(
                        items = files,
                        key = { it.id },
                    ) { file ->
                        LibraryRow(
                            file = file,
                            thumbnailService = thumbnailService,
                            onDetails = onDetails,
                        )
                    }
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd),
            )

            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(horizontalScrollState),
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

@Composable
private fun LibraryHeader() {
    Row(
        modifier = Modifier
            .width(TABLE_WIDTH)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeaderCell("Preview", 96)
        HeaderCell("Filename", 260)
        HeaderCell("Type", 90)
        HeaderCell("Size", 100)
        HeaderCell("Modified", 180)
        HeaderCell("Path", 420)
        HeaderCell("Actions", 410)
    }
}

@Composable
private fun HeaderCell(
    text: String,
    width: Int,
) {
    Text(
        text = text,
        modifier = Modifier.width(width.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LibraryRow(
    file: MediaFile,
    thumbnailService: ThumbnailService,
    onDetails: (MediaFile) -> Unit,
) {
    Row(
        modifier = Modifier
            .width(TABLE_WIDTH)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThumbnailCell(
            file = file,
            thumbnailService = thumbnailService,
        )
        BodyCell(file.filename, 260)
        BodyCell(file.mediaType.name.lowercase().replaceFirstChar { it.uppercase() }, 90)
        BodyCell(formatSize(file.size), 100)
        BodyCell(formatDate(file.modifiedDate), 180)
        BodyCell(file.path, 420)
        Row(
            modifier = Modifier.width(410.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onDetails(file) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("Details")
            }
            OutlinedButton(
                onClick = { openFile(file.path) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("Open File")
            }
            OutlinedButton(
                onClick = { openContainingFolder(file.path) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("Open Folder")
            }
            OutlinedButton(
                onClick = { copyPath(file.path) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("Copy Path")
            }
        }
    }
}

@Composable
private fun DetailsScreen(
    mediaFile: MediaFile,
    metadataService: MetadataService,
    onMetadataLoaded: (MediaFile) -> Unit,
    onClose: () -> Unit,
) {
    var displayedFile by remember(mediaFile.id) { mutableStateOf(mediaFile) }
    var isLoading by remember(mediaFile.id) { mutableStateOf(mediaFile.metadataJson.isNullOrBlank()) }

    LaunchedEffect(mediaFile.id) {
        if (mediaFile.metadataJson.isNullOrBlank()) {
            isLoading = true
            val loadedFile = metadataService.extractAndStore(mediaFile)
            displayedFile = loadedFile
            onMetadataLoaded(loadedFile)
            isLoading = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = displayedFile.filename,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onClose) {
                    Text("Close")
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(
                text = displayedFile.metadataJson ?: "Metadata is not available.",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThumbnailCell(
    file: MediaFile,
    thumbnailService: ThumbnailService,
) {
    val states by thumbnailService.states.collectAsState()
    val thumbnailState = states[file.path]
    var imageBitmap by remember(file.path, thumbnailState?.path) {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(file.path) {
        thumbnailService.request(file)
    }

    LaunchedEffect(thumbnailState?.status, thumbnailState?.path) {
        imageBitmap = if (thumbnailState?.status == ThumbnailStatus.READY) {
            thumbnailState.path?.let { loadThumbnail(it) }
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .width(96.dp)
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = imageBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = file.filename,
                modifier = Modifier.size(96.dp, 64.dp),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = when (thumbnailState?.status) {
                    ThumbnailStatus.QUEUED,
                    ThumbnailStatus.GENERATING -> "..."
                    ThumbnailStatus.FAILED,
                    ThumbnailStatus.UNSUPPORTED,
                    null -> file.mediaType.name.take(1)
                    ThumbnailStatus.READY -> ""
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BodyCell(
    text: String,
    width: Int,
) {
    Text(
        text = text,
        modifier = Modifier.width(width.dp),
        style = MaterialTheme.typography.bodyMedium,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
}

@Composable
private fun PaginationControls(
    page: Long,
    total: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val firstItem = if (total == 0L) 0 else page * PAGE_SIZE + 1
    val lastItem = minOf((page + 1) * PAGE_SIZE, total)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$firstItem-$lastItem of $total",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedButton(
            onClick = onPrevious,
            enabled = page > 0,
        ) {
            Text("Previous")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onNext,
            enabled = (page + 1) * PAGE_SIZE < total,
        ) {
            Text("Next")
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

private fun copyPath(path: String) {
    runCatching {
        Toolkit.getDefaultToolkit()
            .systemClipboard
            .setContents(StringSelection(path), null)
    }
}

private fun loadThumbnail(path: Path): ImageBitmap? = runCatching {
    SkiaImage
        .makeFromEncoded(Files.readAllBytes(path))
        .toComposeImageBitmap()
}.getOrNull()

private fun formatDate(instant: java.time.Instant): String = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())
    .format(instant)

private fun formatSize(size: Long): String {
    if (size < 1024) return "$size B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = size / 1024.0
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex += 1
    }
    return "%.1f %s".format(value, units[unitIndex])
}

private val MediaFileSort.label: String
    get() = when (this) {
        MediaFileSort.MODIFIED_DATE -> "Date"
        MediaFileSort.SIZE -> "Size"
        MediaFileSort.TYPE -> "Type"
    }

private val SortDirection.label: String
    get() = when (this) {
        SortDirection.ASCENDING -> "Ascending"
        SortDirection.DESCENDING -> "Descending"
    }

private const val PAGE_SIZE = 100
private val TABLE_WIDTH = 1_628.dp
