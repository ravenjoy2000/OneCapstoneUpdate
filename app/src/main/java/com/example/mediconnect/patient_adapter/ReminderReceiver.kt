package com.example.mediconnect.patient_adapter
// 📦 Package kung saan nakalagay ang ReminderReceiver class

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.mediconnect.R
// 🔽 Mga import na kailangan para gumana ang notifications at receiver

class ReminderReceiver : BroadcastReceiver() {
    // 📌 Gumagawa ng BroadcastReceiver para makakuha ng event at magpakita ng notification

    override fun onReceive(context: Context, intent: Intent) {
        // 📩 Method na auto na tinatawag kapag may natanggap na broadcast

        val message = intent.getStringExtra("notification_message") ?: return
        // 📝 Kinukuha ang mensahe mula sa intent; kung wala, tumigil agad

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 📢 Kinukuha ang NotificationManager para makapagpakita ng notification

        val channelId = "appointment_reminder_channel"
        // 🆔 Unique ID ng notification channel para sa appointment reminders

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 📋 Check kung Android 8.0 (Oreo) pataas dahil kailangan ng notification channel

            val channel = NotificationChannel(
                channelId, // 🆔 ID ng channel
                "Appointment Reminders", // 📛 Pangalan ng channel
                NotificationManager.IMPORTANCE_HIGH // 📌 Priority ng notification
            ).apply {
                description = "Notifies user about upcoming appointments"
                // ℹ️ Deskripsyon ng channel
            }

            notificationManager.createNotificationChannel(channel)
            // 🛠️ Gumagawa ng notification channel kung wala pa
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.outline_circle_notifications_24)
            // 🔔 Maliit na icon na lalabas sa notification
            .setContentTitle("⏰ Appointment Reminder")
            // 📛 Title ng notification
            .setContentText(message)
            // 📝 Nilalaman ng mensahe mula sa intent
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // ⬆️ Mataas na priority para mas lumabas agad
            .setAutoCancel(true)
            // ✅ Mawawala ang notification kapag na-click
            .build()
        // 🏗️ Gumagawa ng final na notification object

        val id = System.currentTimeMillis().toInt()
        // 🆔 Unique ID ng bawat notification gamit ang kasalukuyang oras

        notificationManager.notify(id, notification)
        // 📢 Ipinapakita ang notification sa user
    }
}
