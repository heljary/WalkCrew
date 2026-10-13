package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.DailyStepData
import com.example.data.FirebaseInitializer
import com.example.data.UserStepStats
import com.example.data.WalkCrewLogger
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class StepFirestoreDebugInfo(
    val status: String = "Loading", // "Loading", "Success", "Error: ...", "No Document"
    val documentPath: String = "",
    val rawValue: Long? = null,
    val lastSyncTimeMillis: Long? = null,
    val isFromCache: Boolean = false,
    val errorMessage: String? = null
)

sealed class HomeStepUiState {
    data object Loading : HomeStepUiState()
    data class Success(
        val stats: UserStepStats,
        val streakDays: Int,
        val isFromCache: Boolean = false,
        val lastSyncTimeMillis: Long = System.currentTimeMillis()
    ) : HomeStepUiState()
    data class NoData(val goal: Long = 8000L) : HomeStepUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : HomeStepUiState()
}

class StepLogRepository private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("walkcrew_steps_cache", Context.MODE_PRIVATE)

    val db: FirebaseFirestore by lazy {
        FirebaseInitializer.getFirestore()
    }

    private val _stepUiState = MutableStateFlow<HomeStepUiState>(getInitialCachedUiState())
    val stepUiState: StateFlow<HomeStepUiState> = _stepUiState.asStateFlow()

    private val _debugInfo = MutableStateFlow(
        StepFirestoreDebugInfo(
            status = "Loading",
            documentPath = "users/{uid}/stepLogs/${LocalDate.now()}"
        )
    )
    val debugInfo: StateFlow<StepFirestoreDebugInfo> = _debugInfo.asStateFlow()

    private var activeListener: ListenerRegistration? = null
    private var currentUserId: String? = null
    private var currentGoal: Long = 8000L

    /**
     * Start real-time snapshot listener on users/{userId}/stepLogs collection.
     */
    @Synchronized
    fun startObservingUserSteps(userId: String, goal: Long) {
        if (currentUserId == userId && activeListener != null && currentGoal == goal) {
            return
        }
        currentUserId = userId
        currentGoal = goal

        activeListener?.remove()

        val todayDate = LocalDate.now().toString()
        val docPath = "users/$userId/stepLogs/$todayDate"

        _debugInfo.value = _debugInfo.value.copy(
            status = "Listening...",
            documentPath = docPath
        )

        val collectionRef = db.collection("users").document(userId).collection("stepLogs")
        WalkCrewLogger.logRead("users/$userId/stepLogs", details = "subscribing to real-time stepLogs snapshot listener")

        activeListener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                WalkCrewLogger.logError("observeUserSteps($userId)", error)
                val errMsg = error.localizedMessage ?: "Failed to read steps from Firestore"
                _stepUiState.value = HomeStepUiState.Error(errMsg)
                _debugInfo.value = StepFirestoreDebugInfo(
                    status = "Error: $errMsg",
                    documentPath = docPath,
                    rawValue = null,
                    lastSyncTimeMillis = System.currentTimeMillis(),
                    isFromCache = false,
                    errorMessage = errMsg
                )
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val isFromCache = snapshot.metadata.isFromCache
                val stepLogsMap = mutableMapOf<String, Long>()
                var todayDocSteps: Long? = null
                var todayDocTimestamp: Long? = null

                for (doc in snapshot.documents) {
                    val dateKey = doc.id
                    val steps = doc.getLong("steps") ?: 0L
                    stepLogsMap[dateKey] = steps
                    if (dateKey == todayDate) {
                        todayDocSteps = steps
                        val ts = doc.getTimestamp("updatedAt")
                        todayDocTimestamp = ts?.toDate()?.time ?: System.currentTimeMillis()
                    }
                }

                val finalTodaySteps = todayDocSteps ?: 0L
                val streak = calculateStreak(stepLogsMap, currentGoal, LocalDate.now())
                val history = buildWeeklyHistory(stepLogsMap, currentGoal, finalTodaySteps)

                val stats = UserStepStats.fromSteps(
                    steps = finalTodaySteps,
                    weeklyHistory = history,
                    goal = currentGoal,
                    isSimulated = false
                )

                // Save to local disk cache for instant availability on next cold start
                saveCachedStats(stats, streak)

                Log.d("WalkCrew", "Step snapshot received: path=$docPath, docCount=${snapshot.documents.size}, isFromCache=$isFromCache, todaySteps=$finalTodaySteps, streak=$streak")

                _stepUiState.value = HomeStepUiState.Success(
                    stats = stats,
                    streakDays = streak,
                    isFromCache = isFromCache,
                    lastSyncTimeMillis = todayDocTimestamp ?: System.currentTimeMillis()
                )

                _debugInfo.value = StepFirestoreDebugInfo(
                    status = if (todayDocSteps != null) "Success" else "Success (No document for today yet)",
                    documentPath = docPath,
                    rawValue = finalTodaySteps,
                    lastSyncTimeMillis = todayDocTimestamp ?: System.currentTimeMillis(),
                    isFromCache = isFromCache,
                    errorMessage = null
                )
            }
        }
    }

    fun stopObserving() {
        activeListener?.remove()
        activeListener = null
    }

    /**
     * Compute streak in ONE place from stored daily totals:
     * Count consecutive days, ending today or yesterday, where steps >= dailyGoal.
     * Today does not break the streak if the goal is not reached yet.
     */
    fun calculateStreak(
        stepLogsByDate: Map<String, Long>,
        dailyGoal: Long,
        today: LocalDate = LocalDate.now()
    ): Int {
        val todayStr = today.toString()
        val todaySteps = stepLogsByDate[todayStr] ?: 0L
        val todayMetGoal = todaySteps >= dailyGoal

        val yesterday = today.minusDays(1)
        val yesterdayStr = yesterday.toString()
        val yesterdaySteps = stepLogsByDate[yesterdayStr] ?: 0L
        val yesterdayMetGoal = yesterdaySteps >= dailyGoal

        if (!yesterdayMetGoal && !todayMetGoal) {
            return 0
        }

        var count = 0
        if (todayMetGoal) {
            var curr = today
            while (true) {
                val s = stepLogsByDate[curr.toString()] ?: 0L
                if (s >= dailyGoal) {
                    count++
                    curr = curr.minusDays(1)
                } else {
                    break
                }
            }
        } else {
            // Today in progress, count from yesterday
            var curr = yesterday
            while (true) {
                val s = stepLogsByDate[curr.toString()] ?: 0L
                if (s >= dailyGoal) {
                    count++
                    curr = curr.minusDays(1)
                } else {
                    break
                }
            }
        }
        return count
    }

    private fun buildWeeklyHistory(
        stepLogsByDate: Map<String, Long>,
        dailyGoal: Long,
        todaySteps: Long
    ): List<DailyStepData> {
        val today = LocalDate.now()
        val history = mutableListOf<DailyStepData>()
        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.toString()
            val steps = if (i == 0) todaySteps else (stepLogsByDate[dateStr] ?: 0L)
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val dayInitial = dayName.take(1).uppercase(Locale.getDefault())

            history.add(
                DailyStepData(
                    date = date,
                    dayInitial = dayInitial,
                    dayName = dayName,
                    steps = steps,
                    isToday = (i == 0),
                    goal = dailyGoal
                )
            )
        }
        return history
    }

    /**
     * Requirement 2:
     * Write ONLY if newSteps > current stored value (Firestore transaction).
     * If newSteps <= 0, do NOT write anything.
     */
    suspend fun recordStepsIfGreater(
        userId: String,
        date: String,
        newSteps: Long,
        source: String
    ): Result<Long> {
        if (newSteps <= 0L) {
            return Result.success(0L)
        }

        return try {
            val docRef = db.collection("users").document(userId).collection("stepLogs").document(date)
            val userRef = db.collection("users").document(userId)
            val todayStr = LocalDate.now().toString()

            val finalSteps = db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentSteps = snapshot.getLong("steps") ?: 0L

                if (newSteps > currentSteps) {
                    val distanceKm = (newSteps * 0.000762 * 10).toLong() / 10.0
                    val calories = (newSteps * 0.04).toInt()
                    val activeMinutes = (newSteps / 100).toInt()

                    val data = hashMapOf<String, Any>(
                        "userId" to userId,
                        "date" to date,
                        "steps" to newSteps,
                        "distanceKm" to distanceKm,
                        "caloriesKcal" to calories,
                        "activeMinutes" to activeMinutes,
                        "source" to source,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    if (!snapshot.exists()) {
                        data["createdAt"] = FieldValue.serverTimestamp()
                    }

                    transaction.set(docRef, data, SetOptions.merge())

                    // If this is today, also keep user profile todaySteps in sync
                    if (date == todayStr) {
                        transaction.set(
                            userRef,
                            mapOf(
                                "todaySteps" to newSteps,
                                "updatedAt" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        )
                    }

                    Log.d("WalkCrew", "Step write transaction: path=${docRef.path}, oldSteps=$currentSteps, newSteps=$newSteps, updated=true, source=$source")
                    newSteps
                } else {
                    Log.d("WalkCrew", "Step write transaction: path=${docRef.path}, oldSteps=$currentSteps, newSteps=$newSteps, updated=false (ignored lower or equal)")
                    currentSteps
                }
            }.await()

            // Also keep user crews updated if today's steps changed
            if (date == todayStr && finalSteps > 0L) {
                CrewRepository.getInstance(context).syncUserStepsToCrews(userId, finalSteps)
            }

            Result.success(finalSteps)
        } catch (e: Exception) {
            WalkCrewLogger.logError("recordStepsIfGreater($userId, $date, $newSteps)", e)
            Result.failure(e)
        }
    }

    /**
     * Add simulated steps using a Firestore transaction (increases existing steps by amount).
     */
    suspend fun addSteps(userId: String, date: String, amount: Long): Result<Long> {
        if (amount <= 0L) return Result.success(0L)

        return try {
            val docRef = db.collection("users").document(userId).collection("stepLogs").document(date)
            val userRef = db.collection("users").document(userId)
            val todayStr = LocalDate.now().toString()

            val finalSteps = db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentSteps = snapshot.getLong("steps") ?: 0L
                val newSteps = currentSteps + amount

                val distanceKm = (newSteps * 0.000762 * 10).toLong() / 10.0
                val calories = (newSteps * 0.04).toInt()
                val activeMinutes = (newSteps / 100).toInt()

                val data = hashMapOf<String, Any>(
                    "userId" to userId,
                    "date" to date,
                    "steps" to newSteps,
                    "distanceKm" to distanceKm,
                    "caloriesKcal" to calories,
                    "activeMinutes" to activeMinutes,
                    "source" to "manual",
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                if (!snapshot.exists()) {
                    data["createdAt"] = FieldValue.serverTimestamp()
                }

                transaction.set(docRef, data, SetOptions.merge())

                if (date == todayStr) {
                    transaction.set(
                        userRef,
                        mapOf(
                            "todaySteps" to newSteps,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                }

                Log.d("WalkCrew", "Step write (add): path=${docRef.path}, oldSteps=$currentSteps, newSteps=$newSteps")
                newSteps
            }.await()

            if (date == todayStr && finalSteps > 0L) {
                CrewRepository.getInstance(context).syncUserStepsToCrews(userId, finalSteps)
            }

            Result.success(finalSteps)
        } catch (e: Exception) {
            WalkCrewLogger.logError("addSteps($userId, $date, $amount)", e)
            Result.failure(e)
        }
    }

    /**
     * Requirement 6: Reset test data in debug mode.
     * Clears the current day's stepLogs document and resets todaySteps to 0.
     */
    suspend fun resetTestData(userId: String): Result<Unit> {
        return try {
            val todayStr = LocalDate.now().toString()
            val docRef = db.collection("users").document(userId).collection("stepLogs").document(todayStr)
            val userRef = db.collection("users").document(userId)

            val oldSteps = try {
                docRef.get().await().getLong("steps") ?: 0L
            } catch (e: Exception) {
                0L
            }

            db.runBatch { batch ->
                batch.delete(docRef)
                batch.set(
                    userRef,
                    mapOf(
                        "todaySteps" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }.await()

            Log.d("WalkCrew", "Step write (reset): path=${docRef.path}, oldSteps=$oldSteps, newSteps=0")

            prefs.edit()
                .remove("cached_today_steps")
                .remove("cached_date")
                .remove("cached_streak")
                .apply()

            CrewRepository.getInstance(context).syncUserStepsToCrews(userId, 0L)
            Result.success(Unit)
        } catch (e: Exception) {
            WalkCrewLogger.logError("resetTestData($userId)", e)
            Result.failure(e)
        }
    }

    /**
     * Milestone celebration persistence (3, 7, 14, 30 days).
     */
    fun getLastCelebratedMilestone(userId: String): Int {
        return prefs.getInt("milestone_$userId", 0)
    }

    suspend fun recordCelebratedMilestone(userId: String, milestone: Int) {
        prefs.edit().putInt("milestone_$userId", milestone).apply()
        try {
            db.collection("users").document(userId).set(
                mapOf("lastCelebratedMilestone" to milestone),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            Log.w("StepLogRepository", "Could not sync celebrated milestone to Firestore: ${e.message}")
        }
    }

    private fun getInitialCachedUiState(): HomeStepUiState {
        val cachedSteps = prefs.getLong("cached_today_steps", -1L)
        val cachedDate = prefs.getString("cached_date", "")
        val todayStr = LocalDate.now().toString()

        if (cachedSteps >= 0 && cachedDate == todayStr) {
            val cachedGoal = prefs.getLong("cached_goal", 8000L)
            val cachedStreak = prefs.getInt("cached_streak", 0)
            val history = buildWeeklyHistory(emptyMap(), cachedGoal, cachedSteps)
            val stats = UserStepStats.fromSteps(cachedSteps, history, cachedGoal)
            return HomeStepUiState.Success(
                stats = stats,
                streakDays = cachedStreak,
                isFromCache = true,
                lastSyncTimeMillis = prefs.getLong("cached_time", System.currentTimeMillis())
            )
        }
        return HomeStepUiState.Loading
    }

    private fun saveCachedStats(stats: UserStepStats, streak: Int) {
        prefs.edit()
            .putLong("cached_today_steps", stats.steps)
            .putString("cached_date", LocalDate.now().toString())
            .putLong("cached_goal", stats.goal)
            .putInt("cached_streak", streak)
            .putLong("cached_time", System.currentTimeMillis())
            .apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: StepLogRepository? = null

        fun getInstance(context: Context): StepLogRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StepLogRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
