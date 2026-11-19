package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Goat(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tag: String = "",
    val name: String = "",
    val breed: String = "",
    val gender: String = "Doe", // Doe, Buck, Wether
    val status: String = "Active", // Active, Sold, Culled, Deceased, Dry, Lactating, Pregnant
    val birthDate: String = "", // YYYY-MM-DD
    val weightKg: Double = 0.0,
    val hornStatus: String = "Polled", // Polled, Dehorned, Horned
    val sireTag: String = "",
    val damTag: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "tag" to tag.trim(),
            "name" to name.trim(),
            "breed" to breed.trim(),
            "gender" to gender,
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (birthDate.isNotBlank()) map["birthDate"] = birthDate.trim()
        if (weightKg > 0.0) map["weightKg"] = weightKg
        if (hornStatus.isNotBlank()) map["hornStatus"] = hornStatus
        if (sireTag.isNotBlank()) map["sireTag"] = sireTag.trim()
        if (damTag.isNotBlank()) map["damTag"] = damTag.trim()
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map
    }

    fun toUpdateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "tag" to tag.trim(),
            "name" to name.trim(),
            "breed" to breed.trim(),
            "gender" to gender,
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (birthDate.isNotBlank()) map["birthDate"] = birthDate.trim()
        map["weightKg"] = weightKg
        if (hornStatus.isNotBlank()) map["hornStatus"] = hornStatus
        if (sireTag.isNotBlank()) map["sireTag"] = sireTag.trim()
        if (damTag.isNotBlank()) map["damTag"] = damTag.trim()
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map
    }
}

@IgnoreExtraProperties
data class BreedingRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val doeTag: String = "",               // Dam ID / Mother Doe ID
    val buckTag: String = "",              // Sire ID / Father Buck ID
    val breedingDate: String = "",         // Mating Date (YYYY-MM-DD)
    val expectedKiddingDate: String = "",  // Expected Delivery Date (YYYY-MM-DD)
    val actualKiddingDate: String = "",    // Actual delivery date if kidded
    val kidsBorn: Long = 0L,
    val status: String = "Bred",           // Bred, Confirmed Pregnant, Kidded, Failed
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    // Explicit property accessors matching domain terminology
    val sireId: String get() = buckTag
    val damId: String get() = doeTag
    val matingDate: String get() = breedingDate
    val expectedDeliveryDate: String get() = expectedKiddingDate

    companion object {
        /**
         * Calculates expected kidding/delivery date based on average goat gestation of 150 days (~5 months).
         */
        fun calculateExpectedDeliveryDate(matingDateStr: String): String {
            return try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val date = java.time.LocalDate.parse(matingDateStr.trim(), formatter)
                date.plusDays(150).format(formatter)
            } catch (e: Exception) {
                ""
            }
        }

        /**
         * Factory helper using sireId, damId, matingDate, and expectedDeliveryDate.
         */
        fun create(
            damId: String,
            sireId: String,
            matingDate: String,
            expectedDeliveryDate: String = "",
            status: String = "Bred",
            notes: String = ""
        ): BreedingRecord {
            val calcDelivery = if (expectedDeliveryDate.isNotBlank()) {
                expectedDeliveryDate.trim()
            } else {
                calculateExpectedDeliveryDate(matingDate)
            }
            return BreedingRecord(
                doeTag = damId.trim(),
                buckTag = sireId.trim(),
                breedingDate = matingDate.trim(),
                expectedKiddingDate = calcDelivery,
                status = status,
                notes = notes.trim()
            )
        }
    }

    /**
     * Returns days until expected delivery date.
     * Positive = due in X days, 0 = due today, negative = overdue by X days.
     */
    fun daysUntilDelivery(): Long? {
        val dateStr = expectedDeliveryDate.ifBlank { expectedKiddingDate }
        if (dateStr.isBlank()) return null
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val target = java.time.LocalDate.parse(dateStr.trim(), formatter)
            val today = java.time.LocalDate.now()
            java.time.temporal.ChronoUnit.DAYS.between(today, target)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculates days elapsed in gestation since breeding date (0 to 150+).
     */
    fun gestationDaysElapsed(): Long? {
        val dateStr = matingDate.ifBlank { breedingDate }
        if (dateStr.isBlank()) return null
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val mating = java.time.LocalDate.parse(dateStr.trim(), formatter)
            val today = java.time.LocalDate.now()
            val diff = java.time.temporal.ChronoUnit.DAYS.between(mating, today)
            if (diff < 0) 0L else diff
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns gestation progress ratio between 0.0f and 1.0f (based on 150-day average).
     */
    fun gestationProgressPercentage(): Float {
        val days = gestationDaysElapsed() ?: return 0f
        return (days.coerceIn(0L, 150L).toFloat() / 150f)
    }

    /**
     * Identifies current trimester / stage based on gestation days.
     */
    fun gestationTrimester(): String {
        if (status.equals("Kidded", ignoreCase = true)) return "Delivered"
        if (status.equals("Failed", ignoreCase = true)) return "Failed"
        val days = gestationDaysElapsed() ?: return "Bred"
        return when {
            days < 50 -> "1st Trimester (Days 1–50)"
            days < 100 -> "2nd Trimester (Days 51–100)"
            days < 140 -> "3rd Trimester (Days 101–140)"
            else -> "Kidding Watch (Days 141–150+)"
        }
    }

    fun toCreateMap(uid: String): Map<String, Any> {
        val deliveryDate = if (expectedKiddingDate.isNotBlank()) {
            expectedKiddingDate.trim()
        } else {
            calculateExpectedDeliveryDate(breedingDate)
        }
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "doeTag" to doeTag.trim(),
            "buckTag" to buckTag.trim(),
            "breedingDate" to breedingDate.trim(),
            "expectedKiddingDate" to deliveryDate,
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (actualKiddingDate.isNotBlank()) map["actualKiddingDate"] = actualKiddingDate.trim()
        if (kidsBorn > 0) map["kidsBorn"] = kidsBorn
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map
    }

    fun toUpdateMap(): Map<String, Any> {
        val deliveryDate = if (expectedKiddingDate.isNotBlank()) {
            expectedKiddingDate.trim()
        } else {
            calculateExpectedDeliveryDate(breedingDate)
        }
        val map = mutableMapOf<String, Any>(
            "doeTag" to doeTag.trim(),
            "buckTag" to buckTag.trim(),
            "breedingDate" to breedingDate.trim(),
            "expectedKiddingDate" to deliveryDate,
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (actualKiddingDate.isNotBlank()) map["actualKiddingDate"] = actualKiddingDate.trim()
        map["kidsBorn"] = kidsBorn
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map
    }
}

@IgnoreExtraProperties
data class MilkLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val doeTag: String = "",
    val date: String = "",
    val session: String = "Morning", // Morning, Evening, Full Day
    val yieldLiters: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "doeTag" to doeTag.trim(),
            "date" to date.trim(),
            "session" to session,
            "yieldLiters" to yieldLiters,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map
    }

    fun toUpdateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "doeTag" to doeTag.trim(),
            "date" to date.trim(),
            "session" to session,
            "yieldLiters" to yieldLiters,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map
    }
}

