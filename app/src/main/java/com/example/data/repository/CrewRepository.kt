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
import com.example.data.WalkCrewLogger
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import com.example.data.local.AppDatabase
import com.example.data.local.toLocal
import com.example.data.local.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

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

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = AppDatabase.getDatabase(context)
    private val crewDao = database.crewDao()
    private val userProfileDao = database.userProfileDao()
    private val stepLogDao = database.stepLogDao()

    val db: FirebaseFirestore by lazy {
        com.example.data.FirebaseInitializer.getFirestore()
    }

    /**
     * Observes all crews that the user belongs to, with local caching.
     */
    fun observeUserCrews(userId: String): Flow<List<CrewWithMembers>> {
        WalkCrewLogger.logRead("crews", details = "observing crews for member $userId (offline-first)")

        // 1. Start Firestore listener to update Room
        repositoryScope.launch {
            try {
                db.collection("crews")
                    .whereArrayContains("members", userId)
                    .snapshots()
                    .collect { snapshot ->
                        val remoteCrews = snapshot.toObjects(CrewModel::class.java)
                        crewDao.insertCrews(remoteCrews.map { it.toLocal() })

                        // Fetch profiles for members to ensure they are cached
                        remoteCrews.forEach { crew ->
                            crew.members.take(10).forEach { memberId ->
                                db.collection("users").document(memberId).get().addOnSuccessListener { userDoc ->
                                    if (userDoc.exists()) {
                                        userDoc.toObject(UserProfile::class.java)?.let { profile ->
                                            repositoryScope.launch {
                                                userProfileDao.insertProfile(profile.toLocal())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                WalkCrewLogger.logError("observeUserCrews.sync($userId)", e)
            }
        }

        // 2. Return reactive Flow from Room
        return crewDao.getCrewsForUser(userId).map { localCrews ->
            localCrews.map { localCrew ->
                val crew = localCrew.toModel()
                val profiles = userProfileDao.getProfiles(crew.members.take(4))
                
                CrewWithMembers(
                    crew = crew,
                    memberProfiles = profiles.map { it.toModel() },
                    userRank = 1, // Will be updated on detail screen or if we add a rank field to LocalCrew
                    totalCrewSteps = 0L
                )
            }
        }.catch { error ->
            WalkCrewLogger.logError("observeUserCrews.local($userId)", error)
            emit(emptyList())
        }
    }

    /**
     * Real-time snapshot listener for a specific crew and its live leaderboard, with local caching.
     */
    fun observeCrewDetails(crewId: String, currentUserId: String): Flow<CrewDetailData> {
        WalkCrewLogger.logRead("crews", crewId, "observing details (offline-first)")

        // 1. Start Firestore listeners to update Room
        repositoryScope.launch {
            try {
                val crewRef = db.collection("crews").document(crewId)
                val logsRef = crewRef.collection("stepLogs")

                // A. Sync Crew Document
                crewRef.snapshots().collect { snapshot ->
                    if (snapshot.exists()) {
                        val crew = snapshot.toObject(CrewModel::class.java)
                        crew?.let {
                            crewDao.insertCrew(it.toLocal())
                            // Fetch missing profiles
                            it.members.forEach { uid ->
                                db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                                    if (userDoc.exists()) {
                                        userDoc.toObject(UserProfile::class.java)?.let { profile ->
                                            repositoryScope.launch { userProfileDao.insertProfile(profile.toLocal()) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                WalkCrewLogger.logError("observeCrewDetails.crewSync($crewId)", e)
            }
        }

        repositoryScope.launch {
            try {
                val logsRef = db.collection("crews").document(crewId).collection("stepLogs")
                logsRef.snapshots().collect { snapshot ->
                    val logs = snapshot.toObjects(StepLogModel::class.java)
                    stepLogDao.insertLogs(logs.map { it.toLocal() })
                }
            } catch (e: Exception) {
                WalkCrewLogger.logError("observeCrewDetails.logsSync($crewId)", e)
            }
        }

        // 2. Combine Room Flows to compute UI State
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

        return combine(
            crewDao.getCrewById(crewId),
            stepLogDao.getLogsForCrew(crewId)
        ) { localCrew, localLogs ->
            if (localCrew == null) return@combine CrewDetailData(isLoading = true)

            val crew = localCrew.toModel()
            val logs = localLogs.map { it.toModel() }
            
            // Get cached profiles for members
            val cachedProfiles = userProfileDao.getProfiles(crew.members).associateBy { it.userId }

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
        }.catch { e ->
            Log.e(TAG, "Error in observeCrewDetails: ${e.message}", e)
            emit(CrewDetailData(isLoading = false))
        }
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

            WalkCrewLogger.logWrite("crews", crewId, "creating new crew: ${name.trim()}")
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

            // Write both crew and invite code in a batch/single logical action
            val batch = db.batch()
            batch.set(crewRef, crewData)
            
            val inviteRef = db.collection("inviteCodes").document(inviteCode)
            WalkCrewLogger.logWrite("inviteCodes", inviteCode, "saving invite code index for crew $crewId")
            batch.set(inviteRef, mapOf(
                "code" to inviteCode,
                "crewId" to crewId,
                "crewName" to name.trim(),
                "ownerId" to ownerId
            ))
            
            batch.commit().await()

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
            WalkCrewLogger.logError("createCrew($name)", e)
            Result.failure(e)
        }
    }

    suspend fun joinCrewWithCode(code: String, userId: String): Result<CrewModel> {
        val sanitizedCode = code.trim().uppercase()
        return try {
            val lookupDoc = try {
                db.collection("inviteCodes").document(sanitizedCode).get().await()
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("offline", ignoreCase = true) || msg.contains("unavailable", ignoreCase = true)) {
                    // Retry once with SERVER source if client is offline/unavailable
                    WalkCrewLogger.logRead("inviteCodes", sanitizedCode, "join retry with Source.SERVER (offline)")
                    db.collection("inviteCodes").document(sanitizedCode).get(com.google.firebase.firestore.Source.SERVER).await()
                } else {
                    throw e
                }
            }
            
            if (!lookupDoc.exists()) {
                return Result.failure(IllegalArgumentException("Invalid code. This crew code does not exist."))
            }

            val crewId = lookupDoc.getString("crewId")
                ?: return Result.failure(IllegalStateException("Crew reference not found for code $sanitizedCode"))

            WalkCrewLogger.logRead("crews", crewId, "joining crew via transaction")
            val crewRef = db.collection("crews").document(crewId)

            val crew = db.runTransaction { transaction ->
                val snapshot = transaction.get(crewRef)
                if (!snapshot.exists()) {
                    throw IllegalArgumentException("The crew associated with this code no longer exists.")
                }

                val currentMembers = (snapshot.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                if (currentMembers.contains(userId)) {
                    return@runTransaction snapshot.toObject(CrewModel::class.java)!!
                }

                if (currentMembers.size >= 50) {
                    throw IllegalStateException("This crew is full (maximum 50 members).")
                }

                transaction.update(crewRef, mapOf(
                    "members" to FieldValue.arrayUnion(userId),
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                snapshot.toObject(CrewModel::class.java)!!
            }.await()

            Result.success(crew)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("PERMISSION_DENIED", ignoreCase = true) || 
                msg.contains("Missing or insufficient permissions", ignoreCase = true)) {
                Result.failure(Exception("Permission denied, check Firestore rules. Ensure you are signed in and the crew is not full."))
            } else if (msg.contains("offline", ignoreCase = true) || msg.contains("unavailable", ignoreCase = true)) {
                val projectId = com.google.firebase.FirebaseApp.getInstance().options.projectId
                Result.failure(Exception("Cannot reach Firestore. Check internet, and that Firestore is created in Firebase project $projectId."))
            } else {
                WalkCrewLogger.logError("joinCrewWithCode($code, $userId)", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Migration: ensure existing crews owned by the user have an invite code document.
     */
    suspend fun migrateOwnedCrewsToInviteCodes(userId: String) {
        try {
            WalkCrewLogger.logRead("crews", details = "migration: checking crews owned by $userId")
            val ownedCrews = db.collection("crews")
                .whereEqualTo("ownerId", userId)
                .get()
                .await()

            for (doc in ownedCrews.documents) {
                val crew = doc.toObject(CrewModel::class.java) ?: continue
                if (crew.inviteCode.isNotBlank()) {
                    val inviteRef = db.collection("inviteCodes").document(crew.inviteCode)
                    val inviteDoc = inviteRef.get().await()
                    if (!inviteDoc.exists()) {
                        WalkCrewLogger.logWrite("inviteCodes", crew.inviteCode, "migration: creating missing code for crew ${crew.crewId}")
                        inviteRef.set(mapOf(
                            "code" to crew.inviteCode,
                            "crewId" to crew.crewId,
                            "crewName" to crew.name,
                            "ownerId" to crew.ownerId
                        )).await()
                    }
                }
            }
        } catch (e: Exception) {
            WalkCrewLogger.logError("migrateOwnedCrewsToInviteCodes", e)
        }
    }

    suspend fun syncUserStepsToCrews(userId: String, steps: Long): Result<Unit> {
        return try {
            val todayDate = LocalDate.now().toString()
            WalkCrewLogger.logRead("crews", details = "querying user crews to sync $steps steps for $userId")
            val userCrewsSnapshot = db.collection("crews")
                .whereArrayContains("members", userId)
                .get()
                .await()

            val logId = "${userId}_$todayDate"

            for (doc in userCrewsSnapshot.documents) {
                val crewId = doc.id
                val logRef = doc.reference.collection("stepLogs").document(logId)
                WalkCrewLogger.logWrite("crews/$crewId/stepLogs", logId, "recording $steps steps for $userId on $todayDate")
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
            WalkCrewLogger.logError("syncUserStepsToCrews($userId, $steps)", e)
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

            WalkCrewLogger.logWrite("crews/$crewId/nudges", nudgeDocId, "sending nudge to $toUserId from $fromUserId")
            nudgeRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            WalkCrewLogger.logError("sendNudge($crewId, $toUserId)", e)
            Result.failure(e)
        }
    }

    fun observeCrewReactions(crewId: String): Flow<Map<String, Map<String, Int>>> = callbackFlow<Map<String, Map<String, Int>>> {
        WalkCrewLogger.logRead("crews/$crewId/reactions", details = "observing crew reactions")
        val reactionsRef = db.collection("crews").document(crewId).collection("reactions")
        val listener = reactionsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                WalkCrewLogger.logError("observeCrewReactions($crewId)", error)
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
        WalkCrewLogger.logError("observeCrewReactions($crewId)", it)
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
            WalkCrewLogger.logWrite("crews/$crewId/reactions", targetUserId, "adding reaction $reactionType from $fromUserId")
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
            WalkCrewLogger.logError("addReaction($crewId, $targetUserId, $reactionType)", e)
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
