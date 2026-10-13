package com.example.health

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.StepRepository

class StepSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val stepRepo = StepRepository.getInstance(applicationContext)
            stepRepo.syncSteps()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
