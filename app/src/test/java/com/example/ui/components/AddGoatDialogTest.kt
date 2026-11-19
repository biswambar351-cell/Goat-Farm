package com.example.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.GoatFarmDatabase
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GoatViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddGoatDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: GoatFarmDatabase
    private lateinit var viewModel: GoatViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GoatFarmDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        viewModel = GoatViewModel(database.goatDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun enteringAllFieldsAndSaving_persistsGoatViaViewModel() {
        var dismissed = false
        var savedId = 0L

        composeRule.setContent {
            MyApplicationTheme {
                AddGoatDialog(
                    goatViewModel = viewModel,
                    onDismiss = { dismissed = true },
                    onSaved = { savedId = it }
                )
            }
        }

        composeRule.onNodeWithTag("add_goat_name_input").performScrollTo().performTextInput("Bella")
        composeRule.onNodeWithTag("add_goat_birth_date_input").performScrollTo().performTextInput("2024-04-12")
        composeRule.onNodeWithTag("add_goat_breed_input").performScrollTo().performTextInput("Boer")
        composeRule.onNodeWithTag("add_goat_health_status_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("add_goat_health_status_input").performScrollTo().performTextInput("Needs Checkup")

        composeRule.onNodeWithTag("add_goat_save_button").performClick()
        composeRule.waitForIdle()

        assertTrue(savedId > 0L)
        assertTrue(dismissed)

        val storedGoats = runBlocking { database.goatDao().getAllGoats().first() }
        assertEquals(1, storedGoats.size)
        val saved = storedGoats.first()
        assertEquals("Bella", saved.name)
        assertEquals("2024-04-12", saved.birthDate)
        assertEquals("Boer", saved.breed)
        assertEquals("Needs Checkup", saved.healthStatus)
    }

    @Test
    fun blankNameShowsValidationErrorAndDoesNotDismiss() {
        var dismissed = false

        composeRule.setContent {
            MyApplicationTheme {
                AddGoatDialog(
                    goatViewModel = viewModel,
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeRule.onNodeWithTag("add_goat_birth_date_input").performScrollTo().performTextInput("2024-04-12")
        composeRule.onNodeWithTag("add_goat_breed_input").performScrollTo().performTextInput("Saanen")
        composeRule.onNodeWithTag("add_goat_save_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("add_goat_error_banner").assertIsDisplayed()
        assertTrue(!dismissed)
    }
}
