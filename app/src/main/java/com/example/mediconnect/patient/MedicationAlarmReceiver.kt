package com.example.mediconnect.patient

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mediconnect.R

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("medName") ?: "Medication"

        try {
            // Check if notification permission is granted
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted; optionally log or handle
                return
            }

            // Build the notification
            val builder = NotificationCompat.Builder(context, "medication_channel")
                .setSmallIcon(R.drawable.ic_medication)
                .setContentTitle("Medication Reminder")
                .setContentText("Time to take your medication: $medName")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            // Show notification
            NotificationManagerCompat.from(context).notify(
                System.currentTimeMillis().toInt(),
                builder.build()
            )

        } catch (e: SecurityException) {
            e.printStackTrace() // Handle potential security exception
        }
    }
}