@IgnoreExtraProperties
data class HealthRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val goatTag: String = "",
    val recordType: String = "Vaccination", // Vaccination, Deworming, Hoof Trimming, Medication, Checkup
    val treatment: String = "",
    val dosage: String = "",
    val administeredDate: String = "",
    val nextDueDate: String = "",
    val cost: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "goatTag" to goatTag.trim(),
            "recordType" to recordType,
            "treatment" to treatment.trim(),
            "administeredDate" to administeredDate.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (dosage.isNotBlank()) map["dosage"] = dosage.trim()
        if (nextDueDate.isNotBlank()) map["nextDueDate"] = nextDueDate.trim()
        if (cost > 0.0) map["cost"] = cost
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map
    }

    fun toUpdateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "goatTag" to goatTag.trim(),
            "recordType" to recordType,
            "treatment" to treatment.trim(),
            "administeredDate" to administeredDate.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (dosage.isNotBlank()) map["dosage"] = dosage.trim()
        if (nextDueDate.isNotBlank()) map["nextDueDate"] = nextDueDate.trim()
        map["cost"] = cost
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map
    }
}

/**
 * Data class representing a daily financial entry (expense or income) for the goat farm.
 * Tracks daily expenses (feed, veterinary, utilities) and income (milk sales, livestock sales).
 */
