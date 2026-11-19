package com.example.data.model

/**
 * Structured model representing an AI disease diagnosis or health suggestion.
 */
data class DiseaseSuggestion(
    val conditionName: String = "",
    val urgency: String = "Moderate", // "Critical / Emergency", "Moderate", "Mild / Informational"
    val likelihood: String = "High",   // "High", "Medium", "Possible"
    val summary: String = "",
    val symptomsMatched: List<String> = emptyList(),
    val immediateActions: List<String> = emptyList(),
    val treatmentsAndMedications: List<String> = emptyList(),
    val preventionAndBiosecurity: List<String> = emptyList(),
    val notesAndWithdrawal: String = "",
    val disclaimer: String = "Veterinary advisory guide. Please contact a licensed veterinarian for urgent emergencies or definitive medical prescription."
)

/**
 * Category of AI suggestions.
 */
enum class SuggestionCategory(val displayName: String, val description: String) {
    DISEASE_DIAGNOSIS("Disease & Symptoms", "Identify potential illnesses, severity & immediate emergency first-aid"),
    PREVENTATIVE_HEALTH("Health & Vaccines", "Vaccination, deworming schedules, hoof care & herd biosecurity"),
    NUTRITION_AND_FEED("Feed & Minerals", "Rations, mineral deficiencies, lactation nutrition & kid weaning"),
    BREEDING_AND_KIDDING("Breeding & Kidding", "Pregnancy toxemia, labor dystocia, newborn colostrum & care"),
    GENERAL_ADVISOR("Ask Farm Vet", "Ask any custom veterinary, husbandry, or farm management question")
}

/**
 * Preset common symptoms for fast multi-select triage.
 */
data class SymptomOption(
    val id: String,
    val label: String,
    val category: String
)

val COMMON_GOAT_SYMPTOMS = listOf(
    SymptomOption("lethargy", "Lethargy / Dullness", "General"),
    SymptomOption("fever", "High Body Temp (>104°F / 40°C)", "General"),
    SymptomOption("hypothermia", "Low Temp (<101.5°F / 38.6°C)", "General"),
    SymptomOption("loss_of_appetite", "Off Feed / Anorexia", "General"),
    SymptomOption("diarrhea", "Scours / Watery Diarrhea", "Digestive"),
    SymptomOption("bloat", "Left Flank Swelling (Bloat)", "Digestive"),
    SymptomOption("cud_stopped", "Not Chewing Cud", "Digestive"),
    SymptomOption("coughing", "Persistent Coughing", "Respiratory"),
    SymptomOption("nasal_discharge", "Nasal Discharge / Snot", "Respiratory"),
    SymptomOption("labored_breathing", "Rapid / Labored Breathing", "Respiratory"),
    SymptomOption("pale_mucosa", "Pale Inner Eyelids (FAMACHA 4-5)", "Parasitic"),
    SymptomOption("bottle_jaw", "Swelling Under Jaw (Bottle Jaw)", "Parasitic"),
    SymptomOption("rough_coat", "Rough / Unthrifty Hair Coat", "Parasitic"),
    SymptomOption("limping", "Limping / Lameness", "Hoof & Joint"),
    SymptomOption("foul_foot_odor", "Foul Odor / Rot in Hooves", "Hoof & Joint"),
    SymptomOption("swollen_joints", "Swollen Knees / Hock Joints", "Hoof & Joint"),
    SymptomOption("swollen_udder", "Hard / Hot / Painful Udder", "Mammary"),
    SymptomOption("abnormal_milk", "Clumpy / Stringy / Bloody Milk", "Mammary"),
    SymptomOption("head_pressing", "Star Gazing / Head Pressing", "Neurological"),
    SymptomOption("circling", "Circling / Incoordination", "Neurological"),
    SymptomOption("mouth_blisters", "Crusty Scabs on Lips / Mouth", "Skin & Oral")
)

/**
 * Individual medicine, tonic, or supplement recommendation tailored to goat weight & health.
 */
data class MedicineTonicItem(
    val name: String = "",
    val category: String = "Tonic", // "Medicine", "Tonic", "Supplement"
    val activeIngredients: String = "",
    val purposeAndBenefits: String = "",
    val dosageForWeight: String = "",
    val administrationRoute: String = "",
    val safetyAndWithdrawal: String = ""
)

/**
 * Structured AI evaluation result for goat weight, health/photo status,
 * best medicines/tonics/supplements, and better alternatives if current status or product is not good.
 */
data class MedicineTonicSearchResult(
    val conditionVerdict: String = "Needs Assessment",
    val isStatusGood: Boolean = true,
    val assessmentSummary: String = "",
    val weightEvaluation: String = "",
    val recommendations: List<MedicineTonicItem> = emptyList(),
    val betterAlternativesIfNotGood: List<String> = emptyList(),
    val feedingAndCareTips: List<String> = emptyList(),
    val disclaimer: String = "Veterinary advisory guide. Always verify exact product concentration on the label and consult a licensed veterinarian for prescription dosing."
)

data class MedicineSearchPreset(
    val id: String,
    val label: String,
    val query: String,
    val categoryFilter: String
)

val COMMON_MEDICINE_TONIC_PRESETS = listOf(
    MedicineSearchPreset(
        id = "liver_tonic",
        label = "Liver & Appetite Tonic",
        query = "Best liver tonic and appetite booster for weak or off-feed goats",
        categoryFilter = "Tonic"
    ),
    MedicineSearchPreset(
        id = "weight_gain",
        label = "Weight Gain & Growth",
        query = "Best weight gain supplement, amino acid & mineral mix for underweight goats",
        categoryFilter = "Supplement"
    ),
    MedicineSearchPreset(
        id = "anemia_tonic",
        label = "Anemia & Weakness Tonic",
        query = "Best iron, B-complex and hematinic tonic for pale eyelids or post-worm recovery",
        categoryFilter = "Tonic"
    ),
    MedicineSearchPreset(
        id = "calcium_milk",
        label = "Lactation Calcium Tonic",
        query = "Best calcium, phosphorus and vitamin AD3E tonic for milking does",
        categoryFilter = "Tonic"
    ),
    MedicineSearchPreset(
        id = "deworm_medicine",
        label = "Dewormer & Gut Care",
        query = "Best broad-spectrum dewormer medicine and rumen probiotic recovery",
        categoryFilter = "Medicine"
    ),
    MedicineSearchPreset(
        id = "immunity_electrolyte",
        label = "Immunity & Stress Booster",
        query = "Best electrolyte, vitamin C and immunity supplement for kids or heat stress",
        categoryFilter = "Supplement"
    )
)

