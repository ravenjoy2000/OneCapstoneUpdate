package com.example.mediconnect.patient

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mediconnect.R

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("medName") ?: "Medication"
        val dosage = intent.getStringExtra("dosage") ?: ""
        val reminderId = intent.getIntExtra("reminderId", System.currentTimeMillis().toInt())

        try {
            // Check notification permission (Android 13+)
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("MedicationAlarmReceiver", "POST_NOTIFICATIONS permission not granted")
                return
            }

            // Create notification channel (Android 8+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = "medication_channel"
                val channelName = "Medication Reminders"
                val channelDesc = "Reminders to take your medications"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(channelId, channelName, importance).apply {
                    description = channelDesc
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }

            // Intent to open the medication log activity when tapped
            val openIntent = Intent(context, MedicationLogActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

            val pendingIntent = PendingIntent.getActivity(
                context,
                reminderId, // unique request code
                openIntent,
                pendingIntentFlags
            )

            // Build the notification
            val builder = NotificationCompat.Builder(context, "medication_channel")
                .setSmallIcon(R.drawable.ic_medication)
                .setContentTitle("Medication Reminder")
                .setContentText("Time to take: $medName ${if (dosage.isNotEmpty()) "($dosage)" else ""}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            // Show notification
            NotificationManagerCompat.from(context).notify(reminderId, builder.build())
            Log.d("MedicationAlarmReceiver", "Notification shown for $medName ($dosage)")

        } catch (e: SecurityException) {
            Log.e("MedicationAlarmReceiver", "SecurityException: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("MedicationAlarmReceiver", "Error showing notification", e)
        }
    }
}
