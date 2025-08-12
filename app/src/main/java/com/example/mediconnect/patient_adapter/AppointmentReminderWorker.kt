package com.example.mediconnect.patient_adapter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.mediconnect.R

// Worker class na nagha-handle ng pagpapadala ng appointment reminder notification
class AppointmentReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    // Ang gagawin kapag tumakbo ang worker (background task)
    override fun doWork(): Result {
        // Kunin ang date mula sa input data, kung wala return failure
        val date = inputData.getString("date") ?: return Result.failure()
        // Kunin ang time mula sa input data, kung wala return failure
        val time = inputData.getString("time") ?: return Result.failure()
        // Kunin ang mode (e.g. in-person o teleconsult), default ay "in-person"
        val mode = inputData.getString("mode") ?: "in-person"

        // Tawagin ang function para mag-send ng notification gamit ang mga nakuhang data
        sendNotification(date, time, mode)

        // Ibalik ang success kung walang error
        return Result.success()
    }

    // Function para gumawa at magpakita ng notification
    private fun sendNotification(date: String, time: String, mode: String) {
        // ID ng notification channel
        val channelId = "reminder_channel"
        // Kumuha ng NotificationManager system service
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Para sa Android Oreo (API 26) pataas, gumawa ng notification channel kung wala pa
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,               // ID ng channel
                "Reminders",             // Pangalan ng channel na makikita ng user
                NotificationManager.IMPORTANCE_HIGH  // Importansya ng notification
            )
            notificationManager.createNotificationChannel(channel) // I-register ang channel
        }

        // Gumawa ng notification builder na gagamitin para gumawa ng notification
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.iconhearth)  // Icon ng notification
            .setContentTitle("Upcoming Appointment")  // Title ng notification
            .setContentText("Reminder: Your appointment is at $time on $date ($mode).") // Mensahe ng notification
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Priority ng notification para lumabas agad
            .setAutoCancel(true)  // Awtomatikong mawawala ang notification kapag pinindot

        // Ipakita ang notification gamit ang NotificationManager, gamit ang ID 1002 para makilala
        notificationManager.notify(1002, builder.build())
    }
}
