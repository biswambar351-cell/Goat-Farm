package com.example.util

import com.example.data.model.BreedingRecord
import com.example.data.model.FinanceRecord
import com.example.data.model.Goat
import com.example.data.model.HealthRecord
import com.example.data.model.MilkLog
import com.example.notification.FarmNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CsvExportHelperTest {

    @Test
    fun testExportFinancesCsv_correctHeadersAndValues() {
        val records = listOf(
            FinanceRecord(
                id = "fin_1",
                date = "2026-11-01",
                type = "Expense",
                category = "Feed",
                amount = 150.50,
                description = "Alfalfa hay bales, high protein"
            ),
            FinanceRecord(
                id = "fin_2",
                date = "2026-11-05",
                type = "Income",
                category = "Milk Sales",
                amount = 320.00,
                description = "Raw goat milk sold to local dairy"
            )
        )

        val csv = CsvExportHelper.exportFinancesCsv(records)
        val lines = csv.trim().split("\n")

        assertEquals(3, lines.size)
        assertEquals("Date,Type,Category,Amount ($),Description,Record ID", lines[0])
        // Should sort descending by date: fin_2 first (2026-11-05), then fin_1 (2026-11-01)
        assertTrue(lines[1].startsWith("2026-11-05,Income,Milk Sales,320.00,"))
        assertTrue(lines[2].startsWith("2026-11-01,Expense,Feed,150.50,\"Alfalfa hay bales, high protein\",fin_1"))
    }

    @Test
    fun testExportHealthLogsCsv_correctHeadersAndValues() {
        val records = listOf(
            HealthRecord(
                id = "health_1",
                goatTag = "GT-001",
                recordType = "Vaccination",
                treatment = "CDT Booster",
                dosage = "2 ml",
                administeredDate = "2026-10-15",
                nextDueDate = "2027-10-15",
                cost = 25.0,
                notes = "Annual vaccination done"
            )
        )

        val csv = CsvExportHelper.exportHealthLogsCsv(records)
        val lines = csv.trim().split("\n")

        assertEquals(2, lines.size)
        assertEquals("Administered Date,Goat Tag,Record Type,Treatment,Dosage,Next Due Date,Cost ($),Notes,Record ID", lines[0])
        assertEquals("2026-10-15,GT-001,Vaccination,CDT Booster,2 ml,2027-10-15,25.00,Annual vaccination done,health_1", lines[1])
    }

    @Test
    fun testExportMilkProductionCsv_correctHeadersAndValues() {
        val records = listOf(
            MilkLog(
                id = "milk_1",
                doeTag = "GT-002",
                date = "2026-11-08",
                session = "Morning",
                yieldLiters = 3.75,
                notes = "Peak lactation"
            )
        )

        val csv = CsvExportHelper.exportMilkProductionCsv(records)
        val lines = csv.trim().split("\n")

        assertEquals(2, lines.size)
        assertEquals("Date,Doe Tag,Session,Yield (Liters),Notes,Record ID", lines[0])
        assertEquals("2026-11-08,GT-002,Morning,3.75,Peak lactation,milk_1", lines[1])
    }

    @Test
    fun testExportBreedingRecordsCsv_correctHeadersAndValues() {
        val records = listOf(
            BreedingRecord(
                id = "breed_1",
                doeTag = "GT-002",
                buckTag = "GT-003",
                breedingDate = "2026-06-01",
                expectedKiddingDate = "2026-10-29",
                actualKiddingDate = "2026-10-28",
                kidsBorn = 2,
                status = "Delivered",
                notes = "Twins born healthy, 1 male, 1 female"
            )
        )

        val csv = CsvExportHelper.exportBreedingRecordsCsv(records)
        val lines = csv.trim().split("\n")

        assertEquals(2, lines.size)
        assertEquals("Mating Date,Dam ID (Doe),Sire ID (Buck),Expected Delivery Date,Actual Delivery Date,Kids Born,Status,Notes,Record ID", lines[0])
        assertTrue(lines[1].startsWith("2026-06-01,GT-002,GT-003,2026-10-29,2026-10-28,2,Delivered,"))
    }

    @Test
    fun testComprehensiveExport_containsAllSections() {
        val goats = listOf(
            Goat(
                id = "goat_1",
                tag = "GT-001",
                name = "Bella",
                breed = "Boer",
                gender = "Female",
                status = "Active",
                birthDate = "2023-01-10",
                weightKg = 48.5
            )
        )

        val csv = CsvExportHelper.exportComprehensiveFarmDataCsv(
            farmName = "Khulana Goat Farm",
            finances = emptyList(),
            health = emptyList(),
            milk = emptyList(),
            goats = goats,
            breedings = emptyList()
        )

        assertTrue(csv.contains("Khulana Goat Farm"))
        assertTrue(csv.contains("### FINANCIAL TRANSACTIONS (EXPENSES & INCOME) ###"))
        assertTrue(csv.contains("### HEALTH & MEDICAL RECORDS ###"))
        assertTrue(csv.contains("### MILK PRODUCTION LOGS ###"))
        assertTrue(csv.contains("### BREEDING & GESTATION RECORDS ###"))
        assertTrue(csv.contains("### HERD INVENTORY ###"))
        assertTrue(csv.contains("GT-001,Bella,Boer,Female,Active,2023-01-10,48.5,"))
    }

    @Test
    fun testUpcomingAlerts_healthAndBreedingCalculations() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        // 3 days in the future
        cal.add(Calendar.DAY_OF_YEAR, 3)
        val futureDue = sdf.format(cal.time)

        // 30 days in the future (beyond 14 days window)
        cal.add(Calendar.DAY_OF_YEAR, 27)
        val farFutureDue = sdf.format(cal.time)

        val healthRecords = listOf(
            HealthRecord(
                id = "h1",
                goatTag = "GT-001",
                recordType = "Deworming",
                treatment = "Ivermectin",
                nextDueDate = futureDue
            ),
            HealthRecord(
                id = "h2",
                goatTag = "GT-002",
                recordType = "Vaccination",
                treatment = "Rabies",
                nextDueDate = farFutureDue
            )
        )

        val breedingRecords = listOf(
            BreedingRecord(
                id = "b1",
                doeTag = "GT-003",
                buckTag = "GT-004",
                breedingDate = "2026-06-01",
                expectedKiddingDate = futureDue,
                status = "Confirmed Pregnant"
            ),
            BreedingRecord(
                id = "b2",
                doeTag = "GT-005",
                buckTag = "GT-004",
                breedingDate = "2026-06-01",
                expectedKiddingDate = futureDue,
                status = "Delivered" // Should not alert because already delivered
            )
        )

        val alerts = FarmNotificationManager.getUpcomingAlerts(
            healthRecords = healthRecords,
            breedingRecords = breedingRecords,
            daysAhead = 14
        )

        // Expected 2 alerts: h1 (health) and b1 (breeding confirmed pregnant). h2 is >14 days, b2 is delivered
        assertEquals(2, alerts.size)
        assertTrue(alerts.any { it.goatTag == "GT-001" && it.type == FarmNotificationManager.FarmAlert.AlertType.HEALTH_CHECKUP })
        assertTrue(alerts.any { it.goatTag == "GT-003" && it.type == FarmNotificationManager.FarmAlert.AlertType.BREEDING_DELIVERY })
        assertFalse(alerts.any { it.goatTag == "GT-005" })
        assertFalse(alerts.any { it.goatTag == "GT-002" })
    }

    @Test
    fun testOverdueAlerts_correctlyFlagsOverdueAndDamIdFallback() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -2) // 2 days ago = Overdue
        val overdueDate = sdf.format(cal.time)

        val healthRecords = listOf(
            HealthRecord(
                id = "h_overdue",
                goatTag = "GT-099",
                recordType = "Vaccination",
                treatment = "CDT Booster",
                nextDueDate = overdueDate
            )
        )

        val breedingRecords = listOf(
            BreedingRecord(
                id = "b_overdue",
                doeTag = "DOE-100",
                buckTag = "BUCK-200",
                breedingDate = "2026-05-01",
                expectedKiddingDate = overdueDate,
                status = "Bred"
            )
        )

        val alerts = FarmNotificationManager.getUpcomingAlerts(
            healthRecords = healthRecords,
            breedingRecords = breedingRecords,
            daysAhead = 7
        )

        assertEquals(2, alerts.size)
        val healthAlert = alerts.first { it.id == "health_h_overdue" }
        assertTrue(healthAlert.isOverdue)
        assertTrue(healthAlert.daysRemaining < 0)

        val breedingAlert = alerts.first { it.goatTag == "DOE-100" }
        assertTrue(breedingAlert.isOverdue)
        assertEquals("DOE-100", breedingAlert.goatTag)
    }

    @Test
    fun testGoatSearchFilter_matchesByNameOrIdOrBreed() {
        val herd = listOf(
            Goat(id = "1", tag = "GT-001", name = "Bella", breed = "Boer", gender = "Female"),
            Goat(id = "2", tag = "GT-002", name = "Daisy", breed = "Nubian", gender = "Female"),
            Goat(id = "3", tag = "BUCK-99", name = "Thor", breed = "Alpine", gender = "Male")
        )

        // Search by name
        val matchByName = herd.filter { it.name.contains("bell", ignoreCase = true) || it.tag.contains("bell", ignoreCase = true) }
        assertEquals(1, matchByName.size)
        assertEquals("GT-001", matchByName[0].tag)

        // Search by Tag ID
        val matchById = herd.filter { it.name.contains("BUCK", ignoreCase = true) || it.tag.contains("BUCK", ignoreCase = true) }
        assertEquals(1, matchById.size)
        assertEquals("Thor", matchById[0].name)

        // Search by partial Tag number
        val matchByNumber = herd.filter { it.name.contains("002", ignoreCase = true) || it.tag.contains("002", ignoreCase = true) }
        assertEquals(1, matchByNumber.size)
        assertEquals("Daisy", matchByNumber[0].name)
    }

    @Test
    fun testBreedingCycleGestationProgressAndTrimester() {
        val today = java.time.LocalDate.now()
        val mating60DaysAgo = today.minusDays(60).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val expectedDelivery = today.plusDays(90).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val record = BreedingRecord(
            doeTag = "DOE-001",
            buckTag = "BUCK-001",
            breedingDate = mating60DaysAgo,
            expectedKiddingDate = expectedDelivery,
            status = "Confirmed Pregnant"
        )

        assertEquals(60L, record.gestationDaysElapsed())
        assertTrue(record.gestationProgressPercentage() > 0.39f && record.gestationProgressPercentage() < 0.41f)
        assertEquals("2nd Trimester (Days 51–100)", record.gestationTrimester())
        assertEquals(90L, record.daysUntilDelivery())
    }

    @Test
    fun testBreedingRecordToCreateMap_validFieldsForFirestore() {
        val record = BreedingRecord(
            doeTag = "DOE-005",
            buckTag = "BUCK-002",
            breedingDate = "2026-06-01",
            expectedKiddingDate = "2026-10-29",
            status = "Bred",
            notes = "Sire: Titan (Boer ABGA #1920); Natural pasture service"
        )

        val map = record.toCreateMap("user_123")
        assertEquals("user_123", map["userId"])
        assertEquals("DOE-005", map["doeTag"])
        assertEquals("BUCK-002", map["buckTag"])
        assertEquals("2026-06-01", map["breedingDate"])
        assertEquals("2026-10-29", map["expectedKiddingDate"])
        assertEquals("Bred", map["status"])
        assertTrue((map["notes"] as String).contains("Titan"))
    }

    @Test
    fun testAiDiseaseSuggestion_dataIntegrityAndLogging() {
        val suggestion = com.example.data.model.DiseaseSuggestion(
            conditionName = "Caprine Coccidiosis",
            urgency = "Moderate",
            likelihood = "High",
            summary = "Intestinal parasitic infection caused by Eimeria protozoa in young kids.",
            symptomsMatched = listOf("Scours / Watery Diarrhea", "Lethargy / Dullness"),
            immediateActions = listOf("Isolate affected kid", "Provide oral electrolyte hydration"),
            treatmentsAndMedications = listOf("Sulfadimethoxine (Albon) 55 mg/kg Day 1, then 27.5 mg/kg", "Probiotics"),
            preventionAndBiosecurity = listOf("Elevate feed and water troughs", "Keep pen dry with clean pine shavings")
        )

        assertEquals("Caprine Coccidiosis", suggestion.conditionName)
        assertEquals("Moderate", suggestion.urgency)
        assertEquals(2, suggestion.symptomsMatched.size)
        assertEquals(2, suggestion.treatmentsAndMedications.size)

        // Verify conversion into HealthRecord for 1-click logging
        val healthLog = HealthRecord(
            goatTag = "GT-012",
            recordType = "Checkup/Medication",
            treatment = suggestion.treatmentsAndMedications.first(),
            administeredDate = "2026-11-11",
            dosage = "Weight-dosed oral",
            notes = "AI Diagnosis: ${suggestion.conditionName} (${suggestion.urgency})"
        )

        assertEquals("GT-012", healthLog.goatTag)
        assertTrue(healthLog.treatment.contains("Sulfadimethoxine"))
        assertTrue(healthLog.notes.contains("Caprine Coccidiosis"))
    }
}
