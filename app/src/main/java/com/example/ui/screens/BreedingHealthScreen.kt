package com.example.ui.screens

import com.example.ui.components.MonthlyHealthEventsBarChart
import com.example.ui.components.BreedingCycleCard
import com.example.ui.components.BreedingCycleFormDialog

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BreedingRecord
import com.example.data.model.Goat
import com.example.data.model.HealthRecord
import com.example.notification.FarmNotificationManager
import com.example.ui.viewmodel.GoatFarmViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingHealthScreen(
    viewModel: GoatFarmViewModel,
    isAddBreedingDialogOpen: Boolean,
    onCloseAddBreedingDialog: () -> Unit,
    onOpenAddBreedingDialog: () -> Unit,
    isAddHealthDialogOpen: Boolean,
    onCloseAddHealthDialog: () -> Unit,
    onOpenAddHealthDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Breeding, 1: Health
    val breedingsState by viewModel.breedings.collectAsStateWithLifecycle()
    val healthState by viewModel.healthRecords.collectAsStateWithLifecycle()
    val goatsState by viewModel.goats.collectAsStateWithLifecycle()

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            FarmNotificationManager.sendImmediateNotification(
                context = context,
                title = "🔔 Farm Notifications Enabled",
                message = "You will receive automated reminders for vaccination boosters, deworming cycles, and kidding dates."
            )
        }
    }

    val doesList = if (goatsState is UiState.Success) {
        (goatsState as UiState.Success<List<Goat>>).data.filter { it.gender.equals("Doe", ignoreCase = true) }
    } else emptyList()

    val bucksList = if (goatsState is UiState.Success) {
        (goatsState as UiState.Success<List<Goat>>).data.filter { it.gender.equals("Buck", ignoreCase = true) }
    } else emptyList()

    val allGoatsList = if (goatsState is UiState.Success) {
        (goatsState as UiState.Success<List<Goat>>).data
    } else emptyList()

    val aiViewModel: com.example.ui.viewmodel.AiHealthAdvisorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Breeding & Kidding") },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_breeding")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Health & Reminders") },
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_health")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("AI Advisor") },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_ai_advisor")
                )
            }

            when (selectedTab) {
                0 -> {
                    BreedingListContent(
                        breedingsState = breedingsState,
                        onOpenAdd = onOpenAddBreedingDialog,
                        onUpdateStatus = { id, record, newStatus, kids ->
                            val updated = record.copy(status = newStatus, kidsBorn = kids)
                            viewModel.updateBreeding(id, updated)
                        },
                        onDelete = { id -> viewModel.deleteBreeding(id) }
                    )
                }
                1 -> {
                    HealthListContent(
                        healthState = healthState,
                        breedingState = breedingsState,
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onOpenAdd = onOpenAddHealthDialog,
                        onDelete = { id -> viewModel.deleteHealthRecord(id) }
                    )
                }
                2 -> {
                    AiHealthAdvisorScreen(
                        aiViewModel = aiViewModel,
                        goats = allGoatsList,
                        onLogTreatmentToHealth = { record ->
                            viewModel.addHealthRecord(record) { success ->
                                if (success) {
                                    selectedTab = 1
                                }
                            }
                        }
                    )
                }
            }
        }

        // FAB based on tab (hidden on AI Advisor tab)
        if (selectedTab != 2) {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) onOpenAddBreedingDialog() else onOpenAddHealthDialog()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("breeding_health_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Record")
            }
        }
    }

    if (isAddBreedingDialogOpen) {
        BreedingCycleFormDialog(
            does = doesList,
            bucks = bucksList,
            onDismiss = onCloseAddBreedingDialog,
            onSave = { record ->
                viewModel.addBreeding(record) { success ->
                    if (success) {
                        FarmNotificationManager.scheduleKiddingReminder(
                            context = context,
                            doeTag = record.doeTag,
                            buckTag = record.buckTag,
                            expectedDateString = record.expectedKiddingDate
                        )
                        onCloseAddBreedingDialog()
                    }
                }
            }
        )
    }

    if (isAddHealthDialogOpen) {
        HealthFormDialog(
            goats = allGoatsList,
            onDismiss = onCloseAddHealthDialog,
            onSave = { record ->
                viewModel.addHealthRecord(record) { success ->
                    if (success) {
                        if (record.nextDueDate.isNotBlank()) {
                            FarmNotificationManager.scheduleHealthReminder(
                                context = context,
                                goatTag = record.goatTag,
                                recordType = record.recordType,
                                treatmentName = record.treatment,
                                dueDateString = record.nextDueDate
                            )
                        }
                        onCloseAddHealthDialog()
                    }
                }
            }
        )
    }
}

