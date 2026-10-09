package com.example.ui.crews

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.CrewModel
import com.example.data.models.CrewWithMembers
import com.example.data.repository.AuthRepository
import com.example.data.repository.CrewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CrewViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.getInstance(application)
    private val crewRepository = CrewRepository.getInstance(application)

    private val currentUserId: String?
        get() = authRepository.currentUserId

    private val _crews = MutableStateFlow<List<CrewWithMembers>>(emptyList())
    val crews: StateFlow<List<CrewWithMembers>> = _crews.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showCreateSheet = MutableStateFlow(false)
    val showCreateSheet: StateFlow<Boolean> = _showCreateSheet.asStateFlow()

    private val _showJoinSheet = MutableStateFlow(false)
    val showJoinSheet: StateFlow<Boolean> = _showJoinSheet.asStateFlow()

    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    private val _paywallReason = MutableStateFlow<String?>(null)
    val paywallReason: StateFlow<String?> = _paywallReason.asStateFlow()

    private val revenueCatManager = com.example.billing.RevenueCatManager.getInstance(application)
    val isPremium: StateFlow<Boolean> = revenueCatManager.isPremium

    init {
        loadUserCrews()
    }

    fun loadUserCrews() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            crewRepository.observeUserCrews(uid).collect { list ->
                _crews.value = list
            }
        }
    }

    fun openCreateSheet() {
        _errorMessage.value = null
        if (!isPremium.value && _crews.value.size >= 2) {
            _paywallReason.value = "Free members can join up to 2 crews. Upgrade to Premium for unlimited crews!"
            _showPaywall.value = true
            return
        }
        _showCreateSheet.value = true
    }

    fun closeCreateSheet() {
        _showCreateSheet.value = false
    }

    fun openJoinSheet() {
        _errorMessage.value = null
        if (!isPremium.value && _crews.value.size >= 2) {
            _paywallReason.value = "Free members can join up to 2 crews. Upgrade to Premium for unlimited crews!"
            _showPaywall.value = true
            return
        }
        _showJoinSheet.value = true
    }

    fun closeJoinSheet() {
        _showJoinSheet.value = false
    }

    fun openPaywall(reason: String? = null) {
        _paywallReason.value = reason
        _showPaywall.value = true
    }

    fun closePaywall() {
        _showPaywall.value = false
        _paywallReason.value = null
    }

    fun createCrew(name: String, duration: String = "Weekly", onCreated: (CrewModel) -> Unit = {}) {
        val uid = currentUserId ?: return
        if (!isPremium.value && _crews.value.size >= 2) {
            _showCreateSheet.value = false
            _paywallReason.value = "Free members can join up to 2 crews. Upgrade to Premium for unlimited crews!"
            _showPaywall.value = true
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = crewRepository.createCrew(name, duration, uid)
            _isLoading.value = false

            result.onSuccess { crew ->
                _showCreateSheet.value = false
                onCreated(crew)
                loadUserCrews()
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Failed to create crew"
            }
        }
    }

    fun joinCrewWithCode(code: String, onJoined: (CrewModel) -> Unit = {}) {
        val uid = currentUserId ?: return
        if (!isPremium.value && _crews.value.size >= 2) {
            _showJoinSheet.value = false
            _paywallReason.value = "Free members can join up to 2 crews. Upgrade to Premium for unlimited crews!"
            _showPaywall.value = true
            return
        }
        if (code.length != 6) {
            _errorMessage.value = "Invite code must be 6 characters"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = crewRepository.joinCrewWithCode(code, uid)
            _isLoading.value = false

            result.onSuccess { crew ->
                _showJoinSheet.value = false
                onJoined(crew)
                loadUserCrews()
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Could not find crew with code $code"
            }
        }
    }

    fun shareInviteCode(context: Context, code: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Join my WalkCrew: $code")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share WalkCrew Invite Code")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
