package com.example.ui.crews

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.CrewHistoryModel
import com.example.data.repository.AuthRepository
import com.example.data.repository.CrewDetailData
import com.example.data.repository.CrewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class LeaderboardTab(val title: String) {
    TODAY("Today"),
    THIS_WEEK("This week")
}

class CrewDetailViewModel(
    application: Application,
    private val crewId: String
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.getInstance(application)
    private val crewRepository = CrewRepository.getInstance(application)

    val currentUserId: String = authRepository.currentUserId ?: ""

    private val _detailState = MutableStateFlow(CrewDetailData(isLoading = true))
    val detailState: StateFlow<CrewDetailData> = _detailState.asStateFlow()

    private val _selectedTab = MutableStateFlow(LeaderboardTab.THIS_WEEK)
    val selectedTab: StateFlow<LeaderboardTab> = _selectedTab.asStateFlow()

    private val _reactions = MutableStateFlow<Map<String, Map<String, Int>>>(emptyMap())
    val reactions: StateFlow<Map<String, Map<String, Int>>> = _reactions.asStateFlow()

    private val _nudgedUsersToday = MutableStateFlow<Set<String>>(emptySet())
    val nudgedUsersToday: StateFlow<Set<String>> = _nudgedUsersToday.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        observeDetails()
        observeReactions()
    }

    private fun observeDetails() {
        viewModelScope.launch {
            crewRepository.observeCrewDetails(crewId, currentUserId).collect { data ->
                _detailState.value = data
            }
        }
    }

    private fun observeReactions() {
        viewModelScope.launch {
            crewRepository.observeCrewReactions(crewId).collect { map ->
                _reactions.value = map
            }
        }
    }

    fun sendNudge(toUserId: String, toUserName: String) {
        if (_nudgedUsersToday.value.contains(toUserId)) {
            _snackbarMessage.value = "You already nudged $toUserName today!"
            return
        }

        viewModelScope.launch {
            val userRepo = com.example.data.repository.UserRepository.getInstance(getApplication())
            val myProfile = userRepo.getUserProfile(currentUserId)
            val myName = myProfile?.displayName ?: "A teammate"
            val crewName = _detailState.value.crew.name

            val result = crewRepository.sendNudge(
                crewId = crewId,
                toUserId = toUserId,
                fromUserId = currentUserId,
                fromUserName = myName,
                crewName = crewName
            )

            if (result.isSuccess) {
                _nudgedUsersToday.value = _nudgedUsersToday.value + toUserId
                _snackbarMessage.value = "Nudged $toUserName! 👟"
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to nudge"
                _snackbarMessage.value = err
            }
        }
    }

    fun addReaction(targetUserId: String, reactionType: String) {
        viewModelScope.launch {
            crewRepository.addReaction(
                crewId = crewId,
                targetUserId = targetUserId,
                fromUserId = currentUserId,
                reactionType = reactionType
            )
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun selectTab(tab: LeaderboardTab) {
        _selectedTab.value = tab
    }

    fun shareInviteCode(context: Context) {
        val code = _detailState.value.crew.inviteCode
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Join my WalkCrew: $code")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share WalkCrew Invite Code")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }

    fun archiveWeekResults() {
        val data = _detailState.value
        val topMember = data.weekLeaderboard.firstOrNull() ?: return
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val weekId = "${today.year}-W${data.currentWeekNumber}"

        viewModelScope.launch {
            val history = CrewHistoryModel(
                weekId = weekId,
                crewId = crewId,
                weekRange = "$monday to $sunday",
                winnerUserId = topMember.userId,
                winnerName = topMember.displayName,
                winnerSteps = topMember.steps,
                totalCrewSteps = data.weekLeaderboard.sumOf { it.steps }
            )
            crewRepository.saveWeeklyHistory(crewId, history)
        }
    }
}
