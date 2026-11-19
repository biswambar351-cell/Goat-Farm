package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Goat
import com.example.data.model.HealthLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Detailed view for a specific [Goat] entity.
 * Displays comprehensive goat demographics, pedigree lineage, health events ([HealthLog] entries),
 * and provides options to Edit and Delete the goat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoatDetailScreen(
    goat: Goat,
    healthLogs: List<HealthLog>,
    onBack: () -> Unit,
    onEditGoat: () -> Unit,
    onDeleteGoat: () -> Unit,
    onAddHealthLog: (HealthLog) -> Unit,
    onDeleteHealthLog: (String) -> Unit = {},
    milkLogs: List<com.example.data.model.MilkProduction> = emptyList(),
    onAddMilkProduction: (com.example.data.model.MilkProduction) -> Unit = {},
    breedingRecords: List<com.example.data.model.BreedingRecord> = emptyList(),
    onAddBreedingRecord: (com.example.data.model.BreedingRecord) -> Unit = {},
    onDeleteBreedingRecord: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddLogDialog by remember { mutableStateOf(false) }
    var showAddMilkDialog by remember { mutableStateOf(false) }
    var showAddBreedingDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Goat Profile: ${goat.tag}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditGoat, modifier = Modifier.testTag("detail_edit_goat_button")) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Goat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("detail_delete_goat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Goat",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
            // 1. Hero Identity Banner
            item {
                GoatHeroCard(goat = goat, onEdit = onEditGoat)
            }

            // 2. Demographics & Traits Grid
            item {
                GoatDemographicsCard(goat = goat)
            }

            // 3. Pedigree Lineage Card
            item {
                GoatPedigreeCard(goat = goat)
            }

            // 4. Notes Card (if present)
            if (goat.notes.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Care & Pedigree Notes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = goat.notes,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // 5. Breeding & Gestation History Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Breeding & Gestation",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val activeCount = breedingRecords.count { it.status == "Confirmed Pregnant" || it.status == "Bred" }
                        Text(
                            text = if (activeCount > 0) "$activeCount active gestation(s) / ${breedingRecords.size} total" else "${breedingRecords.size} records logged",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showAddBreedingDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("add_breeding_record_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Breeding")
                    }
                }
            }

            if (breedingRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pets,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No breeding records for this goat yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Track mating dates, sire/dam IDs, and 150-day expected delivery countdown.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(breedingRecords, key = { it.id.ifBlank { "${it.breedingDate}-${it.buckTag}-${it.doeTag}" } }) { record ->
                    BreedingRecordCard(
                        record = record,
                        currentGoat = goat,
                        onDelete = { onDeleteBreedingRecord(record.id) }
                    )
                }
            }

            // 6. Milk Production History Section (for Does or lactating goats)
            val isDoe = goat.gender.equals("Doe", ignoreCase = true)
            if (isDoe || milkLogs.isNotEmpty()) {
                item {
                    val totalYield = milkLogs.sumOf { it.quantityLiters }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Milk Production Logs",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total Logged: %.1f L (%d records)".format(totalYield, milkLogs.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showAddMilkDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("add_milk_log_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Yield")
                        }
                    }
                }

                if (milkLogs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalDrink,
                                    contentDescription = null,
                                    tint = Color(0xFF0288D1),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No milking records for ${goat.name} yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap 'Log Yield' above to record daily milk volume.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(milkLogs.take(5), key = { it.id.ifBlank { it.date + it.quantityLiters } }) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE1F5FE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalDrink,
                                            contentDescription = null,
                                            tint = Color(0xFF0288D1),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Date: ${log.date}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Goat ID: ${log.goatId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFFE1F5FE),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "%.1f Liters".format(log.quantityLiters),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0288D1)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Health Events Header with Add Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Health Events & Logs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${healthLogs.size} recorded events for ${goat.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showAddLogDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("add_health_event_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Event")
                    }
                }
            }

            // 6. List of HealthLog entries
            if (healthLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No health logs for this goat yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Record vaccinations, dewormings, hoof trims or health checkups above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(healthLogs, key = { it.id.ifBlank { it.date + it.type } }) { log ->
                    HealthLogItemCard(
                        log = log,
                        onDelete = { onDeleteHealthLog(log.id) }
                    )
                }
            }
        }
    }

    // Add Health Log Dialog
    if (showAddLogDialog) {
        AddHealthLogDialog(
            goatTag = goat.tag,
            onDismiss = { showAddLogDialog = false },
            onSave = { newLog ->
                onAddHealthLog(newLog)
                showAddLogDialog = false
            }
        )
    }

    // Add Milk Log Dialog
    if (showAddMilkDialog) {
        RecordMilkDialog(
            goatTag = goat.tag,
            onDismiss = { showAddMilkDialog = false },
            onSave = { newMilk ->
                onAddMilkProduction(newMilk)
                showAddMilkDialog = false
            }
        )
    }

    // Add Breeding Record Dialog
    if (showAddBreedingDialog) {
        AddBreedingRecordDialog(
            currentGoat = goat,
            onDismiss = { showAddBreedingDialog = false },
            onSave = { newRecord ->
                onAddBreedingRecord(newRecord)
                showAddBreedingDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Goat?") },
            text = {
                Text("Are you sure you want to remove ${goat.name} (${goat.tag}) from the herd? All associated records will be permanently removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteGoat()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_detail_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GoatHeroCard(
    goat: Goat,
    onEdit: () -> Unit
) {
    val isDoe = goat.gender.equals("Doe", ignoreCase = true)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDoe) Color(0xFFF1F8E9) else Color(0xFFFFF8E1)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(if (isDoe) Color(0xFFC8E6C9) else Color(0xFFFFE0B2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDoe) Icons.Default.Female else Icons.Default.Male,
                    contentDescription = goat.gender,
                    tint = if (isDoe) Color(0xFF2E7D32) else Color(0xFFE65100),
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goat.name.ifBlank { "Unnamed" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = goat.tag,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = goat.breed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Status Badge
                val statusBg = when (goat.status) {
                    "Lactating" -> Color(0xFFE8F5E9)
                    "Pregnant" -> Color(0xFFFFF3E0)
                    "Active" -> Color(0xFFE3F2FD)
                    "Dry" -> Color(0xFFEFEBE9)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val statusTextColor = when (goat.status) {
                    "Lactating" -> Color(0xFF1B5E20)
                    "Pregnant" -> Color(0xFFE65100)
                    "Active" -> Color(0xFF0D47A1)
                    "Dry" -> Color(0xFF4E342E)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Status: ${goat.status}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun GoatDemographicsCard(goat: Goat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Physical Demographics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Gender
            ProfileInfoRow(
                icon = if (goat.gender.equals("Doe", true)) Icons.Default.Female else Icons.Default.Male,
                label = "Gender",
                value = goat.gender
            )

            // Age & Date of Birth
            val ageString = calculateAgeString(goat.birthDate)
            ProfileInfoRow(
                icon = Icons.Default.CalendarToday,
                label = "Date of Birth",
                value = if (goat.birthDate.isNotBlank()) "${goat.birthDate} ($ageString)" else "Not specified"
            )

            // Weight
            ProfileInfoRow(
                icon = Icons.Default.Scale,
                label = "Weight",
                value = if (goat.weightKg > 0) "${goat.weightKg} kg" else "Not weighed"
            )

            // Horn Status
            ProfileInfoRow(
                icon = Icons.Default.Shield,
                label = "Horn Status",
                value = goat.hornStatus.ifBlank { "Polled" }
            )
        }
    }
}

@Composable
private fun GoatPedigreeCard(goat: Goat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Pedigree & Parentage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Sire (Father)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = goat.sireTag.ifBlank { "Unknown" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (goat.sireTag.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Dam (Mother)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = goat.damTag.ifBlank { "Unknown" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (goat.damTag.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HealthLogItemCard(
    log: HealthLog,
    onDelete: () -> Unit
) {
    val (typeColor, typeBg, typeIcon) = when (log.type) {
        "Vaccination" -> Triple(Color(0xFF1565C0), Color(0xFFE3F2FD), Icons.Default.Healing)
        "Deworming" -> Triple(Color(0xFFE65100), Color(0xFFFFF3E0), Icons.Default.LocalPharmacy)
        "Hoof Trim" -> Triple(Color(0xFF4E342E), Color(0xFFEFEBE9), Icons.Default.ContentCut)
        "Medication" -> Triple(Color(0xFF6A1B9A), Color(0xFFF3E5F5), Icons.Default.MedicalServices)
        else -> Triple(Color(0xFF2E7D32), Color(0xFFE8F5E9), Icons.Default.CheckCircle)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(typeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = log.type,
                    tint = typeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = typeBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = log.type,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }

                    Text(
                        text = log.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.description.ifBlank { "${log.type} administered" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal
                )

                if (log.nextDueDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Follow-up Due: ${log.nextDueDate}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Log",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AddHealthLogDialog(
    goatTag: String,
    onDismiss: () -> Unit,
    onSave: (HealthLog) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var date by remember { mutableStateOf(today) }
    var selectedType by remember { mutableStateOf("Vaccination") }
    var description by remember { mutableStateOf("") }
    var nextDueDate by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val logTypes = listOf("Vaccination", "Deworming", "Medication", "Hoof Trim", "Checkup")

    fun addDaysToDate(days: Int): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val base = sdf.parse(date) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = base
                add(Calendar.DAY_OF_YEAR, days)
            }
            sdf.format(cal.time)
        } catch (_: Exception) {
            ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Health Event ($goatTag)",
                fontWeight = FontWeight.Bold
            )
        },
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

                Text(text = "Event Type", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    logTypes.take(3).forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    logTypes.drop(3).forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date Administered (YYYY-MM-DD) *") },
                    modifier = Modifier.fillMaxWidth().testTag("health_log_input_date"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Treatment / Medicine *") },
                    placeholder = {
                        val placeholder = when (selectedType) {
                            "Vaccination" -> "e.g., CDT 2ml Sub-Q booster"
                            "Deworming" -> "e.g., Albendazole 5ml oral drench, FAMACHA score 3"
                            "Hoof Trim" -> "e.g., Front and rear hooves leveled, no foot rot"
                            else -> "e.g., Antibiotic penicillin injection, wound care"
                        }
                        Text(placeholder)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("health_log_input_desc"),
                    maxLines = 3
                )

                // Next Due Date / Reminder Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Next Check-up / Booster Due Date (Optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = nextDueDate,
                        onValueChange = { nextDueDate = it },
                        label = { Text("Next Due Date (YYYY-MM-DD)") },
                        placeholder = { Text("e.g., 2026-12-15") },
                        supportingText = { Text("Schedules an alert & notification reminder") },
                        modifier = Modifier.fillMaxWidth().testTag("health_log_input_due_date"),
                        singleLine = true
                    )

                    // Quick schedule presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { nextDueDate = addDaysToDate(30) },
                            label = { Text("+30d", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { nextDueDate = addDaysToDate(90) },
                            label = { Text("+90d", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { nextDueDate = addDaysToDate(180) },
                            label = { Text("+6mo", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { nextDueDate = addDaysToDate(365) },
                            label = { Text("+1yr", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (date.isBlank()) {
                        errorMessage = "Please enter the date."
                        return@Button
                    }
                    if (description.isBlank()) {
                        errorMessage = "Please enter a description for the event."
                        return@Button
                    }
                    onSave(
                        HealthLog(
                            goatTag = goatTag,
                            date = date.trim(),
                            type = selectedType,
                            description = description.trim(),
                            nextDueDate = nextDueDate.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_health_log_button")
            ) {
                Text("Save Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RecordMilkDialog(
    goatTag: String,
    onDismiss: () -> Unit,
    onSave: (com.example.data.model.MilkProduction) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var date by remember { mutableStateOf(today) }
    var quantityStr by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Milk Yield ($goatTag)",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD) *") },
                    modifier = Modifier.fillMaxWidth().testTag("milk_log_input_date"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity (Liters) *") },
                    placeholder = { Text("e.g., 2.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("milk_log_input_liters"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (date.isBlank()) {
                        errorMessage = "Please enter date."
                        return@Button
                    }
                    val qty = quantityStr.toDoubleOrNull()
                    if (qty == null || qty <= 0.0) {
                        errorMessage = "Please enter a valid milk volume greater than 0."
                        return@Button
                    }
                    onSave(
                        com.example.data.model.MilkProduction(
                            goatId = goatTag,
                            date = date.trim(),
                            quantityLiters = qty
                        )
                    )
                },
                modifier = Modifier.testTag("save_milk_yield_button")
            ) {
                Text("Save Yield")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Calculates human-readable age string from YYYY-MM-DD birthDate.
 */
private fun calculateAgeString(birthDate: String): String {
    if (birthDate.isBlank()) return "Unknown"
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dob = Calendar.getInstance().apply { time = sdf.parse(birthDate) ?: return "Unknown" }
        val now = Calendar.getInstance()

        var years = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        var months = now.get(Calendar.MONTH) - dob.get(Calendar.MONTH)

        if (months < 0) {
            years--
            months += 12
        }

        when {
            years > 0 && months > 0 -> "$years yr $months mo"
            years > 0 -> "$years yr${if (years > 1) "s" else ""}"
            months > 0 -> "$months mo${if (months > 1) "s" else ""}"
            else -> "Newborn"
        }
    } catch (_: Exception) {
        "Unknown"
    }
}

@Composable
private fun BreedingRecordCard(
    record: com.example.data.model.BreedingRecord,
    currentGoat: Goat,
    onDelete: () -> Unit
) {
    val daysUntil = record.daysUntilDelivery()
    val isDoe = currentGoat.gender.equals("Doe", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Status badge & delivery countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (statusBg, statusTextColor) = when (record.status) {
                    "Confirmed Pregnant" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                    "Bred" -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
                    "Kidded" -> Color(0xFFEDE7F6) to Color(0xFF512DA8)
                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = record.status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor
                    )
                }

                // Delivery countdown or kid count
                if (record.status == "Kidded") {
                    Surface(
                        color = Color(0xFFEDE7F6),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (record.kidsBorn > 0) "${record.kidsBorn} Kids Born" else "Delivered",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF512DA8)
                        )
                    }
                } else if (daysUntil != null && record.status != "Failed") {
                    val (badgeBg, badgeColor, badgeText) = when {
                        daysUntil > 0 -> Triple(Color(0xFFE3F2FD), Color(0xFF0277BD), "Due in $daysUntil days")
                        daysUntil == 0L -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Due Today!")
                        else -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Overdue by ${-daysUntil} days")
                    }
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            // Sire / Dam identification badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dam (Mother)
                Surface(
                    color = if (isDoe) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "DAM (DOE / MOTHER)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Female, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF689F38))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = record.damId.ifBlank { record.doeTag },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Sire (Father)
                Surface(
                    color = if (!isDoe) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "SIRE (BUCK / FATHER)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Male, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF57F17))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = record.sireId.ifBlank { record.buckTag },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Dates Row: Mating Date & Expected Delivery Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mating Date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.matingDate.ifBlank { record.breedingDate },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Expected Delivery (150d)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.expectedDeliveryDate.ifBlank { record.expectedKiddingDate },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (record.notes.isNotBlank()) {
                Text(
                    text = "Notes: ${record.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBreedingRecordDialog(
    currentGoat: Goat,
    onDismiss: () -> Unit,
    onSave: (com.example.data.model.BreedingRecord) -> Unit
) {
    val isDoe = currentGoat.gender.equals("Doe", ignoreCase = true)
    val isBuck = currentGoat.gender.equals("Buck", ignoreCase = true)

    val todayStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date())
    }

    var damId by remember { mutableStateOf(if (isDoe || !isBuck) currentGoat.tag else "") }
    var sireId by remember { mutableStateOf(if (isBuck) currentGoat.tag else "") }
    var matingDate by remember { mutableStateOf(todayStr) }
    var expectedDeliveryDate by remember {
        mutableStateOf(com.example.data.model.BreedingRecord.calculateExpectedDeliveryDate(todayStr))
    }
    var selectedStatus by remember { mutableStateOf("Bred") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val statusOptions = listOf("Bred", "Confirmed Pregnant", "Kidded", "Failed")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Breeding Event (${currentGoat.tag})",
                fontWeight = FontWeight.Bold
            )
        },
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

                // Dam ID
                OutlinedTextField(
                    value = damId,
                    onValueChange = { damId = it },
                    label = { Text("Dam ID (Doe / Mother) *") },
                    placeholder = { Text("e.g., DOE-001") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("breeding_input_dam_id"),
                    singleLine = true
                )

                // Sire ID
                OutlinedTextField(
                    value = sireId,
                    onValueChange = { sireId = it },
                    label = { Text("Sire ID (Buck / Father) *") },
                    placeholder = { Text("e.g., BUCK-001") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("breeding_input_sire_id"),
                    singleLine = true
                )

                // Mating Date
                OutlinedTextField(
                    value = matingDate,
                    onValueChange = { newMatingDate ->
                        matingDate = newMatingDate
                        val calculated = com.example.data.model.BreedingRecord.calculateExpectedDeliveryDate(newMatingDate)
                        if (calculated.isNotBlank()) {
                            expectedDeliveryDate = calculated
                        }
                    },
                    label = { Text("Mating Date (YYYY-MM-DD) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("breeding_input_mating_date"),
                    singleLine = true
                )

                // Expected Delivery Date (calculated 150 days)
                OutlinedTextField(
                    value = expectedDeliveryDate,
                    onValueChange = { expectedDeliveryDate = it },
                    label = { Text("Expected Delivery Date (150d) *") },
                    supportingText = { Text("Calculated: 150 days average gestation") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("breeding_input_delivery_date"),
                    singleLine = true
                )

                // Status selection chips
                Text(
                    text = "Breeding Status",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statusOptions.take(2).forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statusOptions.drop(2).forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Natural mating, artificial insemination, ultrasound notes...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("breeding_input_notes"),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (damId.isBlank()) {
                        errorMessage = "Please specify Dam ID (Doe / Mother)."
                        return@Button
                    }
                    if (sireId.isBlank()) {
                        errorMessage = "Please specify Sire ID (Buck / Father)."
                        return@Button
                    }
                    if (matingDate.isBlank()) {
                        errorMessage = "Please specify Mating Date (YYYY-MM-DD)."
                        return@Button
                    }
                    val finalDelivery = if (expectedDeliveryDate.isNotBlank()) {
                        expectedDeliveryDate.trim()
                    } else {
                        com.example.data.model.BreedingRecord.calculateExpectedDeliveryDate(matingDate)
                    }
                    onSave(
                        com.example.data.model.BreedingRecord.create(
                            damId = damId.trim(),
                            sireId = sireId.trim(),
                            matingDate = matingDate.trim(),
                            expectedDeliveryDate = finalDelivery,
                            status = selectedStatus,
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_breeding_record_button")
            ) {
                Text("Save Breeding Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