@Composable
private fun BreedingListContent(
    breedingsState: UiState<List<BreedingRecord>>,
    onOpenAdd: () -> Unit,
    onUpdateStatus: (String, BreedingRecord, String, Long) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    var recordToUpdateOutcome by remember { mutableStateOf<BreedingRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<BreedingRecord?>(null) }
    var alertDispatchedMsg by remember { mutableStateOf<String?>(null) }

    when (breedingsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: ${breedingsState.message}", color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            val list = breedingsState.data
            if (list.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No breeding records yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Plan matings and track 150-day gestation schedules with automated kidding reminders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onOpenAdd) {
                        Text("Add Breeding Record")
                    }
                }
            } else {
            val upcomingKiddings = remember(list) {
                FarmNotificationManager.getUpcomingAlerts(emptyList(), list, daysAhead = 21)
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (upcomingKiddings.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("breeding_kidding_alerts_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Kidding Watchlist (${upcomingKiddings.size} Due Soon)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val sent = FarmNotificationManager.dispatchAlertNotifications(context, upcomingKiddings)
                                            alertDispatchedMsg = "Dispatched $sent kidding alert notification(s)!"
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("notify_kidding_alerts_button")
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Notify Me", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Text(
                                    text = "Pregnant does within 21 days of their 150-day gestation date:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF5D4037)
                                )

                                upcomingKiddings.forEach { alert ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Doe ${alert.goatTag}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Expected: ${alert.dueDate}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                color = when {
                                                    alert.isDueToday -> Color(0xFFFFCCBC)
                                                    alert.isOverdue -> Color(0xFFFFCDD2)
                                                    else -> Color(0xFFFFE0B2)
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = when {
                                                        alert.isDueToday -> "🍼 DUE TODAY"
                                                        alert.isOverdue -> "⚠️ OVERDUE"
                                                        else -> "In ${alert.daysRemaining}d"
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        alert.isDueToday -> Color(0xFFD84315)
                                                        alert.isOverdue -> Color(0xFFC62828)
                                                        else -> Color(0xFFE65100)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (alertDispatchedMsg != null) {
                                    Text(
                                        text = alertDispatchedMsg!!,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Gestation & Breeding Tracker (${list.size} records)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(list, key = { it.id.ifBlank { "${it.doeTag}_${it.breedingDate}" } }) { item ->
                    BreedingCycleCard(
                        record = item,
                        onMarkKidded = { recordToUpdateOutcome = item },
                        onMarkConfirmed = { onUpdateStatus(item.id, item, "Confirmed Pregnant", item.kidsBorn) },
                        onDelete = { recordToDelete = item }
                    )
                }
                }
            }
        }
    }

    if (recordToUpdateOutcome != null) {
        var kidsBornStr by remember { mutableStateOf("2") }
        var kiddingDate by remember {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            mutableStateOf(today)
        }
        AlertDialog(
            onDismissRequest = { recordToUpdateOutcome = null },
            title = { Text("Record Kidding Outcome") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Doe: ${recordToUpdateOutcome!!.doeTag} kidded!")
                    OutlinedTextField(
                        value = kidsBornStr,
                        onValueChange = { kidsBornStr = it },
                        label = { Text("Number of Kids Born") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = kiddingDate,
                        onValueChange = { kiddingDate = it },
                        label = { Text("Kidding Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val count = kidsBornStr.toLongOrNull() ?: 1L
                    val rec = recordToUpdateOutcome!!.copy(actualKiddingDate = kiddingDate)
                    recordToUpdateOutcome = null
                    onUpdateStatus(rec.id, rec, "Kidded", count)
                }) {
                    Text("Save Outcome")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToUpdateOutcome = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Breeding Record?") },
            text = { Text("Delete breeding entry for Doe ${recordToDelete!!.doeTag}?") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = recordToDelete!!.id
                        recordToDelete = null
                        onDelete(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BreedingCard(
    record: BreedingRecord,
    onMarkKidded: () -> Unit,
    onMarkConfirmed: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Doe: ${record.doeTag}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "x Sire: ${record.buckTag}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = when (record.status) {
                        "Kidded" -> Color(0xFFE8F5E9)
                        "Confirmed Pregnant" -> Color(0xFFFFF3E0)
                        else -> MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = record.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (record.status) {
                            "Kidded" -> Color(0xFF1B5E20)
                            "Confirmed Pregnant" -> Color(0xFFE65100)
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bred: ${record.breedingDate} • Expected: ${record.expectedKiddingDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (record.status == "Kidded" && record.kidsBorn > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🎉 Kidded: ${record.kidsBorn} kids born${if (record.actualKiddingDate.isNotBlank()) " on ${record.actualKiddingDate}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row
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
                        Text("Confirm Pregnant", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (record.status != "Kidded") {
                    Button(
                        onClick = onMarkKidded,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Log Kidding", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthListContent(
    healthState: UiState<List<HealthRecord>>,
    breedingState: UiState<List<BreedingRecord>>,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAdd: () -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    var recordToDelete by remember { mutableStateOf<HealthRecord?>(null) }
    var scanResultMessage by remember { mutableStateOf<String?>(null) }

    when (healthState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: ${healthState.message}", color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            val list = healthState.data
            val breedings = if (breedingState is UiState.Success) breedingState.data else emptyList()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Reminder Hub Card
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notification_reminder_hub_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Duration & Reminder Alerts",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Surface(
                                    color = if (hasNotificationPermission) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (hasNotificationPermission) "Enabled" else "Permission Required",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasNotificationPermission) Color(0xFF1B5E20) else Color(0xFFC62828)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "System notifications trigger automatically when vaccination boosters, deworming intervals (FAMACHA), hoof trims, or 150-day kidding dates complete their duration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!hasNotificationPermission) {
                                    Button(
                                        onClick = onRequestPermission,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.testTag("enable_notifications_button")
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Allow Notifications", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        FarmNotificationManager.sendImmediateNotification(
                                            context = context,
                                            title = "💉 Sample Vaccination Reminder",
                                            message = "Goat TAG-001 has completed its duration for CD&T booster. Scheduled care is due today!"
                                        )
                                        scanResultMessage = "Sample vaccination notification dispatched!"
                                    },
                                    modifier = Modifier.testTag("send_test_notification_button")
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Notification", style = MaterialTheme.typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val alerts = FarmNotificationManager.checkAndNotifyDueRecords(context, list, breedings)
                                        scanResultMessage = if (alerts > 0) {
                                            "Found and notified $alerts due farm care events!"
                                        } else {
                                            "All vaccination and deworming durations are up-to-date! No immediate alerts."
                                        }
                                    },
                                    modifier = Modifier.testTag("scan_due_notifications_button")
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Check Due Now", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            if (scanResultMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = scanResultMessage!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Monthly Health Events Frequency Bar Chart
                item {
                    MonthlyHealthEventsBarChart(
                        records = list,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (list.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No health records yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Record vaccinations, dewormings (FAMACHA), hoof trims, and treatments with automatic duration countdowns.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onOpenAdd) {
                                Text("Record Health Event")
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Medical & Care Logs (${list.size} events)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(list, key = { it.id.ifBlank { "${it.goatTag}_${it.treatment}_${it.administeredDate}" } }) { item ->
                        HealthCard(record = item, onDelete = { recordToDelete = item })
                    }
                }
            }
        }
    }

    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Health Record?") },
            text = { Text("Delete ${recordToDelete!!.treatment} record for ${recordToDelete!!.goatTag}?") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = recordToDelete!!.id
                        recordToDelete = null
                        onDelete(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HealthCard(
    record: HealthRecord,
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.treatment,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = record.recordType,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Target: ${record.goatTag} • Date: ${record.administeredDate}${if (record.dosage.isNotBlank()) " • Dose: ${record.dosage}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (record.nextDueDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Next Due: ${record.nextDueDate}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (record.notes.isNotBlank()) {
                    Text(
                        text = record.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Health Record",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun BreedingFormDialog(
    does: List<Goat>,
    bucks: List<Goat>,
    onDismiss: () -> Unit,
    onSave: (BreedingRecord) -> Unit
) {
    var doeTag by remember { mutableStateOf(if (does.isNotEmpty()) does.first().tag else "") }
    var buckTag by remember { mutableStateOf(if (bucks.isNotEmpty()) bucks.first().tag else "") }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var breedingDate by remember { mutableStateOf(today) }

    // Auto-compute 150 days gestation due date
    val initialDue = remember(breedingDate) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance().apply { time = sdf.parse(breedingDate) ?: Date() }
            cal.add(Calendar.DAY_OF_YEAR, 150)
            sdf.format(cal.time)
        } catch (e: Exception) {
            ""
        }
    }
    var expectedDate by remember { mutableStateOf(initialDue) }
    var notes by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Breeding Service", fontWeight = FontWeight.Bold) },
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

                // Doe Tag selector
                Text("Doe (Female) *", style = MaterialTheme.typography.labelMedium)
                if (does.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(does) { d ->
                            FilterChip(
                                selected = doeTag == d.tag,
                                onClick = { doeTag = d.tag },
                                label = { Text("${d.name} (${d.tag})") }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = doeTag,
                    onValueChange = { doeTag = it },
                    label = { Text("Doe Tag *") },
                    modifier = Modifier.fillMaxWidth().testTag("breeding_input_doe_tag"),
                    singleLine = true
                )

                // Buck Tag selector
                Text("Buck / Sire *", style = MaterialTheme.typography.labelMedium)
                if (bucks.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(bucks) { b ->
                            FilterChip(
                                selected = buckTag == b.tag,
                                onClick = { buckTag = b.tag },
                                label = { Text("${b.name} (${b.tag})") }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = buckTag,
                    onValueChange = { buckTag = it },
                    label = { Text("Buck Tag *") },
                    modifier = Modifier.fillMaxWidth().testTag("breeding_input_buck_tag"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = breedingDate,
                    onValueChange = {
                        breedingDate = it
                        try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val cal = Calendar.getInstance().apply { time = sdf.parse(it) ?: Date() }
                            cal.add(Calendar.DAY_OF_YEAR, 150)
                            expectedDate = sdf.format(cal.time)
                        } catch (e: Exception) { }
                    },
                    label = { Text("Breeding Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = expectedDate,
                    onValueChange = { expectedDate = it },
                    label = { Text("Expected Due Date (~150 Days)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("A 150-day kidding reminder will be scheduled for $expectedDate", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Mating Notes / Heat Observations") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (doeTag.isBlank() || buckTag.isBlank()) {
                        formError = "Please specify both Doe and Buck tags."
                        return@Button
                    }
                    onSave(
                        BreedingRecord(
                            doeTag = doeTag.trim(),
                            buckTag = buckTag.trim(),
                            breedingDate = breedingDate.trim(),
                            expectedKiddingDate = expectedDate.trim(),
                            status = "Bred",
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("breeding_save_button")
            ) {
                Text("Save & Schedule")
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
fun HealthFormDialog(
    goats: List<Goat>,
    onDismiss: () -> Unit,
    onSave: (HealthRecord) -> Unit
) {
    var goatTag by remember { mutableStateOf(if (goats.isNotEmpty()) goats.first().tag else "ALL") }
    var recordType by remember { mutableStateOf("Vaccination") }
    var treatment by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var administeredDate by remember { mutableStateOf(today) }
    var nextDueDate by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    fun applyDurationOffset(days: Int) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance().apply { time = sdf.parse(administeredDate) ?: Date() }
            cal.add(Calendar.DAY_OF_YEAR, days)
            nextDueDate = sdf.format(cal.time)
        } catch (e: Exception) { }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Health Treatment", fontWeight = FontWeight.Bold) },
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

                // Type selector
                Text("Record Type *", style = MaterialTheme.typography.labelMedium)
                val types = listOf("Vaccination", "Deworming", "Hoof Trimming", "Medication", "Checkup")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(types) { t ->
                        FilterChip(
                            selected = recordType == t,
                            onClick = {
                                recordType = t
                                when (t) {
                                    "Vaccination" -> {
                                        if (treatment.isBlank()) treatment = "CD&T Vaccine"
                                        applyDurationOffset(365) // 1 year booster default
                                    }
                                    "Deworming" -> {
                                        if (treatment.isBlank()) treatment = "Ivermectin / Valbazen"
                                        applyDurationOffset(30) // 30 day cycle
                                    }
                                    "Hoof Trimming" -> {
                                        if (treatment.isBlank()) treatment = "Hoof Trimming & Inspection"
                                        applyDurationOffset(60) // 60 days
                                    }
                                }
                            },
                            label = { Text(t) }
                        )
                    }
                }

                // Goat Target
                Text("Target Goat *", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = goatTag == "ALL",
                            onClick = { goatTag = "ALL" },
                            label = { Text("ALL (Whole Herd)") }
                        )
                    }
                    items(goats) { g ->
                        FilterChip(
                            selected = goatTag == g.tag,
                            onClick = { goatTag = g.tag },
                            label = { Text("${g.name} (${g.tag})") }
                        )
                    }
                }

                OutlinedTextField(
                    value = goatTag,
                    onValueChange = { goatTag = it },
                    label = { Text("Or Type Ear Tag") },
                    modifier = Modifier.fillMaxWidth().testTag("health_input_goat_tag"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = treatment,
                    onValueChange = { treatment = it },
                    label = { Text("Treatment / Medicine Name * (e.g. CD&T, Ivermectin)") },
                    modifier = Modifier.fillMaxWidth().testTag("health_input_treatment"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage / Administration (e.g. 2ml Sub-Q)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = administeredDate,
                    onValueChange = { administeredDate = it },
                    label = { Text("Date Administered (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = nextDueDate,
                    onValueChange = { nextDueDate = it },
                    label = { Text("Next Booster / Follow-up Due Date") },
                    modifier = Modifier.fillMaxWidth().testTag("health_input_next_due"),
                    singleLine = true
                )

                // Duration Calculation Presets
                Text("Quick Duration Presets:", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        OutlinedButton(
                            onClick = { applyDurationOffset(21) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+21 Days (Booster)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { applyDurationOffset(30) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+30 Days (Dewormer)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { applyDurationOffset(60) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+60 Days (Hooves)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { applyDurationOffset(365) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+1 Year (Annual)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (nextDueDate.isNotBlank()) "Notification scheduled for: $nextDueDate" else "Set due date to schedule notification",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("Cost ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (FAMACHA score, milk withdrawal, vet notes)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (goatTag.isBlank()) {
                        formError = "Please select or enter a goat tag."
                        return@Button
                    }
                    if (treatment.isBlank()) {
                        formError = "Please enter the treatment or vaccine name."
                        return@Button
                    }
                    val cost = costStr.toDoubleOrNull() ?: 0.0
                    onSave(
                        HealthRecord(
                            goatTag = goatTag.trim(),
                            recordType = recordType,
                            treatment = treatment.trim(),
                            dosage = dosage.trim(),
                            administeredDate = administeredDate.trim(),
                            nextDueDate = nextDueDate.trim(),
                            cost = cost,
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("health_save_button")
            ) {
                Text("Save & Set Reminder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
