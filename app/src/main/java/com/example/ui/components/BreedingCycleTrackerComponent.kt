package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BreedingRecord
import com.example.data.model.Goat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Card component to visualize and track an active or completed goat breeding cycle.
 * Displays dam information, sire details, gestation progress bar (0..150 days),
 * expected delivery date, and trimester status.
 */
@Composable
fun BreedingCycleCard(
    record: BreedingRecord,
    onMarkKidded: () -> Unit,
    onMarkConfirmed: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysRemaining = record.daysUntilDelivery()
    val daysElapsed = record.gestationDaysElapsed() ?: 0L
    val progress = record.gestationProgressPercentage()
    val trimester = record.gestationTrimester()

    val isDelivered = record.status.equals("Kidded", ignoreCase = true)
    val isConfirmed = record.status.equals("Confirmed Pregnant", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("breeding_cycle_card_${record.id.ifBlank { record.doeTag }}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Doe (Dam) & Sire (Buck) Badges + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFCE4EC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Breeding Cycle",
                            tint = Color(0xFFC2185B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Female, contentDescription = "Dam", tint = Color(0xFFE91E63), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Doe: ${record.doeTag}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Male, contentDescription = "Sire", tint = Color(0xFF1976D2), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Sire: ${record.buckTag}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Status chip
                Surface(
                    color = when {
                        isDelivered -> Color(0xFFE8F5E9)
                        isConfirmed -> Color(0xFFFFF3E0)
                        record.status == "Failed" -> Color(0xFFFFEBEE)
                        else -> MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = record.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isDelivered -> Color(0xFF1B5E20)
                            isConfirmed -> Color(0xFFE65100)
                            record.status == "Failed" -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            // Sire Information Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "SIRE & SERVICE INFORMATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sire (Buck): ${record.buckTag}${if (record.notes.isNotBlank()) " • ${record.notes}" else ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Gestation Progress Bar (unless delivered or failed)
            if (!isDelivered && record.status != "Failed") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gestation: Day $daysElapsed of 150 (${(progress * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = trimester,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            daysRemaining != null && daysRemaining <= 7 -> Color(0xFFE65100)
                            isConfirmed -> MaterialTheme.colorScheme.primary
                            else -> Color(0xFF8E24AA)
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Date Schedule & Countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mated: ${record.breedingDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Expected Kidding: ${record.expectedKiddingDate}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Delivery Countdown Chip
                if (!isDelivered && daysRemaining != null) {
                    Surface(
                        color = when {
                            daysRemaining == 0L -> Color(0xFFFFCCBC)
                            daysRemaining < 0L -> Color(0xFFFFCDD2)
                            daysRemaining <= 14L -> Color(0xFFFFF3E0)
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when {
                                daysRemaining == 0L -> "🍼 DUE TODAY"
                                daysRemaining < 0L -> "⚠️ ${-daysRemaining}d Overdue"
                                else -> "🍼 In $daysRemaining days"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                daysRemaining == 0L -> Color(0xFFD84315)
                                daysRemaining < 0L -> Color(0xFFC62828)
                                daysRemaining <= 14L -> Color(0xFFE65100)
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                } else if (isDelivered && record.kidsBorn > 0) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "🎉 ${record.kidsBorn} Kids Born",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (record.status == "Bred") {
                    OutlinedButton(
                        onClick = onMarkConfirmed,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirm Pregnancy", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (!isDelivered) {
                    Button(
                        onClick = onMarkKidded,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Log Kidding", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Record",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dialog component to create and record a new goat breeding cycle with complete sire information
 * and auto-calculated expected kidding date (150 days).
 */
@Composable
fun BreedingCycleFormDialog(
    does: List<Goat>,
    bucks: List<Goat>,
    onDismiss: () -> Unit,
    onSave: (BreedingRecord) -> Unit
) {
    var selectedDoeTag by remember { mutableStateOf(if (does.isNotEmpty()) does.first().tag else "") }
    var selectedBuckTag by remember { mutableStateOf(if (bucks.isNotEmpty()) bucks.first().tag else "") }
    var sireName by remember { mutableStateOf("") }
    var sireBreed by remember {
        mutableStateOf(bucks.firstOrNull()?.breed ?: "")
    }
    var sireRegistration by remember { mutableStateOf("") }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var matingDate by remember { mutableStateOf(todayStr) }

    fun calculate150DaysDelivery(dateString: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance().apply { time = sdf.parse(dateString) ?: Date() }
            cal.add(Calendar.DAY_OF_YEAR, 150)
            sdf.format(cal.time)
        } catch (_: Exception) {
            ""
        }
    }

    var expectedDeliveryDate by remember { mutableStateOf(calculate150DaysDelivery(todayStr)) }
    var cycleStage by remember { mutableStateOf("Bred") }
    var breedingNotes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Breeding Cycle", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // 1. Dam (Doe) Selection
                Text("Dam (Doe / Mother) *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (does.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(does) { doe ->
                            FilterChip(
                                selected = selectedDoeTag == doe.tag,
                                onClick = { selectedDoeTag = doe.tag },
                                label = { Text("${doe.name} (${doe.tag})", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = selectedDoeTag,
                    onValueChange = { selectedDoeTag = it },
                    label = { Text("Doe Tag ID *") },
                    placeholder = { Text("e.g. DOE-001") },
                    modifier = Modifier.fillMaxWidth().testTag("breeding_cycle_input_doe_tag"),
                    singleLine = true
                )

                // 2. Sire (Buck) Information
                Text("Sire (Buck / Father Information) *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (bucks.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(bucks) { buck ->
                            FilterChip(
                                selected = selectedBuckTag == buck.tag,
                                onClick = {
                                    selectedBuckTag = buck.tag
                                    sireName = buck.name
                                    sireBreed = buck.breed
                                },
                                label = { Text("${buck.name} (${buck.tag})", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = selectedBuckTag,
                    onValueChange = { selectedBuckTag = it },
                    label = { Text("Sire Tag / ID *") },
                    placeholder = { Text("e.g. BUCK-001") },
                    modifier = Modifier.fillMaxWidth().testTag("breeding_cycle_input_sire_tag"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sireName,
                        onValueChange = { sireName = it },
                        label = { Text("Sire Name") },
                        placeholder = { Text("e.g. Thor") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sireBreed,
                        onValueChange = { sireBreed = it },
                        label = { Text("Sire Breed") },
                        placeholder = { Text("e.g. Boer") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = sireRegistration,
                    onValueChange = { sireRegistration = it },
                    label = { Text("Sire Pedigree / Reg #") },
                    placeholder = { Text("e.g. ABGA #89214") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 3. Mating Date & Expected Kidding Date
                Text("Breeding Timeline (150-Day Gestation)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = matingDate,
                    onValueChange = { newDate ->
                        matingDate = newDate
                        expectedDeliveryDate = calculate150DaysDelivery(newDate)
                    },
                    label = { Text("Mating Date (YYYY-MM-DD) *") },
                    modifier = Modifier.fillMaxWidth().testTag("breeding_cycle_input_mating_date"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = expectedDeliveryDate,
                    onValueChange = { expectedDeliveryDate = it },
                    label = { Text("Expected Kidding Date (~150 Days) *") },
                    supportingText = { Text("Calculated automatically: 150 days average gestation") },
                    modifier = Modifier.fillMaxWidth().testTag("breeding_cycle_input_kidding_date"),
                    singleLine = true
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Automatic kidding notification will trigger for $expectedDeliveryDate.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // 4. Notes & Observations
                OutlinedTextField(
                    value = breedingNotes,
                    onValueChange = { breedingNotes = it },
                    label = { Text("Breeding Notes / Observations") },
                    placeholder = { Text("e.g. Natural service, ultrasound scheduled in 45 days") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedDoeTag.isBlank() || selectedBuckTag.isBlank()) {
                        errorMessage = "Please enter both Doe and Sire tags."
                        return@Button
                    }
                    if (matingDate.isBlank()) {
                        errorMessage = "Please enter the mating date."
                        return@Button
                    }

                    // Format full sire description into notes if present
                    val combinedNotes = buildString {
                        if (sireName.isNotBlank() || sireBreed.isNotBlank() || sireRegistration.isNotBlank()) {
                            append("Sire: ${sireName.ifBlank { selectedBuckTag }}")
                            if (sireBreed.isNotBlank()) append(" ($sireBreed)")
                            if (sireRegistration.isNotBlank()) append(" [Reg: $sireRegistration]")
                            if (breedingNotes.isNotBlank()) append(" • ")
                        }
                        if (breedingNotes.isNotBlank()) append(breedingNotes.trim())
                    }

                    onSave(
                        BreedingRecord(
                            doeTag = selectedDoeTag.trim(),
                            buckTag = selectedBuckTag.trim(),
                            breedingDate = matingDate.trim(),
                            expectedKiddingDate = expectedDeliveryDate.trim(),
                            status = cycleStage,
                            notes = combinedNotes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_breeding_cycle_button")
            ) {
                Text("Save to Farm Database")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
