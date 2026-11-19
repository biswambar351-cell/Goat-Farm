package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.example.data.model.FarmProfile
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FarmLogoImageHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FarmLogoEditorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun farmLogoView_rendersDefaultEmblemAndPhotoModes() {
        val defaultProfile = FarmProfile()
        composeRule.setContent {
            MyApplicationTheme {
                FarmLogoView(profile = defaultProfile, size = 56.dp)
            }
        }
        composeRule.onNodeWithTag("farm_logo_default_image").assertIsDisplayed()
    }

    @Test
    fun farmLogoView_rendersEmblemWithInitials() {
        val emblemProfile = FarmProfile(
            logoType = "emblem",
            logoEmblemId = "pasture_eco",
            logoColorIndex = 1,
            logoInitials = "KGF",
            logoShape = "rounded"
        )
        composeRule.setContent {
            MyApplicationTheme {
                FarmLogoView(profile = emblemProfile, size = 56.dp)
            }
        }
        composeRule.onNodeWithTag("farm_logo_emblem_container").assertIsDisplayed()
        composeRule.onNodeWithText("KGF").assertIsDisplayed()
    }

    @Test
    fun farmLogoView_rendersCustomPhotoWhenBase64Present() {
        val sampleBitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        val encoded = FarmLogoImageHelper.encodeBitmapToBase64(sampleBitmap)
        val photoProfile = FarmProfile(
            logoType = "photo",
            logoImageBase64 = encoded,
            logoShape = "circle"
        )
        composeRule.setContent {
            MyApplicationTheme {
                FarmLogoView(profile = photoProfile, size = 56.dp)
            }
        }
        composeRule.onNodeWithTag("farm_logo_photo_image").assertIsDisplayed()
    }

    @Test
    fun editLogoDialog_customizingEmblemAndSaving_returnsUpdatedProfile() {
        var savedProfile: FarmProfile? = null

        composeRule.setContent {
            MyApplicationTheme {
                EditLogoDialog(
                    currentProfile = FarmProfile(),
                    onDismiss = {},
                    onSave = { savedProfile = it }
                )
            }
        }

        composeRule.onNodeWithTag("logo_shape_badge").performScrollTo().performClick()
        composeRule.onNodeWithTag("logo_emblem_pasture_eco").performScrollTo().performClick()
        composeRule.onNodeWithTag("logo_color_2").performScrollTo().performClick()
        composeRule.onNodeWithTag("logo_initials_input").performScrollTo().performTextInput("kgf")
        composeRule.onNodeWithTag("edit_logo_save_button").performClick()
        composeRule.waitForIdle()

        assertNotNull(savedProfile)
        assertEquals("emblem", savedProfile!!.logoType)
        assertEquals("badge", savedProfile!!.logoShape)
        assertEquals("pasture_eco", savedProfile!!.logoEmblemId)
        assertEquals(2, savedProfile!!.logoColorIndex)
        assertEquals("KGF", savedProfile!!.logoInitials)
    }

    @Test
    fun editLogoDialog_resetToDefault_clearsCustomLogoFields() {
        var savedProfile: FarmProfile? = null
        val customInitial = FarmProfile(
            logoType = "emblem",
            logoEmblemId = "shield",
            logoColorIndex = 4,
            logoInitials = "ABC",
            logoShape = "badge",
            logoImageBase64 = "abc"
        )

        composeRule.setContent {
            MyApplicationTheme {
                EditLogoDialog(
                    currentProfile = customInitial,
                    onDismiss = {},
                    onSave = { savedProfile = it }
                )
            }
        }

        composeRule.onNodeWithTag("logo_reset_default_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("edit_logo_save_button").performClick()
        composeRule.waitForIdle()

        assertNotNull(savedProfile)
        assertEquals("default", savedProfile!!.logoType)
        assertEquals("circle", savedProfile!!.logoShape)
        assertEquals("", savedProfile!!.logoInitials)
        assertEquals("", savedProfile!!.logoImageBase64)
    }
}
