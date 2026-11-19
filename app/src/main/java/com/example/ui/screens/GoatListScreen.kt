package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.model.HealthLog
import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Goat
import com.example.data.repository.GoatRepository
import com.example.ui.viewmodel.GoatListViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.flow.collectLatest

/**
 * Jetpack Compose screen that displays a real-time list of goats fetched from
 * Firestore using [GoatRepository], featuring an interactive FloatingActionButton
 * to add new goat records.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoatListScreen(
    repository: GoatRepository,
    userId: String,
    modifier: Modifier = Modifier,
    viewModel: GoatListViewModel = viewModel(
        factory = GoatListViewModel.provideFactory(repository, userId),
        key = userId
    )
) {
    val goatsState by viewModel.goatsState.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var selectedGoatForDetail by remember { mutableStateOf<Goat?>(null) }
    var goatToEdit by remember { mutableStateOf<Goat?>(null) }
    var goatToDelete by remember { mutableStateOf<Goat?>(null) }
    val scope = rememberCoroutineScope()

    if (selectedGoatForDetail != null) {
        val currentGoat = selectedGoatForDetail!!
        BackHandler { selectedGoatForDetail = null }

        val healthLogs by remember(currentGoat.tag) {
            repository.observeHealthLogsForGoat(userId, currentGoat.tag)
        }.collectAsStateWithLifecycle(emptyList())

        val milkLogs by remember(currentGoat.tag) {
            repository.observeMilkProductionForGoat(userId, currentGoat.tag)
        }.collectAsStateWithLifecycle(emptyList())

        val breedingRecords by remember(currentGoat.tag) {
            repository.observeBreedingRecordsForGoat(userId, currentGoat.tag)
        }.collectAsStateWithLifecycle(emptyList())

        GoatDetailScreen(
            goat = currentGoat,
            healthLogs = healthLogs,
            milkLogs = milkLogs,
            breedingRecords = breedingRecords,
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
                scope.launch {
                    repository.addHealthLog(userId, newLog)
                }
            },
            onDeleteHealthLog = { logId ->
                scope.launch {
                    repository.deleteHealthLog(userId, logId)
                }
            },
            onAddMilkProduction = { newMilk ->
                scope.launch {
                    repository.storeMilkProduction(userId, newMilk)
                }
            },
            onAddBreedingRecord = { newBreeding ->
                scope.launch {
                    repository.storeBreedingRecord(userId, newBreeding)
                }
            },
            onDeleteBreedingRecord = { recordId ->
                scope.launch {
                    repository.deleteBreedingRecord(userId, recordId)
                }
            },
            modifier = modifier
        )

        // Edit Dialog
        if (goatToEdit != null) {
            GoatEntryDialog(
                dialogTitle = "Edit Goat (${goatToEdit!!.tag})",
                initialGoat = goatToEdit!!,
                isSubmitting = isSubmitting,
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
                text = {
                    Text("Are you sure you want to delete ${goatToDelete!!.name} (${goatToDelete!!.tag}) from Firestore? This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val g = goatToDelete!!
                            goatToDelete = null
                            selectedGoatForDetail = null
                            viewModel.deleteGoat(g.id, g.tag)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_goat_button")
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

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    LaunchedEffect(viewModel) {
        viewModel.feedbackMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Herd Registry",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddDialogOpen = true },
                modifier = Modifier
                    .testTag("add_goat_fab")
                    .padding(bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Goat"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

            // Content State
            when (val state = goatsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.testTag("goats_loading_indicator"))
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Unable to load herd",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                        GoatEmptyState(
                            hasSearch = searchQuery.isNotBlank() || selectedFilter != "All",
                            onAddGoat = { isAddDialogOpen = true }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            item {
                                Text(
                                    text = "Showing ${filtered.size} of ${allGoats.size} registered goats",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(filtered, key = { it.id.ifBlank { it.tag } }) { goat ->
                                GoatItemCard(
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
    }

    // Add New Goat Dialog
    if (isAddDialogOpen) {
        GoatEntryDialog(
            dialogTitle = "Add New Goat",
            initialGoat = Goat(),
            isSubmitting = isSubmitting,
            onDismiss = { isAddDialogOpen = false },
            onSave = { newGoat ->
                viewModel.addGoat(newGoat) { success ->
                    if (success) isAddDialogOpen = false
                }
            }
        )
    }

    // Edit Goat Dialog
    if (goatToEdit != null) {
        GoatEntryDialog(
            dialogTitle = "Edit Goat (${goatToEdit!!.tag})",
            initialGoat = goatToEdit!!,
            isSubmitting = isSubmitting,
            onDismiss = { goatToEdit = null },
            onSave = { updatedGoat ->
                viewModel.updateGoat(goatToEdit!!.id, updatedGoat) { success ->
                    if (success) goatToEdit = null
                }
            }
        )
    }

    // View Details Dialog
    if (selectedGoatForDetail != null) {
        GoatInfoDialog(
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
            text = {
                Text("Are you sure you want to delete ${goatToDelete!!.name} (${goatToDelete!!.tag}) from Firestore? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val g = goatToDelete!!
                        goatToDelete = null
                        viewModel.deleteGoat(g.id, g.tag)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_goat_button")
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
private fun GoatItemCard(
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
            val isDoe = goat.gender.equals("Doe", ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isDoe) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)),
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
private fun GoatEmptyState(
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
            text = if (hasSearch) "Try adjusting your search criteria" else "Tap the '+' button below to add your first goat to Firestore.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!hasSearch) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(onClick = onAddGoat) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Goat Now")
            }
        }
    }
}

@Composable
private fun GoatEntryDialog(
    dialogTitle: String,
    initialGoat: Goat,
    isSubmitting: Boolean,
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
        title = { Text(text = dialogTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (formError != null) {
                    Text(
                        text = formError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
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

                Text("Status *", style = MaterialTheme.typography.labelMedium)
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
                    label = { Text("Care & Pedigree Notes") },
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
                enabled = !isSubmitting,
                modifier = Modifier.testTag("goat_save_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GoatInfoDialog(
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
                InfoRow("Breed", goat.breed)
                InfoRow("Gender", goat.gender)
                if (goat.birthDate.isNotBlank()) InfoRow("Birth Date", goat.birthDate)
                if (goat.weightKg > 0) InfoRow("Weight", "${goat.weightKg} kg")
                InfoRow("Horn Status", goat.hornStatus)
                if (goat.sireTag.isNotBlank()) InfoRow("Sire (Father)", goat.sireTag)
                if (goat.damTag.isNotBlank()) InfoRow("Dam (Mother)", goat.damTag)
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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
