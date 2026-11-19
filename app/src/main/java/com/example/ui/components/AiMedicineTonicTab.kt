package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.COMMON_MEDICINE_TONIC_PRESETS
import com.example.data.model.Goat
import com.example.data.model.HealthRecord
import com.example.data.model.MedicineTonicItem
import com.example.data.model.MedicineTonicSearchResult
import com.example.ui.viewmodel.AiAdvisorUiState
import com.example.util.AiMediaHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicineTonicFinderTab(
    goats: List<Goat>,
    initialGoatTag: String?,
    uiState: AiAdvisorUiState,
    onSearchAndEvaluate: (
        searchQuery: String,
        categoryFilter: String,
        goatTag: String,
        breed: String,
        ageGroup: String,
        weightKg: String,
        healthNotes: String,
        attachedBitmap: Bitmap?,
        photoSubject: String
    ) -> Unit,
    onReset: () -> Unit,
    onLogMedicine: (HealthRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initialGoat = remember(initialGoatTag, goats) {
        goats.firstOrNull { it.tag.equals(initialGoatTag, ignoreCase = true) } ?: goats.firstOrNull()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedGoatTag by remember { mutableStateOf(initialGoat?.tag ?: "") }
    var selectedBreed by remember { mutableStateOf(initialGoat?.breed ?: "") }
    var weightKgText by remember {
        mutableStateOf(
            if (initialGoat != null && initialGoat.weightKg > 0.0) initialGoat.weightKg.toString() else ""
        )
    }
    var selectedAgeGroup by remember {
        mutableStateOf(
            when {
                initialGoat?.status?.contains("Lactating", true) == true -> "Lactating Doe"
                initialGoat?.status?.contains("Pregnant", true) == true -> "Pregnant Doe"
                initialGoat?.gender?.equals("Buck", true) == true -> "Breeding Buck"
                else -> "Adult Doe"
            }
        )
    }
    var healthAndOtherNotes by remember { mutableStateOf(initialGoat?.notes ?: "") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoSubject by remember { mutableStateOf("Goat Body / Health") }
    var photoError by remember { mutableStateOf<String?>(null) }

    val photoLoadErrorMsg = stringResource(R.string.error_ai_photo_load)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val decoded = AiMediaHelper.decodeUriToBitmap(context, uri)
            if (decoded != null) {
                attachedBitmap = decoded
                photoError = null
            } else {
                photoError = photoLoadErrorMsg
            }
        }
    }

    val categories = listOf("All", "Medicine", "Tonic", "Supplement")
    val ageGroups = listOf(
        "Kid (< 3 mo)",
        "Growing (3-9 mo)",
        "Adult Doe",
        "Pregnant Doe",
        "Lactating Doe",
        "Breeding Buck"
    )
    val photoSubjects = listOf("Goat Body / Health", "Medicine / Tonic Label")

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Result Card
        if (uiState is AiAdvisorUiState.MedicineSearchSuccess) {
            MedicineTonicResultCard(
                result = uiState.result,
                goatTag = selectedGoatTag,
                onReset = onReset,
                onLogItem = { item ->
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val recordType = when {
                        item.category.equals("Medicine", ignoreCase = true) -> "Medication"
                        item.category.equals("Tonic", ignoreCase = true) -> "Tonic / Booster"
                        else -> "Supplement"
                    }
                    onLogMedicine(
                        HealthRecord(
                            goatTag = selectedGoatTag.ifBlank { "Herd" },
                            recordType = recordType,
                            treatment = item.name,
                            administeredDate = today,
                            dosage = item.dosageForWeight.ifBlank { item.administrationRoute },
                            notes = "AI ${item.category} (${uiState.result.conditionVerdict}): ${item.purposeAndBenefits}. Safety: ${item.safetyAndWithdrawal}",
                            cost = 0.0
                        )
                    )
                }
            )
        }

        // Loading State
        if (uiState is AiAdvisorUiState.Loading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(30.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Searching best medicines, tonics & supplements...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Evaluating goat weight, health status, dosage safety, and alternatives.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Error State
        if (uiState is AiAdvisorUiState.Error) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medicine_search_error_banner"),
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
                        Text(
                            text = "AI Search Error",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    IconButton(onClick = onReset) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Clear Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Search & Quick Discovery Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("medicine_search_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.title_ai_medicine_finder),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.subtitle_ai_medicine_finder),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Medicine, Tonic or Supplement") },
                    placeholder = { Text("e.g., best liver tonic, weight gain powder, calcium booster...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_medicine_search_query"),
                    singleLine = true
                )

                // Category Focus Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val selected = selectedCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.testTag("medicine_filter_${cat.lowercase()}")
                        )
                    }
                }

                // Quick Search Presets
                Text(
                    text = "Quick Search Topics",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(COMMON_MEDICINE_TONIC_PRESETS) { preset ->
                        FilterChip(
                            selected = searchQuery == preset.query,
                            onClick = {
                                searchQuery = preset.query
                                selectedCategory = preset.categoryFilter
                            },
                            label = { Text(preset.label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("medicine_preset_${preset.id}")
                        )
                    }
                }
            }
        }

        // Weight, Health & Photo Upload Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("medicine_weight_health_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonitorWeight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload Weight, Health & Photo Check",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quick Select from Herd Goats
                if (goats.isNotEmpty()) {
                    Text(
                        text = "Fill from Herd Goat:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(goats) { goat ->
                            val isSelected = selectedGoatTag.equals(goat.tag, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedGoatTag = goat.tag
                                    selectedBreed = goat.breed
                                    if (goat.weightKg > 0.0) {
                                        weightKgText = goat.weightKg.toString()
                                    }
                                    selectedAgeGroup = when {
                                        goat.status.contains("Lactating", true) -> "Lactating Doe"
                                        goat.status.contains("Pregnant", true) -> "Pregnant Doe"
                                        goat.gender.equals("Buck", true) -> "Breeding Buck"
                                        else -> "Adult Doe"
                                    }
                                    if (goat.notes.isNotBlank() && healthAndOtherNotes.isBlank()) {
                                        healthAndOtherNotes = goat.notes
                                    }
                                },
                                label = {
                                    val w = if (goat.weightKg > 0.0) " (${goat.weightKg}kg)" else ""
                                    Text("${goat.name.ifBlank { goat.tag }}$w", fontSize = 11.sp)
                                }
                            )
                        }
                    }
                }

                // Weight & Tag Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weightKgText,
                        onValueChange = { weightKgText = it },
                        label = { Text("Weight (kg)") },
                        placeholder = { Text("e.g. 18.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_medicine_weight_kg"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = selectedGoatTag,
                        onValueChange = { selectedGoatTag = it },
                        label = { Text("Goat Tag / Name") },
                        placeholder = { Text("e.g. GT-101") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_medicine_goat_tag"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = selectedBreed,
                    onValueChange = { selectedBreed = it },
                    label = { Text("Breed") },
                    placeholder = { Text("e.g. Black Bengal, Boer, Jamunapari") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_medicine_breed"),
                    singleLine = true
                )

                // Age / Stage Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ageGroups.forEach { stage ->
                        FilterChip(
                            selected = selectedAgeGroup == stage,
                            onClick = { selectedAgeGroup = stage },
                            label = { Text(stage, fontSize = 11.sp) }
                        )
                    }
                }

                // Health, Current Tonic & Other Notes
                OutlinedTextField(
                    value = healthAndOtherNotes,
                    onValueChange = { healthAndOtherNotes = it },
                    label = { Text("Health Status, Current Medicine / Tonic & Other Info") },
                    placeholder = {
                        Text("e.g. Goat weighs only 14kg at 1 yr, weak appetite, rough coat. Currently using X tonic — is it good?")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_medicine_health_notes"),
                    minLines = 2,
                    maxLines = 4
                )

                // Zero-Permission Photo Picker Upload for Goat / Medicine Label
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Optional Photo Check",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            FilledTonalButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("btn_upload_medicine_photo"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (attachedBitmap == null) "Upload Photo" else "Change Photo",
                                    fontSize = 12.sp
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            photoSubjects.forEach { subject ->
                                FilterChip(
                                    selected = photoSubject == subject,
                                    onClick = { photoSubject = subject },
                                    label = { Text(subject, fontSize = 11.sp) },
                                    modifier = Modifier.testTag(
                                        if (subject.startsWith("Goat")) "photo_subject_goat" else "photo_subject_label"
                                    )
                                )
                            }
                        }

                        if (photoError != null) {
                            Text(
                                text = photoError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (attachedBitmap != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    bitmap = attachedBitmap!!.asImageBitmap(),
                                    contentDescription = "Uploaded photo for AI health evaluation",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .testTag("medicine_photo_preview")
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Photo attached ($photoSubject)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "AI will inspect this photo along with weight & health info.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { attachedBitmap = null },
                                    modifier = Modifier.testTag("btn_remove_medicine_photo")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove uploaded photo",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                val canSubmit = (searchQuery.isNotBlank() ||
                    weightKgText.isNotBlank() ||
                    healthAndOtherNotes.isNotBlank() ||
                    attachedBitmap != null) && uiState !is AiAdvisorUiState.Loading

                Button(
                    onClick = {
                        onSearchAndEvaluate(
                            searchQuery,
                            selectedCategory,
                            selectedGoatTag,
                            selectedBreed,
                            selectedAgeGroup,
                            weightKgText,
                            healthAndOtherNotes,
                            attachedBitmap,
                            photoSubject
                        )
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_search_medicine_tonic")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search & Evaluate with AI", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MedicineTonicResultCard(
    result: MedicineTonicSearchResult,
    goatTag: String,
    onReset: () -> Unit,
    onLogItem: (MedicineTonicItem) -> Unit
) {
    val statusContainerColor = if (result.isStatusGood) {
        Color(0xFFE8F5E9)
    } else {
        Color(0xFFFFEBEE)
    }
    val statusContentColor = if (result.isStatusGood) {
        Color(0xFF1B5E20)
    } else {
        Color(0xFFB71C1C)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_medicine_result_card"),
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
            // Verdict Banner (Good vs Found Not Good)
            Surface(
                color = statusContainerColor,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medicine_verdict_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (result.isStatusGood) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = statusContentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.conditionVerdict,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusContentColor
                        )
                        if (result.assessmentSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.assessmentSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = statusContentColor.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }

            // Weight Evaluation Box
            if (result.weightEvaluation.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Weight & Body Evaluation${if (goatTag.isNotBlank()) " ($goatTag)" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = result.weightEvaluation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Better Alternatives if Found Not Good
            if (!result.isStatusGood || result.betterAlternativesIfNotGood.isNotEmpty()) {
                if (result.betterAlternativesIfNotGood.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medicine_not_good_alternatives_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TipsAndUpdates,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (!result.isStatusGood) {
                                        "Found Not Good — Recommended Better Actions:"
                                    } else {
                                        "Alternative Options & Upgrades:"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                            result.betterAlternativesIfNotGood.forEach { alt ->
                                Text(
                                    text = "• $alt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF5D4037)
                                )
                            }
                        }
                    }
                }
            }

            // Ranked Medicines, Tonics & Supplements
            if (result.recommendations.isNotEmpty()) {
                Text(
                    text = "Best Suggested Medicines, Tonics & Supplements",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                result.recommendations.forEachIndexed { index, item ->
                    val badgeColors = when {
                        item.category.equals("Medicine", ignoreCase = true) ->
                            Color(0xFFE3F2FD) to Color(0xFF0D47A1)
                        item.category.equals("Tonic", ignoreCase = true) ->
                            Color(0xFFE8F5E9) to Color(0xFF1B5E20)
                        else ->
                            Color(0xFFFFF8E1) to Color(0xFFE65100)
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medicine_recommendation_item_$index")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (item.activeIngredients.isNotBlank()) {
                                        Text(
                                            text = "Active: ${item.activeIngredients}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    color = badgeColors.first,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = item.category,
                                        color = badgeColors.second,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (item.purposeAndBenefits.isNotBlank()) {
                                Text(
                                    text = item.purposeAndBenefits,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (item.dosageForWeight.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Weight Dosage: ${item.dosageForWeight}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (item.administrationRoute.isNotBlank()) {
                                Text(
                                    text = "Route: ${item.administrationRoute}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (item.safetyAndWithdrawal.isNotBlank()) {
                                Text(
                                    text = "Safety: ${item.safetyAndWithdrawal}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { onLogItem(item) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_log_medicine_item_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Log to Health Records", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Feeding & Care Tips
            if (result.feedingAndCareTips.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Nutrition & Support Tips:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    result.feedingAndCareTips.forEach { tip ->
                        Text(
                            text = "✓ $tip",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "⚠️ ${result.disclaimer}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReset) {
                    Text("New Search / Check")
                }
            }
        }
    }
}
