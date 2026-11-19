package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.GoatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Dialog component for entering a new goat's name, birth date, breed, and health status,
 * saving the record into the Room database through [GoatViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddGoatDialog(
    goatViewModel: GoatViewModel,
    onDismiss: () -> Unit,
    onSaved: ((Long) -> Unit)? = null
) {
    val isSaving by goatViewModel.isOperationInProgress.collectAsStateWithLifecycle()
    val vmError by goatViewModel.errorMessage.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var healthStatus by remember { mutableStateOf("Healthy") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val quickBreeds = listOf("Boer", "Saanen", "Nubian", "Alpine", "Black Bengal")
    val quickHealthStatuses = listOf("Healthy", "Needs Checkup", "Under Treatment", "Pregnant", "Recovering")

    Dialog(
        onDismissRequest = {
            if (!isSaving) {
                goatViewModel.clearError()
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("add_goat_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = "New Goat",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Add Goat Record",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Save goat details to local Room database",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Form Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationError = null
                            if (vmError != null) goatViewModel.clearError()
                        },
                        label = { Text("Goat Name *") },
                        placeholder = { Text("e.g. Bella") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Pets, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_goat_name_input")
                    )

                    // 2. Birth Date
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = {
                            birthDate = it
                            validationError = null
                        },
                        label = { Text("Birth Date *") },
                        placeholder = { Text("YYYY-MM-DD (e.g. 2024-03-15)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.testTag("add_goat_date_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Pick Birth Date",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_goat_birth_date_input")
                    )

                    // 3. Breed
                    OutlinedTextField(
                        value = breed,
                        onValueChange = {
                            breed = it
                            validationError = null
                        },
                        label = { Text("Breed *") },
                        placeholder = { Text("e.g. Boer, Saanen, Nubian") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Category, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_goat_breed_input")
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        quickBreeds.forEach { preset ->
                            FilterChip(
                                selected = breed.equals(preset, ignoreCase = true),
                                onClick = {
                                    breed = preset
                                    validationError = null
                                },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (breed.equals(preset, ignoreCase = true)) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    // 4. Health Status
                    OutlinedTextField(
                        value = healthStatus,
                        onValueChange = {
                            healthStatus = it
                            validationError = null
                        },
                        label = { Text("Health Status *") },
                        placeholder = { Text("e.g. Healthy, Needs Checkup") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.MedicalServices, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_goat_health_status_input")
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        quickHealthStatuses.forEach { preset ->
                            val isSelected = healthStatus.equals(preset, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    healthStatus = preset
                                    validationError = null
                                },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }

                val activeError = validationError ?: vmError
                if (!activeError.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .testTag("add_goat_error_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activeError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            goatViewModel.clearError()
                            onDismiss()
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("add_goat_cancel_button")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            when {
                                name.isBlank() -> validationError = "Please enter the goat's name."
                                birthDate.isBlank() -> validationError = "Please enter or pick a birth date."
                                breed.isBlank() -> validationError = "Please enter the goat's breed."
                                healthStatus.isBlank() -> validationError = "Please enter the goat's health status."
                                else -> {
                                    validationError = null
                                    goatViewModel.addGoat(
                                        name = name,
                                        birthDate = birthDate,
                                        breed = breed,
                                        healthStatus = healthStatus
                                    ) { result ->
                                        result.onSuccess { rowId ->
                                            onSaved?.invoke(rowId)
                                            onDismiss()
                                        }.onFailure { err ->
                                            validationError = err.message ?: "Failed to save goat."
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("add_goat_save_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Save Goat", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            birthDate = sdf.format(Date(millis))
                            validationError = null
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
