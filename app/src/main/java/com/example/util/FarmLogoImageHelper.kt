package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Helper for center-cropping, scaling, and Base64-encoding custom farm logo images
 * picked via Android Photo Picker so they remain lightweight for Firestore persistence.
 */
object FarmLogoImageHelper {

    private const val MAX_LOGO_DIMENSION_PX = 200
    private const val JPEG_QUALITY = 84

    fun loadScaledLogoBase64FromUri(context: Context, uri: Uri): String? {
        return try {
            val resolver = context.contentResolver

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: return null

            val srcWidth = boundsOptions.outWidth
            val srcHeight = boundsOptions.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return null

            var sampleSize = 1
            while (srcWidth / sampleSize > MAX_LOGO_DIMENSION_PX * 2 &&
                srcHeight / sampleSize > MAX_LOGO_DIMENSION_PX * 2
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val encoded = encodeBitmapToBase64(decoded)
            if (!decoded.isRecycled) {
                decoded.recycle()
            }
            encoded
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    fun encodeBitmapToBase64(
        source: Bitmap,
        targetSizePx: Int = MAX_LOGO_DIMENSION_PX,
        quality: Int = JPEG_QUALITY
    ): String {
        val minSide = max(1, min(source.width, source.height))
        val cropX = (source.width - minSide) / 2
        val cropY = (source.height - minSide) / 2

        val squared = if (source.width == source.height) {
            source
        } else {
            Bitmap.createBitmap(source, cropX, cropY, minSide, minSide)
        }

        val scaled = if (squared.width > targetSizePx) {
            Bitmap.createScaledBitmap(squared, targetSizePx, targetSizePx, true)
        } else {
            squared
        }

        val outStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
        val bytes = outStream.toByteArray()

        if (scaled !== squared && !scaled.isRecycled) scaled.recycle()
        if (squared !== source && !squared.isRecycled) squared.recycle()

        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }
}
