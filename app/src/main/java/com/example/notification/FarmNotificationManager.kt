package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.BreedingRecord
import com.example.data.model.HealthRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FarmNotificationManager {

    private const val TAG = "FarmNotificationManager"
    const val CHANNEL_ID = "caprafarm_health_reminders"
    private const val CHANNEL_NAME = "Vaccination, Deworming & Breeding Reminders"
    private const val CHANNEL_DESC = "Alerts for completing durations of vaccination, deworming, and goat care schedules."

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendImmediateNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification", e)
        }
    }

    fun scheduleHealthReminder(
        context: Context,
        goatTag: String,
        recordType: String,
        treatmentName: String,
        dueDateString: String
    ) {
        if (dueDateString.isBlank()) return

        val title = when (recordType.lowercase()) {
            "vaccination" -> "💉 Vaccination Booster Due"
            "deworming" -> "🪱 Deworming Cycle Due"
            "hoof trimming" -> "✂️ Hoof Trimming Scheduled"
            else -> "🏥 Care Treatment Due"
        }
        val message = "Goat $goatTag has reached duration for $treatmentName ($recordType). Scheduled date: $dueDateString."

        val triggerMillis = parseDateToMorningMillis(dueDateString)
        val notificationId = ("$goatTag$treatmentName$dueDateString".hashCode() and 0x7FFFFFFF) % 100000

        scheduleAlarm(context, title, message, triggerMillis, notificationId)
    }

    fun scheduleKiddingReminder(
        context: Context,
        doeTag: String,
        buckTag: String,
        expectedDateString: String
    ) {
        if (expectedDateString.isBlank()) return

        val title = "🍼 Expected Kidding Due Soon"
        val message = "Doe $doeTag (Sire: $buckTag) has reached her 150-day gestation date: $expectedDateString. Prepare kidding stall!"

        val triggerMillis = parseDateToMorningMillis(expectedDateString)
        val notificationId = ("kidding_$doeTag$expectedDateString".hashCode() and 0x7FFFFFFF) % 100000

        scheduleAlarm(context, title, message, triggerMillis, notificationId)
    }

    private fun scheduleAlarm(
        context: Context,
        title: String,
        message: String,
        triggerTimeMillis: Long,
        notificationId: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, FarmNotificationReceiver::class.java).apply {
            putExtra(FarmNotificationReceiver.EXTRA_TITLE, title)
            putExtra(FarmNotificationReceiver.EXTRA_MESSAGE, message)
            putExtra(FarmNotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // If the date is already passed or today, trigger soon or set alarm
            val now = System.currentTimeMillis()
            val targetTime = if (triggerTimeMillis <= now) now + 5000L else triggerTimeMillis

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, targetTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled reminder for '$title' at $targetTime")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }

    private fun parseDateToMorningMillis(dateString: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateString) ?: Date()
            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 9) // 9:00 AM
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            calendar.timeInMillis
        } catch (e: Exception) {
            System.currentTimeMillis() + 60000L
        }
    }

    /**
     * Represents an actionable farm alert for an upcoming breeding or health event.
     */
    data class FarmAlert(
        val id: String,
        val title: String,
        val message: String,
        val dueDate: String,
        val daysRemaining: Long,
        val isOverdue: Boolean,
        val isDueToday: Boolean,
        val type: AlertType,
        val goatTag: String
    ) {
        enum class AlertType { HEALTH_CHECKUP, BREEDING_DELIVERY }
    }

    /**
     * Scans health and breeding records for upcoming check-ups or delivery dates within [daysAhead] days,
     * as well as overdue items.
     */
    fun getUpcomingAlerts(
        healthRecords: List<HealthRecord>,
        breedingRecords: List<BreedingRecord>,
        daysAhead: Int = 14
    ): List<FarmAlert> {
        val alerts = mutableListOf<FarmAlert>()
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 1. Health check-ups, vaccinations, and dewormings
        healthRecords.forEach { record ->
            if (record.nextDueDate.isNotBlank()) {
                try {
                    val parsedDate = sdf.parse(record.nextDueDate) ?: return@forEach
                    val targetCal = Calendar.getInstance().apply {
                        time = parsedDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val diffDays = (targetCal.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)

                    if (diffDays <= daysAhead) {
                        val isDueToday = diffDays == 0L
                        val isOverdue = diffDays < 0L
                        val title = when {
                            isDueToday -> "💉 ${record.recordType} Due Today"
                            isOverdue -> "⚠️ ${record.recordType} Overdue (${-diffDays}d)"
                            else -> "📅 Upcoming ${record.recordType} in $diffDays days"
                        }
                        val msg = "Goat ${record.goatTag}: ${record.treatment} scheduled for ${record.nextDueDate}."
                        alerts.add(
                            FarmAlert(
                                id = "health_${record.id.ifBlank { "${record.goatTag}_${record.nextDueDate}" }}",
                                title = title,
                                message = msg,
                                dueDate = record.nextDueDate,
                                daysRemaining = diffDays,
                                isOverdue = isOverdue,
                                isDueToday = isDueToday,
                                type = FarmAlert.AlertType.HEALTH_CHECKUP,
                                goatTag = record.goatTag
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }

        // 2. Breeding & Gestation due dates
        breedingRecords.forEach { breeding ->
            val status = breeding.status.trim()
            val isDeliveredOrFailed = status.equals("Kidded", ignoreCase = true) ||
                    status.equals("Delivered", ignoreCase = true) ||
                    status.equals("Failed", ignoreCase = true)

            if (!isDeliveredOrFailed) {
                val dateStr = breeding.expectedDeliveryDate.ifBlank { breeding.expectedKiddingDate }.trim()
                if (dateStr.isNotBlank()) {
                    try {
                        val parsedDate = sdf.parse(dateStr) ?: return@forEach
                        val targetCal = Calendar.getInstance().apply {
                            time = parsedDate
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val diffDays = (targetCal.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)

                        if (diffDays <= daysAhead) {
                            val isDueToday = diffDays == 0L
                            val isOverdue = diffDays < 0L
                            val doe = breeding.doeTag.ifBlank { breeding.damId }
                            val buck = breeding.buckTag.ifBlank { breeding.sireId }
                            val title = when {
                                isDueToday -> "🍼 Kidding Expected Today: Doe $doe"
                                isOverdue -> "⚠️ Kidding Overdue by ${-diffDays} days: Doe $doe"
                                else -> "🍼 Kidding Due in $diffDays days: Doe $doe"
                            }
                            val msg = "Doe $doe (Sire: $buck) expected kidding on $dateStr (150-day gestation)."
                            alerts.add(
                                FarmAlert(
                                    id = "breeding_${breeding.id.ifBlank { "${doe}_${breeding.breedingDate}" }}",
                                    title = title,
                                    message = msg,
                                    dueDate = dateStr,
                                    daysRemaining = diffDays,
                                    isOverdue = isOverdue,
                                    isDueToday = isDueToday,
                                    type = FarmAlert.AlertType.BREEDING_DELIVERY,
                                    goatTag = doe
                                )
                            )
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        return alerts.sortedBy { it.daysRemaining }
    }

    /**
     * Dispatches system push notifications for all alerts that are due today or in the upcoming window.
     */
    fun dispatchAlertNotifications(context: Context, alerts: List<FarmAlert>): Int {
        var count = 0
        alerts.forEach { alert ->
            val notifId = (alert.id.hashCode() and 0x7FFFFFFF) % 100000
            sendImmediateNotification(context, alert.title, alert.message, notifId)
            count++
        }
        return count
    }

    /**
     * Checks all health records and breeding records, and dispatches notifications
     * for any treatments or kiddings that are due today or overdue.
     */
    fun checkAndNotifyDueRecords(
        context: Context,
        healthRecords: List<HealthRecord>,
        breedingRecords: List<BreedingRecord>
    ): Int {
        val alerts = getUpcomingAlerts(healthRecords, breedingRecords, daysAhead = 7)
        return dispatchAlertNotifications(context, alerts)
    }
}
