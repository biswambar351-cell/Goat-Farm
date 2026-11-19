package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.ui.platform.LocalContext
import com.example.util.CsvExportHelper
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FarmTransaction
import com.example.data.model.FinanceRecord
import com.example.ui.viewmodel.GoatFarmViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Monthly Financial Summary View for tracking daily expenses (feed, veterinary, utilities)
 * and income (milk sales, livestock sales) using the [FinanceRecord] entity.
 */
@Composable
fun FinanceScreen(
    viewModel: GoatFarmViewModel,
    isAddDialogOpen: Boolean,
    onCloseAddDialog: () -> Unit,
    onOpenAddDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transState by viewModel.transactions.collectAsStateWithLifecycle()

    val currentMonthKey = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }
    // "yyyy-MM" string (e.g., "2026-11") or "ALL"
    var selectedMonth by remember { mutableStateOf(currentMonthKey) }
    var filterType by remember { mutableStateOf("All") } // All, Income, Expense
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var transToDelete by remember { mutableStateOf<FinanceRecord?>(null) }

    val context = LocalContext.current
    var pendingCsvContent by remember { mutableStateOf("") }
    val createFinanceDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && pendingCsvContent.isNotBlank()) {
            val ok = CsvExportHelper.writeCsvToUri(context, uri, pendingCsvContent)
            if (ok) {
                Toast.makeText(context, "Exported financial records to device storage!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to write CSV file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = transState) {
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
                    val allRecords: List<FinanceRecord> = state.data

                    // Filter by selected month
                    val monthRecords = if (selectedMonth == "ALL") {
                        allRecords
                    } else {
                        allRecords.filter { it.date.startsWith(selectedMonth) }
                    }

                    // Monthly Summary Totals
                    val monthIncome = monthRecords.filter { it.isIncome }.sumOf { it.amount }
                    val monthExpense = monthRecords.filter { it.isExpense }.sumOf { it.amount }
                    val monthNet = monthIncome - monthExpense
                    val profitMargin = if (monthIncome > 0) (monthNet / monthIncome) * 100.0 else 0.0

                    // Filter by Type & Category for list display
                    val filteredRecords = monthRecords.filter { record ->
                        val matchesType = when (filterType) {
                            "Income" -> record.isIncome
                            "Expense" -> record.isExpense
                            else -> true
                        }
                        val matchesCategory = if (selectedCategoryFilter == "All") {
                            true
                        } else {
                            record.category.equals(selectedCategoryFilter, ignoreCase = true)
                        }
                        matchesType && matchesCategory
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
                    ) {
                        // 1. Month Navigation Header
                        item {
                            MonthNavigatorCard(
                                selectedMonth = selectedMonth,
                                currentMonthKey = currentMonthKey,
                                onMonthChange = { newMonth ->
                                    selectedMonth = newMonth
                                    selectedCategoryFilter = "All"
                                },
                                onExportCsv = {
                                    pendingCsvContent = CsvExportHelper.exportFinancesCsv(monthRecords)
                                    val fn = if (selectedMonth == "ALL") "farm_finances_all.csv" else "farm_finances_${selectedMonth}.csv"
                                    createFinanceDocLauncher.launch(fn)
                                }
                            )
                        }

                        // 2. High-Impact Monthly Summary Hero Card
                        item {
                            MonthlySummaryHeroCard(
                                monthLabel = if (selectedMonth == "ALL") "All-Time Balance" else formatYearMonthDisplay(selectedMonth),
                                totalIncome = monthIncome,
                                totalExpense = monthExpense,
                                netBalance = monthNet,
                                profitMargin = profitMargin,
                                incomeCount = monthRecords.count { it.isIncome },
                                expenseCount = monthRecords.count { it.isExpense }
                            )
                        }

                        // 3. Category Breakdown Section (Feed, Veterinary, Utilities & Milk, Livestock Sales)
                        if (monthRecords.isNotEmpty()) {
                            item {
                                MonthlyCategoryBreakdownCard(
                                    records = monthRecords,
                                    totalExpense = monthExpense,
                                    totalIncome = monthIncome,
                                    onCategorySelected = { cat ->
                                        selectedCategoryFilter = if (selectedCategoryFilter == cat) "All" else cat
                                    }
                                )
                            }
                        }

                        // 4. Daily Transactions Header & Filters
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Daily Transactions",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${filteredRecords.size} records in ${if (selectedMonth == "ALL") "All Time" else formatYearMonthDisplay(selectedMonth)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = onOpenAddDialog,
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Log Record")
                                    }
                                }

                                // Type Filter Chips
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("All", "Expense", "Income").forEach { f ->
                                        val label = when (f) {
                                            "Expense" -> "Expenses (-)"
                                            "Income" -> "Income (+)"
                                            else -> "All Types"
                                        }
                                        FilterChip(
                                            selected = filterType == f,
                                            onClick = {
                                                filterType = f
                                                selectedCategoryFilter = "All"
                                            },
                                            label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                                        )
                                    }
                                }

                                // Category Quick Filter Chips
                                val categoriesToShow = when (filterType) {
                                    "Expense" -> listOf("All") + FinanceRecord.EXPENSE_CATEGORIES.take(5)
                                    "Income" -> listOf("All") + FinanceRecord.INCOME_CATEGORIES.take(5)
                                    else -> listOf("All", "Feed", "Veterinary", "Utilities", "Milk Sales", "Livestock Sales")
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(categoriesToShow) { cat ->
                                        FilterChip(
                                            selected = selectedCategoryFilter == cat,
                                            onClick = { selectedCategoryFilter = cat },
                                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Daily Transaction List Items
                        if (filteredRecords.isEmpty()) {
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
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No records for this selection",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Track feed, veterinary care, utilities, milk sales, and livestock sales.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = onOpenAddDialog) {
                                            Text("Record Daily Transaction")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filteredRecords, key = { it.id.ifBlank { "${it.date}_${it.description}_${it.amount}" } }) { record ->
                                FinanceRecordCard(
                                    record = record,
                                    onDelete = { transToDelete = record }
                                )
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
                .testTag("add_transaction_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
        }
    }

    // Interactive Transaction Entry Modal
    if (isAddDialogOpen) {
        TransactionFormDialog(
            onDismiss = onCloseAddDialog,
            onSave = { newRecord ->
                viewModel.addTransaction(newRecord) { success ->
                    if (success) onCloseAddDialog()
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (transToDelete != null) {
        val record = transToDelete!!
        AlertDialog(
            onDismissRequest = { transToDelete = null },
            title = { Text("Delete Transaction?") },
            text = { Text("Are you sure you want to delete ${record.description} ($${"%.2f".format(record.amount)}) on ${record.date}?") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = record.id
                        transToDelete = null
                        viewModel.deleteTransaction(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { transToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Month Navigator component with previous/next month navigation buttons and quick jump chip.
 */
@Composable
private fun MonthNavigatorCard(
    selectedMonth: String,
    currentMonthKey: String,
    onMonthChange: (String) -> Unit,
    onExportCsv: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (selectedMonth == "ALL") {
                        onMonthChange(currentMonthKey)
                    } else {
                        onMonthChange(shiftMonth(selectedMonth, -1))
                    }
                },
                modifier = Modifier.testTag("month_prev_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Month"
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedMonth == "ALL") "All-Time Records" else formatYearMonthDisplay(selectedMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onExportCsv,
                    modifier = Modifier.size(28.dp).testTag("finance_export_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export Finances CSV",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedMonth != currentMonthKey && selectedMonth != "ALL") {
                    TextButton(
                        onClick = { onMonthChange(currentMonthKey) },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("This Month", style = MaterialTheme.typography.labelSmall)
                    }
                }

                IconButton(
                    onClick = {
                        if (selectedMonth == "ALL") {
                            onMonthChange(currentMonthKey)
                        } else {
                            onMonthChange(shiftMonth(selectedMonth, 1))
                        }
                    },
                    modifier = Modifier.testTag("month_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month"
                    )
                }
            }
        }
    }
}

/**
 * Monthly Summary Hero Card displaying Net Profit/Loss, Income, and Expenses.
 */
@Composable
private fun MonthlySummaryHeroCard(
    monthLabel: String,
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double,
    profitMargin: Double,
    incomeCount: Int,
    expenseCount: Int
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("finance_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Net Balance Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$monthLabel Summary",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = (if (netBalance >= 0) "+$" else "-$") + "%.2f".format(Math.abs(netBalance)),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (netBalance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }

                Surface(
                    color = if (netBalance >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (netBalance >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (netBalance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (netBalance >= 0) "Profit (%.1f%%)".format(profitMargin) else "Deficit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (netBalance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }

            // Income vs Expense Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Income Card
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFF1F8E9),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Income",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Total Income",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "+$%.2f".format(totalIncome),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "$incomeCount transaction(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Expense Card
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFC62828)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Expense",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Total Expenses",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFC62828)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "-$%.2f".format(totalExpense),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )
                        Text(
                            text = "$expenseCount transaction(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Breakdown of monthly expenses (feed, veterinary, utilities) and income (milk sales, livestock sales).
 */
@Composable
private fun MonthlyCategoryBreakdownCard(
    records: List<FinanceRecord>,
    totalExpense: Double,
    totalIncome: Double,
    onCategorySelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Monthly Cost & Revenue Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Key Expenses: Feed, Veterinary, Utilities
            Text(
                text = "EXPENSES BY CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val expenseByCategory = records.filter { it.isExpense }
                .groupBy { normalizeCategory(it.category) }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }

            if (expenseByCategory.isEmpty()) {
                Text(
                    text = "No expenses logged this month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                expenseByCategory.forEach { (cat, amount) ->
                    val fraction = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                    CategoryProgressRow(
                        categoryName = cat,
                        amount = amount,
                        fraction = fraction,
                        isIncome = false,
                        onClick = { onCategorySelected(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Key Income: Milk Sales, Livestock Sales
            Text(
                text = "INCOME BY CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val incomeByCategory = records.filter { it.isIncome }
                .groupBy { normalizeCategory(it.category) }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }

            if (incomeByCategory.isEmpty()) {
                Text(
                    text = "No income logged this month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                incomeByCategory.forEach { (cat, amount) ->
                    val fraction = if (totalIncome > 0) (amount / totalIncome).toFloat() else 0f
                    CategoryProgressRow(
                        categoryName = cat,
                        amount = amount,
                        fraction = fraction,
                        isIncome = true,
                        onClick = { onCategorySelected(cat) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryProgressRow(
    categoryName: String,
    amount: Double,
    fraction: Float,
    isIncome: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getCategoryIcon(categoryName),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "$%.2f (%.1f%%)".format(amount, fraction * 100),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (isIncome) Color(0xFF4CAF50) else Color(0xFFEF5350),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Individual Transaction Card.
 */
@Composable
private fun FinanceRecordCard(
    record: FinanceRecord,
    onDelete: () -> Unit
) {
    val isIncome = record.isIncome
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(record.category),
                        contentDescription = record.category,
                        tint = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = record.description.ifBlank { record.category },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = record.category,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = record.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (if (isIncome) "+$" else "-$") + "%.2f".format(record.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Record",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dialog for recording a daily transaction (expense or income).
 */
@Composable
fun TransactionFormDialog(
    onDismiss: () -> Unit,
    onSave: (FinanceRecord) -> Unit
) {
    var type by remember { mutableStateOf("Expense") }
    val expenseCategories = FinanceRecord.EXPENSE_CATEGORIES
    val incomeCategories = FinanceRecord.INCOME_CATEGORIES
    val currentCategories = if (type == "Expense") expenseCategories else incomeCategories

    var category by remember { mutableStateOf(currentCategories.first()) }
    var amountStr by remember { mutableStateOf("") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var date by remember { mutableStateOf(today) }
    var description by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Daily $type",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (formError != null) {
                    Text(
                        text = formError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Type Toggle: Expense vs Income
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = type == "Expense",
                        onClick = {
                            type = "Expense"
                            category = "Feed"
                        },
                        label = { Text("Expense (-)") }
                    )
                    FilterChip(
                        selected = type == "Income",
                        onClick = {
                            type = "Income"
                            category = "Milk Sales"
                        },
                        label = { Text("Income (+)") }
                    )
                }

                // Category Selector (Feed, Veterinary, Utilities, Milk Sales, Livestock Sales)
                Text(
                    text = "Category (${if (type == "Expense") "Feed, Vet, Utilities..." else "Milk, Livestock..."}) *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(currentCategories) { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($) *") },
                    placeholder = { Text("e.g. 150.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trans_input_amount"),
                    singleLine = true
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description *") },
                    placeholder = {
                        Text(
                            if (type == "Expense") "e.g. 10 bags alfalfa feed, vaccination booster"
                            else "e.g. 50L fresh goat milk, Boer buck sold"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trans_input_desc"),
                    singleLine = true
                )

                // Date
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        formError = "Please enter a valid amount greater than 0."
                        return@Button
                    }
                    if (description.isBlank()) {
                        formError = "Please enter a brief description."
                        return@Button
                    }
                    if (date.isBlank()) {
                        formError = "Please enter transaction date (YYYY-MM-DD)."
                        return@Button
                    }
                    onSave(
                        FinanceRecord(
                            type = type,
                            category = category,
                            amount = amt,
                            date = date.trim(),
                            description = description.trim()
                        )
                    )
                },
                modifier = Modifier
                    .testTag("trans_save_button")
                    .testTag("save_finance_record_button")
            ) {
                Text("Save $type")
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
 * Returns an appropriate icon for finance categories.
 */
private fun getCategoryIcon(category: String): ImageVector {
    val norm = category.lowercase(Locale.getDefault())
    return when {
        norm.contains("feed") || norm.contains("hay") || norm.contains("grain") -> Icons.Default.Restaurant
        norm.contains("vet") || norm.contains("med") || norm.contains("health") -> Icons.Default.MedicalServices
        norm.contains("util") || norm.contains("electric") || norm.contains("water") || norm.contains("power") -> Icons.Default.Bolt
        norm.contains("milk") || norm.contains("dairy") || norm.contains("cheese") -> Icons.Default.LocalDrink
        norm.contains("livestock") || norm.contains("goat") || norm.contains("animal") -> Icons.Default.Pets
        else -> Icons.Default.Payments
    }
}

/**
 * Normalizes category label for aggregation.
 */
private fun normalizeCategory(category: String): String {
    val norm = category.lowercase(Locale.getDefault())
    return when {
        norm.contains("feed") -> "Feed"
        norm.contains("vet") -> "Veterinary"
        norm.contains("util") -> "Utilities"
        norm.contains("milk") -> "Milk Sales"
        norm.contains("livestock") -> "Livestock Sales"
        norm.contains("breed") -> "Breeding Fees"
        norm.contains("supplement") -> "Supplements"
        norm.contains("equip") -> "Equipment"
        norm.contains("labor") -> "Labor"
        else -> category.trim()
    }
}

/**
 * Formats "yyyy-MM" into human-readable "Month Year" (e.g., "November 2026").
 */
private fun formatYearMonthDisplay(yearMonth: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = parser.parse(yearMonth) ?: return yearMonth
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        formatter.format(date)
    } catch (_: Exception) {
        yearMonth
    }
}

/**
 * Shifts a "yyyy-MM" string by delta months.
 */
private fun shiftMonth(yearMonth: String, deltaMonths: Int): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val cal = Calendar.getInstance().apply {
            time = parser.parse(yearMonth) ?: Date()
            add(Calendar.MONTH, deltaMonths)
        }
        parser.format(cal.time)
    } catch (_: Exception) {
        yearMonth
    }
}
