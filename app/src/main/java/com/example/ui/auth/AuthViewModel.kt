package com.example.ui.auth

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Loading : AuthUiState()
    data object Unauthenticated : AuthUiState()
    data class NeedsProfileSetup(val user: FirebaseUser, val defaultName: String) : AuthUiState()
    data class Authenticated(val profile: UserProfile) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.getInstance(application)
    private val userRepository = UserRepository.getInstance(application)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        checkCurrentUser()
    }

    fun checkCurrentUser() {
        val user = authRepository.auth.currentUser
        if (user == null) {
            _uiState.value = AuthUiState.Unauthenticated
        } else {
            loadUserProfile(user)
        }
    }

    private fun loadUserProfile(user: FirebaseUser) {
        viewModelScope.launch {
            com.example.billing.RevenueCatManager.getInstance(getApplication()).setAppUserId(user.uid)
            val profile = userRepository.getUserProfile(user.uid)
            if (profile != null && profile.displayName.isNotBlank()) {
                _uiState.value = AuthUiState.Authenticated(profile)
            } else {
                _uiState.value = AuthUiState.NeedsProfileSetup(
                    user = user,
                    defaultName = user.displayName ?: "Walker"
                )
            }
        }
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val result = authRepository.signInWithGoogle(activity)
            _isSigningIn.value = false

            result.onSuccess { user ->
                loadUserProfile(user)
            }.onFailure { error ->
                _errorMessage.value = error.localizedMessage ?: "Failed to sign in with Google"
            }
        }
    }

    fun completeProfileSetup(displayName: String, avatarColorIndex: Int) {
        val currentUser = authRepository.auth.currentUser ?: return
        viewModelScope.launch {
            _isSigningIn.value = true
            val profile = UserProfile(
                userId = currentUser.uid,
                displayName = displayName.trim(),
                avatarColorIndex = avatarColorIndex,
                dailyStepGoal = 8000L,
                todaySteps = 0L
            )
            val result = userRepository.saveUserProfile(profile)
            _isSigningIn.value = false

            result.onSuccess {
                _uiState.value = AuthUiState.Authenticated(profile)
            }.onFailure { error ->
                _errorMessage.value = error.localizedMessage ?: "Failed to save profile"
            }
        }
    }

    fun signOut() {
        com.example.billing.RevenueCatManager.getInstance(getApplication()).logOut()
        authRepository.signOut(viewModelScope) {
            _uiState.value = AuthUiState.Unauthenticated
        }
    }

    fun deleteAccount(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSigningIn.value = true
            com.example.billing.RevenueCatManager.getInstance(getApplication()).logOut()
            val result = authRepository.deleteAccount()
            _isSigningIn.value = false
            result.onSuccess {
                _uiState.value = AuthUiState.Unauthenticated
                onSuccess()
            }.onFailure { error ->
                _errorMessage.value = error.localizedMessage ?: "Failed to delete account"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
