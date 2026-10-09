package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.StepRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val authRepo = AuthRepository.getInstance(applicationContext)
            val uid = authRepo.currentUserId

            if (uid != null) {
                val userRepo = UserRepository.getInstance(applicationContext)
                val profile = userRepo.getUserProfile(uid)
                val reminderEnabled = profile?.reminderEnabled ?: true

                if (reminderEnabled) {
                    val stepRepo = StepRepository.getInstance(applicationContext)
                    stepRepo.syncSteps()
                    val currentSteps = stepRepo.userStats.value.steps
                    val goal = profile?.dailyStepGoal ?: 8000L

                    // Only notify if user is below 60% of their goal
                    if (currentSteps < (goal * 0.60)) {
                        val percent = ((currentSteps.toFloat() / goal.toFloat()) * 100).toInt()
                        NotificationHelper.showLocalNotification(
                            context = applicationContext,
                            title = "Time to catch up! 👟",
                            message = "You're at $currentSteps steps ($percent% of your $goal goal). An evening walk will keep your streak alive!",
                            channelId = NotificationHelper.CHANNEL_REMINDERS,
                            notificationId = 1800
                        )
                    }
                }
            }

            // Automatically schedule for tomorrow at 18:00
            scheduleDailyReminder(applicationContext)
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "WalkCrewDailyReminderWork"

        fun scheduleDailyReminder(context: Context) {
            val now = LocalDateTime.now()
            var targetTime = now.with(LocalTime.of(18, 0, 0)) // 6:00 PM
            if (now.isAfter(targetTime)) {
                targetTime = targetTime.plusDays(1)
            }

            val delayMillis = Duration.between(now, targetTime).toMillis()

            val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelDailyReminder(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
