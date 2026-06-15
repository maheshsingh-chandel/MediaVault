package com.mediavault.metadata

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.mediavault.core.model.MediaFile
import java.io.File
import javax.imageio.ImageIO
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ImageMetadataExtractor : MetadataExtractor {
    override fun extract(mediaFile: MediaFile): String {
        val file = File(mediaFile.path)
        val dimensions = runCatching { ImageIO.read(file) }.getOrNull()
        val metadata = runCatching { ImageMetadataReader.readMetadata(file) }.getOrNull()

        return baseMetadata(mediaFile) {
            put("status", "READY")
            put(
                "image",
                buildJsonObject {
                    if (dimensions != null) {
                        put("width", dimensions.width)
                        put("height", dimensions.height)
                    }
                    metadata?.cameraInfo()?.let { put("camera", it) }
                    metadata?.gpsInfo()?.let { put("gps", it) }
                    metadata?.exifInfo()?.let { put("exif", it) }
                },
            )
        }
    }

    private fun Metadata.cameraInfo(): JsonObject? {
        val ifd0 = getFirstDirectoryOfType(ExifIFD0Directory::class.java) ?: return null
        val make = ifd0.getString(ExifIFD0Directory.TAG_MAKE)
        val model = ifd0.getString(ExifIFD0Directory.TAG_MODEL)
        if (make.isNullOrBlank() && model.isNullOrBlank()) return null

        return buildJsonObject {
            if (!make.isNullOrBlank()) put("make", make)
            if (!model.isNullOrBlank()) put("model", model)
        }
    }

    private fun Metadata.gpsInfo(): JsonObject? {
        val location = getFirstDirectoryOfType(GpsDirectory::class.java)?.geoLocation ?: return null
        if (location.isZero) return null

        return buildJsonObject {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
        }
    }

    private fun Metadata.exifInfo(): JsonObject? {
        val ifd0 = getFirstDirectoryOfType(ExifIFD0Directory::class.java)
        val subIfd = getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)

        val values = buildJsonObject {
            ifd0?.getString(ExifIFD0Directory.TAG_SOFTWARE)?.let { put("software", it) }
            ifd0?.getString(ExifIFD0Directory.TAG_DATETIME)?.let { put("dateTime", it) }
            subIfd?.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)?.let { put("exposureTime", it) }
            subIfd?.getString(ExifSubIFDDirectory.TAG_FNUMBER)?.let { put("fNumber", it) }
            subIfd?.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)?.let { put("iso", it) }
            subIfd?.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)?.let { put("focalLength", it) }
            subIfd?.getString(ExifSubIFDDirectory.TAG_LENS_MODEL)?.let { put("lensModel", it) }
        }

        return values.takeIf { it.isNotEmpty() }
    }
}
