package com.example.mediconnect.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.mediconnect.R

class AppointmentReminderWorker (
    context: Context,
    workerParams: WorkerParameters
): Worker(context, workerParams) {

    override fun doWork(): Result {
        val date = inputData.getString("date") ?: return Result.failure()
        val time = inputData.getString("time") ?: return Result.failure()
        val mode = inputData.getString("mode") ?: "in-person"

        sendNotification(date, time, mode)

        return Result.success()
    }

    private fun sendNotification(date: String, time: String, mode: String) {
        val channelId = "reminder_channel"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.iconhearth)
            .setContentTitle("Upcoming Appointment")
            .setContentText("Reminder: Your appointment is at $time on $date ($mode).")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1002, builder.build())
    }
}
