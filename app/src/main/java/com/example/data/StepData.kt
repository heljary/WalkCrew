package com.example.data

import java.time.LocalDate

data class DailyStepData(
    val date: LocalDate,
    val dayInitial: String, // e.g. "M", "T", "W"
    val dayName: String,    // e.g. "Mon"
    val steps: Long,
    val isToday: Boolean = false,
    val goal: Long = 8000L
) {
    val progressRatio: Float
        get() = (steps.toFloat() / goal.toFloat()).coerceIn(0f, 2f)
}

data class UserStepStats(
    val steps: Long = 0L,
    val goal: Long = 8000L,
    val distanceKm: Double = 0.0,
    val caloriesKcal: Int = 0,
    val activeMinutes: Int = 0,
    val weeklyHistory: List<DailyStepData> = emptyList(),
    val lastSyncTimeMillis: Long = System.currentTimeMillis(),
    val isSimulated: Boolean = false
) {
    companion object {
        fun fromSteps(
            steps: Long,
            weeklyHistory: List<DailyStepData>,
            goal: Long = 8000L,
            isSimulated: Boolean = false
        ): UserStepStats {
            // Estimates:
            // ~0.762 meters per step -> 0.000762 km/step
            // ~0.04 calories burned per step
            // ~100 steps per active walking minute
            val distanceKm = (steps * 0.000762 * 10).toLong() / 10.0
            val calories = (steps * 0.04).toInt()
            val activeMinutes = (steps / 100).toInt()

            return UserStepStats(
                steps = steps,
                goal = goal,
                distanceKm = distanceKm,
                caloriesKcal = calories,
                activeMinutes = activeMinutes,
                weeklyHistory = weeklyHistory,
                lastSyncTimeMillis = System.currentTimeMillis(),
                isSimulated = isSimulated
            )
        }
    }
}

sealed class HealthConnectState {
    data object Loading : HealthConnectState()
    data object AvailableAndGranted : HealthConnectState()
    data object NeedsPermission : HealthConnectState()
    data class PermissionDenied(val reason: String = "WalkCrew needs step permissions to calculate progress and compete.") : HealthConnectState()
    data object NotInstalled : HealthConnectState()
    data object NotSupported : HealthConnectState()
}
