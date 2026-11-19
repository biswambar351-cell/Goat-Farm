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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Goat
import com.example.data.model.MilkLog
import com.example.ui.viewmodel.GoatFarmViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MilkScreen(
    viewModel: GoatFarmViewModel,
    isAddDialogOpen: Boolean,
    onCloseAddDialog: () -> Unit,
    onOpenAddDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val milkLogsState by viewModel.milkLogs.collectAsStateWithLifecycle()
    val goatsState by viewModel.goats.collectAsStateWithLifecycle()
    var logToDelete by remember { mutableStateOf<MilkLog?>(null) }

    val doesList = if (goatsState is UiState.Success) {
        (goatsState as UiState.Success<List<Goat>>).data.filter { it.gender.equals("Doe", ignoreCase = true) }
    } else {
        emptyList()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = milkLogsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    val logs = state.data
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val todayTotal = logs.filter { it.date == todayStr }.sumOf { it.yieldLiters }
                    val totalLiters = logs.sumOf { it.yieldLiters }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // Summary Banner
                        item {
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("milk_stats_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Today's Yield",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "%.1f L".format(todayTotal),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalDrink,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "All-Time Logged",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "%.1f L".format(totalLiters),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        if (logs.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalDrink,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No milk logs recorded yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Track daily milk output for individual does or whole herd bulk yields.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = onOpenAddDialog) {
                                        Text("Log First Milking")
                                    }
                                }
                            }
                        } else {
                            item {
                                Text(
                                    text = "Milking History (${logs.size} entries)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(logs, key = { it.id.ifBlank { "${it.doeTag}_${it.date}_${it.session}" } }) { log ->
                                MilkLogCard(log = log, onDelete = { logToDelete = log })
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onOpenAddDialog,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("log_milk_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Log Milking")
        }
    }

    if (isAddDialogOpen) {
        MilkFormDialog(
            does = doesList,
            onDismiss = onCloseAddDialog,
            onSave = { newLog ->
                viewModel.addMilkLog(newLog) { success ->
                    if (success) onCloseAddDialog()
                }
            }
        )
    }

    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Delete Milk Entry?") },
            text = { Text("Delete milking record of ${logToDelete!!.yieldLiters}L for ${logToDelete!!.doeTag} on ${logToDelete!!.date}?") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = logToDelete!!.id
                        logToDelete = null
                        viewModel.deleteMilkLog(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MilkLogCard(
    log: MilkLog,
    onDelete: () -> Unit
) {
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (log.session == "Morning") Icons.Default.WbSunny else Icons.Default.WbTwilight,
                        contentDescription = log.session,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = log.doeTag,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = log.session,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Text(
                        text = log.date + if (log.notes.isNotBlank()) " • ${log.notes}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "%.1f L".format(log.yieldLiters),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Log",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MilkFormDialog(
    does: List<Goat>,
    onDismiss: () -> Unit,
    onSave: (MilkLog) -> Unit
) {
    var doeTag by remember { mutableStateOf(if (does.isNotEmpty()) does.first().tag else "HERD") }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var date by remember { mutableStateOf(today) }
    var session by remember { mutableStateOf("Morning") }
    var yieldStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Milk Yield", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (formError != null) {
                    Text(text = formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // Doe Selector Chips
                Text("Doe / Source *", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = doeTag == "HERD",
                            onClick = { doeTag = "HERD" },
                            label = { Text("HERD (Bulk)") }
                        )
                    }
                    items(does) { d ->
                        FilterChip(
                            selected = doeTag == d.tag,
                            onClick = { doeTag = d.tag },
                            label = { Text("${d.name} (${d.tag})") }
                        )
                    }
                }

                OutlinedTextField(
                    value = doeTag,
                    onValueChange = { doeTag = it },
                    label = { Text("Or Type Ear Tag") },
                    modifier = Modifier.fillMaxWidth().testTag("milk_input_doe_tag"),
                    singleLine = true
                )

                // Session
                Text("Milking Session *", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Morning", "Evening", "Full Day").forEach { s ->
                        FilterChip(
                            selected = session == s,
                            onClick = { session = s },
                            label = { Text(s) }
                        )
                    }
                }

                OutlinedTextField(
                    value = yieldStr,
                    onValueChange = { yieldStr = it },
                    label = { Text("Yield in Liters * (e.g. 2.5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("milk_input_yield"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Quality / Mastitis Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val y = yieldStr.toDoubleOrNull()
                    if (y == null || y <= 0.0) {
                        formError = "Please enter a valid milk yield greater than 0."
                        return@Button
                    }
                    if (doeTag.isBlank()) {
                        formError = "Please select or enter a Doe Tag."
                        return@Button
                    }
                    onSave(
                        MilkLog(
                            doeTag = doeTag.trim(),
                            date = date.trim(),
                            session = session,
                            yieldLiters = y,
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("milk_save_button")
            ) {
                Text("Save Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
