package com.example.ui.screens

import com.example.ui.components.MonthlyHealthEventsBarChart

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BreedingRecord
import com.example.data.model.FarmProfile
import com.example.data.model.FinanceRecord
import com.example.data.model.Goat
import com.example.data.model.HealthRecord
import com.example.data.model.MilkLog
import com.example.notification.FarmNotificationManager
import com.example.ui.components.EditLogoDialog
import com.example.ui.components.FarmLogoView
import com.example.ui.viewmodel.GoatFarmViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.CsvExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: GoatFarmViewModel,
    onNavigateToHerd: () -> Unit,
    onNavigateToMilk: () -> Unit,
    onNavigateToBreeding: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onOpenAddGoat: () -> Unit,
    onOpenLogMilk: () -> Unit,
    onOpenAddBreeding: () -> Unit,
    onOpenAddHealth: () -> Unit,
    onOpenAiAdvisor: () -> Unit = {},
    onOpenEditLogo: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val farmProfile by viewModel.farmProfile.collectAsStateWithLifecycle()
    val breedingsState by viewModel.breedings.collectAsStateWithLifecycle()
    val healthState by viewModel.healthRecords.collectAsStateWithLifecycle()
    val goatsState by viewModel.goats.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactions.collectAsStateWithLifecycle()
    val milkLogsState by viewModel.milkLogs.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLocalLogoDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var goatSearchQuery by remember { mutableStateOf("") }
    var selectedGoatForDetail by remember { mutableStateOf<Goat?>(null) }
    val triggerEditLogo: () -> Unit = {
        if (onOpenEditLogo != null) {
            onOpenEditLogo()
        } else {
            showLocalLogoDialog = true
        }
    }

    // CSV storage export launcher
    var pendingCsvContent by remember { mutableStateOf("") }
    var pendingFilename by remember { mutableStateOf("farm_export.csv") }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && pendingCsvContent.isNotBlank()) {
            val success = CsvExportHelper.writeCsvToUri(context, uri, pendingCsvContent)
            if (success) {
                Toast.makeText(context, "Exported successfully to device storage!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to write CSV file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission launcher for system notifications (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val healthList = (healthState as? UiState.Success)?.data ?: emptyList()
            val breedingList = (breedingsState as? UiState.Success)?.data ?: emptyList()
            val count = FarmNotificationManager.checkAndNotifyDueRecords(context, healthList, breedingList)
            Toast.makeText(context, "Notification permission granted! Sent $count alert(s).", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    // Prepare lists
    val allGoats = (goatsState as? UiState.Success)?.data ?: emptyList()
    val allHealth = (healthState as? UiState.Success)?.data ?: emptyList()
    val allBreedings = (breedingsState as? UiState.Success)?.data ?: emptyList()
    val allTransactions = (transactionsState as? UiState.Success)?.data ?: emptyList()
    val allMilkLogs = (milkLogsState as? UiState.Success)?.data ?: emptyList()

    // Upcoming Alerts calculation
    val upcomingAlerts = remember(allHealth, allBreedings) {
        FarmNotificationManager.getUpcomingAlerts(allHealth, allBreedings, daysAhead = 14)
    }

    var selectedGenderFilter by remember { mutableStateOf("All") }

    // Filter goats based on search query (Name or ID) and quick filter chips
    val matchingGoats = remember(goatSearchQuery, selectedGenderFilter, allGoats) {
        var baseList = allGoats
        if (selectedGenderFilter == "Does") {
            baseList = baseList.filter { it.gender.equals("Doe", ignoreCase = true) || it.gender.equals("Female", ignoreCase = true) }
        } else if (selectedGenderFilter == "Bucks") {
            baseList = baseList.filter { it.gender.equals("Buck", ignoreCase = true) || it.gender.equals("Male", ignoreCase = true) }
        }

        if (goatSearchQuery.isBlank()) {
            if (selectedGenderFilter != "All") baseList else emptyList()
        } else {
            val q = goatSearchQuery.trim()
            baseList.filter { goat ->
                goat.name.contains(q, ignoreCase = true) ||
                goat.tag.contains(q, ignoreCase = true) ||
                goat.breed.contains(q, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Search Bar at the Top of Herd Dashboard
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_goat_search_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = goatSearchQuery,
                        onValueChange = { goatSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_goat_search_input"),
                        placeholder = {
                            Text(
                                text = "Search goats by name or ID (e.g. Bella, GT-001)...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Goats by Name or ID",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (goatSearchQuery.isNotBlank()) {
                                IconButton(onClick = { goatSearchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Quick category filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Filter:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = selectedGenderFilter == "All",
                            onClick = { selectedGenderFilter = "All" },
                            label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("search_filter_all")
                        )
                        FilterChip(
                            selected = selectedGenderFilter == "Does",
                            onClick = {
                                selectedGenderFilter = if (selectedGenderFilter == "Does") "All" else "Does"
                            },
                            label = { Text("Does ♀", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("search_filter_does")
                        )
                        FilterChip(
                            selected = selectedGenderFilter == "Bucks",
                            onClick = {
                                selectedGenderFilter = if (selectedGenderFilter == "Bucks") "All" else "Bucks"
                            },
                            label = { Text("Bucks ♂", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("search_filter_bucks")
                        )
                    }
                }
            }
        }

        // 1.1 Search Results (Displayed dynamically when search bar or filter is active)
        if (goatSearchQuery.isNotBlank() || selectedGenderFilter != "All") {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_goat_search_results"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Search Results (${matchingGoats.size} found)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    goatSearchQuery = ""
                                    selectedGenderFilter = "All"
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (matchingGoats.isEmpty()) {
                            Text(
                                text = if (goatSearchQuery.isNotBlank())
                                    "No goats found matching \"$goatSearchQuery\". Try searching by name (e.g. Bella) or ID (e.g. GT-001)."
                                else
                                    "No goats found in this category.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            matchingGoats.forEach { goat ->
                                SearchResultGoatItem(
                                    goat = goat,
                                    onClick = { selectedGoatForDetail = goat }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Customizable Farm Banner with Editable Name & Address
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_banner_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        FarmLogoView(
                            profile = farmProfile,
                            size = 56.dp,
                            showEditBadge = true,
                            onClick = triggerEditLogo,
                            modifier = Modifier.testTag("dashboard_farm_logo_button")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = farmProfile.farmName.ifBlank { "Khulana Goat Farm" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { showEditProfileDialog = true },
                                    modifier = Modifier.size(28.dp).testTag("edit_farm_profile_icon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Farm Name & Address",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Address row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = farmProfile.address.ifBlank { "Khulna, Bangladesh" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (farmProfile.ownerName.isNotBlank()) {
                                Text(
                                    text = "Owner: ${farmProfile.ownerName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                )
                            }
                        }

                        // App Ownership Badge / Edit Profile Action
                        OutlinedButton(
                            onClick = { showEditProfileDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("edit_farm_button")
                        ) {
                            Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Own This App", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val currentDate = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date())
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // 3. Notification System Alerts & Reminders Card (Breeding Dates & Health Check-ups)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_alerts_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (upcomingAlerts.any { it.isDueToday || it.isOverdue }) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (upcomingAlerts.any { it.isDueToday || it.isOverdue }) Color(0xFFFFE0B2) else Color(0xFFE8F5E9)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (upcomingAlerts.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = "Alerts",
                                    tint = if (upcomingAlerts.any { it.isDueToday || it.isOverdue }) Color(0xFFE65100) else Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Upcoming Alerts & Reminders",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${upcomingAlerts.size} events due within 14 days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Send Push Notification / Test Alerts Button
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val perm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                                    if (perm != PackageManager.PERMISSION_GRANTED) {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        return@Button
                                    }
                                }
                                val sentCount = FarmNotificationManager.dispatchAlertNotifications(context, upcomingAlerts)
                                if (sentCount > 0) {
                                    Toast.makeText(context, "Dispatched $sentCount alert notification(s) to status bar!", Toast.LENGTH_SHORT).show()
                                } else {
                                    FarmNotificationManager.sendImmediateNotification(
                                        context,
                                        "🔔 Herd Reminders Active",
                                        "All breeding deliveries and health treatments are up to date!"
                                    )
                                    Toast.makeText(context, "All reminders up to date! Test alert sent.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("send_alert_notifications_button")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Notify Me", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (upcomingAlerts.isEmpty()) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✅ All clear! No pending health check-ups or kidding deliveries in the next 14 days.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            upcomingAlerts.take(4).forEach { alert ->
                                AlertItemRow(alert = alert, onGoatClick = {
                                    val match = allGoats.firstOrNull { g -> g.tag.equals(alert.goatTag, ignoreCase = true) }
                                    if (match != null) selectedGoatForDetail = match else onNavigateToBreeding()
                                })
                            }

                            if (upcomingAlerts.size > 4) {
                                TextButton(
                                    onClick = onNavigateToBreeding,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("View all ${upcomingAlerts.size} alerts →", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Primary Summary Statistics Section (Total Goat Count, Active Health Alerts, Current Month's Milk)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Herd Summary Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Stat 1: Total Goat Count
                    PrimaryStatCard(
                        modifier = Modifier.weight(1f),
                        value = "${summary.totalGoats}",
                        title = "Total Goats",
                        subtext = "${summary.doeCount} Does • ${summary.buckCount} Bucks",
                        icon = Icons.Default.Pets,
                        cardColor = MaterialTheme.colorScheme.surface,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToHerd,
                        testTag = "stat_total_goats"
                    )

                    // Stat 2: Active Health Alerts
                    val hasAlerts = summary.activeHealthAlerts > 0
                    PrimaryStatCard(
                        modifier = Modifier.weight(1f),
                        value = "${summary.activeHealthAlerts}",
                        title = "Health Alerts",
                        subtext = if (hasAlerts) "${summary.activeHealthAlerts} due within 7d" else "All up to date",
                        icon = if (hasAlerts) Icons.Default.Warning else Icons.Default.MedicalServices,
                        cardColor = if (hasAlerts) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                        accentColor = if (hasAlerts) Color(0xFFE65100) else Color(0xFF2E7D32),
                        onClick = onNavigateToBreeding,
                        testTag = "stat_health_alerts"
                    )
                }

                // Stat 3: Current Month's Total Milk Production
                val monthName = summary.currentMonthName.ifBlank { "This Month" }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToMilk)
                        .testTag("stat_monthly_milk"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE1F5FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalDrink,
                                contentDescription = null,
                                tint = Color(0xFF0288D1),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$monthName Total Milk",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "%.1f".format(summary.currentMonthMilkLiters),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0288D1)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Liters",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                    color = Color(0xFF0288D1)
                                )
                            }
                            Text(
                                text = "Today's Yield: %.1f L • %d lactating does".format(summary.todayMilkLiters, summary.lactatingCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onOpenLogMilk,
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 5. Quick Action Buttons Row (Includes Export CSV)
        item {
            Column {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ActionChipButton(
                            text = "+ Goat",
                            icon = Icons.Default.Pets,
                            onClick = onOpenAddGoat,
                            testTag = "quick_action_add_goat"
                        )
                    }
                    item {
                        ActionChipButton(
                            text = "+ Milking",
                            icon = Icons.Default.LocalDrink,
                            onClick = onOpenLogMilk,
                            testTag = "quick_action_log_milk"
                        )
                    }
                    item {
                        ActionChipButton(
                            text = "+ Breeding",
                            icon = Icons.Default.CalendarMonth,
                            onClick = onOpenAddBreeding,
                            testTag = "quick_action_add_breeding"
                        )
                    }
                    item {
                        ActionChipButton(
                            text = "+ Treatment",
                            icon = Icons.Default.MedicalServices,
                            onClick = onOpenAddHealth,
                            testTag = "quick_action_add_health"
                        )
                    }
                    item {
                        ActionChipButton(
                            text = "AI Vet Suggestions",
                            icon = Icons.Default.AutoAwesome,
                            onClick = onOpenAiAdvisor,
                            testTag = "quick_action_ai_advisor"
                        )
                    }
                    item {
                        ActionChipButton(
                            text = "Export (CSV)",
                            icon = Icons.Default.FileDownload,
                            onClick = { showExportDialog = true },
                            testTag = "quick_action_export_csv"
                        )
                    }
                    item {
                        ActionChipButton(
                            text = "Farm Profile",
                            icon = Icons.Default.Home,
                            onClick = { showEditProfileDialog = true },
                            testTag = "quick_action_farm_profile"
                        )
                    }
                }
            }
        }

        // AI Disease, Health & Care Advisor Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAiAdvisor() }
                    .testTag("dashboard_ai_advisor_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Vet, Medicine & Tonic Finder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Search best medicines, tonics & supplements, check goat weight or upload photos for instant vet care guidance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open AI Advisor",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 6. CSV Export Card (Allows farmer to backup financial, health, and milk data)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_export_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export CSV",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Export Farm Data (CSV)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Download financial, health & milk records to storage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { showExportDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("export_farm_data_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }
                }
            }
        }

        // 7. Herd Demographics Breakdown
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Herd Demographics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DemographicCard(
                        modifier = Modifier.weight(1f),
                        title = "Does",
                        count = summary.doeCount,
                        subtext = "${summary.lactatingCount} milking",
                        color = Color(0xFFE8F5E9),
                        textColor = Color(0xFF1B5E20),
                        onClick = onNavigateToHerd
                    )
                    DemographicCard(
                        modifier = Modifier.weight(1f),
                        title = "Bucks",
                        count = summary.buckCount,
                        subtext = "Breeding sires",
                        color = Color(0xFFFFF3E0),
                        textColor = Color(0xFFE65100),
                        onClick = onNavigateToHerd
                    )
                    DemographicCard(
                        modifier = Modifier.weight(1f),
                        title = "Kids & Wethers",
                        count = summary.kidCount,
                        subtext = "Growing herd",
                        color = Color(0xFFE3F2FD),
                        textColor = Color(0xFF0D47A1),
                        onClick = onNavigateToHerd
                    )
                }
            }
        }

        // Monthly Health Events Frequency Bar Chart
        item {
            MonthlyHealthEventsBarChart(
                records = allHealth,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // 8. Financial Margin Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToFinance),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Financial Snapshot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$%.2f".format(summary.totalIncome), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Column {
                            Text("Total Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$%.2f".format(summary.totalExpense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                        Column {
                            Text("Net Margin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val net = summary.netProfit
                            Text(
                                "$%.2f".format(net),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (net >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }
    }

    // Export Farm Data Dialog (CSV Export to Device Storage)
    if (showExportDialog) {
        ExportFarmDataDialog(
            farmName = farmProfile.farmName.ifBlank { "Khulana Goat Farm" },
            finances = allTransactions,
            healthRecords = allHealth,
            milkLogs = allMilkLogs,
            goats = allGoats,
            breedings = allBreedings,
            onDismiss = { showExportDialog = false },
            onExportToStorage = { filename, content ->
                pendingFilename = filename
                pendingCsvContent = content
                createDocumentLauncher.launch(filename)
                showExportDialog = false
            },
            onShare = { filename, content ->
                val intent = CsvExportHelper.createShareIntent(context, filename, content)
                if (intent != null) {
                    context.startActivity(Intent.createChooser(intent, "Share Farm Data CSV"))
                } else {
                    Toast.makeText(context, "Could not open share sheet", Toast.LENGTH_SHORT).show()
                }
                showExportDialog = false
            }
        )
    }

    // Edit Farm Profile Dialog (Allows any farmer to own and customize the app)
    if (showEditProfileDialog) {
        EditFarmProfileDialog(
            currentProfile = farmProfile,
            onDismiss = { showEditProfileDialog = false },
            onEditLogo = {
                showEditProfileDialog = false
                triggerEditLogo()
            },
            onSave = { updatedProfile ->
                viewModel.updateFarmProfile(updatedProfile) { success ->
                    if (success) showEditProfileDialog = false
                }
            }
        )
    }

    if (showLocalLogoDialog) {
        EditLogoDialog(
            currentProfile = farmProfile,
            onDismiss = { showLocalLogoDialog = false },
            onSave = { updatedProfile ->
                viewModel.updateFarmProfile(updatedProfile) { success ->
                    if (success) showLocalLogoDialog = false
                }
            }
        )
    }

    // Quick Goat Profile Detail Dialog (From Search Results)
    if (selectedGoatForDetail != null) {
        QuickGoatDetailDialog(
            goat = selectedGoatForDetail!!,
            onDismiss = { selectedGoatForDetail = null },
            onViewInHerd = {
                selectedGoatForDetail = null
                onNavigateToHerd()
            }
        )
    }
}

/**
 * Renders an individual goat item in the search results card.
 */
@Composable
private fun SearchResultGoatItem(
    goat: Goat,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("search_result_item_${goat.tag}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (goat.gender.equals("Doe", true)) Color(0xFFF1F8E9) else Color(0xFFFFF8E1)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (goat.gender.equals("Doe", true)) Icons.Default.Female else Icons.Default.Male,
                        contentDescription = goat.gender,
                        tint = if (goat.gender.equals("Doe", true)) Color(0xFF558B2F) else Color(0xFFF57F17),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = goat.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ID: ${goat.tag}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = "${goat.breed} • ${goat.gender} • ${goat.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(onClick = onClick) {
                Text("View Profile", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Notification alert row displaying an upcoming breeding delivery or health check-up.
 */
@Composable
private fun AlertItemRow(
    alert: FarmNotificationManager.FarmAlert,
    onGoatClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onGoatClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (alert.type == FarmNotificationManager.FarmAlert.AlertType.HEALTH_CHECKUP) {
                        Icons.Default.MedicalServices
                    } else {
                        Icons.Default.Favorite
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (alert.isOverdue) Color(0xFFC62828) else if (alert.isDueToday) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = alert.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = when {
                    alert.isOverdue -> Color(0xFFFFEBEE)
                    alert.isDueToday -> Color(0xFFFFF3E0)
                    else -> Color(0xFFE3F2FD)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when {
                        alert.isOverdue -> "Overdue"
                        alert.isDueToday -> "Today"
                        else -> "In ${alert.daysRemaining}d"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        alert.isOverdue -> Color(0xFFC62828)
                        alert.isDueToday -> Color(0xFFE65100)
                        else -> Color(0xFF0277BD)
                    }
                )
            }
        }
    }
}

/**
 * Dialog allowing the farmer to export financial, health, and milk production data as CSV to device storage.
 */
@Composable
fun ExportFarmDataDialog(
    farmName: String,
    finances: List<FinanceRecord>,
    healthRecords: List<HealthRecord>,
    milkLogs: List<MilkLog>,
    goats: List<Goat>,
    breedings: List<BreedingRecord>,
    onDismiss: () -> Unit,
    onExportToStorage: (filename: String, content: String) -> Unit,
    onShare: (filename: String, content: String) -> Unit
) {
    val dateStamp = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var selectedExportCategory by remember { mutableStateOf("ALL") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Farm Data (CSV)", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select data category to export as CSV to your device storage:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Category 1: Comprehensive Farm Data
                ExportOptionCard(
                    title = "All Farm Records (Complete Backup)",
                    subtitle = "Finances (${finances.size}), Health (${healthRecords.size}), Milk (${milkLogs.size}), Herd (${goats.size})",
                    isSelected = selectedExportCategory == "ALL",
                    onClick = { selectedExportCategory = "ALL" }
                )

                // Category 2: Financial Data
                ExportOptionCard(
                    title = "Financial Records (CSV)",
                    subtitle = "${finances.size} daily expense and income records",
                    isSelected = selectedExportCategory == "FINANCES",
                    onClick = { selectedExportCategory = "FINANCES" }
                )

                // Category 3: Health & Treatments
                ExportOptionCard(
                    title = "Health & Medical Logs (CSV)",
                    subtitle = "${healthRecords.size} vaccination, deworming, and treatment logs",
                    isSelected = selectedExportCategory == "HEALTH",
                    onClick = { selectedExportCategory = "HEALTH" }
                )

                // Category 4: Milk Production
                ExportOptionCard(
                    title = "Milk Production Logs (CSV)",
                    subtitle = "${milkLogs.size} milking yield logs",
                    isSelected = selectedExportCategory == "MILK",
                    onClick = { selectedExportCategory = "MILK" }
                )

                // Category 5: Breeding Records
                ExportOptionCard(
                    title = "Breeding & Gestation Records (CSV)",
                    subtitle = "${breedings.size} breeding events & kidding countdowns",
                    isSelected = selectedExportCategory == "BREEDING",
                    onClick = { selectedExportCategory = "BREEDING" }
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val (filename, content) = prepareExportPayload(
                            category = selectedExportCategory,
                            dateStamp = dateStamp,
                            farmName = farmName,
                            finances = finances,
                            health = healthRecords,
                            milk = milkLogs,
                            goats = goats,
                            breedings = breedings
                        )
                        onShare(filename, content)
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }

                Button(
                    onClick = {
                        val (filename, content) = prepareExportPayload(
                            category = selectedExportCategory,
                            dateStamp = dateStamp,
                            farmName = farmName,
                            finances = finances,
                            health = healthRecords,
                            milk = milkLogs,
                            goats = goats,
                            breedings = breedings
                        )
                        onExportToStorage(filename, content)
                    },
                    modifier = Modifier.testTag("confirm_export_csv_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save to Device")
                }
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
private fun ExportOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun prepareExportPayload(
    category: String,
    dateStamp: String,
    farmName: String,
    finances: List<FinanceRecord>,
    health: List<HealthRecord>,
    milk: List<MilkLog>,
    goats: List<Goat>,
    breedings: List<BreedingRecord>
): Pair<String, String> {
    return when (category) {
        "FINANCES" -> "farm_finances_$dateStamp.csv" to CsvExportHelper.exportFinancesCsv(finances)
        "HEALTH" -> "farm_health_records_$dateStamp.csv" to CsvExportHelper.exportHealthLogsCsv(health)
        "MILK" -> "farm_milk_production_$dateStamp.csv" to CsvExportHelper.exportMilkProductionCsv(milk)
        "BREEDING" -> "farm_breeding_records_$dateStamp.csv" to CsvExportHelper.exportBreedingRecordsCsv(breedings)
        else -> "farm_all_records_$dateStamp.csv" to CsvExportHelper.exportComprehensiveFarmDataCsv(
            farmName = farmName,
            finances = finances,
            health = health,
            milk = milk,
            goats = goats,
            breedings = breedings
        )
    }
}

/**
 * Quick profile dialog when tapping a search result goat.
 */
@Composable
private fun QuickGoatDetailDialog(
    goat: Goat,
    onDismiss: () -> Unit,
    onViewInHerd: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${goat.name} (${goat.tag})", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileDetailRow(label = "Tag ID", value = goat.tag)
                ProfileDetailRow(label = "Breed", value = goat.breed)
                ProfileDetailRow(label = "Gender", value = goat.gender)
                ProfileDetailRow(label = "Status", value = goat.status)
                if (goat.birthDate.isNotBlank()) ProfileDetailRow(label = "Birth Date", value = goat.birthDate)
                if (goat.weightKg > 0.0) ProfileDetailRow(label = "Weight", value = "%.1f kg".format(goat.weightKg))
                if (goat.sireTag.isNotBlank()) ProfileDetailRow(label = "Sire (Father)", value = goat.sireTag)
                if (goat.damTag.isNotBlank()) ProfileDetailRow(label = "Dam (Mother)", value = goat.damTag)
                if (goat.notes.isNotBlank()) ProfileDetailRow(label = "Notes", value = goat.notes)
            }
        },
        confirmButton = {
            Button(onClick = onViewInHerd) {
                Text("View in Herd")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PrimaryStatCard(
    modifier: Modifier,
    value: String,
    title: String,
    subtext: String,
    icon: ImageVector,
    cardColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionChipButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = Modifier.testTag(testTag)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DemographicCard(
    modifier: Modifier,
    title: String,
    count: Int,
    subtext: String,
    color: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun EditFarmProfileDialog(
    currentProfile: FarmProfile,
    onDismiss: () -> Unit,
    onEditLogo: () -> Unit = {},
    onSave: (FarmProfile) -> Unit
) {
    var farmName by remember { mutableStateOf(currentProfile.farmName) }
    var address by remember { mutableStateOf(currentProfile.address) }
    var ownerName by remember { mutableStateOf(currentProfile.ownerName) }
    var phone by remember { mutableStateOf(currentProfile.phone) }
    var notes by remember { mutableStateOf(currentProfile.notes) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "Customize Farm Profile", fontWeight = FontWeight.Bold)
                Text(
                    text = "Set your own farm name and address to own this app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FarmLogoView(
                                profile = currentProfile,
                                size = 44.dp,
                                showEditBadge = false
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Farm Logo",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Custom emblem or photo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onEditLogo,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("profile_dialog_edit_logo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Logo", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = farmName,
                    onValueChange = { farmName = it },
                    label = { Text("Farm Name *") },
                    placeholder = { Text("e.g., Khulana Goat Farm, Green Pastures Farm") },
                    modifier = Modifier.fillMaxWidth().testTag("input_farm_name"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null)
                    }
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Farm Address / Location *") },
                    placeholder = { Text("e.g., Jalma, Batiaghata, Khulna") },
                    modifier = Modifier.fillMaxWidth().testTag("input_farm_address"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                    }
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Farmer / Owner Name") },
                    placeholder = { Text("e.g., Biswambar") },
                    modifier = Modifier.fillMaxWidth().testTag("input_farm_owner"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    }
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone") },
                    placeholder = { Text("e.g., +880 1700-000000") },
                    modifier = Modifier.fillMaxWidth().testTag("input_farm_phone"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null)
                    }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Farm Description / Slogan") },
                    placeholder = { Text("e.g., Premium dairy and meat goat breeding") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (farmName.isBlank()) {
                        errorMsg = "Please enter a farm name."
                        return@Button
                    }
                    if (address.isBlank()) {
                        errorMsg = "Please enter the farm address."
                        return@Button
                    }
                    onSave(
                        currentProfile.copy(
                            farmName = farmName.trim(),
                            address = address.trim(),
                            ownerName = ownerName.trim(),
                            phone = phone.trim(),
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_farm_profile_button")
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
