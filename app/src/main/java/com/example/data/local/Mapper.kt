package com.example.data.local

import com.example.data.models.CrewModel
import com.example.data.models.StepLogModel
import com.example.data.models.UserProfile
import com.example.data.local.entities.LocalCrew
import com.example.data.local.entities.LocalStepLog
import com.example.data.local.entities.LocalUserProfile

fun CrewModel.toLocal(): LocalCrew = LocalCrew(
    crewId = crewId,
    name = name,
    inviteCode = inviteCode,
    ownerId = ownerId,
    members = members,
    duration = duration,
    lastUpdatedMillis = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
)

fun LocalCrew.toModel(): CrewModel = CrewModel(
    crewId = crewId,
    name = name,
    inviteCode = inviteCode,
    ownerId = ownerId,
    members = members,
    duration = duration
)

fun UserProfile.toLocal(): LocalUserProfile = LocalUserProfile(
    userId = userId,
    displayName = displayName,
    photoUrl = photoUrl,
    avatarColorIndex = avatarColorIndex,
    dailyStepGoal = dailyStepGoal,
    todaySteps = todaySteps,
    streakDays = streakDays,
    lastUpdatedMillis = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
)

fun LocalUserProfile.toModel(): UserProfile = UserProfile(
    userId = userId,
    displayName = displayName,
    photoUrl = photoUrl,
    avatarColorIndex = avatarColorIndex,
    dailyStepGoal = dailyStepGoal,
    todaySteps = todaySteps,
    streakDays = streakDays
)

fun StepLogModel.toLocal(): LocalStepLog = LocalStepLog(
    logId = "${userId}_${crewId}_$date",
    userId = userId,
    crewId = crewId,
    date = date,
    steps = steps,
    updatedAtMillis = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
)

fun LocalStepLog.toModel(): StepLogModel = StepLogModel(
    userId = userId,
    crewId = crewId,
    date = date,
    steps = steps
)
