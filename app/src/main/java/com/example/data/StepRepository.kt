package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.repository.AuthRepository
import com.example.data.repository.HomeStepUiState
import com.example.data.repository.StepLogRepository
import com.example.health.HealthConnectManager
import com.example.sensor.StepSensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class StepRepository(
    private val context: Context,
    val healthConnectManager: HealthConnectManager = HealthConnectManager(context),
    val stepSensorManager: StepSensorManager = StepSensorManager.getInstance(context)
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stepLogRepository = StepLogRepository.getInstance(context)
    private val authRepository = AuthRepository.getInstance(context)

    val stepUiState: StateFlow<HomeStepUiState> = stepLogRepository.stepUiState

    val userStats: StateFlow<UserStepStats> = stepLogRepository.stepUiState.map { state ->
        when (state) {
            is HomeStepUiState.Success -> state.stats
            is HomeStepUiState.Loading -> createEmptyInitialStats()
            is HomeStepUiState.NoData -> createEmptyInitialStats(state.goal)
            is HomeStepUiState.Error -> createEmptyInitialStats()
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = createEmptyInitialStats()
    )

    private val _healthConnectState = MutableStateFlow<HealthConnectState>(HealthConnectState.Loading)
    val healthConnectState: StateFlow<HealthConnectState> = _healthConnectState.asStateFlow()

    init {
        // 1. Observe real-time steps directly from device hardware step sensor
        stepSensorManager.stepCount
            .onEach { hardwareSteps ->
                if (hardwareSteps > 0L) {
                    val uid = authRepository.currentUserId
                    if (uid != null) {
                        stepLogRepository.recordStepsIfGreater(
                            userId = uid,
                            date = LocalDate.now().toString(),
                            newSteps = hardwareSteps,
                            source = "hardware_sensor"
                        )
                    }
                }
            }
            .launchIn(scope)

        // 2. Start hardware sensor listener if permissions/sensors are present
        if (stepSensorManager.hasPermission.value) {
            stepSensorManager.startListening()
            stepSensorManager.startForegroundServiceIfPossible()
        }

        // 3. Check Health Connect availability
        checkHealthConnectStatus()
    }

    fun onActivityRecognitionPermissionResult(granted: Boolean) {
        stepSensorManager.updatePermissionStatus(granted)
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
            }
        }
    }

    private suspend fun fetchHealthConnectData() {
        try {
            val uid = authRepository.currentUserId ?: return
            val healthConnectTodaySteps = healthConnectManager.readTodaySteps()
            val healthConnectPast7Days = healthConnectManager.readPast7DaysSteps()

            // Requirement 2:
            // Write ONLY if new value is greater than stored value.
            // If Health Connect returns 0 or fails, do NOT write anything.
            if (healthConnectTodaySteps > 0L) {
                stepLogRepository.recordStepsIfGreater(
                    userId = uid,
                    date = LocalDate.now().toString(),
                    newSteps = healthConnectTodaySteps,
                    source = "health_connect"
                )
            }

            for (day in healthConnectPast7Days) {
                if (day.steps > 0L) {
                    stepLogRepository.recordStepsIfGreater(
                        userId = uid,
                        date = day.date.toString(),
                        newSteps = day.steps,
                        source = "health_connect"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Health Connect data: ${e.message}")
        }
    }

    /**
     * Test / demo helper: adds steps to Firestore and updates UI via real snapshot listener
     */
    fun addSimulatedSteps(amount: Long) {
        val uid = authRepository.currentUserId ?: return
        scope.launch {
            stepLogRepository.addSteps(uid, LocalDate.now().toString(), amount)
        }
    }

    fun resetTestData() {
        stepSensorManager.resetSteps()
        val uid = authRepository.currentUserId ?: return
        scope.launch {
            stepLogRepository.resetTestData(uid)
        }
    }

    fun resetAllProgress() {
        resetTestData()
    }

    fun resetDemoSteps() {
        resetTestData()
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

        private fun createEmptyInitialStats(goal: Long = 8000L): UserStepStats {
            val today = LocalDate.now()
            val history = mutableListOf<DailyStepData>()

            for (i in 6 downTo 0) {
                val date = today.minusDays(i.toLong())
                val dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
                val dayInitial = dayName.take(1).uppercase(java.util.Locale.getDefault())
                history.add(
                    DailyStepData(
                        date = date,
                        dayInitial = dayInitial,
                        dayName = dayName,
                        steps = 0L,
                        isToday = (i == 0),
                        goal = goal
                    )
                )
            }

            return UserStepStats.fromSteps(
                steps = 0L,
                weeklyHistory = history,
                goal = goal,
                isSimulated = false
            )
        }
    }
}
