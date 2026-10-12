package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crews")
data class LocalCrew(
    @PrimaryKey val crewId: String,
    val name: String,
    val inviteCode: String,
    val ownerId: String,
    val members: List<String>, // Needs TypeConverter
    val duration: String,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profiles")
data class LocalUserProfile(
    @PrimaryKey val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val avatarColorIndex: Int,
    val dailyStepGoal: Long,
    val todaySteps: Long,
    val streakDays: Int,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "step_logs")
data class LocalStepLog(
    @PrimaryKey val logId: String, // userId_crewId_date
    val userId: String,
    val crewId: String,
    val date: String,
    val steps: Long,
    val updatedAtMillis: Long
)
