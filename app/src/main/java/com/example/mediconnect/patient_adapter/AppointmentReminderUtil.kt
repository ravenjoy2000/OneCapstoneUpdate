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

// Utility object para mag-schedule ng appointment reminders gamit AlarmManager
object AppointmentReminderUtil {

    // Function para mag-schedule ng mga reminders batay sa appointment date at time slot
    fun scheduleAppointmentReminders(context: Context, appointmentDate: String, timeSlot: String) {
        // Format ng date at time na gagamitin sa pag-parse
        val format = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault())
        val dateTime = "$appointmentDate $timeSlot" // Pinagsamang date at time string

        // I-parse ang dateTime string papuntang Date object, kung may error, huminto
        val appointmentTime: Date = try {
            format.parse(dateTime) ?: return
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        // Kumuha ng AlarmManager system service
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Para sa Android 12+ (API 31+), i-check kung may permission mag-schedule ng exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Ipapaalam sa user na i-enable ang exact alarm permission sa settings
                Toast.makeText(context, "Enable exact alarm permission in settings", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent) // Buksan ang settings para sa permission
                return
            }
        }

        // List ng reminders: mensahe at offset sa milliseconds mula sa appointment time
        val reminders = listOf(
            Pair("Your appointment is in 1 hour.", -60 * 60 * 1000),    // 1 oras bago
            Pair("Your appointment is in 30 minutes.", -30 * 60 * 1000)  // 30 minuto bago
        )

        // Loop sa bawat reminder para i-schedule ito
        for ((message, offsetMillis) in reminders) {
            // Kumuha ng calendar instance at i-set sa appointment time, tapos i-adjust sa offset
            val reminderTime = Calendar.getInstance().apply {
                time = appointmentTime
                add(Calendar.MILLISECOND, offsetMillis)  // Ibawas ang offset para sa reminder time
            }

            // Kung ang reminder time ay nasa nakaraan na, huwag nang i-schedule
            if (reminderTime.before(Calendar.getInstance())) continue

            // Gumawa ng Intent para sa BroadcastReceiver na magha-handle ng notification
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("notification_message", message) // Isama ang mensahe sa intent extras
            }

            // Gumawa ng unique requestCode base sa reminder time (bitwise para di mag-overlap)
            val requestCode = reminderTime.timeInMillis.toInt() and 0xFFFFFFF

            // Gumawa ng PendingIntent para sa broadcast
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // I-schedule ang alarm gamit ang tamang method depende sa Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+ para i-set exact alarm kahit idle device (doze mode)
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime.timeInMillis,
                    pendingIntent
                )
            } else {
                // Mas lumang Android version, exact alarm lang
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
}
