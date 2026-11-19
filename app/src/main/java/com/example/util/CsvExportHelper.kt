package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.BreedingRecord
import com.example.data.model.FinanceRecord
import com.example.data.model.Goat
import com.example.data.model.HealthRecord
import com.example.data.model.MilkLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility helper to format and export farm financial, health, and milk production data as CSV files
 * and write them directly to device storage or share them via the Android system sharesheet.
 */
object CsvExportHelper {

    private const val TAG = "CsvExportHelper"

    /**
     * Formats a list of [FinanceRecord] items into CSV format.
     */
    fun exportFinancesCsv(records: List<FinanceRecord>): String {
        val sb = StringBuilder()
        sb.append("Date,Type,Category,Amount ($),Description,Record ID\n")
        records.sortedByDescending { it.date }.forEach { r ->
            sb.append(escapeCsv(r.date)).append(",")
            sb.append(escapeCsv(r.type)).append(",")
            sb.append(escapeCsv(r.category)).append(",")
            sb.append(String.format(Locale.US, "%.2f", r.amount)).append(",")
            sb.append(escapeCsv(r.description)).append(",")
            sb.append(escapeCsv(r.id)).append("\n")
        }
        return sb.toString()
    }

    /**
     * Formats a list of [HealthRecord] items into CSV format.
     */
    fun exportHealthLogsCsv(records: List<HealthRecord>): String {
        val sb = StringBuilder()
        sb.append("Administered Date,Goat Tag,Record Type,Treatment,Dosage,Next Due Date,Cost ($),Notes,Record ID\n")
        records.sortedByDescending { it.administeredDate }.forEach { r ->
            sb.append(escapeCsv(r.administeredDate)).append(",")
            sb.append(escapeCsv(r.goatTag)).append(",")
            sb.append(escapeCsv(r.recordType)).append(",")
            sb.append(escapeCsv(r.treatment)).append(",")
            sb.append(escapeCsv(r.dosage)).append(",")
            sb.append(escapeCsv(r.nextDueDate)).append(",")
            sb.append(String.format(Locale.US, "%.2f", r.cost)).append(",")
            sb.append(escapeCsv(r.notes)).append(",")
            sb.append(escapeCsv(r.id)).append("\n")
        }
        return sb.toString()
    }

    /**
     * Formats a list of [MilkLog] items into CSV format.
     */
    fun exportMilkProductionCsv(records: List<MilkLog>): String {
        val sb = StringBuilder()
        sb.append("Date,Doe Tag,Session,Yield (Liters),Notes,Record ID\n")
        records.sortedByDescending { it.date }.forEach { r ->
            sb.append(escapeCsv(r.date)).append(",")
            sb.append(escapeCsv(r.doeTag)).append(",")
            sb.append(escapeCsv(r.session)).append(",")
            sb.append(String.format(Locale.US, "%.2f", r.yieldLiters)).append(",")
            sb.append(escapeCsv(r.notes)).append(",")
            sb.append(escapeCsv(r.id)).append("\n")
        }
        return sb.toString()
    }

    /**
     * Formats a list of [BreedingRecord] items into CSV format.
     */
    fun exportBreedingRecordsCsv(records: List<BreedingRecord>): String {
        val sb = StringBuilder()
        sb.append("Mating Date,Dam ID (Doe),Sire ID (Buck),Expected Delivery Date,Actual Delivery Date,Kids Born,Status,Notes,Record ID\n")
        records.sortedByDescending { it.breedingDate }.forEach { r ->
            sb.append(escapeCsv(r.breedingDate)).append(",")
            sb.append(escapeCsv(r.doeTag)).append(",")
            sb.append(escapeCsv(r.buckTag)).append(",")
            sb.append(escapeCsv(r.expectedKiddingDate)).append(",")
            sb.append(escapeCsv(r.actualKiddingDate)).append(",")
            sb.append(r.kidsBorn).append(",")
            sb.append(escapeCsv(r.status)).append(",")
            sb.append(escapeCsv(r.notes)).append(",")
            sb.append(escapeCsv(r.id)).append("\n")
        }
        return sb.toString()
    }

    /**
     * Generates a comprehensive combined CSV containing all farm records (Finances, Health, Milk, Breeding, Herd).
     */
    fun exportComprehensiveFarmDataCsv(
        farmName: String,
        finances: List<FinanceRecord>,
        health: List<HealthRecord>,
        milk: List<MilkLog>,
        goats: List<Goat>,
        breedings: List<BreedingRecord>
    ): String {
        val sb = StringBuilder()
        val exportTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        sb.append("# FARM EXPORT: ").append(escapeCsv(farmName)).append("\n")
        sb.append("# Generated At: ").append(exportTimestamp).append("\n\n")

        // 1. FINANCIAL SUMMARY & RECORDS
        sb.append("### FINANCIAL TRANSACTIONS (EXPENSES & INCOME) ###\n")
        sb.append(exportFinancesCsv(finances)).append("\n")

        // 2. HEALTH & MEDICAL LOGS
        sb.append("### HEALTH & MEDICAL RECORDS ###\n")
        sb.append(exportHealthLogsCsv(health)).append("\n")

        // 3. MILK PRODUCTION LOGS
        sb.append("### MILK PRODUCTION LOGS ###\n")
        sb.append(exportMilkProductionCsv(milk)).append("\n")

        // 4. BREEDING & GESTATION RECORDS
        sb.append("### BREEDING & GESTATION RECORDS ###\n")
        sb.append(exportBreedingRecordsCsv(breedings)).append("\n")

        // 5. HERD INVENTORY
        sb.append("### HERD INVENTORY ###\n")
        sb.append("Tag,Name,Breed,Gender,Status,Birth Date,Weight (kg),Sire Tag,Dam Tag,Notes\n")
        goats.sortedBy { it.tag }.forEach { g ->
            sb.append(escapeCsv(g.tag)).append(",")
            sb.append(escapeCsv(g.name)).append(",")
            sb.append(escapeCsv(g.breed)).append(",")
            sb.append(escapeCsv(g.gender)).append(",")
            sb.append(escapeCsv(g.status)).append(",")
            sb.append(escapeCsv(g.birthDate)).append(",")
            sb.append(String.format(Locale.US, "%.1f", g.weightKg)).append(",")
            sb.append(escapeCsv(g.sireTag)).append(",")
            sb.append(escapeCsv(g.damTag)).append(",")
            sb.append(escapeCsv(g.notes)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Writes CSV string content directly into the destination storage [Uri] selected by the user.
     */
    fun writeCsvToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing CSV to URI $uri", e)
            false
        }
    }

    /**
     * Creates a share intent using [FileProvider] to send the CSV file to other apps.
     */
    fun createShareIntent(context: Context, filename: String, content: String): Intent? {
        return try {
            val cacheFile = File(context.cacheDir, filename)
            cacheFile.writeText(content, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Farm Data Export - $filename")
                putExtra(Intent.EXTRA_TEXT, "Here is the exported CSV file from Khulana Goat Farm.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create share intent for $filename", e)
            null
        }
    }

    private fun escapeCsv(value: String): String {
        if (!value.contains(",") && !value.contains("\"") && !value.contains("\n") && !value.contains("\r")) {
            return value
        }
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
