package com.mediavault.ui.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.mediavault.core.model.MediaFile
import com.mediavault.core.model.MediaType
import com.mediavault.player.AudioPlaylistController
import com.mediavault.player.ImageSlideshowController
import com.mediavault.player.RepeatMode
import com.mediavault.player.VlcjAudioPlayer
import com.mediavault.player.VlcjVideoPlayerPanel
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.delay
import org.jetbrains.skia.Image as SkiaImage

@Composable
fun MediaViewerWindow(
    mediaFile: MediaFile,
    pageFiles: List<MediaFile>,
    onClose: () -> Unit,
) {
    val windowState = rememberWindowState(
        width = 1100.dp,
        height = 760.dp,
    )
    var isFullscreen by remember { mutableStateOf(false) }

    Window(
        onCloseRequest = onClose,
        title = mediaFile.filename,
        state = windowState,
    ) {
        LaunchedEffect(isFullscreen) {
            windowState.placement = if (isFullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
        }

        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                when (mediaFile.mediaType) {
                    MediaType.IMAGE -> ImageViewer(
                        mediaFile = mediaFile,
                        pageFiles = pageFiles.filter { it.mediaType == MediaType.IMAGE },
                        isFullscreen = isFullscreen,
                        onToggleFullscreen = { isFullscreen = !isFullscreen },
                    )
                    MediaType.VIDEO -> VideoViewer(
                        mediaFile = mediaFile,
                        isFullscreen = isFullscreen,
                        onToggleFullscreen = { isFullscreen = !isFullscreen },
                    )
                    MediaType.AUDIO -> AudioViewer(
                        mediaFile = mediaFile,
                        playlist = pageFiles.filter { it.mediaType == MediaType.AUDIO },
                    )
                    MediaType.OTHER -> UnsupportedViewer(mediaFile)
                }
            }
        }
    }
}

@Composable
private fun ImageViewer(
    mediaFile: MediaFile,
    pageFiles: List<MediaFile>,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
) {
    val controller = remember { ImageSlideshowController(pageFiles) }
    var currentFile by remember { mutableStateOf(mediaFile) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var isRunning by remember { mutableStateOf(false) }
    val image = remember(currentFile.path) { loadImage(Path.of(currentFile.path)) }

    LaunchedEffect(pageFiles, mediaFile.path) {
        controller.setImages(pageFiles, mediaFile)
        currentFile = controller.current ?: mediaFile
    }

    LaunchedEffect(isRunning, currentFile.path) {
        while (isRunning) {
            delay(3_000)
            currentFile = controller.next() ?: currentFile
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        ViewerToolbar(currentFile.filename) {
            OutlinedButton(onClick = { zoom = (zoom - 0.25f).coerceAtLeast(0.25f) }) { Text("-") }
            OutlinedButton(onClick = { zoom = (zoom + 0.25f).coerceAtMost(4f) }) { Text("+") }
            OutlinedButton(onClick = { currentFile = controller.previous() ?: currentFile }) { Text("Previous") }
            OutlinedButton(onClick = { currentFile = controller.next() ?: currentFile }) { Text("Next") }
            Button(onClick = { isRunning = !isRunning }) { Text(if (isRunning) "Stop Slideshow" else "Slideshow") }
            OutlinedButton(onClick = onToggleFullscreen) { Text(if (isFullscreen) "Exit Fullscreen" else "Fullscreen") }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = currentFile.filename,
                    modifier = Modifier.fillMaxSize(zoom.coerceAtMost(1f)),
                    contentScale = if (zoom <= 1f) ContentScale.Fit else ContentScale.Crop,
                )
            } else {
                Text("Unable to open image")
            }
        }
    }
}

@Composable
private fun VideoViewer(
    mediaFile: MediaFile,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
) {
    val panel = remember { VlcjVideoPlayerPanel() }
    var seekMillis by remember { mutableLongStateOf(0) }

    DisposableEffect(panel) {
        panel.play(mediaFile.path)
        onDispose { panel.release() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ViewerToolbar(mediaFile.filename) {
            Button(onClick = { panel.resume() }) { Text("Play") }
            OutlinedButton(onClick = { panel.pause() }) { Text("Pause") }
            OutlinedButton(onClick = {
                seekMillis = (seekMillis + 10_000).coerceAtLeast(0)
                panel.seek(seekMillis)
            }) { Text("+10s") }
            OutlinedButton(onClick = {
                seekMillis = (seekMillis - 10_000).coerceAtLeast(0)
                panel.seek(seekMillis)
            }) { Text("-10s") }
            OutlinedButton(onClick = {
                panel.toggleFullscreen()
                onToggleFullscreen()
            }) { Text(if (isFullscreen) "Exit Fullscreen" else "Fullscreen") }
        }

        SwingPanel(
            modifier = Modifier.fillMaxSize(),
            factory = { panel },
        )
    }
}

@Composable
private fun AudioViewer(
    mediaFile: MediaFile,
    playlist: List<MediaFile>,
) {
    val audioPlayer = remember { VlcjAudioPlayer() }
    val controller = remember { AudioPlaylistController() }
    var currentFile by remember { mutableStateOf(mediaFile) }
    var isPlaying by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(RepeatMode.OFF) }
    var shuffleEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(playlist, mediaFile.path) {
        controller.setPlaylist(playlist.ifEmpty { listOf(mediaFile) }, mediaFile)
        currentFile = controller.current ?: mediaFile
    }

    DisposableEffect(audioPlayer) {
        onDispose { audioPlayer.release() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = currentFile.filename,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "${playlist.size.coerceAtLeast(1)} tracks in playlist",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                audioPlayer.play(currentFile.path)
                isPlaying = true
            }) { Text("Play") }
            OutlinedButton(onClick = {
                audioPlayer.pause()
                isPlaying = false
            }) { Text("Pause") }
            OutlinedButton(onClick = {
                controller.previous()?.let {
                    currentFile = it
                    audioPlayer.play(it.path)
                    isPlaying = true
                }
            }) { Text("Previous") }
            OutlinedButton(onClick = {
                controller.next()?.let {
                    currentFile = it
                    audioPlayer.play(it.path)
                    isPlaying = true
                }
            }) { Text("Next") }
            OutlinedButton(onClick = {
                controller.toggleShuffle()
                shuffleEnabled = controller.shuffleEnabled
            }) { Text(if (shuffleEnabled) "Shuffle On" else "Shuffle Off") }
            OutlinedButton(onClick = {
                controller.cycleRepeatMode()
                repeatMode = controller.repeatMode
            }) { Text("Repeat $repeatMode") }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isPlaying) "Playing" else "Paused",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UnsupportedViewer(mediaFile: MediaFile) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("No viewer available for ${mediaFile.filename}")
    }
}

@Composable
private fun ViewerToolbar(
    title: String,
    controls: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        controls()
    }
}

private fun loadImage(path: Path): ImageBitmap? = runCatching {
    SkiaImage
        .makeFromEncoded(Files.readAllBytes(path))
        .toComposeImageBitmap()
}.getOrNull()
