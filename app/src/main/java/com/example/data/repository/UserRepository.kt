package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.models.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(private val context: Context) {

    val db: FirebaseFirestore by lazy {
        com.example.data.FirebaseInitializer.ensureInitialized(context)
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java)
                } else {
                    null
                }
            }
            .catch { error ->
                Log.e(TAG, "Error observing user profile: ${error.message}", error)
                emit(null)
            }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) doc.toObject(UserProfile::class.java) else null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile: ${e.message}", e)
            null
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(profile.userId)
            val doc = userRef.get().await()

            val payload = hashMapOf<String, Any>(
                "userId" to profile.userId,
                "displayName" to profile.displayName,
                "avatarColorIndex" to profile.avatarColorIndex,
                "dailyStepGoal" to profile.dailyStepGoal,
                "todaySteps" to profile.todaySteps,
                "reminderEnabled" to profile.reminderEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (profile.fcmToken.isNotBlank()) {
                payload["fcmToken"] = profile.fcmToken
            }

            if (!doc.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }

            userRef.set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateDailyGoal(userId: String, goal: Long): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(userId)
            userRef.update(
                mapOf(
                    "dailyStepGoal" to goal,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating daily goal: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateReminderEnabled(userId: String, enabled: Boolean): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(userId)
            userRef.update(
                mapOf(
                    "reminderEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating reminder setting: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateTodaySteps(userId: String, steps: Long): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(userId)
            userRef.update(
                mapOf(
                    "todaySteps" to steps,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating steps: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "UserRepository"

        @Volatile
        private var INSTANCE: UserRepository? = null

        fun getInstance(context: Context): UserRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
