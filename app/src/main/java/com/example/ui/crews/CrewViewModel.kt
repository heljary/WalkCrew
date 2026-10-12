package com.example.ui.crews

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StepRepository
import com.example.data.models.CrewModel
import com.example.data.models.CrewWithMembers
import com.example.data.models.LeaderboardMember
import com.example.data.repository.AuthRepository
import com.example.data.repository.CrewRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class FriendFilter(val label: String) {
    RANKED("Leaderboard"),
    GOAL_MET("Goal Met"),
    ALL("All Friends")
}

class CrewViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.getInstance(application)
    private val crewRepository = CrewRepository.getInstance(application)
    private val stepRepository = StepRepository.getInstance(application)

    val currentUserId: String?
        get() = authRepository.currentUserId

    private val _crews = MutableStateFlow<List<CrewWithMembers>>(emptyList())
    val crews: StateFlow<List<CrewWithMembers>> = _crews.asStateFlow()

    private val _selectedCrewId = MutableStateFlow<String?>(null)
    val selectedCrewId: StateFlow<String?> = _selectedCrewId.asStateFlow()

    private val _activeCrew = MutableStateFlow<CrewWithMembers?>(null)
    val activeCrew: StateFlow<CrewWithMembers?> = _activeCrew.asStateFlow()

    private val _friendsProgress = MutableStateFlow<List<LeaderboardMember>>(emptyList())
    val friendsProgress: StateFlow<List<LeaderboardMember>> = _friendsProgress.asStateFlow()

    private val _friendFilter = MutableStateFlow(FriendFilter.RANKED)
    val friendFilter: StateFlow<FriendFilter> = _friendFilter.asStateFlow()

    private val _nudgedFriends = MutableStateFlow<Set<String>>(emptySet())
    val nudgedFriends: StateFlow<Set<String>> = _nudgedFriends.asStateFlow()

    private val _friendReactions = MutableStateFlow<Map<String, Map<String, Int>>>(emptyMap())
    val friendReactions: StateFlow<Map<String, Map<String, Int>>> = _friendReactions.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

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

    private var crewDetailsJob: Job? = null
    private var reactionsJob: Job? = null

    init {
        loadUserCrews()
        runMigration()
    }

    private fun runMigration() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            crewRepository.migrateOwnedCrewsToInviteCodes(uid)
        }
    }

    fun loadUserCrews() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            crewRepository.observeUserCrews(uid).collect { list ->
                _crews.value = list
                if (list.isNotEmpty()) {
                    val currentSelected = _selectedCrewId.value
                    val targetCrew = list.find { it.crew.crewId == currentSelected } ?: list.first()
                    _selectedCrewId.value = targetCrew.crew.crewId
                    _activeCrew.value = targetCrew
                    observeCrewFriends(targetCrew.crew.crewId)
                } else {
                    _activeCrew.value = null
                    _friendsProgress.value = emptyList()
                }
            }
        }
    }

    fun selectCrew(crewId: String) {
        _selectedCrewId.value = crewId
        val crew = _crews.value.find { it.crew.crewId == crewId }
        _activeCrew.value = crew
        observeCrewFriends(crewId)
    }

    private fun observeCrewFriends(crewId: String) {
        val uid = currentUserId ?: ""
        crewDetailsJob?.cancel()
        reactionsJob?.cancel()

        crewDetailsJob = viewModelScope.launch {
            crewRepository.observeCrewDetails(crewId, uid).collect { details ->
                _friendsProgress.value = details.todayLeaderboard
            }
        }

        reactionsJob = viewModelScope.launch {
            crewRepository.observeCrewReactions(crewId).collect { map ->
                _friendReactions.value = map
            }
        }
    }

    fun setFilter(filter: FriendFilter) {
        _friendFilter.value = filter
    }

    fun sendNudge(friendId: String, friendName: String) {
        if (_nudgedFriends.value.contains(friendId)) {
            _snackbarMessage.value = "You already nudged $friendName today! 👟"
            return
        }

        _nudgedFriends.value = _nudgedFriends.value + friendId
        _snackbarMessage.value = "Nudged $friendName! 👟"

        val uid = currentUserId ?: return
        val activeId = _selectedCrewId.value ?: return

        viewModelScope.launch {
            val userRepo = com.example.data.repository.UserRepository.getInstance(getApplication())
            val myProfile = userRepo.getUserProfile(uid)
            val myName = myProfile?.displayName ?: "A teammate"
            val crewName = _activeCrew.value?.crew?.name ?: "WalkCrew"

            crewRepository.sendNudge(
                crewId = activeId,
                toUserId = friendId,
                fromUserId = uid,
                fromUserName = myName,
                crewName = crewName
            )
        }
    }

    fun cheerFriend(friendId: String, friendName: String, emoji: String = "🔥") {
        val activeId = _selectedCrewId.value ?: ""
        val currentReactions = _friendReactions.value.toMutableMap()
        val friendMap = currentReactions[friendId]?.toMutableMap() ?: mutableMapOf()
        friendMap[emoji] = (friendMap[emoji] ?: 0) + 1
        currentReactions[friendId] = friendMap
        _friendReactions.value = currentReactions

        _snackbarMessage.value = "Sent $emoji to $friendName!"

        val uid = currentUserId ?: return
        if (activeId.isNotBlank()) {
            viewModelScope.launch {
                crewRepository.addReaction(
                    crewId = activeId,
                    targetUserId = friendId,
                    fromUserId = uid,
                    reactionType = emoji
                )
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun openCreateSheet() {
        _errorMessage.value = null
        if (revenueCatManager.isAvailable && !isPremium.value && _crews.value.size >= 2) {
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
        if (revenueCatManager.isAvailable && !isPremium.value && _crews.value.size >= 2) {
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
        if (!revenueCatManager.isAvailable) return
        _paywallReason.value = reason
        _showPaywall.value = true
    }

    fun closePaywall() {
        _showPaywall.value = false
        _paywallReason.value = null
    }

    fun createCrew(name: String, duration: String = "Weekly", onCreated: (CrewModel) -> Unit = {}) {
        val uid = currentUserId ?: return
        if (revenueCatManager.isAvailable && !isPremium.value && _crews.value.size >= 2) {
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
        if (revenueCatManager.isAvailable && !isPremium.value && _crews.value.size >= 2) {
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
                // Successful join will trigger the observeUserCrews flow automatically,
                // but we call loadUserCrews to be sure local state is fresh.
                loadUserCrews()
            }.onFailure { e ->
                val msg = e.localizedMessage ?: "Could not join crew with code $code"
                if (msg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                    _errorMessage.value = "Permission denied, check Firestore rules. Ensure you are signed in."
                } else {
                    _errorMessage.value = msg
                }
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
