package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.COMMON_GOAT_SYMPTOMS
import com.example.data.model.DiseaseSuggestion
import com.example.data.model.Goat
import com.example.data.model.HealthRecord
import com.example.ui.components.MedicineTonicFinderTab
import com.example.ui.viewmodel.AiAdvisorUiState
import com.example.ui.viewmodel.AiHealthAdvisorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen providing AI-powered suggestions for goat diseases, medicine/tonic/supplement search
 * with weight/photo evaluation, preventative health, nutrition, and veterinary advice.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AiHealthAdvisorScreen(
    aiViewModel: AiHealthAdvisorViewModel,
    goats: List<Goat>,
    initialGoatTag: String? = null,
    initialTab: Int = 0,
    onLogTreatmentToHealth: (HealthRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 4)) }
    val uiState by aiViewModel.uiState.collectAsStateWithLifecycle()

    val categories = listOf(
        "Disease Diagnosis" to Icons.Default.MedicalServices,
        "Medicine & Tonics" to Icons.Default.Medication,
        "Preventative Health" to Icons.Default.Healing,
        "Feed & Minerals" to Icons.Default.Pets,
        "Ask Farm Vet" to Icons.Default.AutoAwesome
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Category Header Navigation Tabs
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedCategoryTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 8.dp,
            modifier = Modifier.testTag("ai_advisor_tabs")
        ) {
            categories.forEachIndexed { index, pair ->
                Tab(
                    selected = selectedCategoryTab == index,
                    onClick = {
                        selectedCategoryTab = index
                        aiViewModel.resetState()
                    },
                    text = { Text(pair.first, fontSize = 12.sp, fontWeight = if (selectedCategoryTab == index) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(pair.second, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("ai_tab_$index")
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            when (selectedCategoryTab) {
                0 -> DiseaseDiagnosisTab(
                    goats = goats,
                    initialGoatTag = initialGoatTag,
                    uiState = uiState,
                    onDiagnose = { tag, breed, age, symptoms, notes ->
                        aiViewModel.diagnoseDisease(tag, breed, age, symptoms, notes)
                    },
                    onReset = { aiViewModel.resetState() },
                    onLogTreatment = onLogTreatmentToHealth
                )
                1 -> MedicineTonicFinderTab(
                    goats = goats,
                    initialGoatTag = initialGoatTag,
                    uiState = uiState,
                    onSearchAndEvaluate = { query, category, tag, breed, age, weight, notes, bitmap, subject ->
                        aiViewModel.searchMedicineTonicAndEvaluateHealth(
                            searchQuery = query,
                            categoryFilter = category,
                            goatTag = tag,
                            breed = breed,
                            ageGroup = age,
                            weightKg = weight,
                            healthNotes = notes,
                            attachedBitmap = bitmap,
                            photoSubject = subject
                        )
                    },
                    onReset = { aiViewModel.resetState() },
                    onLogMedicine = onLogTreatmentToHealth,
                    modifier = Modifier.fillMaxSize()
                )
                2 -> PreventativeHealthTab(
                    uiState = uiState,
                    onAskTopic = { title, prompt ->
                        aiViewModel.askAdvisorStream(title, prompt)
                    },
                    onReset = { aiViewModel.resetState() }
                )
                3 -> NutritionAndCareTab(
                    uiState = uiState,
                    onAskTopic = { title, prompt ->
                        aiViewModel.askAdvisorStream(title, prompt)
                    },
                    onReset = { aiViewModel.resetState() }
                )
                4 -> AskFarmVetTab(
                    uiState = uiState,
                    onSendQuery = { query ->
                        aiViewModel.askAdvisorStream("AI Farm Advisor Answer", query)
                    },
                    onReset = { aiViewModel.resetState() }
                )
            }
        }
    }
}

/**
 * Tab 1: Comprehensive Symptom-to-Disease AI Diagnosis
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DiseaseDiagnosisTab(
    goats: List<Goat>,
    initialGoatTag: String?,
    uiState: AiAdvisorUiState,
    onDiagnose: (String, String, String, List<String>, String) -> Unit,
    onReset: () -> Unit,
    onLogTreatment: (HealthRecord) -> Unit
) {
    var selectedGoatTag by remember { mutableStateOf(initialGoatTag ?: (goats.firstOrNull()?.tag ?: "")) }
    var selectedBreed by remember { mutableStateOf(goats.firstOrNull { it.tag == selectedGoatTag }?.breed ?: "") }
    var selectedAgeGroup by remember { mutableStateOf("Adult Doe") }
    val selectedSymptoms = remember { mutableStateListOf<String>() }
    var clinicalNotes by remember { mutableStateOf("") }

    val ageGroups = listOf("Kid (< 3 months)", "Weanling (3-6 mo)", "Yearling", "Adult Doe", "Pregnant Doe", "Lactating Doe", "Breeding Buck")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Diagnosis Results View if Available
        if (uiState is AiAdvisorUiState.StructuredSuccess) {
            val diag = uiState.diagnosis
            DiseaseDiagnosisCard(
                diagnosis = diag,
                goatTag = selectedGoatTag,
                onReset = onReset,
                onLogTreatment = { treatmentTitle ->
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val record = HealthRecord(
                        goatTag = selectedGoatTag.ifBlank { "Herd" },
                        recordType = "Checkup/Medication",
                        treatment = treatmentTitle.ifBlank { diag.conditionName },
                        administeredDate = today,
                        dosage = "As advised by Vet/AI protocol",
                        notes = "AI Diagnosis: ${diag.conditionName} (${diag.urgency}). Treatment protocol: ${diag.treatmentsAndMedications.firstOrNull() ?: diag.summary}",
                        cost = 0.0
                    )
                    onLogTreatment(record)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState is AiAdvisorUiState.Loading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Text(
                        text = "Analyzing goat symptoms with AI...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Checking clinical veterinary database for conditions, urgency, and medications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (uiState is AiAdvisorUiState.Error) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Advisory Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(uiState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Triage Input Form Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("disease_triage_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Disease & Health Triage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select observed symptoms to get instant veterinary guidance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Goat Identification Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedGoatTag,
                        onValueChange = { tag ->
                            selectedGoatTag = tag
                            val matched = goats.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
                            if (matched != null) {
                                selectedBreed = matched.breed
                            }
                        },
                        label = { Text("Goat Tag / ID") },
                        placeholder = { Text("e.g. GT-101") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_ai_goat_tag"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = selectedBreed,
                        onValueChange = { selectedBreed = it },
                        label = { Text("Breed") },
                        placeholder = { Text("e.g. Boer / Nubian") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_ai_breed"),
                        singleLine = true
                    )
                }

                // Age / Life Stage Selector
                Text(
                    text = "Age / Life Stage",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ageGroups.forEach { age ->
                        val isSelected = selectedAgeGroup == age
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAgeGroup = age },
                            label = { Text(age, fontSize = 11.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Symptom Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Observed Symptoms (${selectedSymptoms.size} selected)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (selectedSymptoms.isNotEmpty()) {
                        Text(
                            text = "Clear all",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { selectedSymptoms.clear() }
                                .padding(4.dp)
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    COMMON_GOAT_SYMPTOMS.forEach { sym ->
                        val isSelected = selectedSymptoms.contains(sym.label)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedSymptoms.remove(sym.label)
                                else selectedSymptoms.add(sym.label)
                            },
                            label = { Text(sym.label, fontSize = 11.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.testTag("symptom_${sym.id}")
                        )
                    }
                }

                // Free-form Clinical Notes
                OutlinedTextField(
                    value = clinicalNotes,
                    onValueChange = { clinicalNotes = it },
                    label = { Text("Additional Clinical Observations / History") },
                    placeholder = { Text("e.g. Temperature 104.2 F, started yesterday after heavy grazing, rumen sounds absent...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ai_clinical_notes"),
                    maxLines = 3
                )

                // Submit Action Button
                Button(
                    onClick = {
                        onDiagnose(
                            selectedGoatTag,
                            selectedBreed,
                            selectedAgeGroup,
                            selectedSymptoms.toList(),
                            clinicalNotes
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_analyze_symptoms"),
                    enabled = (selectedSymptoms.isNotEmpty() || clinicalNotes.isNotBlank()) && uiState !is AiAdvisorUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze Symptoms with AI", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Diagnosis Presentation Card displaying clinical results, urgency, first aid, treatments,
 * and a 1-click action to log the treatment directly to Firestore!
 */
@Composable
private fun DiseaseDiagnosisCard(
    diagnosis: DiseaseSuggestion,
    goatTag: String,
    onReset: () -> Unit,
    onLogTreatment: (String) -> Unit
) {
    val urgencyColor = when {
        diagnosis.urgency.contains("Critical", ignoreCase = true) || diagnosis.urgency.contains("Emergency", ignoreCase = true) -> Color(0xFFD32F2F)
        diagnosis.urgency.contains("Moderate", ignoreCase = true) -> Color(0xFFF57C00)
        else -> Color(0xFF388E3C)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_diagnosis_result_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Disease Name and Urgency Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Clinical Assessment",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = diagnosis.conditionName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = urgencyColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = diagnosis.urgency,
                        color = urgencyColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Summary
            Text(
                text = diagnosis.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Immediate Actions / First Aid
            if (diagnosis.immediateActions.isNotEmpty()) {
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Immediate First-Aid / Isolation:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE65100))
                        }
                        diagnosis.immediateActions.forEach { act ->
                            Row(modifier = Modifier.padding(top = 2.dp)) {
                                Text("• ", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                                Text(act, fontSize = 12.sp, color = Color(0xFF5D4037))
                            }
                        }
                    }
                }
            }

            // Treatments & Medications
            if (diagnosis.treatmentsAndMedications.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recommended Treatments & Supportive Care:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        diagnosis.treatmentsAndMedications.forEach { tr ->
                            Row(modifier = Modifier.padding(top = 2.dp)) {
                                Text("✓ ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(tr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // Prevention & Biosecurity
            if (diagnosis.preventionAndBiosecurity.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Prevention & Herd Biosecurity:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        diagnosis.preventionAndBiosecurity.forEach { prev ->
                            Row(modifier = Modifier.padding(top = 2.dp)) {
                                Text("→ ", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                Text(prev, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Withdrawal & Notes
            if (diagnosis.notesAndWithdrawal.isNotBlank()) {
                Text(
                    text = "ℹ️ ${diagnosis.notesAndWithdrawal}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Veterinary Disclaimer
            Text(
                text = "⚠️ Disclaimer: ${diagnosis.disclaimer}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline
            )

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("New Triage")
                }

                Button(
                    onClick = {
                        val firstMed = diagnosis.treatmentsAndMedications.firstOrNull() ?: diagnosis.conditionName
                        onLogTreatment(firstMed)
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("btn_log_ai_treatment"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Treatment Record", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Tab 2: Preventative Health, Vaccines & Deworming Protocols
 */
@Composable
private fun PreventativeHealthTab(
    uiState: AiAdvisorUiState,
    onAskTopic: (String, String) -> Unit,
    onReset: () -> Unit
) {
    val quickTopics = listOf(
        "💉 CD&T Vaccination Protocol" to "Explain the complete CD&T vaccination schedule for adult goats, pregnant does (weeks before kidding), and growing kids including dosages and injection sites.",
        "🐛 FAMACHA & Deworming Guide" to "Provide a practical FAMACHA scoring guide for Barber's Pole worm, dewormer class rotation (benzimidazoles, macrocyclic lactones, imidazothiazoles), and fecal egg count thresholds.",
        "🦶 Hoof Rot vs Scald & Care" to "Explain how to diagnose, treat, and prevent foot rot vs foot scald in goat herds, including zinc sulfate / copper sulfate foot baths and trimming technique.",
        "🥛 Mastitis Prevention in Does" to "Provide best practices for mastitis prevention in lactating dairy and meat does, teat dipping protocols, and how to identify subclinical mastitis."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AdvisorResponseSection(uiState = uiState, onReset = onReset)

        Text(
            text = "Preventative Health & Vaccine Guides",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tap any topic for detailed veterinary-backed protocols tailored for goat herds:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        quickTopics.forEach { (title, prompt) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAskTopic(title, prompt) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = prompt.take(90) + "...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Ask AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tab 3: Nutrition, Minerals & Breeding Management
 */
@Composable
private fun NutritionAndCareTab(
    uiState: AiAdvisorUiState,
    onAskTopic: (String, String) -> Unit,
    onReset: () -> Unit
) {
    val quickTopics = listOf(
        "🌾 Balanced Feed Rations" to "What is the recommended daily forage-to-concentrate ratio and protein requirement for lactating dairy does vs dry meat goats?",
        "🧪 Copper & Mineral Deficiencies" to "Detail signs of copper deficiency (fish tail hair, faded black coat) and copper toxicity in goats, plus recommended loose mineral feeding.",
        "🍼 Kidding Emergencies & Dystocia" to "Provide a step-by-step emergency troubleshooting guide for abnormal kidding presentations (breech, head turned back) and newborn resuscitation.",
        "⚡ Pregnancy Toxemia & Ketosis" to "Explain how to identify, treat (propylene glycol), and prevent pregnancy toxemia / ketosis in late-gestation does carrying multiple kids."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AdvisorResponseSection(uiState = uiState, onReset = onReset)

        Text(
            text = "Nutrition, Minerals & Breeding Care",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Select a management area to receive expert caprine recommendations:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        quickTopics.forEach { (title, prompt) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAskTopic(title, prompt) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = prompt.take(90) + "...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = "Ask AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tab 4: Conversational Streaming Farm Vet Assistant
 */
@Composable
private fun AskFarmVetTab(
    uiState: AiAdvisorUiState,
    onSendQuery: (String) -> Unit,
    onReset: () -> Unit
) {
    var queryText by remember { mutableStateOf("") }

    val presetQuestions = listOf(
        "How to treat bloat in goats with baking soda and oil?",
        "What causes sudden milk drop in a 3-year-old doe?",
        "Safe dewormers during early pregnancy in goats",
        "How to treat newborn kid watery yellow scours?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Query Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ask Khulana Farm AI Advisor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    label = { Text("Ask any disease, health, or goat care question") },
                    placeholder = { Text("e.g. My goat ate wilted cherry leaves, is it toxic?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ask_vet_query"),
                    maxLines = 4
                )

                Button(
                    onClick = {
                        if (queryText.isNotBlank()) {
                            onSendQuery(queryText)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_send_vet_query"),
                    enabled = queryText.isNotBlank() && uiState !is AiAdvisorUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ask AI Advisor", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Suggestion Chips
        Text(
            text = "Suggested Inquiries:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            presetQuestions.forEach { question ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            queryText = question
                            onSendQuery(question)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(question, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        AdvisorResponseSection(uiState = uiState, onReset = onReset)
    }
}

/**
 * Reusable Card rendering streaming or finished Text response from Gemini AI.
 */
@Composable
private fun AdvisorResponseSection(
    uiState: AiAdvisorUiState,
    onReset: () -> Unit
) {
    when (uiState) {
        is AiAdvisorUiState.Loading -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Consulting AI Veterinary Knowledge Base...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        is AiAdvisorUiState.Streaming -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_streaming_response_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Generating Advisory Guidance...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.partialResponse,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        }
        is AiAdvisorUiState.TextSuccess -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_text_response_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = onReset, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Close", modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        text = uiState.content,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                    Text(
                        text = "⚠️ Advisory advice only. In emergencies, consult a local veterinarian.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        is AiAdvisorUiState.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Advisory Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(uiState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
        else -> Unit
    }
}
