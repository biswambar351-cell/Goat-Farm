package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HealthRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Categorized monthly frequency data for a specific month.
 */
data class MonthlyHealthBreakdown(
    val monthIndex: Int,          // 1..12
    val monthName: String,         // "Jan", "Feb", ...
    val fullName: String,          // "January", ...
    val vaccinationCount: Int,
    val dewormingCount: Int,
    val checkupCount: Int,
    val hoofTrimCount: Int,
    val otherCount: Int
) {
    val totalCount: Int get() = vaccinationCount + dewormingCount + checkupCount + hoofTrimCount + otherCount
}

/**
 * Visualizes the monthly frequency of health-related events (vaccinations, check-ups,
 * dewormings, and hoof trims) for the current year as a responsive bar chart with interactive breakdown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyHealthEventsBarChart(
    records: List<HealthRecord>,
    modifier: Modifier = Modifier,
    targetYear: Int = Calendar.getInstance().get(Calendar.YEAR)
) {
    val monthsShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val monthsFull = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Parse and group health records by month for the target year
    val monthlyData = remember(records, targetYear) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val map = (1..12).associateWith { monthIdx ->
            var vacc = 0
            var deworm = 0
            var checkup = 0
            var trim = 0
            var other = 0

            records.forEach { r ->
                val dateStr = r.administeredDate.ifBlank { "" }
                if (dateStr.isNotBlank()) {
                    try {
                        val d = sdf.parse(dateStr)
                        if (d != null) {
                            val cal = Calendar.getInstance().apply { time = d }
                            val year = cal.get(Calendar.YEAR)
                            val month = cal.get(Calendar.MONTH) + 1
                            if (year == targetYear && month == monthIdx) {
                                val typeNorm = r.recordType.lowercase()
                                when {
                                    typeNorm.contains("vaccin") -> vacc++
                                    typeNorm.contains("deworm") -> deworm++
                                    typeNorm.contains("trim") || typeNorm.contains("hoof") -> trim++
                                    typeNorm.contains("check") || typeNorm.contains("medic") || typeNorm.contains("treat") -> checkup++
                                    else -> other++
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            MonthlyHealthBreakdown(
                monthIndex = monthIdx,
                monthName = monthsShort[monthIdx - 1],
                fullName = monthsFull[monthIdx - 1],
                vaccinationCount = vacc,
                dewormingCount = deworm,
                checkupCount = checkup,
                hoofTrimCount = trim,
                otherCount = other
            )
        }
        map.values.toList()
    }

    val totalYearEvents = remember(monthlyData) { monthlyData.sumOf { it.totalCount } }
    val maxMonthCount = remember(monthlyData) { (monthlyData.maxOfOrNull { it.totalCount } ?: 0).coerceAtLeast(4) }
    val currentMonthIndex = remember { Calendar.getInstance().get(Calendar.MONTH) + 1 }
    var selectedMonthIndex by remember { mutableIntStateOf(currentMonthIndex) }

    val selectedBreakdown = monthlyData.firstOrNull { it.monthIndex == selectedMonthIndex } ?: monthlyData.first()

    // Event colors
    val colorVaccination = Color(0xFF1976D2)   // Blue
    val colorDeworming = Color(0xFFF57C00)     // Amber/Orange
    val colorCheckup = Color(0xFF7B1FA2)       // Purple
    val colorHoofTrim = Color(0xFF00796B)      // Teal
    val colorOther = Color(0xFF616161)         // Grey

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_health_events_bar_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title and Target Year Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Health Events Chart",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Health Events Frequency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monthly veterinary & care distribution",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$targetYear",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Summary Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = "Total Logged",
                    value = "$totalYearEvents events",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                val peakMonth = monthlyData.maxByOrNull { it.totalCount }
                MetricChip(
                    label = "Peak Month",
                    value = if (peakMonth != null && peakMonth.totalCount > 0) "${peakMonth.monthName} (${peakMonth.totalCount})" else "None",
                    color = Color(0xFFFFF3E0),
                    contentColor = Color(0xFFE65100),
                    modifier = Modifier.weight(1f)
                )
            }

            // Color-Coded Category Legend
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LegendItem(color = colorVaccination, label = "Vaccination")
                LegendItem(color = colorDeworming, label = "Deworming")
                LegendItem(color = colorCheckup, label = "Check-up")
                LegendItem(color = colorHoofTrim, label = "Hoof Trim")
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Recharts-inspired Cartesian Chart with Grid Lines & Y-Axis ticks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(start = 6.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Y-Axis Ticks (Recharts <YAxis />)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 26.dp, end = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$maxMonthCount",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${maxMonthCount / 2}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Chart Area: Cartesian Grid + Bars
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Cartesian Grid Lines (Recharts <CartesianGrid strokeDasharray="3 3" />)
                    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(bottom = 26.dp)
                    ) {
                        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                        // Top horizontal gridline (max)
                        drawLine(
                            color = gridColor,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = stroke.pathEffect
                        )
                        // Mid horizontal gridline (mid)
                        drawLine(
                            color = gridColor,
                            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = stroke.pathEffect
                        )
                        // Bottom horizontal gridline (baseline)
                        drawLine(
                            color = gridColor.copy(alpha = 0.8f),
                            start = androidx.compose.ui.geometry.Offset(0f, size.height),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // 12 Monthly Bars
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        itemsIndexed(monthlyData) { _, item ->
                            val isSelected = item.monthIndex == selectedMonthIndex
                            val isCurrent = item.monthIndex == currentMonthIndex

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .width(36.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else Color.Transparent
                                    )
                                    .clickable { selectedMonthIndex = item.monthIndex }
                                    .padding(horizontal = 2.dp)
                                    .testTag("health_bar_month_${item.monthIndex}")
                            ) {
                                // Count Badge above bar
                                Text(
                                    text = if (item.totalCount > 0) "${item.totalCount}" else "",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Stacked Bar
                                val barMaxHeightDp = 105.dp
                                val calculatedHeight = if (item.totalCount > 0) {
                                    (barMaxHeightDp * (item.totalCount.toFloat() / maxMonthCount)).coerceAtLeast(10.dp)
                                } else {
                                    4.dp
                                }

                                Box(
                                    modifier = Modifier
                                        .width(if (isSelected) 18.dp else 14.dp)
                                        .height(calculatedHeight)
                                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                        .background(
                                            if (item.totalCount == 0) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                            else MaterialTheme.colorScheme.primary
                                        )
                                ) {
                                    if (item.totalCount > 0) {
                                        Column(modifier = Modifier.fillMaxHeight()) {
                                            val total = item.totalCount.toFloat()
                                            if (item.vaccinationCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(item.vaccinationCount / total)
                                                        .background(colorVaccination)
                                                )
                                            }
                                            if (item.dewormingCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(item.dewormingCount / total)
                                                        .background(colorDeworming)
                                                )
                                            }
                                            if (item.checkupCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(item.checkupCount / total)
                                                        .background(colorCheckup)
                                                )
                                            }
                                            if (item.hoofTrimCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(item.hoofTrimCount / total)
                                                        .background(colorHoofTrim)
                                                )
                                            }
                                            if (item.otherCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(item.otherCount / total)
                                                        .background(colorOther)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Month Label (Recharts <XAxis />)
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.monthName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Month Detail Breakdown Card
            AnimatedVisibility(visible = true) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("month_breakdown_detail_card")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedBreakdown.fullName} $targetYear Summary",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${selectedBreakdown.totalCount} Event(s)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (selectedBreakdown.totalCount == 0) {
                            Text(
                                text = "No health events logged for ${selectedBreakdown.fullName}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (selectedBreakdown.vaccinationCount > 0) {
                                    CategoryPill(
                                        count = selectedBreakdown.vaccinationCount,
                                        label = "Vaccine",
                                        color = colorVaccination
                                    )
                                }
                                if (selectedBreakdown.dewormingCount > 0) {
                                    CategoryPill(
                                        count = selectedBreakdown.dewormingCount,
                                        label = "Deworm",
                                        color = colorDeworming
                                    )
                                }
                                if (selectedBreakdown.checkupCount > 0) {
                                    CategoryPill(
                                        count = selectedBreakdown.checkupCount,
                                        label = "Check-up",
                                        color = colorCheckup
                                    )
                                }
                                if (selectedBreakdown.hoofTrimCount > 0) {
                                    CategoryPill(
                                        count = selectedBreakdown.hoofTrimCount,
                                        label = "Hoof Trim",
                                        color = colorHoofTrim
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = contentColor.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun CategoryPill(
    count: Int,
    label: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count $label",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
