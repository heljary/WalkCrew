package com.example.health

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.StepRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.CrewRepository
import com.example.data.repository.UserRepository

class StepSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val stepRepo = StepRepository.getInstance(applicationContext)
            stepRepo.syncSteps()

            val authRepo = AuthRepository.getInstance(applicationContext)
            val currentUserId = authRepo.currentUserId

            if (currentUserId != null) {
                val steps = stepRepo.userStats.value.steps
                val userRepo = UserRepository.getInstance(applicationContext)
                userRepo.updateTodaySteps(currentUserId, steps)

                val crewRepo = CrewRepository.getInstance(applicationContext)
                crewRepo.syncUserStepsToCrews(currentUserId, steps)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
