package com.mediavault.thumbnail

import com.mediavault.core.model.MediaFile
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.min

class ImageThumbnailGenerator(
    private val maxSize: Int = 96,
) : ThumbnailGenerator {
    override fun generate(mediaFile: MediaFile, outputPath: Path): Boolean {
        val source = ImageIO.read(File(mediaFile.path)) ?: return false
        val scale = min(
            maxSize.toDouble() / source.width.toDouble(),
            maxSize.toDouble() / source.height.toDouble(),
        ).coerceAtMost(1.0)
        val width = (source.width * scale).toInt().coerceAtLeast(1)
        val height = (source.height * scale).toInt().coerceAtLeast(1)
        val thumbnail = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = thumbnail.createGraphics()

        try {
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, width, height)
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.drawImage(source, 0, 0, width, height, null)
        } finally {
            graphics.dispose()
        }

        return ImageIO.write(thumbnail, "jpg", outputPath.toFile())
    }
}
