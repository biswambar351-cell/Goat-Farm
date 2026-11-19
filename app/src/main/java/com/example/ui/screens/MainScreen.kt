package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.BoxWithConstraints
import com.example.R
import com.example.ui.auth.GoogleSignInHelper
import com.example.ui.components.EditLogoDialog
import com.example.ui.components.FarmLogoView
import com.example.ui.viewmodel.GoatFarmViewModel
import com.example.ui.viewmodel.UiState
import com.example.notification.FarmNotificationManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GoatFarmViewModel,
    onSignOutComplete: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    // Dialog state controllers that can be opened from Dashboard quick actions or specific screens
    var isAddGoatOpen by remember { mutableStateOf(false) }
    var isLogMilkOpen by remember { mutableStateOf(false) }
    var isAddBreedingOpen by remember { mutableStateOf(false) }
    var isAddHealthOpen by remember { mutableStateOf(false) }
    var isAddTransactionOpen by remember { mutableStateOf(false) }
    var showAiAdvisorDialog by remember { mutableStateOf(false) }
    var showEditLogoDialog by remember { mutableStateOf(false) }

    // Listen to ViewModel user messages (toasts/snackbars)
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val healthRecordsState by viewModel.healthRecords.collectAsStateWithLifecycle()
    val breedingsState by viewModel.breedings.collectAsStateWithLifecycle()
    var hasAutoCheckedAlerts by remember { mutableStateOf(false) }

    LaunchedEffect(healthRecordsState, breedingsState) {
        if (!hasAutoCheckedAlerts && healthRecordsState is UiState.Success && breedingsState is UiState.Success) {
            hasAutoCheckedAlerts = true
            val healthList = (healthRecordsState as UiState.Success).data
            val breedingList = (breedingsState as UiState.Success).data
            val urgentAlerts = FarmNotificationManager.getUpcomingAlerts(healthList, breedingList, daysAhead = 2)
                .filter { it.isDueToday || it.isOverdue }
            if (urgentAlerts.isNotEmpty()) {
                FarmNotificationManager.dispatchAlertNotifications(context, urgentAlerts)
            }
        }
    }

    val currentUser = Firebase.auth.currentUser
    val userName = currentUser?.displayName ?: currentUser?.email?.substringBefore('@') ?: "Farmer"

    val farmProfile by viewModel.farmProfile.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth > 700.dp

        Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FarmLogoView(
                            profile = farmProfile,
                            size = 34.dp,
                            showEditBadge = false,
                            onClick = { showEditLogoDialog = true },
                            modifier = Modifier.testTag("topbar_farm_logo_button")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = farmProfile.farmName.ifBlank { "Khulana Goat Farm" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = { showAiAdvisorDialog = true },
                        modifier = Modifier.testTag("ai_advisor_topbar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Suggestions for Disease & Health",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = { showSignOutConfirm = true },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (!isExpanded) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Overview") },
                        modifier = Modifier.testTag("nav_item_dashboard")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Pets, contentDescription = "Herd") },
                        label = { Text("Herd") },
                        modifier = Modifier.testTag("nav_item_herd")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.LocalDrink, contentDescription = "Milking") },
                        label = { Text("Milk") },
                        modifier = Modifier.testTag("nav_item_milk")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Breeding & Health") },
                        label = { Text("Breeding") },
                        modifier = Modifier.testTag("nav_item_breeding")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Payments, contentDescription = "Finances") },
                        label = { Text("Finance") },
                        modifier = Modifier.testTag("nav_item_finance")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Adaptive Side Navigation Rail for tablets/wide screens
            if (isExpanded) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationRailItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Overview") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Pets, contentDescription = "Herd") },
                        label = { Text("Herd") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.LocalDrink, contentDescription = "Milking") },
                        label = { Text("Milk") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Breeding & Health") },
                        label = { Text("Breeding") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Payments, contentDescription = "Finances") },
                        label = { Text("Finance") }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToHerd = { selectedTab = 1 },
                        onNavigateToMilk = { selectedTab = 2 },
                        onNavigateToBreeding = { selectedTab = 3 },
                        onNavigateToFinance = { selectedTab = 4 },
                        onOpenAddGoat = { isAddGoatOpen = true },
                        onOpenLogMilk = { isLogMilkOpen = true },
                        onOpenAddBreeding = { isAddBreedingOpen = true },
                        onOpenAddHealth = { isAddHealthOpen = true },
                        onOpenAiAdvisor = { showAiAdvisorDialog = true },
                        onOpenEditLogo = { showEditLogoDialog = true }
                    )
                    1 -> HerdScreen(
                        viewModel = viewModel,
                        isAddDialogOpen = isAddGoatOpen,
                        onCloseAddDialog = { isAddGoatOpen = false },
                        onOpenAddDialog = { isAddGoatOpen = true }
                    )
                    2 -> MilkScreen(
                        viewModel = viewModel,
                        isAddDialogOpen = isLogMilkOpen,
                        onCloseAddDialog = { isLogMilkOpen = false },
                        onOpenAddDialog = { isLogMilkOpen = true }
                    )
                    3 -> BreedingHealthScreen(
                        viewModel = viewModel,
                        isAddBreedingDialogOpen = isAddBreedingOpen,
                        onCloseAddBreedingDialog = { isAddBreedingOpen = false },
                        onOpenAddBreedingDialog = { isAddBreedingOpen = true },
                        isAddHealthDialogOpen = isAddHealthOpen,
                        onCloseAddHealthDialog = { isAddHealthOpen = false },
                        onOpenAddHealthDialog = { isAddHealthOpen = true }
                    )
                    4 -> FinanceScreen(
                        viewModel = viewModel,
                        isAddDialogOpen = isAddTransactionOpen,
                        onCloseAddDialog = { isAddTransactionOpen = false },
                        onOpenAddDialog = { isAddTransactionOpen = true }
                    )
                }
            }
        }
    }
    }

    // Sign out confirmation dialog
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out of $userName's Khulana Goat Farm account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        GoogleSignInHelper.signOut(
                            credentialManager = credentialManager,
                            scope = coroutineScope,
                            onSignOutComplete = onSignOutComplete
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Disease & Health Suggestions Dialog
    if (showAiAdvisorDialog) {
        val goatsState by viewModel.goats.collectAsStateWithLifecycle()
        val allGoats = (goatsState as? UiState.Success)?.data ?: emptyList()
        val aiViewModel: com.example.ui.viewmodel.AiHealthAdvisorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAiAdvisorDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Health & Disease Advisor",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showAiAdvisorDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    AiHealthAdvisorScreen(
                        aiViewModel = aiViewModel,
                        goats = allGoats,
                        onLogTreatmentToHealth = { record ->
                            viewModel.addHealthRecord(record) { success ->
                                if (success) {
                                    showAiAdvisorDialog = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Editable Farm Logo Dialog
    if (showEditLogoDialog) {
        EditLogoDialog(
            currentProfile = farmProfile,
            onDismiss = { showEditLogoDialog = false },
            onSave = { updatedProfile ->
                viewModel.updateFarmProfile(updatedProfile) { success ->
                    if (success) {
                        showEditLogoDialog = false
                    }
                }
            }
        )
    }
}
