package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.models.CrewHistoryModel
import com.example.data.models.CrewModel
import com.example.data.models.CrewWithMembers
import com.example.data.models.LeaderboardMember
import com.example.data.models.StepLogModel
import com.example.data.models.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

data class CrewDetailData(
    val crew: CrewModel = CrewModel(),
    val todayLeaderboard: List<LeaderboardMember> = emptyList(),
    val weekLeaderboard: List<LeaderboardMember> = emptyList(),
    val userTodaySteps: Long = 0L,
    val userWeekSteps: Long = 0L,
    val currentWeekNumber: Int = 1,
    val timeLeftFormatted: String = "",
    val isLoading: Boolean = false
)

class CrewRepository(private val context: Context) {

    val db: FirebaseFirestore by lazy {
        com.example.data.FirebaseInitializer.ensureInitialized(context)
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    /**
     * Observes all crews that the user belongs to
     */
    fun observeUserCrews(userId: String): Flow<List<CrewWithMembers>> {
        return db.collection("crews")
            .whereArrayContains("members", userId)
            .snapshots()
            .map { snapshot ->
                val crews = snapshot.toObjects(CrewModel::class.java)
                crews.map { crew ->
                    val memberProfiles = mutableListOf<UserProfile>()
                    val memberIds = crew.members.take(4)
                    for (memberId in memberIds) {
                        try {
                            val userDoc = db.collection("users").document(memberId).get().await()
                            if (userDoc.exists()) {
                                userDoc.toObject(UserProfile::class.java)?.let { memberProfiles.add(it) }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to load member profile $memberId: ${e.message}")
                        }
                    }

                    val rank = calculateUserRank(crew.crewId, userId)

                    CrewWithMembers(
                        crew = crew,
                        memberProfiles = memberProfiles,
                        userRank = rank,
                        totalCrewSteps = 0L
                    )
                }
            }
            .catch { error ->
                Log.e(TAG, "Error observing user crews: ${error.message}", error)
                emit(emptyList())
            }
    }

    /**
     * Real-time snapshot listener for a specific crew and its live leaderboard
     */
    fun observeCrewDetails(crewId: String, currentUserId: String): Flow<CrewDetailData> = callbackFlow {
        val crewRef = db.collection("crews").document(crewId)
        val logsRef = crewRef.collection("stepLogs")

        var currentCrew: CrewModel? = null
        val cachedProfiles = mutableMapOf<String, UserProfile>()

        // Date calculations in user's timezone
        val today = LocalDate.now()
        val todayDate = today.toString()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val mondayDate = monday.toString()
        val sundayDate = sunday.toString()

        val weekFields = WeekFields.of(Locale.getDefault())
        val currentWeekNumber = today.get(weekFields.weekOfWeekBasedYear())

        // Calculate time left until Sunday 23:59:59
        val now = LocalDateTime.now()
        val sundayEnd = sunday.atTime(23, 59, 59)
        val duration = Duration.between(now, sundayEnd).coerceAtLeast(Duration.ZERO)
        val daysLeft = duration.toDays()
        val hoursLeft = duration.toHours() % 24
        val timeLeftFormatted = "${daysLeft}d ${hoursLeft}h left"

        fun recalculateAndEmit(logs: List<StepLogModel>) {
            val crew = currentCrew ?: return

            // --- 1. Compute TODAY leaderboard ---
            val todayLogsByUser = logs.filter { it.date == todayDate }.associateBy { it.userId }
            val todayMembers = crew.members.map { uid ->
                val log = todayLogsByUser[uid]
                val profile = cachedProfiles[uid]
                val stepGoal = profile?.dailyStepGoal?.takeIf { it > 0 } ?: 8000L
                val streak = profile?.streakDays ?: 0
                val memberSteps = log?.steps ?: profile?.todaySteps ?: 0L
                val statusText = if (memberSteps >= stepGoal) "Goal reached! 🎉" else "Walking today"
                LeaderboardMember(
                    userId = uid,
                    displayName = profile?.displayName?.ifBlank { "Walker" } ?: "Walker",
                    avatarColorIndex = profile?.avatarColorIndex ?: 0,
                    steps = memberSteps,
                    lastUpdatedMillis = log?.updatedAt?.toDate()?.time ?: 0L,
                    isCurrentUser = (uid == currentUserId),
                    dailyStepGoal = stepGoal,
                    streakDays = streak,
                    statusText = statusText
                )
            }.sortedWith(
                compareByDescending<LeaderboardMember> { it.steps }
                    .thenBy { it.lastUpdatedMillis }
            ).mapIndexed { index, m -> m.copy(rank = index + 1) }

            // --- 2. Compute THIS WEEK leaderboard (Monday to Sunday) ---
            val weekLogs = logs.filter { it.date >= mondayDate && it.date <= sundayDate }
            val weekStepsByUser = mutableMapOf<String, Long>()
            val weekLatestUpdateByUser = mutableMapOf<String, Long>()

            for (log in weekLogs) {
                weekStepsByUser[log.userId] = (weekStepsByUser[log.userId] ?: 0L) + log.steps
                val logTime = log.updatedAt?.toDate()?.time ?: 0L
                if (logTime > (weekLatestUpdateByUser[log.userId] ?: 0L)) {
                    weekLatestUpdateByUser[log.userId] = logTime
                }
            }

            val weekMembers = crew.members.map { uid ->
                val profile = cachedProfiles[uid]
                val steps = weekStepsByUser[uid] ?: 0L
                val stepGoal = profile?.dailyStepGoal?.takeIf { it > 0 } ?: 8000L
                val streak = profile?.streakDays ?: 0
                LeaderboardMember(
                    userId = uid,
                    displayName = profile?.displayName?.ifBlank { "Walker" } ?: "Walker",
                    avatarColorIndex = profile?.avatarColorIndex ?: 0,
                    steps = steps,
                    lastUpdatedMillis = weekLatestUpdateByUser[uid] ?: 0L,
                    isCurrentUser = (uid == currentUserId),
                    dailyStepGoal = stepGoal,
                    streakDays = streak,
                    statusText = "${streak}d streak 🔥"
                )
            }.sortedWith(
                compareByDescending<LeaderboardMember> { it.steps }
                    .thenBy { it.lastUpdatedMillis }
            ).mapIndexed { index, m -> m.copy(rank = index + 1) }

            val userToday = todayMembers.find { it.userId == currentUserId }?.steps ?: 0L
            val userWeek = weekMembers.find { it.userId == currentUserId }?.steps ?: 0L

            trySend(
                CrewDetailData(
                    crew = crew,
                    todayLeaderboard = todayMembers,
                    weekLeaderboard = weekMembers,
                    userTodaySteps = userToday,
                    userWeekSteps = userWeek,
                    currentWeekNumber = currentWeekNumber,
                    timeLeftFormatted = timeLeftFormatted,
                    isLoading = false
                )
            )
        }

        var latestLogs: List<StepLogModel> = emptyList()

        // 1. Crew document listener
        val crewRegistration = crewRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to crew: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                currentCrew = snapshot.toObject(CrewModel::class.java)

                // Fetch missing member profiles asynchronously
                currentCrew?.members?.forEach { uid ->
                    if (!cachedProfiles.containsKey(uid)) {
                        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                userDoc.toObject(UserProfile::class.java)?.let {
                                    cachedProfiles[uid] = it
                                    recalculateAndEmit(latestLogs)
                                }
                            }
                        }
                    }
                }

                recalculateAndEmit(latestLogs)
            }
        }

        // 2. Step logs listener
        val logsRegistration = logsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to step logs: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                latestLogs = snapshot.toObjects(StepLogModel::class.java)
                recalculateAndEmit(latestLogs)
            }
        }

        awaitClose {
            crewRegistration.remove()
            logsRegistration.remove()
        }
    }.catch { e ->
        Log.e(TAG, "Error in observeCrewDetails: ${e.message}", e)
        emit(CrewDetailData(isLoading = false))
    }

    private suspend fun calculateUserRank(crewId: String, userId: String): Int {
        return try {
            val todayDate = LocalDate.now().toString()
            val logsSnapshot = db.collection("crews").document(crewId)
                .collection("stepLogs")
                .whereEqualTo("date", todayDate)
                .get()
                .await()

            val logs = logsSnapshot.toObjects(StepLogModel::class.java)
            if (logs.isEmpty()) return 1

            val sorted = logs.sortedByDescending { it.steps }
            val index = sorted.indexOfFirst { it.userId == userId }
            if (index >= 0) index + 1 else 1
        } catch (e: Exception) {
            1
        }
    }

    suspend fun createCrew(
        name: String,
        duration: String = "Weekly",
        ownerId: String
    ): Result<CrewModel> {
        return try {
            val inviteCode = generateInviteCode()
            val crewRef = db.collection("crews").document()
            val crewId = crewRef.id

            val crewData = hashMapOf<String, Any>(
                "crewId" to crewId,
                "name" to name.trim(),
                "inviteCode" to inviteCode,
                "ownerId" to ownerId,
                "members" to listOf(ownerId),
                "duration" to duration,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            crewRef.set(crewData).await()

            db.collection("inviteCodes").document(inviteCode).set(
                mapOf(
                    "code" to inviteCode,
                    "crewId" to crewId,
                    "crewName" to name.trim()
                )
            ).await()

            val crew = CrewModel(
                crewId = crewId,
                name = name.trim(),
                inviteCode = inviteCode,
                ownerId = ownerId,
                members = listOf(ownerId),
                duration = duration
            )
            Result.success(crew)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create crew: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun joinCrewWithCode(code: String, userId: String): Result<CrewModel> {
        val sanitizedCode = code.trim().uppercase()
        return try {
            val lookupDoc = db.collection("inviteCodes").document(sanitizedCode).get().await()
            if (!lookupDoc.exists()) {
                return Result.failure(IllegalArgumentException("Invalid crew code. Please verify and try again."))
            }

            val crewId = lookupDoc.getString("crewId")
                ?: return Result.failure(IllegalStateException("Crew reference not found for code $sanitizedCode"))

            val crewRef = db.collection("crews").document(crewId)
            val crewDoc = crewRef.get().await()

            if (!crewDoc.exists()) {
                return Result.failure(IllegalArgumentException("The crew associated with this code no longer exists."))
            }

            val currentMembers = (crewDoc.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            if (currentMembers.contains(userId)) {
                val crew = crewDoc.toObject(CrewModel::class.java)!!
                return Result.success(crew)
            }

            crewRef.update(
                mapOf(
                    "members" to FieldValue.arrayUnion(userId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val updatedDoc = crewRef.get().await()
            val crew = updatedDoc.toObject(CrewModel::class.java)!!
            Result.success(crew)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join crew: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun syncUserStepsToCrews(userId: String, steps: Long): Result<Unit> {
        return try {
            val todayDate = LocalDate.now().toString()
            val userCrewsSnapshot = db.collection("crews")
                .whereArrayContains("members", userId)
                .get()
                .await()

            val logId = "${userId}_$todayDate"

            for (doc in userCrewsSnapshot.documents) {
                val crewId = doc.id
                val logRef = doc.reference.collection("stepLogs").document(logId)
                val logPayload = hashMapOf<String, Any>(
                    "userId" to userId,
                    "crewId" to crewId,
                    "date" to todayDate,
                    "steps" to steps,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val existingDoc = logRef.get().await()
                if (!existingDoc.exists()) {
                    logPayload["createdAt"] = FieldValue.serverTimestamp()
                }
                logRef.set(logPayload, SetOptions.merge()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing steps to crews: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveWeeklyHistory(crewId: String, history: CrewHistoryModel): Result<Unit> {
        return try {
            val weekId = history.weekId.ifBlank { "week_${System.currentTimeMillis()}" }
            db.collection("crews").document(crewId)
                .collection("history").document(weekId)
                .set(
                    mapOf(
                        "weekId" to weekId,
                        "crewId" to crewId,
                        "weekRange" to history.weekRange,
                        "winnerUserId" to history.winnerUserId,
                        "winnerName" to history.winnerName,
                        "winnerSteps" to history.winnerSteps,
                        "totalCrewSteps" to history.totalCrewSteps,
                        "completedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving weekly history: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun sendNudge(
        crewId: String,
        toUserId: String,
        fromUserId: String,
        fromUserName: String,
        crewName: String
    ): Result<Unit> {
        val todayStr = LocalDate.now().toString()
        val nudgeDocId = "${fromUserId}_${toUserId}_$todayStr"
        val nudgeRef = db.collection("crews").document(crewId)
            .collection("nudges").document(nudgeDocId)

        return try {
            val existing = nudgeRef.get().await()
            if (existing.exists()) {
                return Result.failure(IllegalStateException("You already nudged ${fromUserName.ifBlank { "this member" }} today!"))
            }

            val friendlyMessages = listOf(
                "$fromUserName thinks you can catch up today! 👟",
                "$fromUserName says: time for an afternoon walk! 🚶",
                "$fromUserName is rooting for you to hit your goal today! 🔥",
                "$fromUserName sent you a nudge — every step counts! 🚀"
            )
            val selectedMessage = friendlyMessages.random()

            val payload = hashMapOf<String, Any>(
                "crewId" to crewId,
                "fromUserId" to fromUserId,
                "toUserId" to toUserId,
                "fromUserName" to fromUserName,
                "crewName" to crewName,
                "message" to selectedMessage,
                "date" to todayStr,
                "createdAt" to FieldValue.serverTimestamp()
            )

            nudgeRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send nudge: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun observeCrewReactions(crewId: String): Flow<Map<String, Map<String, Int>>> = callbackFlow<Map<String, Map<String, Int>>> {
        val reactionsRef = db.collection("crews").document(crewId).collection("reactions")
        val listener = reactionsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing reactions: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val map = mutableMapOf<String, Map<String, Int>>()
                for (doc in snapshot.documents) {
                    val targetUid = doc.id
                    val counts = mutableMapOf<String, Int>()
                    val rawCounts = doc.get("counts") as? Map<*, *>
                    rawCounts?.forEach { (k, v) ->
                        if (k is String && v is Number) {
                            counts[k] = v.toInt()
                        }
                    }
                    map[targetUid] = counts
                }
                trySend(map.toMap())
            }
        }
        awaitClose { listener.remove() }
    }.catch {
        emit(emptyMap())
    }

    suspend fun addReaction(
        crewId: String,
        targetUserId: String,
        fromUserId: String,
        reactionType: String
    ): Result<Unit> {
        val docRef = db.collection("crews").document(crewId)
            .collection("reactions").document(targetUserId)

        return try {
            val payload = hashMapOf<String, Any>(
                "crewId" to crewId,
                "targetUserId" to targetUserId,
                "counts.$reactionType" to FieldValue.increment(1),
                "lastReactionBy" to fromUserId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add reaction: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    companion object {
        private const val TAG = "CrewRepository"

        @Volatile
        private var INSTANCE: CrewRepository? = null

        fun getInstance(context: Context): CrewRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CrewRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
