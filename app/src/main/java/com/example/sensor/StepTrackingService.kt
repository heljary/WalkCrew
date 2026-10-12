package com.example.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class StepTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var stepSensorManager: StepSensorManager

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "StepTrackingService created")
        stepSensorManager = StepSensorManager.getInstance(applicationContext)

        createNotificationChannel()
        startForegroundServiceNotification(stepSensorManager.stepCount.value)

        // Observe live step updates and update notification subtly
        stepSensorManager.stepCount
            .onEach { steps ->
                updateNotification(steps)
            }
            .launchIn(serviceScope)

        stepSensorManager.startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                Log.i(TAG, "Stopping StepTrackingService via intent action")
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                stepSensorManager.startListening()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification(currentSteps: Long) {
        try {
            val notification = buildNotification(currentSteps)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                } else {
                    0
                }
                startForeground(NOTIFICATION_ID, notification, foregroundServiceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground notification: ${e.message}", e)
        }
    }

    private fun updateNotification(currentSteps: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildNotification(currentSteps))
    }

    private fun buildNotification(currentSteps: Long): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedSteps = java.text.NumberFormat.getNumberInstance().format(currentSteps)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WalkCrew Step Tracker")
            .setContentText("$formattedSteps steps today • Tracking in real time")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Step Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live step count while walking with WalkCrew"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "StepTrackingService destroyed")
        serviceScope.cancel()
        // Do not unregister sensor if still needed, but StepSensorManager maintains its own lifecycle
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "StepTrackingService"
        private const val CHANNEL_ID = "walkcrew_live_step_tracking_channel"
        private const val NOTIFICATION_ID = 2001
        const val ACTION_STOP_SERVICE = "com.example.sensor.ACTION_STOP_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, StepTrackingService::class.java)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, StepTrackingService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop foreground service: ${e.message}")
            }
        }
    }
}
