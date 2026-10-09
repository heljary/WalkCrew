package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_NUDGES = "walkcrew_nudges"
    const val CHANNEL_REMINDERS = "walkcrew_reminders"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val nudgesChannel = NotificationChannel(
                CHANNEL_NUDGES,
                "Crew Nudges",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Friendly nudges from teammates to encourage your daily walk"
                enableVibration(true)
            }

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Daily Goal Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily 6:00 PM reminders if you are below 60% of your daily step goal"
            }

            notificationManager.createNotificationChannel(nudgesChannel)
            notificationManager.createNotificationChannel(remindersChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showLocalNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = CHANNEL_NUDGES,
        notificationId: Int = 1001
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (channelId == CHANNEL_NUDGES) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF0E3B2C.toInt())
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission missing
        }
    }
}
