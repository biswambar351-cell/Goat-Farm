package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.data.model.HealthLog
import com.example.data.model.HealthRecord
import androidx.compose.ui.platform.LocalContext
import com.example.notification.FarmNotificationManager
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.Goat as LocalGoat
import com.example.data.model.Goat
import com.example.ui.components.AddGoatDialog
import com.example.ui.viewmodel.GoatFarmViewModel
import com.example.ui.viewmodel.GoatUiState
import com.example.ui.viewmodel.GoatViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HerdScreen(
    viewModel: GoatFarmViewModel,
    isAddDialogOpen: Boolean,
    onCloseAddDialog: () -> Unit,
    onOpenAddDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val goatViewModel: GoatViewModel = viewModel(factory = GoatViewModel.factory(context))
    var selectedHerdTab by remember { mutableIntStateOf(0) }
    var isLocalAddDialogOpen by remember { mutableStateOf(false) }

    val goatsState by viewModel.goats.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedGoatForDetail by remember { mutableStateOf<Goat?>(null) }
    var goatToEdit by remember { mutableStateOf<Goat?>(null) }
    var goatToDelete by remember { mutableStateOf<Goat?>(null) }

    if (selectedGoatForDetail != null) {
        val currentGoat = selectedGoatForDetail!!
        BackHandler { selectedGoatForDetail = null }

        val healthState by viewModel.healthRecords.collectAsStateWithLifecycle()
        val goatHealthLogs = when (val h = healthState) {
            is UiState.Success -> h.data
                .filter { it.goatTag.equals(currentGoat.tag, ignoreCase = true) }
                .map { r ->
                    HealthLog(
                        id = r.id,
                        userId = r.userId,
                        goatTag = r.goatTag,
                        date = r.administeredDate,
                        type = r.recordType,
                        description = if (r.notes.isNotBlank() && r.notes != r.treatment) "${r.treatment} - ${r.notes}" else r.treatment,
                        nextDueDate = r.nextDueDate,
                        createdAt = r.createdAt,
                        updatedAt = r.updatedAt
                    )
                }
            else -> emptyList()
        }

        val milkLogsState by viewModel.milkLogs.collectAsStateWithLifecycle()
        val goatMilkLogs = when (val m = milkLogsState) {
            is UiState.Success -> m.data
                .filter { it.doeTag.equals(currentGoat.tag, ignoreCase = true) }
                .map {
                    com.example.data.model.MilkProduction(
                        id = it.id,
                        userId = it.userId,
                        goatId = it.doeTag,
                        date = it.date,
                        quantityLiters = it.yieldLiters,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                }
            else -> emptyList()
        }

        val breedingsState by viewModel.breedings.collectAsStateWithLifecycle()
        val goatBreedings = when (val b = breedingsState) {
            is UiState.Success -> b.data.filter {
                it.doeTag.equals(currentGoat.tag, ignoreCase = true) || it.buckTag.equals(currentGoat.tag, ignoreCase = true)
            }
            else -> emptyList()
        }

        GoatDetailScreen(
            goat = currentGoat,
            healthLogs = goatHealthLogs,
            milkLogs = goatMilkLogs,
            breedingRecords = goatBreedings,
            onBack = { selectedGoatForDetail = null },
            onEditGoat = {
                val g = currentGoat
                selectedGoatForDetail = null
                goatToEdit = g
            },
            onDeleteGoat = {
                val g = currentGoat
                selectedGoatForDetail = null
                goatToDelete = g
            },
            onAddHealthLog = { newLog ->
                val record = HealthRecord(
                    goatTag = newLog.goatTag,
                    recordType = newLog.type,
                    treatment = newLog.description,
                    administeredDate = newLog.date,
                    nextDueDate = newLog.nextDueDate,
                    notes = newLog.description
                )
                viewModel.addHealthRecord(record) { success ->
                    if (success && record.nextDueDate.isNotBlank()) {
                        FarmNotificationManager.scheduleHealthReminder(
                            context = context,
                            goatTag = record.goatTag,
                            recordType = record.recordType,
                            treatmentName = record.treatment,
                            dueDateString = record.nextDueDate
                        )
                    }
                }
            },
            onDeleteHealthLog = { logId ->
                viewModel.deleteHealthRecord(logId)
            },
            onAddMilkProduction = { newMilk ->
                viewModel.addMilkProduction(newMilk)
            },
            onAddBreedingRecord = { newBreeding ->
                viewModel.addBreeding(newBreeding) { success ->
                    if (success) {
                        val dateStr = newBreeding.expectedDeliveryDate.ifBlank { newBreeding.expectedKiddingDate }
                        if (dateStr.isNotBlank()) {
                            FarmNotificationManager.scheduleKiddingReminder(
                                context = context,
                                doeTag = newBreeding.doeTag.ifBlank { newBreeding.damId },
                                buckTag = newBreeding.buckTag.ifBlank { newBreeding.sireId },
                                expectedDateString = dateStr
                            )
                        }
                    }
                }
            },
            onDeleteBreedingRecord = { recordId ->
                viewModel.deleteBreeding(recordId)
            },
            modifier = modifier
        )

        // Edit Dialog
        if (goatToEdit != null) {
            GoatFormDialog(
                title = "Edit Goat (${goatToEdit!!.tag})",
                initialGoat = goatToEdit!!,
                onDismiss = { goatToEdit = null },
                onSave = { updatedGoat ->
                    viewModel.updateGoat(goatToEdit!!.id, updatedGoat) { success ->
                        if (success) {
                            goatToEdit = null
                            selectedGoatForDetail = updatedGoat
                        }
                    }
                }
            )
        }

        // Delete Dialog
        if (goatToDelete != null) {
            AlertDialog(
                onDismissRequest = { goatToDelete = null },
                title = { Text("Delete Goat?") },
                text = { Text("Are you sure you want to remove ${goatToDelete!!.name} (${goatToDelete!!.tag}) from the herd? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            val g = goatToDelete!!
                            goatToDelete = null
                            selectedGoatForDetail = null
                            viewModel.deleteGoat(g.id, g.tag)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { goatToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = selectedHerdTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedHerdTab == 0,
                    onClick = { selectedHerdTab = 0 },
                    text = { Text("Cloud Herd") },
                    icon = { Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_cloud_herd")
                )
                Tab(
                    selected = selectedHerdTab == 1,
                    onClick = { selectedHerdTab = 1 },
                    text = { Text("Local Room DB") },
                    icon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_local_herd")
                )
            }

            if (selectedHerdTab == 1) {
                LocalRoomHerdContent(
                    goatViewModel = goatViewModel,
                    onOpenAddLocalGoat = { isLocalAddDialogOpen = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goat_search_input"),
                placeholder = { Text("Search by Tag, Name or Breed...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            val filters = listOf("All", "Does", "Bucks", "Kids", "Lactating", "Pregnant")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        modifier = Modifier.testTag("filter_chip_$filter")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Herd List or States
            when (val state = goatsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Error loading herd: ${state.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is UiState.Success -> {
                    val allGoats = state.data
                    val filtered = allGoats.filter { goat ->
                        val queryMatch = searchQuery.isBlank() ||
                                goat.tag.contains(searchQuery, ignoreCase = true) ||
                                goat.name.contains(searchQuery, ignoreCase = true) ||
                                goat.breed.contains(searchQuery, ignoreCase = true)

                        val filterMatch = when (selectedFilter) {
                            "Does" -> goat.gender.equals("Doe", ignoreCase = true)
                            "Bucks" -> goat.gender.equals("Buck", ignoreCase = true)
                            "Kids" -> goat.gender.equals("Wether", ignoreCase = true) || goat.status.equals("Kid", ignoreCase = true)
                            "Lactating" -> goat.status.equals("Lactating", ignoreCase = true)
                            "Pregnant" -> goat.status.equals("Pregnant", ignoreCase = true)
                            else -> true
                        }
                        queryMatch && filterMatch
                    }

                    if (filtered.isEmpty()) {
                        EmptyHerdView(
                            hasSearch = searchQuery.isNotBlank() || selectedFilter != "All",
                            onAddGoat = onOpenAddDialog
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                Text(
                                    text = "Showing ${filtered.size} of ${allGoats.size} goats",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(filtered, key = { it.id.ifBlank { it.tag } }) { goat ->
                                GoatCard(
                                    goat = goat,
                                    onClick = { selectedGoatForDetail = goat },
                                    onEdit = { goatToEdit = goat },
                                    onDelete = { goatToDelete = goat }
                                )
                            }
                        }
                    }
                }
            }
        }
        } // end Cloud Herd else
        } // end outer Column

        // Floating Action Button to Add Goat
        FloatingActionButton(
            onClick = {
                if (selectedHerdTab == 1) isLocalAddDialogOpen = true else onOpenAddDialog()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_goat_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Goat")
        }
    }

    // Room Local Add Goat Dialog (integrated with GoatViewModel)
    if (isLocalAddDialogOpen) {
        AddGoatDialog(
            goatViewModel = goatViewModel,
            onDismiss = { isLocalAddDialogOpen = false }
        )
    }

    // Add / Edit Dialog
    if (isAddDialogOpen) {
        GoatFormDialog(
            title = "Register New Goat",
            initialGoat = Goat(),
            onDismiss = onCloseAddDialog,
            onSave = { newGoat ->
                viewModel.addGoat(newGoat) { success ->
                    if (success) onCloseAddDialog()
                }
            }
        )
    }

    if (goatToEdit != null) {
        GoatFormDialog(
            title = "Edit Goat (${goatToEdit!!.tag})",
            initialGoat = goatToEdit!!,
            onDismiss = { goatToEdit = null },
            onSave = { updatedGoat ->
                viewModel.updateGoat(goatToEdit!!.id, updatedGoat) { success ->
                    if (success) goatToEdit = null
                }
            }
        )
    }

    // Detail Dialog
    if (selectedGoatForDetail != null) {
        GoatDetailDialog(
            goat = selectedGoatForDetail!!,
            onDismiss = { selectedGoatForDetail = null },
            onEdit = {
                val g = selectedGoatForDetail!!
                selectedGoatForDetail = null
                goatToEdit = g
            },
            onDelete = {
                val g = selectedGoatForDetail!!
                selectedGoatForDetail = null
                goatToDelete = g
            }
        )
    }

    // Delete Confirmation Dialog
    if (goatToDelete != null) {
        AlertDialog(
            onDismissRequest = { goatToDelete = null },
            title = { Text("Delete Goat?") },
            text = { Text("Are you sure you want to remove ${goatToDelete!!.name} (${goatToDelete!!.tag}) from the herd? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val g = goatToDelete!!
                        goatToDelete = null
                        viewModel.deleteGoat(g.id, g.tag)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { goatToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GoatCard(
    goat: Goat,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("goat_card_${goat.tag}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gender/Breed Avatar
            val isDoe = goat.gender.equals("Doe", ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDoe) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDoe) Icons.Default.Female else Icons.Default.Male,
                    contentDescription = goat.gender,
                    tint = if (isDoe) Color(0xFF2E7D32) else Color(0xFFE65100),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goat.name.ifBlank { "Unnamed" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = goat.tag,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${goat.breed} • ${goat.gender}${if (goat.weightKg > 0) " • ${goat.weightKg} kg" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (goat.status.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val statusColor = when (goat.status) {
                        "Lactating" -> Color(0xFF1B5E20)
                        "Pregnant" -> Color(0xFFE65100)
                        "Dry" -> Color(0xFF5D4037)
                        "Sold" -> Color(0xFF616161)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Text(
                        text = "Status: ${goat.status}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Goat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Goat",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHerdView(
    hasSearch: Boolean,
    onAddGoat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Pets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasSearch) "No goats match your filter" else "No goats registered yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (hasSearch) "Try adjusting your search criteria" else "Build your herd registry by adding your first doe, buck or kid.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!hasSearch) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(onClick = onAddGoat) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add First Goat")
            }
        }
    }
}

@Composable
fun GoatFormDialog(
    title: String,
    initialGoat: Goat,
    onDismiss: () -> Unit,
    onSave: (Goat) -> Unit
) {
    var tag by remember { mutableStateOf(initialGoat.tag) }
    var name by remember { mutableStateOf(initialGoat.name) }
    var breed by remember { mutableStateOf(initialGoat.breed) }
    var gender by remember { mutableStateOf(initialGoat.gender) }
    var status by remember { mutableStateOf(initialGoat.status) }
    var birthDate by remember { mutableStateOf(initialGoat.birthDate) }
    var weightStr by remember { mutableStateOf(if (initialGoat.weightKg > 0) initialGoat.weightKg.toString() else "") }
    var hornStatus by remember { mutableStateOf(initialGoat.hornStatus) }
    var sireTag by remember { mutableStateOf(initialGoat.sireTag) }
    var damTag by remember { mutableStateOf(initialGoat.damTag) }
    var notes by remember { mutableStateOf(initialGoat.notes) }

    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (formError != null) {
                    Text(text = formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Ear Tag / ID *") },
                    modifier = Modifier.fillMaxWidth().testTag("goat_input_tag"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goat Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("goat_input_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Breed * (e.g. Boer, Nubian, Alpine)") },
                    modifier = Modifier.fillMaxWidth().testTag("goat_input_breed"),
                    singleLine = true
                )

                // Gender selector
                Text("Gender *", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Doe", "Buck", "Wether").forEach { g ->
                        FilterChip(
                            selected = gender == g,
                            onClick = { gender = g },
                            label = { Text(g) }
                        )
                    }
                }

                // Status selector
                Text("Current Status *", style = MaterialTheme.typography.labelMedium)
                val statusOptions = listOf("Active", "Lactating", "Pregnant", "Dry", "Sold", "Culled")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(statusOptions) { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st) }
                        )
                    }
                }

                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    label = { Text("Birth Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Horn Status
                Text("Horns", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Polled", "Dehorned", "Horned").forEach { h ->
                        FilterChip(
                            selected = hornStatus == h,
                            onClick = { hornStatus = h },
                            label = { Text(h) }
                        )
                    }
                }

                OutlinedTextField(
                    value = sireTag,
                    onValueChange = { sireTag = it },
                    label = { Text("Sire Tag (Father)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = damTag,
                    onValueChange = { damTag = it },
                    label = { Text("Dam Tag (Mother)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Pedigree & Care Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tag.isBlank()) {
                        formError = "Please enter an Ear Tag / ID."
                        return@Button
                    }
                    if (name.isBlank()) {
                        formError = "Please enter a goat name."
                        return@Button
                    }
                    if (breed.isBlank()) {
                        formError = "Please specify a breed."
                        return@Button
                    }
                    val weight = weightStr.toDoubleOrNull() ?: 0.0
                    onSave(
                        initialGoat.copy(
                            tag = tag.trim(),
                            name = name.trim(),
                            breed = breed.trim(),
                            gender = gender,
                            status = status,
                            birthDate = birthDate.trim(),
                            weightKg = weight,
                            hornStatus = hornStatus,
                            sireTag = sireTag.trim(),
                            damTag = damTag.trim(),
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("goat_save_button")
            ) {
                Text("Save")
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
fun GoatDetailDialog(
    goat: Goat,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = goat.name, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Tag: ${goat.tag}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = goat.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Breed", goat.breed)
                DetailRow("Gender", goat.gender)
                if (goat.birthDate.isNotBlank()) DetailRow("Birth Date", goat.birthDate)
                if (goat.weightKg > 0) DetailRow("Weight", "${goat.weightKg} kg")
                DetailRow("Horn Status", goat.hornStatus)
                if (goat.sireTag.isNotBlank()) DetailRow("Sire (Father)", goat.sireTag)
                if (goat.damTag.isNotBlank()) DetailRow("Dam (Mother)", goat.damTag)
                if (goat.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Notes:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(goat.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onEdit) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LocalRoomHerdContent(
    goatViewModel: GoatViewModel,
    onOpenAddLocalGoat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by goatViewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by goatViewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by goatViewModel.healthStatusFilter.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { goatViewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("local_goat_search_input"),
            placeholder = { Text("Search local goats by Name, Breed, or ID...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search Local Goats")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { goatViewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        val healthFilters = listOf("All", "Healthy", "Needs Checkup", "Under Treatment", "Pregnant")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(healthFilters) { filter ->
                FilterChip(
                    selected = statusFilter == filter,
                    onClick = { goatViewModel.setHealthStatusFilter(filter) },
                    label = { Text(filter) },
                    modifier = Modifier.testTag("local_filter_chip_$filter")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (val state = uiState) {
            is GoatUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is GoatUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is GoatUiState.Success -> {
                val goats = state.goats
                if (goats.isEmpty()) {
                    EmptyHerdView(
                        hasSearch = searchQuery.isNotBlank() || statusFilter != "All",
                        onAddGoat = onOpenAddLocalGoat
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${goats.size} local Room record(s)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = onOpenAddLocalGoat,
                                    modifier = Modifier.testTag("open_local_add_goat_dialog_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Local Goat")
                                }
                            }
                        }
                        items(goats, key = { it.id }) { goat ->
                            LocalGoatCard(
                                goat = goat,
                                onDelete = { goatViewModel.deleteGoat(goat) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalGoatCard(
    goat: LocalGoat,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("local_goat_card_${goat.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = goat.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goat.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ID #${goat.id}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${goat.breed.ifBlank { "Unknown Breed" }} • Born: ${goat.birthDate.ifBlank { "N/A" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                val healthColor = when {
                    goat.healthStatus.contains("Healthy", ignoreCase = true) -> Color(0xFF2E7D32)
                    goat.healthStatus.contains("Treatment", ignoreCase = true) ||
                        goat.healthStatus.contains("Sick", ignoreCase = true) -> Color(0xFFC62828)
                    else -> Color(0xFFE65100)
                }
                Text(
                    text = "Health: ${goat.healthStatus}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = healthColor
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_local_goat_${goat.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${goat.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
