package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.model.Goat
import com.example.data.model.MedicineTonicItem
import com.example.data.model.MedicineTonicSearchResult
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AiAdvisorUiState
import com.example.ui.viewmodel.AiHealthAdvisorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AiMedicineTonicTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun parseMedicineTonicSearchResult_parsesNotGoodStatusAndAlternatives() {
        val sampleJson = """
            {
              "conditionVerdict": "Not Good - Underweight & Anemia Risk",
              "isStatusGood": false,
              "assessmentSummary": "At 12 kg for an adult Black Bengal doe, body weight is below healthy target and coat suggests nutritional deficit.",
              "weightEvaluation": "12 kg is ~30% below expected adult weight (18-22 kg). Dose liquids strictly for 12 kg.",
              "recommendations": [
                {
                  "name": "Herbal Liver & Appetite Tonic (Liv-52 Vet / Silymarin)",
                  "category": "Tonic",
                  "activeIngredients": "Silymarin, B-Complex, Choline",
                  "purposeAndBenefits": "Restores liver metabolism and stimulates dry matter appetite.",
                  "dosageForWeight": "10 ml twice daily for a 12 kg goat for 7-10 days",
                  "administrationRoute": "Oral drench",
                  "safetyAndWithdrawal": "Safe for pregnant and lactating does; 0 days withdrawal"
                },
                {
                  "name": "Chelated Trace Mineral & Vitamin AD3E Supplement",
                  "category": "Supplement",
                  "activeIngredients": "Zinc, Copper, Cobalt, Vitamin A/D3/E",
                  "purposeAndBenefits": "Improves weight gain, coat quality, and immunity.",
                  "dosageForWeight": "10-15 g daily mixed in concentrate for 12 kg",
                  "administrationRoute": "Mixed in feed",
                  "safetyAndWithdrawal": "Provide clean water ad libitum"
                }
              ],
              "betterAlternativesIfNotGood": [
                "Switch from plain mineral salt to chelated amino-acid + liver tonic combo",
                "Check FAMACHA eyelid score and run fecal exam before iron supplementation"
              ],
              "feedingAndCareTips": [
                "Increase green leguminous forage and offer 200 g crushed grain mix daily"
              ],
              "disclaimer": "Consult a licensed veterinarian for clinical exam."
            }
        """.trimIndent()

        val parsed = AiHealthAdvisorViewModel.parseMedicineTonicSearchResult(sampleJson)
        assertFalse(parsed.isStatusGood)
        assertEquals("Not Good - Underweight & Anemia Risk", parsed.conditionVerdict)
        assertEquals(2, parsed.recommendations.size)
        assertEquals("Tonic", parsed.recommendations[0].category)
        assertTrue(parsed.recommendations[0].dosageForWeight.contains("12 kg"))
        assertEquals(2, parsed.betterAlternativesIfNotGood.size)
        assertEquals(1, parsed.feedingAndCareTips.size)
    }

    @Test
    fun medicineTonicFinderTab_submitsSearchQueryWeightAndHealthNotes() {
        var capturedQuery = ""
        var capturedCategory = ""
        var capturedWeight = ""
        var capturedNotes = ""

        composeRule.setContent {
            MyApplicationTheme {
                MedicineTonicFinderTab(
                    goats = listOf(
                        Goat(tag = "GT-12", name = "Mina", breed = "Black Bengal", weightKg = 14.0)
                    ),
                    initialGoatTag = null,
                    uiState = AiAdvisorUiState.Idle,
                    onSearchAndEvaluate = { query, category, _, _, _, weight, notes, _, _ ->
                        capturedQuery = query
                        capturedCategory = category
                        capturedWeight = weight
                        capturedNotes = notes
                    },
                    onReset = {},
                    onLogMedicine = {}
                )
            }
        }

        composeRule.onNodeWithTag("input_medicine_search_query").performScrollTo().performTextInput("best liver tonic")
        composeRule.onNodeWithTag("medicine_filter_tonic").performScrollTo().performClick()
        composeRule.onNodeWithTag("input_medicine_health_notes").performScrollTo().performTextInput("Weak after kidding")
        composeRule.onNodeWithTag("btn_search_medicine_tonic").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("best liver tonic", capturedQuery)
        assertEquals("Tonic", capturedCategory)
        assertEquals("14.0", capturedWeight)
        assertEquals("Weak after kidding", capturedNotes)
    }

    @Test
    fun medicineTonicResultCard_showsNotGoodBannerAndLogsSelectedItem() {
        var loggedItem: MedicineTonicItem? = null
        val sampleResult = MedicineTonicSearchResult(
            conditionVerdict = "Not Good - Needs Recovery Tonic",
            isStatusGood = false,
            assessmentSummary = "Current weight is low for age and goat needs liver + mineral boost.",
            weightEvaluation = "13.5 kg requires careful dosing.",
            recommendations = listOf(
                MedicineTonicItem(
                    name = "B-Complex Liver Tonic",
                    category = "Tonic",
                    activeIngredients = "Vitamin B1, B12, Liver Extract",
                    purposeAndBenefits = "Appetite and weight recovery",
                    dosageForWeight = "8 ml orally twice daily for 13.5 kg",
                    administrationRoute = "Oral",
                    safetyAndWithdrawal = "No withdrawal"
                )
            ),
            betterAlternativesIfNotGood = listOf("Replace plain glucose water with electrolyte + liver tonic")
        )

        composeRule.setContent {
            MyApplicationTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    MedicineTonicResultCard(
                        result = sampleResult,
                        goatTag = "GT-12",
                        onReset = {},
                        onLogItem = { loggedItem = it }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("medicine_verdict_banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("medicine_not_good_alternatives_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("B-Complex Liver Tonic").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("btn_log_medicine_item_0").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("B-Complex Liver Tonic", loggedItem?.name)
    }

    @Test
    fun emptyInputs_setsValidationErrorStateInViewModel() {
        val vm = AiHealthAdvisorViewModel()
        vm.searchMedicineTonicAndEvaluateHealth(
            searchQuery = "",
            weightKg = "",
            healthNotes = "",
            attachedBitmap = null
        )
        assertTrue(vm.uiState.value is AiAdvisorUiState.Error)
    }
}
