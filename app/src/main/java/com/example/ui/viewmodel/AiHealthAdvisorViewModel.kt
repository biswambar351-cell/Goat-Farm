package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DiseaseSuggestion
import com.example.data.model.MedicineTonicItem
import com.example.data.model.MedicineTonicSearchResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiHealthAdvisorVM"

sealed interface AiAdvisorUiState {
    data object Idle : AiAdvisorUiState
    data object Loading : AiAdvisorUiState
    data class Streaming(val partialResponse: String) : AiAdvisorUiState
    data class StructuredSuccess(val diagnosis: DiseaseSuggestion) : AiAdvisorUiState
    data class MedicineSearchSuccess(val result: MedicineTonicSearchResult) : AiAdvisorUiState
    data class TextSuccess(val title: String, val content: String) : AiAdvisorUiState
    data class Error(val message: String) : AiAdvisorUiState
}

class AiHealthAdvisorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AiAdvisorUiState>(AiAdvisorUiState.Idle)
    val uiState: StateFlow<AiAdvisorUiState> = _uiState.asStateFlow()

    // Structured JSON Schema for Disease & Health Diagnosis
    private val diseaseSchema = Schema.obj(
        properties = mapOf(
            "conditionName" to Schema.string(description = "Primary disease or medical condition name"),
            "urgency" to Schema.string(description = "Urgency level: Critical / Emergency, Moderate, or Mild / Informational"),
            "likelihood" to Schema.string(description = "Likelihood: High, Medium, or Possible"),
            "summary" to Schema.string(description = "Clear 2-3 sentence overview of the diagnosis"),
            "symptomsMatched" to Schema.array(items = Schema.string(), description = "Matched symptoms"),
            "immediateActions" to Schema.array(items = Schema.string(), description = "Immediate first aid and isolation steps"),
            "treatmentsAndMedications" to Schema.array(items = Schema.string(), description = "Recommended veterinary treatments, supportive care, and medication protocols"),
            "preventionAndBiosecurity" to Schema.array(items = Schema.string(), description = "Preventative biosecurity measures"),
            "notesAndWithdrawal" to Schema.string(description = "Special notes regarding age group, milk/meat withdrawal, or pregnancy"),
            "disclaimer" to Schema.string(description = "Veterinary advisory disclaimer")
        ),
        optionalProperties = listOf("notesAndWithdrawal")
    )

    // Structured JSON Schema for Medicine, Tonic & Supplement Search + Weight/Health Suitability Check
    private val medicineSearchSchema = Schema.obj(
        properties = mapOf(
            "conditionVerdict" to Schema.string(
                description = "Overall verdict such as 'Good / Healthy', 'Needs Tonic & Supplement Support', 'Not Good - Needs Treatment', or 'Critical - Immediate Vet Care'"
            ),
            "isStatusGood" to Schema.boolean(
                description = "True if the goat's weight, health, and any uploaded product look good; false if found not good, underweight, unsafe, or needing change"
            ),
            "assessmentSummary" to Schema.string(
                description = "Clear 2-4 sentence evaluation explaining whether the goat's health/weight or uploaded medicine/tonic is good or not good and why"
            ),
            "weightEvaluation" to Schema.string(
                description = "Evaluation of the uploaded weight (kg) relative to breed/age and how weight affects medicine/tonic dosing"
            ),
            "recommendations" to Schema.array(
                description = "Ranked list of best veterinary medicines, tonics, and supplements matching the search and goat profile",
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Product type or generic veterinary medicine/tonic/supplement name"),
                        "category" to Schema.string(description = "Must be one of: Medicine, Tonic, Supplement"),
                        "activeIngredients" to Schema.string(description = "Key active compounds, vitamins, minerals, or herbal extracts"),
                        "purposeAndBenefits" to Schema.string(description = "What it treats or improves for this goat"),
                        "dosageForWeight" to Schema.string(description = "Weight-adjusted dosage guideline (e.g. ml or mg per kg calculated for uploaded weight)"),
                        "administrationRoute" to Schema.string(description = "How to give it: Oral drench, Mixed in feed/water, SubQ, IM, etc."),
                        "safetyAndWithdrawal" to Schema.string(description = "Pregnancy/kid safety and milk/meat withdrawal precautions")
                    ),
                    optionalProperties = listOf("safetyAndWithdrawal")
                )
            ),
            "betterAlternativesIfNotGood" to Schema.array(
                items = Schema.string(),
                description = "Specific better medicines, tonics, dietary changes, or vet escalations if the current weight/health or uploaded product was found not good"
            ),
            "feedingAndCareTips" to Schema.array(
                items = Schema.string(),
                description = "Supportive nutrition, hydration, deworming, and management tips"
            ),
            "disclaimer" to Schema.string(description = "Veterinary advisory disclaimer")
        ),
        optionalProperties = listOf("weightEvaluation", "betterAlternativesIfNotGood", "feedingAndCareTips")
    )

    private val structuredConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = diseaseSchema
    }

    private val medicineSearchConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = medicineSearchSchema
    }

    // Lazy initialization of models using gemini-3.8-flash per Firebase AI Logic guidelines
    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = structuredConfig
        )
    }

    private val medicineSearchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = medicineSearchConfig
        )
    }

    private val textModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    fun resetState() {
        _uiState.value = AiAdvisorUiState.Idle
    }

    /**
     * Analyzes observed goat symptoms, breed, age group, and free-form notes to produce
     * a structured veterinary diagnosis and treatment suggestion.
     */
    fun diagnoseDisease(
        goatTag: String,
        breed: String,
        ageGroup: String,
        symptoms: List<String>,
        additionalNotes: String
    ) {
        viewModelScope.launch {
            _uiState.value = AiAdvisorUiState.Loading
            try {
                val prompt = buildString {
                    appendLine("You are an expert caprine veterinarian and goat herd health specialist.")
                    appendLine("Perform a thorough clinical assessment for the following goat:")
                    if (goatTag.isNotBlank()) appendLine("- Goat Tag / Name: $goatTag")
                    if (breed.isNotBlank()) appendLine("- Breed: $breed")
                    if (ageGroup.isNotBlank()) appendLine("- Age Group / Lifecycle Stage: $ageGroup")
                    appendLine("- Observed Symptoms: ${if (symptoms.isNotEmpty()) symptoms.joinToString(", ") else "None selected explicitly"}")
                    if (additionalNotes.isNotBlank()) appendLine("- Farmer's Clinical Notes: $additionalNotes")
                    appendLine()
                    appendLine("Identify the most probable condition/disease, urgency, immediate first-aid, medical and supportive treatments, prevention, and withdrawal notes.")
                }

                val response = withTimeout(60_000L) {
                    structuredModel.generateContent(prompt)
                }

                val jsonText = response.text
                if (jsonText.isNullOrBlank()) {
                    _uiState.value = AiAdvisorUiState.Error("AI model returned an empty response. Please try again.")
                    return@launch
                }

                val json = JSONObject(jsonText)
                val diagnosis = DiseaseSuggestion(
                    conditionName = json.optString("conditionName", "Caprine Health Assessment"),
                    urgency = json.optString("urgency", "Moderate"),
                    likelihood = json.optString("likelihood", "High"),
                    summary = json.optString("summary", ""),
                    symptomsMatched = json.optJSONArray("symptomsMatched")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList(),
                    immediateActions = json.optJSONArray("immediateActions")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList(),
                    treatmentsAndMedications = json.optJSONArray("treatmentsAndMedications")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList(),
                    preventionAndBiosecurity = json.optJSONArray("preventionAndBiosecurity")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList(),
                    notesAndWithdrawal = json.optString("notesAndWithdrawal", ""),
                    disclaimer = json.optString("disclaimer", "Veterinary advisory guide. Please consult a licensed veterinarian for formal prescription.")
                )

                _uiState.value = AiAdvisorUiState.StructuredSuccess(diagnosis)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Diagnosis generation timed out", e)
                _uiState.value = AiAdvisorUiState.Error("Diagnosis timed out after 60 seconds. Please check your network and try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Disease diagnosis failed", e)
                _uiState.value = AiAdvisorUiState.Error(
                    e.localizedMessage ?: "Failed to generate diagnosis suggestion."
                )
            }
        }
    }

    /**
     * Searches and recommends the best veterinary medicines, tonics, and supplements while
     * evaluating uploaded goat weight, health notes, and optional photo (goat body condition or product bottle/label).
     * Explicitly flags when weight, health, or a checked product is found NOT GOOD and provides better alternatives.
     */
    fun searchMedicineTonicAndEvaluateHealth(
        searchQuery: String,
        categoryFilter: String = "All",
        goatTag: String = "",
        breed: String = "",
        ageGroup: String = "",
        weightKg: String = "",
        healthNotes: String = "",
        attachedBitmap: Bitmap? = null,
        photoSubject: String = "Goat or Product Label"
    ) {
        if (searchQuery.isBlank() && weightKg.isBlank() && healthNotes.isBlank() && attachedBitmap == null) {
            _uiState.value = AiAdvisorUiState.Error("Please enter a search query, goat weight, health note, or upload a photo.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AiAdvisorUiState.Loading
            try {
                val prompt = buildString {
                    appendLine("You are an expert caprine veterinary pharmacologist and goat nutritionist.")
                    appendLine("The farmer wants to search for the best medicines, tonics, and supplements, AND wants an honest evaluation of whether the goat's weight, health status, or any uploaded product/photo is GOOD or NOT GOOD.")
                    appendLine()
                    appendLine("Farmer Inputs:")
                    if (searchQuery.isNotBlank()) appendLine("- Search Query / Goal: $searchQuery")
                    appendLine("- Requested Category Focus: $categoryFilter")
                    if (goatTag.isNotBlank()) appendLine("- Goat Tag / Name: $goatTag")
                    if (breed.isNotBlank()) appendLine("- Breed: $breed")
                    if (ageGroup.isNotBlank()) appendLine("- Age / Life Stage: $ageGroup")
                    if (weightKg.isNotBlank()) appendLine("- Uploaded Current Weight: $weightKg kg")
                    if (healthNotes.isNotBlank()) appendLine("- Health Status, Current Product & Other Notes: $healthNotes")
                    if (attachedBitmap != null) appendLine("- Attached Photo Type: $photoSubject (analyze the image carefully)")
                    appendLine()
                    appendLine("Instructions:")
                    appendLine("1. Evaluate the goat's weight against its breed/age/stage and any health notes or attached photo. If an image of a medicine/tonic/supplement bottle or label is attached, read the ingredients and check if it is safe and effective for goats and for this specific condition.")
                    appendLine("2. Set isStatusGood = false whenever the goat appears underweight/unhealthy, has concerning symptoms, or the checked medicine/tonic is unsuitable or weak for the goat; set isStatusGood = true only if weight/health or product suitability is good.")
                    appendLine("3. Provide 3 to 5 ranked best Medicines, Tonics, and Supplements (respecting '$categoryFilter' focus when specified, otherwise include a balanced mix). Calculate weight-specific dosage guidance using the uploaded weight ($weightKg kg) when provided.")
                    appendLine("4. If found NOT GOOD (or suboptimal), clearly list concrete better alternatives in betterAlternativesIfNotGood and supportive feeding/care tips.")
                }

                val response = withTimeout(60_000L) {
                    if (attachedBitmap != null) {
                        medicineSearchModel.generateContent(
                            content {
                                image(attachedBitmap)
                                text(prompt)
                            }
                        )
                    } else {
                        medicineSearchModel.generateContent(prompt)
                    }
                }

                val jsonText = response.text
                if (jsonText.isNullOrBlank()) {
                    _uiState.value = AiAdvisorUiState.Error("AI returned an empty response. Please try again.")
                    return@launch
                }

                val parsed = parseMedicineTonicSearchResult(jsonText)
                _uiState.value = AiAdvisorUiState.MedicineSearchSuccess(parsed)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Medicine & tonic search timed out", e)
                _uiState.value = AiAdvisorUiState.Error("Request timed out after 60 seconds. Please check your connection and try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Medicine & tonic AI search failed", e)
                _uiState.value = AiAdvisorUiState.Error(
                    e.localizedMessage ?: "Failed to search medicines, tonics, and supplements."
                )
            }
        }
    }

    /**
     * Generates tailored advice for Preventative Health (Vaccines, Deworming, Hoof Trimming)
     * or Nutrition/Feed and Breeding management using real-time streaming to minimize latency.
     */
    fun askAdvisorStream(title: String, prompt: String) {
        viewModelScope.launch {
            _uiState.value = AiAdvisorUiState.Loading
            val accumulated = StringBuilder()
            try {
                val systemPrompt = "You are an expert caprine veterinarian and goat herd consultant at Khulana Goat Farm. Provide practical, accurate, evidence-based advice for goat farmers formatted with clear headers and bullet points.\n\n$prompt"
                withTimeout(60_000L) {
                    textModel.generateContentStream(systemPrompt).collect { chunk ->
                        val text = chunk.text.orEmpty()
                        accumulated.append(text)
                        _uiState.value = AiAdvisorUiState.Streaming(accumulated.toString())
                    }
                }
                if (accumulated.isNotBlank()) {
                    _uiState.value = AiAdvisorUiState.TextSuccess(title, accumulated.toString())
                } else {
                    _uiState.value = AiAdvisorUiState.Error("AI advisor returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Streaming timed out", e)
                _uiState.value = AiAdvisorUiState.Error("Streaming response timed out. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Streaming AI advisory failed", e)
                _uiState.value = AiAdvisorUiState.Error(
                    e.localizedMessage ?: "Failed to generate AI advisory response."
                )
            }
        }
    }

    companion object {
        fun parseMedicineTonicSearchResult(jsonText: String): MedicineTonicSearchResult {
            val json = JSONObject(jsonText)
            val recArray = json.optJSONArray("recommendations")
            val recommendations = if (recArray != null) {
                (0 until recArray.length()).mapNotNull { idx ->
                    val item = recArray.optJSONObject(idx) ?: return@mapNotNull null
                    MedicineTonicItem(
                        name = item.optString("name", "").trim(),
                        category = item.optString("category", "Tonic").trim(),
                        activeIngredients = item.optString("activeIngredients", "").trim(),
                        purposeAndBenefits = item.optString("purposeAndBenefits", "").trim(),
                        dosageForWeight = item.optString("dosageForWeight", "").trim(),
                        administrationRoute = item.optString("administrationRoute", "").trim(),
                        safetyAndWithdrawal = item.optString("safetyAndWithdrawal", "").trim()
                    )
                }.filter { it.name.isNotBlank() }
            } else {
                emptyList()
            }

            val alternativesArray = json.optJSONArray("betterAlternativesIfNotGood")
            val alternatives = if (alternativesArray != null) {
                (0 until alternativesArray.length())
                    .map { alternativesArray.optString(it, "").trim() }
                    .filter { it.isNotBlank() }
            } else {
                emptyList()
            }

            val tipsArray = json.optJSONArray("feedingAndCareTips")
            val tips = if (tipsArray != null) {
                (0 until tipsArray.length())
                    .map { tipsArray.optString(it, "").trim() }
                    .filter { it.isNotBlank() }
            } else {
                emptyList()
            }

            return MedicineTonicSearchResult(
                conditionVerdict = json.optString("conditionVerdict", "Assessment Complete").trim(),
                isStatusGood = json.optBoolean("isStatusGood", true),
                assessmentSummary = json.optString("assessmentSummary", "").trim(),
                weightEvaluation = json.optString("weightEvaluation", "").trim(),
                recommendations = recommendations,
                betterAlternativesIfNotGood = alternatives,
                feedingAndCareTips = tips,
                disclaimer = json.optString(
                    "disclaimer",
                    "Veterinary advisory guide. Always verify label concentration and consult a licensed veterinarian for prescription dosing."
                ).trim()
            )
        }
    }
}

