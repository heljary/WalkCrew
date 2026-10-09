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
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val stepRepository = StepRepository.getInstance(application)
    private val authRepository = AuthRepository.getInstance(application)
    private val userRepository = UserRepository.getInstance(application)
    private val crewRepository = CrewRepository.getInstance(application)

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

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    val userName: StateFlow<String>
        get() = MutableStateFlow(_userProfile.value?.displayName ?: "Alex").asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val uid = authRepository.currentUserId
        if (uid != null) {
            viewModelScope.launch {
                userRepository.observeUserProfile(uid).collect { profile ->
                    _userProfile.value = profile
                }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        stepRepository.onPermissionResult(granted)
    }

    fun refreshSteps() {
        stepRepository.refreshSteps()
        syncToFirestore()
    }

    fun installHealthConnect() {
        stepRepository.healthConnectManager.openHealthConnectPlayStore()
    }

    fun addSimulatedSteps(amount: Long = 1000L) {
        stepRepository.addSimulatedSteps(amount)
        syncToFirestore()
    }

    fun resetDemoSteps() {
        stepRepository.resetDemoSteps()
        syncToFirestore()
    }

    private fun syncToFirestore() {
        val uid = authRepository.currentUserId ?: return
        val steps = userStats.value.steps
        viewModelScope.launch {
            userRepository.updateTodaySteps(uid, steps)
            crewRepository.syncUserStepsToCrews(uid, steps)
        }
    }

    fun updateDailyGoal(newGoal: Long) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            userRepository.updateDailyGoal(uid, newGoal)
            _userProfile.value = _userProfile.value?.copy(dailyStepGoal = newGoal)
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
}
