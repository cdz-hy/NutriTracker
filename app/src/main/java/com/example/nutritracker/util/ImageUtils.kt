package com.example.nutritracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Shared EXIF-aware & OOM-safe image decoding, compression, thumbnail, and cache cleanup utilities.
 */
object ImageUtils {

    private fun openUriStream(context: Context, uri: Uri): InputStream? {
        context.contentResolver.openInputStream(uri)?.let { return it }
        val path = uri.path ?: return null
        val file = File(path)
        return if (file.exists()) file.inputStream() else null
    }

    private fun readExifRotationDegrees(context: Context, uri: Uri): Int = try {
        openUriStream(context, uri)?.use { stream ->
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (_: Exception) {
        0
    }

    /**
     * Safely decodes an image URI with two-pass sampling and EXIF orientation correction.
     */
    fun loadBitmapSafe(context: Context, uri: Uri, maxW: Int, maxH: Int): Bitmap? {
        val rotation = readExifRotationDegrees(context, uri)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsDecoded = try {
            openUriStream(context, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
        if (!boundsDecoded || bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var rawW = bounds.outWidth
        var rawH = bounds.outHeight
        if (rotation == 90 || rotation == 270) {
            rawW = bounds.outHeight
            rawH = bounds.outWidth
        }

        var inSampleSize = 1
        while ((rawH / inSampleSize) > maxH * 2 || (rawW / inSampleSize) > maxW * 2) {
            inSampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            this.inSampleSize = inSampleSize
        }

        var bitmap = try {
            openUriStream(context, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (_: Exception) {
            null
        } ?: return null

        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
                bitmap = rotated
            }
        }

        val maxDim = maxOf(maxW, maxH)
        val scale = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
        if (scale < 1f) {
            val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
            val resized = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            if (resized != bitmap) {
                bitmap.recycle()
                bitmap = resized
            }
        }

        return bitmap
    }

    fun uriToCompressedBase64(context: Context, uri: Uri, maxDim: Int = 1024, quality: Int = 85): String {
        val bitmap = loadBitmapSafe(context, uri, maxDim, maxDim)
            ?: throw IllegalStateException("无法读取或解码图片")
        return try {
            ByteArrayOutputStream().use { baos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            }
        } finally {
            bitmap.recycle()
        }
    }

    fun saveThumbnail(context: Context, imageUri: Uri, maxDim: Int = 1080, quality: Int = 85): String? {
        val bitmap = loadBitmapSafe(context, imageUri, maxDim, maxDim) ?: return null
        return try {
            val dir = File(context.filesDir, "meal_thumbnails")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "meal_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                fos.flush()
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Cleans up temporary capture/cropped images inside cacheDir.
     */
    fun cleanupTempFiles(context: Context, uris: List<Uri>) {
        val cacheRoot = try {
            context.cacheDir.canonicalFile
        } catch (_: Exception) {
            return
        }
        uris.forEach { uri ->
            if (uri.scheme == "file") {
                val path = uri.path ?: return@forEach
                try {
                    val file = File(path).canonicalFile
                    if (file.parentFile == cacheRoot &&
                        (file.name.startsWith("food_") || file.name.startsWith("cropped_"))
                    ) {
                        file.delete()
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}
