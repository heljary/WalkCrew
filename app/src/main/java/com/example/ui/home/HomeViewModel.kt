package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HealthConnectState
import com.example.data.StepRepository
import com.example.data.UserStepStats
import com.example.data.models.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.CrewRepository
import com.example.data.repository.HomeStepUiState
import com.example.data.repository.StepLogRepository
import com.example.data.repository.UserRepository
import com.example.sensor.StepSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val stepRepository = StepRepository.getInstance(application)
    private val stepLogRepository = StepLogRepository.getInstance(application)
    private val stepSensorManager = StepSensorManager.getInstance(application)
    private val authRepository = AuthRepository.getInstance(application)
    private val userRepository = UserRepository.getInstance(application)
    private val crewRepository = CrewRepository.getInstance(application)

    val stepUiState: StateFlow<HomeStepUiState> = stepLogRepository.stepUiState

    val userStats: StateFlow<UserStepStats> = stepRepository.userStats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = stepRepository.userStats.value
        )

    val healthConnectState: StateFlow<HealthConnectState> = stepRepository.healthConnectState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = stepRepository.healthConnectState.value
        )

    val hasPhysicalSensor: Boolean
        get() = stepSensorManager.hasPhysicalSensor()

    val hasActivityRecognitionPermission: StateFlow<Boolean> = stepSensorManager.hasPermission

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    val userName: StateFlow<String>
        get() = MutableStateFlow(_userProfile.value?.displayName?.ifBlank { null } ?: "Walker").asStateFlow()

    init {
        val uid = authRepository.currentUserId
        if (uid != null) {
            stepLogRepository.startObservingUserSteps(uid, 8000L)
        }
        loadUserProfile()
    }

    fun loadUserProfile() {
        val uid = authRepository.currentUserId
        if (uid != null) {
            viewModelScope.launch {
                userRepository.observeUserProfile(uid).collect { profile ->
                    _userProfile.value = profile
                    val goal = profile?.dailyStepGoal ?: 8000L
                    stepLogRepository.startObservingUserSteps(uid, goal)
                }
            }
        }
    }

    fun onActivityRecognitionPermissionResult(granted: Boolean) {
        stepRepository.onActivityRecognitionPermissionResult(granted)
    }

    fun onHealthConnectPermissionResult(granted: Boolean) {
        stepRepository.onHealthConnectPermissionResult(granted)
    }

    fun refreshSteps() {
        val uid = authRepository.currentUserId
        if (uid != null) {
            val goal = _userProfile.value?.dailyStepGoal ?: 8000L
            stepLogRepository.startObservingUserSteps(uid, goal)
        }
        stepRepository.refreshSteps()
    }

    fun retryFetchSteps() {
        refreshSteps()
    }

    fun installHealthConnect() {
        stepRepository.healthConnectManager.openHealthConnectPlayStore()
    }

    fun addSimulatedSteps(amount: Long = 1000L) {
        stepRepository.addSimulatedSteps(amount)
    }

    fun resetTestData() {
        stepRepository.resetTestData()
    }

    fun resetAllProgress() {
        stepRepository.resetAllProgress()
    }

    fun resetDemoSteps() {
        resetAllProgress()
    }

    fun updateDailyGoal(newGoal: Long) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            userRepository.updateDailyGoal(uid, newGoal)
            _userProfile.value = _userProfile.value?.copy(dailyStepGoal = newGoal)
            stepLogRepository.startObservingUserSteps(uid, newGoal)
        }
    }

    fun toggleDailyReminder(enabled: Boolean) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            userRepository.updateReminderEnabled(uid, enabled)
            _userProfile.value = _userProfile.value?.copy(reminderEnabled = enabled)
        }
    }

    fun updateProfile(displayName: String, avatarColorIndex: Int) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            val current = _userProfile.value
            val updated = UserProfile(
                userId = uid,
                displayName = displayName.trim(),
                avatarColorIndex = avatarColorIndex,
                dailyStepGoal = current?.dailyStepGoal ?: 8000L,
                todaySteps = userStats.value.steps,
                reminderEnabled = current?.reminderEnabled ?: true,
                fcmToken = current?.fcmToken ?: "",
                streakDays = current?.streakDays ?: 0
            )
            userRepository.saveUserProfile(updated)
            _userProfile.value = updated
        }
    }

    override fun onCleared() {
        super.onCleared()
        stepLogRepository.stopObserving()
    }
}