@IgnoreExtraProperties
data class FinanceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "Expense", // "Expense" or "Income"
    val category: String = "Feed", // Feed, Veterinary, Utilities, Milk Sales, Livestock Sales, etc.
    val amount: Double = 0.0,
    val date: String = "", // YYYY-MM-DD
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isExpense: Boolean get() = type.equals("Expense", ignoreCase = true)
    val isIncome: Boolean get() = type.equals("Income", ignoreCase = true)

    /**
     * Extracts year-month string (YYYY-MM) for monthly financial summaries.
     */
    val yearMonth: String get() = if (date.length >= 7) date.substring(0, 7) else ""

    fun toCreateMap(uid: String): Map<String, Any> {
        return mapOf(
            "userId" to uid,
            "type" to type.trim(),
            "category" to category.trim(),
            "amount" to amount,
            "date" to date.trim(),
            "description" to (if (description.isNotBlank()) description.trim() else category.trim()),
            "createdAt" to FieldValue.serverTimestamp()
        )
    }

    fun toUpdateMap(): Map<String, Any> {
        return mapOf(
            "type" to type.trim(),
            "category" to category.trim(),
            "amount" to amount,
            "date" to date.trim(),
            "description" to (if (description.isNotBlank()) description.trim() else category.trim()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    companion object {
        val EXPENSE_CATEGORIES = listOf(
            "Feed",
            "Veterinary",
            "Utilities",
            "Supplements & Meds",
            "Equipment & Repairs",
            "Fencing & Shelter",
            "Labor",
            "Other Expense"
        )

        val INCOME_CATEGORIES = listOf(
            "Milk Sales",
            "Livestock Sales",
            "Breeding Services",
            "Manure / Fertilizer",
            "Cheese & Dairy",
            "Other Income"
        )
    }
}

/**
 * Backward-compatibility alias for FinanceRecord.
 */
typealias FarmTransaction = FinanceRecord

/**
 * Represents a health log event (vaccination, deworming, medication, checkup) for a specific goat.
 */
@IgnoreExtraProperties
data class HealthLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val goatTag: String = "",
    val date: String = "",
    val type: String = "Vaccination", // Vaccination, Deworming, Medication, Hoof Trim, Checkup
    val description: String = "",
    val nextDueDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    /**
     * Serializes to Firestore /health_records collection.
     */
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "goatTag" to goatTag.trim(),
            "recordType" to type.trim(),
            "treatment" to (if (description.isNotBlank()) description.trim() else type.trim()),
            "administeredDate" to date.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (nextDueDate.isNotBlank()) {
            map["nextDueDate"] = nextDueDate.trim()
        }
        if (description.isNotBlank()) {
            map["notes"] = description.trim()
        }
        return map
    }

    fun toUpdateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "goatTag" to goatTag.trim(),
            "recordType" to type.trim(),
            "treatment" to (if (description.isNotBlank()) description.trim() else type.trim()),
            "administeredDate" to date.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (nextDueDate.isNotBlank()) {
            map["nextDueDate"] = nextDueDate.trim()
        }
        if (description.isNotBlank()) {
            map["notes"] = description.trim()
        }
        return map
    }
}

/**
 * Represents a milk production log for a specific goat.
 * Includes fields for date, goat ID, and quantity (in liters).
 */
@IgnoreExtraProperties
data class MilkProduction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val goatId: String = "",         // Goat ID / ear tag identifier
    val date: String = "",           // YYYY-MM-DD
    val quantityLiters: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        return mapOf(
            "userId" to uid,
            "doeTag" to goatId.trim(),
            "date" to date.trim(),
            "session" to "Full Day",
            "yieldLiters" to quantityLiters,
            "createdAt" to FieldValue.serverTimestamp()
        )
    }

    fun toUpdateMap(): Map<String, Any> {
        return mapOf(
            "doeTag" to goatId.trim(),
            "date" to date.trim(),
            "session" to "Full Day",
            "yieldLiters" to quantityLiters,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

/**
 * Represents the customizable farm name, address, owner name, contact details,
 * and editable farm logo configuration allowing any farmer to customize the app.
 */
@IgnoreExtraProperties
data class FarmProfile(
    val farmName: String = "Khulana Goat Farm",
    val ownerName: String = "",
    val address: String = "Khulna, Bangladesh",
    val phone: String = "",
    val notes: String = "",
    val logoType: String = "default",
    val logoEmblemId: String = "goat_pets",
    val logoColorIndex: Int = 0,
    val logoInitials: String = "",
    val logoShape: String = "circle",
    val logoImageBase64: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "farmName" to farmName.trim(),
            "address" to address.trim(),
            "logoType" to logoType.ifBlank { "default" },
            "logoEmblemId" to logoEmblemId.ifBlank { "goat_pets" },
            "logoColorIndex" to logoColorIndex,
            "logoInitials" to logoInitials.trim().uppercase().take(4),
            "logoShape" to logoShape.ifBlank { "circle" },
            "logoImageBase64" to logoImageBase64,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (ownerName.isNotBlank()) map["ownerName"] = ownerName.trim()
        if (phone.isNotBlank()) map["phone"] = phone.trim()
        if (notes.isNotBlank()) map["notes"] = notes.trim()
        return map
    }
}


