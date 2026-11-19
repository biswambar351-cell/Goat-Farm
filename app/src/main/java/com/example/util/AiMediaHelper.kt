package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Safely decodes and downsamples user-picked photos (goat body condition or medicine/tonic labels)
 * for multimodal Firebase AI Logic requests while keeping payloads lightweight (< 20 MB).
 */
object AiMediaHelper {

    private const val DEFAULT_MAX_DIMENSION_PX = 1024

    fun decodeUriToBitmap(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = DEFAULT_MAX_DIMENSION_PX
    ): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            } ?: return null

            val width = bounds.outWidth
            val height = bounds.outHeight
            if (width <= 0 || height <= 0) return null

            var sampleSize = 1
            while (width / sampleSize > maxDimensionPx * 2 || height / sampleSize > maxDimensionPx * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val scaled = scaleDownIfNeeded(decoded, maxDimensionPx)
            if (scaled !== decoded && !decoded.isRecycled) {
                decoded.recycle()
            }
            scaled
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    fun scaleDownIfNeeded(source: Bitmap, maxDimensionPx: Int = DEFAULT_MAX_DIMENSION_PX): Bitmap {
        val largestSide = max(source.width, source.height)
        if (largestSide <= maxDimensionPx || largestSide <= 0) {
            return source
        }
        val ratio = maxDimensionPx.toFloat() / largestSide.toFloat()
        val targetWidth = max(1, (source.width * ratio).roundToInt())
        val targetHeight = max(1, (source.height * ratio).roundToInt())
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }
}
