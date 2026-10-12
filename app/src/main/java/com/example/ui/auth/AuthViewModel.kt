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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

/**
 * Tracks the specific auth action currently being processed.
 */
enum class AuthAction {
    NONE,
    GOOGLE,
    EMAIL_SIGN_IN,
    EMAIL_SIGN_UP,
    EMAIL_VERIFY,
    PASSWORD_RESET,
    PROFILE_SETUP,
    REAUTHENTICATE_DELETE
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

    private val _activeAuthAction = MutableStateFlow(AuthAction.NONE)
    val activeAuthAction: StateFlow<AuthAction> = _activeAuthAction.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    // Distinct loading states for individual buttons
    val isGoogleLoading: StateFlow<Boolean> = _activeAuthAction
        .map { it == AuthAction.GOOGLE }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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
        if (_activeAuthAction.value != AuthAction.NONE || _isSigningIn.value) return
        viewModelScope.launch {
            _activeAuthAction.value = AuthAction.EMAIL_SIGN_UP
            _isSigningIn.value = true
            _errorMessage.value = null
            try {
                val result = authRepository.signUpWithEmail(email, pass, name)
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
            } finally {
                _activeAuthAction.value = AuthAction.NONE
                _isSigningIn.value = false
            }
        }
    }

    // --- Sign In With Email ---
    fun signInWithEmail(email: String, pass: String) {
        if (_activeAuthAction.value != AuthAction.NONE || _isSigningIn.value) return
        viewModelScope.launch {
            _activeAuthAction.value = AuthAction.EMAIL_SIGN_IN
            _isSigningIn.value = true
            _errorMessage.value = null
            try {
                val result = authRepository.signInWithEmail(email, pass)
                result.onSuccess { user ->
                    if (!authRepository.isEmailVerified()) {
                        authRepository.sendVerificationCode()
                        startCooldownTimer()
                        _uiState.value = AuthUiState.EmailVerificationRequired(user)
                    } else {
                        loadUserProfile(user)
                    }
                }.onFailure { error ->
                    _errorMessage.value = AuthRepository.getFriendlyErrorMessage(error)
                }
            } finally {
                _activeAuthAction.value = AuthAction.NONE
                _isSigningIn.value = false
            }
        }
    }

    // --- Password Reset ---
    fun sendPasswordReset(email: String, onSuccess: () -> Unit) {
        if (_activeAuthAction.value != AuthAction.NONE || _isSigningIn.value) return
        viewModelScope.launch {
            _activeAuthAction.value = AuthAction.PASSWORD_RESET
            _isSigningIn.value = true
            _errorMessage.value = null
            try {
                val result = authRepository.sendPasswordResetEmail(email)
                result.onSuccess {
                    _successMessage.value = "Password reset link sent to $email! Please check your email."
                    onSuccess()
                }.onFailure { error ->
                    _errorMessage.value = AuthRepository.getFriendlyErrorMessage(error)
                }
            } finally {
                _activeAuthAction.value = AuthAction.NONE
                _isSigningIn.value = false
            }
        }
    }

    // --- Verify Email Code (OTP) ---
    fun verifyEmailCode(code: String) {
        if (_activeAuthAction.value != AuthAction.NONE || _isSigningIn.value || code.length != 6) return
        viewModelScope.launch {
            _activeAuthAction.value = AuthAction.EMAIL_VERIFY
            _isSigningIn.value = true
            _errorMessage.value = null
            try {
                val result = authRepository.verifyEmailCode(code)
                result.onSuccess {
                    _successMessage.value = "Email verified successfully!"
                    val user = authRepository.auth.currentUser
                    if (user != null) {
                        val fallbackName = user.displayName?.takeIf { it.isNotBlank() } ?: "Walker"
                        _uiState.value = AuthUiState.NeedsProfileSetup(
                            user = user,
                            defaultName = fallbackName
                        )
                    }
                }.onFailure { error ->
                    _errorMessage.value = AuthRepository.getFriendlyErrorMessage(error)
                }
            } finally {
                _activeAuthAction.value = AuthAction.NONE
                _isSigningIn.value = false
            }
        }
    }

    // --- Resend Verification Code ---
    fun resendVerificationEmail() {
        if (_resendCooldown.value > 0 || _activeAuthAction.value != AuthAction.NONE || _isSigningIn.value) return
        viewModelScope.launch {
            _activeAuthAction.value = AuthAction.EMAIL_VERIFY
            _isSigningIn.value = true
            _errorMessage.value = null
            try {
                val result = authRepository.sendVerificationCode()
                result.onSuccess {
                    startCooldownTimer()
                    _successMessage.value = "Verification code resent! Check your inbox."
                }.onFailure { error ->
                    _errorMessage.value = AuthRepository.getFriendlyErrorMessage(error)
                }
            } finally {
                _activeAuthAction.value = AuthAction.NONE
                _isSigningIn.value = false
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
        if (_activeAuthAction.value != AuthAction.NONE || _isSigningIn.value) return
        viewModelScope.launch {
            _activeAuthAction.value = AuthAction.GOOGLE
            _isSigningIn.value = true
            _errorMessage.value = null
            try {
                val result = authRepository.signInWithGoogle(activity)
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
            } finally {
                _activeAuthAction.value = AuthAction.NONE
                _isSigningIn.value = false
            }
        }
    }



    fun dismissCollisionDialog() {
        _collisionDialog.value = null
    }

    // --- Complete Profile Setup ---
    fun completeProfileSetup(displayName: String, avatarColorIndex: Int) {
        val currentUser = authRepository.auth.currentUser ?: return
        if (_activeAuthAction.value != AuthAction.NONE || _isSigningIn.value) return
        viewModelScope.launch {
            _activeAuthAction.value = AuthAction.PROFILE_SETUP
            _isSigningIn.value = true
            try {
                val profile = UserProfile(
                    userId = currentUser.uid,
                    displayName = displayName.trim().ifBlank { "Walker" },
                    avatarColorIndex = avatarColorIndex,
                    dailyStepGoal = 8000L,
                    todaySteps = 0L
                )
                userRepository.saveUserProfile(profile)
                _uiState.value = AuthUiState.Authenticated(profile)
            } finally {
                _activeAuthAction.value = AuthAction.NONE
                _isSigningIn.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun getCurrentUserProvider(): String {
        return authRepository.getCurrentUserProvider()
    }

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
            _activeAuthAction.value = AuthAction.REAUTHENTICATE_DELETE
            _isSigningIn.value = true
            _errorMessage.value = null
            try {
                val reauthResult = authRepository.reauthenticateWithPassword(password)
                if (reauthResult.isSuccess) {
                    deleteAccountInternal(onSuccess)
                } else {
                    _errorMessage.value = AuthRepository.getFriendlyErrorMessage(reauthResult.exceptionOrNull())
                }
            } finally {
                _activeAuthAction.value = AuthAction.NONE
                _isSigningIn.value = false
            }
        }
    }

    fun reauthenticateAndDeleteWithGoogle(activity: Activity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _activeAuthAction.value = AuthAction.REAUTHENTICATE_DELETE
            _isSigningIn.value = true
            _errorMessage.value = null
            try {
                val reauthResult = authRepository.reauthenticateWithGoogle(activity)
                if (reauthResult.isSuccess) {
                    deleteAccountInternal(onSuccess)
                } else {
                    _errorMessage.value = AuthRepository.getFriendlyErrorMessage(reauthResult.exceptionOrNull())
                }
            } finally {
                _activeAuthAction.value = AuthAction.NONE
                _isSigningIn.value = false
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

    val currentUserId: String?
        get() = authRepository.currentUserId

    val currentUserEmail: String?
        get() = authRepository.auth.currentUser?.email

    val isEmailVerified: Boolean
        get() = authRepository.isEmailVerified()
}
