package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.health.StepSyncWorker
import com.example.ui.navigation.WalkCrewApp
import com.example.ui.theme.WalkCrewTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    @OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure Firebase is initialized
        com.example.data.FirebaseInitializer.ensureInitialized(this)

        // Schedule periodic step sync every 15 minutes with WorkManager
        scheduleStepSyncWorker()

        // Schedule daily 6:00 PM reminder check
        com.example.notifications.DailyReminderWorker.scheduleDailyReminder(applicationContext)

        setContent {
            val windowSizeClass = androidx.compose.material3.windowsizeclass.calculateWindowSizeClass(this)
            androidx.compose.runtime.CompositionLocalProvider(
                com.example.ui.util.LocalAppWindowSizeClass provides windowSizeClass
            ) {
                WalkCrewTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        WalkCrewApp()
                    }
                }
            }
        }
    }

    private fun scheduleStepSyncWorker() {
        try {
            val syncRequest = PeriodicWorkRequestBuilder<StepSyncWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "WalkCrewStepSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        } catch (e: Exception) {
            // Log or ignore if WorkManager initialization delayed
        }
    }
}
