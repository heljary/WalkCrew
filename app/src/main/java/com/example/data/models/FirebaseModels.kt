package com.example.data.models

import com.google.firebase.Timestamp

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val avatarColorIndex: Int = 0,
    val dailyStepGoal: Long = 8000L,
    val todaySteps: Long = 0L,
    val fcmToken: String = "",
    val reminderEnabled: Boolean = true,
    val streakDays: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class CrewModel(
    val crewId: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val ownerId: String = "",
    val members: List<String> = emptyList(),
    val duration: String = "Weekly",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class StepLogModel(
    val userId: String = "",
    val crewId: String = "",
    val date: String = "", // "YYYY-MM-DD"
    val steps: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class CrewWithMembers(
    val crew: CrewModel,
    val memberProfiles: List<UserProfile> = emptyList(),
    val userRank: Int = 1,
    val totalCrewSteps: Long = 0L
)

data class LeaderboardMember(
    val userId: String = "",
    val displayName: String = "",
    val avatarColorIndex: Int = 0,
    val steps: Long = 0L,
    val rank: Int = 1,
    val lastUpdatedMillis: Long = 0L,
    val isCurrentUser: Boolean = false,
    val dailyStepGoal: Long = 8000L,
    val streakDays: Int = 0,
    val statusText: String = ""
)

data class CrewHistoryModel(
    val weekId: String = "", // e.g. "2026-W41"
    val crewId: String = "",
    val weekRange: String = "",
    val winnerUserId: String = "",
    val winnerName: String = "",
    val winnerSteps: Long = 0L,
    val totalCrewSteps: Long = 0L,
    val completedAt: Timestamp? = null
)
