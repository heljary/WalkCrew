package com.example.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class NudgeCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return Result.success()
        val uid = currentUser.uid
        val todayStr = LocalDate.now().toString()

        try {
            val dbId = applicationContext.getString(com.example.R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            com.example.data.WalkCrewLogger.logRead("crews", details = "querying crews where members contains $uid in NudgeCheckWorker")
            val crewsSnapshot = db.collection("crews")
                .whereArrayContains("members", uid)
                .get()
                .await()

            for (crewDoc in crewsSnapshot.documents) {
                val crewId = crewDoc.id
                val crewName = crewDoc.getString("name") ?: "Your Crew"
                val nudgesSnapshot = crewDoc.reference.collection("nudges")
                    .whereEqualTo("toUserId", uid)
                    .whereEqualTo("date", todayStr)
                    .get()
                    .await()

                for (nudgeDoc in nudgesSnapshot.documents) {
                    val fromName = nudgeDoc.getString("fromUserName") ?: "A teammate"
                    val message = nudgeDoc.getString("message") ?: "$fromName nudged you to get your steps in! 👟"

                    NotificationHelper.showLocalNotification(
                        context = applicationContext,
                        title = "WalkCrew • $crewName",
                        message = message,
                        channelId = NotificationHelper.CHANNEL_NUDGES,
                        notificationId = nudgeDoc.id.hashCode()
                    )
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking nudges: ${e.message}", e)
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "NudgeCheckWorker"

        fun scheduleNudgeCheck(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<NudgeCheckWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "WalkCrewNudgeCheckWork",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (e: Exception) {
                // Ignore if WorkManager initialization delayed
            }
        }
    }
}
