package com.example.data

import android.content.Context
import com.example.health.HealthConnectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class StepRepository(
    private val context: Context,
    val healthConnectManager: HealthConnectManager = HealthConnectManager(context)
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _userStats = MutableStateFlow(createInitialSampleStats())
    val userStats: StateFlow<UserStepStats> = _userStats.asStateFlow()

    private val _healthConnectState = MutableStateFlow<HealthConnectState>(HealthConnectState.Loading)
    val healthConnectState: StateFlow<HealthConnectState> = _healthConnectState.asStateFlow()

    init {
        checkHealthConnectStatus()
    }

    fun checkHealthConnectStatus() {
        scope.launch {
            val availability = healthConnectManager.checkAvailability()
            if (availability is HealthConnectState.NeedsPermission) {
                val hasPerms = healthConnectManager.hasAllPermissions()
                if (hasPerms) {
                    _healthConnectState.value = HealthConnectState.AvailableAndGranted
                    fetchHealthConnectData()
                } else {
                    _healthConnectState.value = HealthConnectState.NeedsPermission
                }
            } else {
                _healthConnectState.value = availability
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        scope.launch {
            if (granted) {
                _healthConnectState.value = HealthConnectState.AvailableAndGranted
                fetchHealthConnectData()
            } else {
                _healthConnectState.value = HealthConnectState.PermissionDenied()
            }
        }
    }

    suspend fun syncSteps() {
        if (_healthConnectState.value is HealthConnectState.AvailableAndGranted) {
            fetchHealthConnectData()
        }
    }

    fun refreshSteps() {
        scope.launch {
            checkHealthConnectStatus()
            if (_healthConnectState.value is HealthConnectState.AvailableAndGranted) {
                fetchHealthConnectData()
            }
        }
    }

    private suspend fun fetchHealthConnectData() {
        val todaySteps = healthConnectManager.readTodaySteps()
        val past7Days = healthConnectManager.readPast7DaysSteps()

        // If Health Connect has valid records, use them!
        // If it returns 0 (e.g. freshly installed or running on test emulator),
        // we merge with realistic baseline demo records so UI is interactive and populated.
        val totalStepsRecorded = todaySteps + past7Days.sumOf { it.steps }
        if (totalStepsRecorded > 0) {
            _userStats.value = UserStepStats.fromSteps(
                steps = todaySteps,
                weeklyHistory = past7Days,
                isSimulated = false
            )
        } else {
            // Keep active sample stats but flag as connected to Health Connect
            val samples = createInitialSampleStats()
            _userStats.value = samples.copy(isSimulated = true)
        }
    }

    /**
     * Test / demo helper: adds steps so the user can test the spring ring animation,
     * live stat computations, and goal completion in emulator.
     */
    fun addSimulatedSteps(amount: Long) {
        val current = _userStats.value
        val newSteps = current.steps + amount
        val updatedHistory = current.weeklyHistory.map { day ->
            if (day.isToday) day.copy(steps = newSteps) else day
        }
        _userStats.value = UserStepStats.fromSteps(
            steps = newSteps,
            weeklyHistory = updatedHistory,
            isSimulated = true
        )
    }

    fun resetDemoSteps() {
        _userStats.value = createInitialSampleStats()
    }

    companion object {
        @Volatile
        private var INSTANCE: StepRepository? = null

        fun getInstance(context: Context): StepRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StepRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun createInitialSampleStats(): UserStepStats {
            val today = LocalDate.now()
            val sampleDailyValues = listOf(6840L, 8210L, 7450L, 9120L, 6300L, 10450L, 5680L)
            val history = mutableListOf<DailyStepData>()

            for (i in 6 downTo 0) {
                val date = today.minusDays(i.toLong())
                val steps = sampleDailyValues[6 - i]
                val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                val dayInitial = dayName.take(1).uppercase(Locale.getDefault())
                history.add(
                    DailyStepData(
                        date = date,
                        dayInitial = dayInitial,
                        dayName = dayName,
                        steps = steps,
                        isToday = (i == 0),
                        goal = 8000L
                    )
                )
            }

            return UserStepStats.fromSteps(
                steps = 5680L,
                weeklyHistory = history,
                goal = 8000L,
                isSimulated = true
            )
        }
    }
}
