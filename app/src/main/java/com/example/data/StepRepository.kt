package com.example.data

import android.content.Context
import android.util.Log
import com.example.health.HealthConnectManager
import com.example.sensor.StepSensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class StepRepository(
    private val context: Context,
    val healthConnectManager: HealthConnectManager = HealthConnectManager(context),
    val stepSensorManager: StepSensorManager = StepSensorManager.getInstance(context)
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _userStats = MutableStateFlow(createInitialSampleStats())
    val userStats: StateFlow<UserStepStats> = _userStats.asStateFlow()

    private val _healthConnectState = MutableStateFlow<HealthConnectState>(HealthConnectState.Loading)
    val healthConnectState: StateFlow<HealthConnectState> = _healthConnectState.asStateFlow()

    private var healthConnectTodaySteps: Long = 0L
    private var healthConnectPast7Days: List<DailyStepData> = emptyList()

    init {
        // 1. Observe real-time steps directly from device hardware step sensor
        stepSensorManager.stepCount
            .onEach { hardwareSteps ->
                updateCombinedStats(hardwareSteps)
            }
            .launchIn(scope)

        // 2. Start hardware sensor listener if permissions/sensors are present
        stepSensorManager.startListening()
        stepSensorManager.startForegroundServiceIfPossible()

        // 3. Check Health Connect availability
        checkHealthConnectStatus()
    }

    fun onActivityRecognitionPermissionResult(granted: Boolean) {
        stepSensorManager.updatePermissionStatus(granted)
        if (granted) {
            stepSensorManager.startListening()
            stepSensorManager.startForegroundServiceIfPossible()
        }
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

    fun onHealthConnectPermissionResult(granted: Boolean) {
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
            } else {
                updateCombinedStats(stepSensorManager.stepCount.value)
            }
        }
    }

    private suspend fun fetchHealthConnectData() {
        try {
            healthConnectTodaySteps = healthConnectManager.readTodaySteps()
            healthConnectPast7Days = healthConnectManager.readPast7DaysSteps()
            updateCombinedStats(stepSensorManager.stepCount.value)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Health Connect data: ${e.message}")
        }
    }

    private fun updateCombinedStats(hardwareSteps: Long) {
        // Dual step tracking logic:
        // Use the highest of hardware sensor steps or Health Connect steps for today.
        // This ensures if the user walks with the phone, the hardware sensor immediately counts.
        // If they wore a smartwatch/external tracker that synced to Health Connect, that higher value is preserved.
        val effectiveTodaySteps = maxOf(hardwareSteps, healthConnectTodaySteps)

        val today = LocalDate.now()
        val history = if (healthConnectPast7Days.isNotEmpty()) {
            healthConnectPast7Days.map { day ->
                if (day.isToday) day.copy(steps = effectiveTodaySteps) else day
            }
        } else {
            // Build base history from local archive if available
            (6 downTo 0).map { i ->
                val date = today.minusDays(i.toLong())
                val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                val dayInitial = dayName.take(1).uppercase(Locale.getDefault())
                val archived = if (i == 0) effectiveTodaySteps else stepSensorManager.getArchivedStepsForDate(date.toString())
                DailyStepData(
                    date = date,
                    dayInitial = dayInitial,
                    dayName = dayName,
                    steps = archived,
                    isToday = (i == 0),
                    goal = 8000L
                )
            }
        }

        val currentGoal = _userStats.value.goal
        _userStats.value = UserStepStats.fromSteps(
            steps = effectiveTodaySteps,
            weeklyHistory = history,
            goal = currentGoal,
            isSimulated = false
        )
    }

    /**
     * Test / demo helper
     */
    fun addSimulatedSteps(amount: Long) {
        stepSensorManager.addManualSteps(amount)
    }

    fun resetAllProgress() {
        stepSensorManager.resetSteps()
        healthConnectTodaySteps = 0L
        _userStats.value = createInitialSampleStats()
    }

    fun resetDemoSteps() {
        resetAllProgress()
    }

    companion object {
        private const val TAG = "StepRepository"

        @Volatile
        private var INSTANCE: StepRepository? = null

        fun getInstance(context: Context): StepRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StepRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun createInitialSampleStats(): UserStepStats {
            val today = LocalDate.now()
            val history = mutableListOf<DailyStepData>()

            for (i in 6 downTo 0) {
                val date = today.minusDays(i.toLong())
                val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                val dayInitial = dayName.take(1).uppercase(Locale.getDefault())
                history.add(
                    DailyStepData(
                        date = date,
                        dayInitial = dayInitial,
                        dayName = dayName,
                        steps = 0L,
                        isToday = (i == 0),
                        goal = 8000L
                    )
                )
            }

            return UserStepStats.fromSteps(
                steps = 0L,
                weeklyHistory = history,
                goal = 8000L,
                isSimulated = false
            )
        }
    }
}
