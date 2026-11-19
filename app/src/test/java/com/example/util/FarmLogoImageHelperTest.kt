package com.example.util

import android.graphics.Bitmap
import com.example.data.model.FarmProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FarmLogoImageHelperTest {

    @Test
    fun encodeAndDecodeBitmap_centerCropsAndScalesToSquare() {
        val wideBitmap = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888)
        val encoded = FarmLogoImageHelper.encodeBitmapToBase64(wideBitmap, targetSizePx = 200)

        assertTrue(encoded.isNotBlank())
        val decoded = FarmLogoImageHelper.decodeBase64ToImageBitmap(encoded)
        assertNotNull(decoded)
        assertEquals(200, decoded!!.width)
        assertEquals(200, decoded.height)
    }

    @Test
    fun decodeInvalidOrBlankBase64_returnsNullSafely() {
        assertNull(FarmLogoImageHelper.decodeBase64ToImageBitmap(""))
        assertNull(FarmLogoImageHelper.decodeBase64ToImageBitmap("   "))
        assertNull(FarmLogoImageHelper.decodeBase64ToImageBitmap("not-valid-image-data"))
    }

    @Test
    fun farmProfileToMap_serializesAndNormalizesLogoFields() {
        val profile = FarmProfile(
            farmName = " Green Valley Farm ",
            address = " Khulna ",
            logoType = "emblem",
            logoEmblemId = "shield",
            logoColorIndex = 3,
            logoInitials = " gvf123 ",
            logoShape = "badge",
            logoImageBase64 = ""
        )

        val map = profile.toMap()
        assertEquals("Green Valley Farm", map["farmName"])
        assertEquals("Khulna", map["address"])
        assertEquals("emblem", map["logoType"])
        assertEquals("shield", map["logoEmblemId"])
        assertEquals(3, map["logoColorIndex"])
        assertEquals("GVF1", map["logoInitials"])
        assertEquals("badge", map["logoShape"])
        assertEquals("", map["logoImageBase64"])
    }
}
