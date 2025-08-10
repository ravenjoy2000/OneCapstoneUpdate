package com.example.mediconnect.patient_adapter

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppointmentReminderUtil {

    fun scheduleAppointmentReminders(context: Context, appointmentDate: String, timeSlot: String) {
        val format = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault())
        val dateTime = "$appointmentDate $timeSlot"
        val appointmentTime: Date = try {
            format.parse(dateTime) ?: return
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // ✅ Android 12+ exact alarm permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(context, "Enable exact alarm permission in settings", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
                return
            }
        }

        val reminders = listOf(
            Pair("Your appointment is in 1 hour.", -60 * 60 * 1000),
            Pair("Your appointment is in 30 minutes.", -30 * 60 * 1000)
        )

        for ((message, offsetMillis) in reminders) {
            val reminderTime = Calendar.getInstance().apply {
                time = appointmentTime
                add(Calendar.MILLISECOND, offsetMillis)
            }

            if (reminderTime.before(Calendar.getInstance())) continue

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("notification_message", message)
            }

            val requestCode = reminderTime.timeInMillis.toInt() and 0xFFFFFFF
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
}