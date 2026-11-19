package com.example.util

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AiMediaHelperTest {

    @Test
    fun scaleDownIfNeeded_downscalesOversizedBitmapProportionally() {
        val large = Bitmap.createBitmap(2048, 1024, Bitmap.Config.ARGB_8888)
        val scaled = AiMediaHelper.scaleDownIfNeeded(large, maxDimensionPx = 1024)

        assertEquals(1024, scaled.width)
        assertEquals(512, scaled.height)
    }

    @Test
    fun scaleDownIfNeeded_keepsSmallBitmapUnchanged() {
        val small = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val result = AiMediaHelper.scaleDownIfNeeded(small, maxDimensionPx = 1024)

        assertSame(small, result)
    }
}
