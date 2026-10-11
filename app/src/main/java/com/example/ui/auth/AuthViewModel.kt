package com.example.ui.auth

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Loading : AuthUiState()
    data object Unauthenticated : AuthUiState()
    data class EmailVerificationRequired(val user: FirebaseUser) : AuthUiState()
    data class NeedsProfileSetup(val user: FirebaseUser, val defaultName: String) : AuthUiState()
    data class Authenticated(val profile: UserProfile) : AuthUiState()
}

enum class UnauthScreen {
    WELCOME,
    SIGN_IN,
    SIGN_UP,
    FORGOT_PASSWORD
}

data class CollisionDialogState(
    val email: String,
    val existingMethod: String
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.getInstance(application)
    private val userRepository = UserRepository.getInstance(application)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _unauthScreen = MutableStateFlow(UnauthScreen.WELCOME)
    val unauthScreen: StateFlow<UnauthScreen> = _unauthScreen.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _collisionDialog = MutableStateFlow<CollisionDialogState?>(null)
    val collisionDialog: StateFlow<CollisionDialogState?> = _collisionDialog.asStateFlow()

    private val _resendCooldown = MutableStateFlow(0)
    val resendCooldown: StateFlow<Int> = _resendCooldown.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        checkCurrentUser()
    }

    fun navigateToUnauthScreen(screen: UnauthScreen) {
        _errorMessage.value = null
        _unauthScreen.value = screen
    }

    fun checkCurrentUser() {
        val user = authRepository.auth.currentUser
        if (user == null) {
            _uiState.value = AuthUiState.Unauthenticated
        } else {
            // Check if user requires email verification (email/password only)
            if (!authRepository.isEmailVerified()) {
                _uiState.value = AuthUiState.EmailVerificationRequired(user)
            } else {
                loadUserProfile(user)
            }
        }
    }

    private fun loadUserProfile(user: FirebaseUser) {
        viewModelScope.launch {
            try {
                com.example.billing.RevenueCatManager.getInstance(getApplication()).setAppUserId(user.uid)
            } catch (e: Exception) {
                // Ignore RevenueCat billing issues
            }

            val profile = userRepository.getUserProfile(user.uid)
            if (profile != null && profile.displayName.isNotBlank()) {
                _uiState.value = AuthUiState.Authenticated(profile)
            } else {
                val fallbackName = user.displayName?.takeIf { it.isNotBlank() } ?: "Walker"
                _uiState.value = AuthUiState.NeedsProfileSetup(
                    user = user,
                    defaultName = fallbackName
                )
            }
        }
    }

    // --- Sign Up With Email ---
    fun signUpWithEmail(name: String, email: String, pass: String) {
        if (_isSigningIn.value) return
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val result = authRepository.signUpWithEmail(email, pass, name)
            _isSigningIn.value = false

            result.onSuccess { user ->
                // Create initial Firestore user document
                val profile = UserProfile(
                    userId = user.uid,
                    displayName = name.trim().ifBlank { "Walker" },
                    avatarColorIndex = 2,
                    dailyStepGoal = 8000L,
                    todaySteps = 0L
                )
                userRepository.saveUserProfile(profile)

                startCooldownTimer()
                _uiState.value = AuthUiState.EmailVerificationRequired(user)
            }.onFailure { error ->
                _errorMessage.value = AuthRepository.getFriendlyErrorMessage(error)
            }
        }
    }

    // --- Sign In With Email ---
    fun signInWithEmail(email: String, pass: String) {
        if (_isSigningIn.value) return
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val result = authRepository.signInWithEmail(email, pass)
            _isSigningIn.value = false

            result.onSuccess { user ->
                if (!authRepository.isEmailVerified()) {
                    _uiState.value = AuthUiState.EmailVerificationRequired(user)
                } else {
                    loadUserProfile(user)
                }
            }.onFailure { error ->
                _errorMessage.value = AuthRepository.getFriendlyErrorMessage(error)
            }
        }
    }

    // --- Password Reset ---
    fun sendPasswordReset(email: String, onSuccess: () -> Unit) {
        if (_isSigningIn.value) return
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val result = authRepository.sendPasswordResetEmail(email)
            _isSigningIn.value = false

            result.onSuccess {
                _successMessage.value = "Password reset link sent to $email! Please check your email."
                onSuccess()
            }.onFailure { error ->
                _errorMessage.value = AuthRepository.getFriendlyErrorMessage(error)
            }
        }
    }

    // --- Check & Reload Email Verification ---
    fun checkEmailVerification() {
        if (_isSigningIn.value) return
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val result = authRepository.reloadUser()
            _isSigningIn.value = false

            result.onSuccess { user ->
                if (user.isEmailVerified) {
                    _successMessage.value = "Email verified successfully!"
                    loadUserProfile(user)
                } else {
                    _errorMessage.value = "Email not verified yet. Please click the link sent to ${user.email}."
                }
            }.onFailure { error ->
                _errorMessage.value = AuthRepository.getFriendlyErrorMessage(error)
            }
        }
    }

    // --- Resend Email Verification ---
    fun resendVerificationEmail() {
        if (_resendCooldown.value > 0 || _isSigningIn.value) return
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val result = authRepository.sendEmailVerification()
            _isSigningIn.value = false

            result.onSuccess {
                startCooldownTimer()
                _successMessage.value = "Verification link resent! Check your inbox."
            }.onFailure { error ->
                _errorMessage.value = AuthRepository.getFriendlyErrorMessage(error)
            }
        }
    }

    private fun startCooldownTimer() {
        cooldownJob?.cancel()
        _resendCooldown.value = 60
        cooldownJob = viewModelScope.launch {
            for (sec in 59 downTo 0) {
                delay(1000L)
                _resendCooldown.value = sec
            }
        }
    }

    // --- Google Sign-In ---
    fun signInWithGoogle(activity: Activity) {
        if (_isSigningIn.value) return
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val result = authRepository.signInWithGoogle(activity)
            _isSigningIn.value = false

            result.onSuccess { user ->
                loadUserProfile(user)
            }.onFailure { error ->
                if (error is FirebaseAuthUserCollisionException) {
                    val collision = authRepository.pendingCollision
                    _collisionDialog.value = CollisionDialogState(
                        email = collision?.email ?: "",
                        existingMethod = collision?.existingProvider ?: "another provider"
                    )
                } else {
                    _errorMessage.value = AuthRepository.getFriendlyErrorMessage(error)
                }
            }
        }
    }

    // --- Facebook Sign-In ---
    fun signInWithFacebook(activity: Activity) {
        if (_isSigningIn.value) return
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val result = authRepository.signInWithFacebook(activity)
            _isSigningIn.value = false

            result.onSuccess { user ->
                loadUserProfile(user)
            }.onFailure { error ->
                if (error is FirebaseAuthUserCollisionException) {
                    val collision = authRepository.pendingCollision
                    _collisionDialog.value = CollisionDialogState(
                        email = collision?.email ?: "",
                        existingMethod = collision?.existingProvider ?: "another provider"
                    )
                } else {
                    _errorMessage.value = AuthRepository.getFriendlyErrorMessage(error)
                }
            }
        }
    }

    fun dismissCollisionDialog() {
        _collisionDialog.value = null
    }

    // --- Complete Profile Setup ---
    fun completeProfileSetup(displayName: String, avatarColorIndex: Int) {
        val currentUser = authRepository.auth.currentUser ?: return
        if (_isSigningIn.value) return
        viewModelScope.launch {
            _isSigningIn.value = true
            val profile = UserProfile(
                userId = currentUser.uid,
                displayName = displayName.trim().ifBlank { "Walker" },
                avatarColorIndex = avatarColorIndex,
                dailyStepGoal = 8000L,
                todaySteps = 0L
            )
            val result = userRepository.saveUserProfile(profile)
            _isSigningIn.value = false

            result.onSuccess {
                _uiState.value = AuthUiState.Authenticated(profile)
            }.onFailure { error ->
                _errorMessage.value = AuthRepository.getFriendlyErrorMessage(error)
            }
        }
    }

    // --- Sign Out ---
    fun signOut() {
        try {
            com.example.billing.RevenueCatManager.getInstance(getApplication()).logOut()
        } catch (e: Exception) {
            // Ignore
        }
        authRepository.signOut(viewModelScope) {
            _unauthScreen.value = UnauthScreen.WELCOME
            _uiState.value = AuthUiState.Unauthenticated
        }
    }

    // --- Re-Authenticate & Delete Account ---
    fun reauthenticateAndDeleteWithPassword(password: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val reauthResult = authRepository.reauthenticateWithPassword(password)
            if (reauthResult.isSuccess) {
                deleteAccountInternal(onSuccess)
            } else {
                _isSigningIn.value = false
                _errorMessage.value = AuthRepository.getFriendlyErrorMessage(reauthResult.exceptionOrNull())
            }
        }
    }

    fun reauthenticateAndDeleteWithGoogle(activity: Activity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val reauthResult = authRepository.reauthenticateWithGoogle(activity)
            if (reauthResult.isSuccess) {
                deleteAccountInternal(onSuccess)
            } else {
                _isSigningIn.value = false
                _errorMessage.value = AuthRepository.getFriendlyErrorMessage(reauthResult.exceptionOrNull())
            }
        }
    }

    fun reauthenticateAndDeleteWithFacebook(activity: Activity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val reauthResult = authRepository.reauthenticateWithFacebook(activity)
            if (reauthResult.isSuccess) {
                deleteAccountInternal(onSuccess)
            } else {
                _isSigningIn.value = false
                _errorMessage.value = AuthRepository.getFriendlyErrorMessage(reauthResult.exceptionOrNull())
            }
        }
    }

    private suspend fun deleteAccountInternal(onSuccess: () -> Unit) {
        val uid = authRepository.currentUserId
        if (uid != null) {
            try {
                userRepository.deleteUserAccount(uid)
            } catch (e: Exception) {
                // Proceed with auth deletion
            }
        }
        val result = authRepository.deleteAccount()
        _isSigningIn.value = false
        result.onSuccess {
            _unauthScreen.value = UnauthScreen.WELCOME
            _uiState.value = AuthUiState.Unauthenticated
            onSuccess()
        }.onFailure { error ->
            _errorMessage.value = AuthRepository.getFriendlyErrorMessage(error)
        }
    }

    fun getCurrentUserProvider(): String {
        return authRepository.getCurrentUserProvider()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
