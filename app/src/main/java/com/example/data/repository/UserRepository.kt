package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.models.UserProfile
import com.google.firebase.firestore.FieldValue
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
        com.example.data.WalkCrewLogger.logRead("users", userId, "observing user profile")
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
                com.example.data.WalkCrewLogger.logError("observeUserProfile($userId)", error)
                emit(null)
            }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            com.example.data.WalkCrewLogger.logRead("users", userId, "fetching user profile")
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) doc.toObject(UserProfile::class.java) else null
        } catch (e: Exception) {
            com.example.data.WalkCrewLogger.logError("getUserProfile($userId)", e)
            null
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            com.example.data.WalkCrewLogger.logWrite("users", profile.userId, "saving user profile")
            val userRef = db.collection("users").document(profile.userId)

            val payload = hashMapOf<String, Any>(
                "userId" to profile.userId,
                "displayName" to profile.displayName,
                "avatarColorIndex" to profile.avatarColorIndex,
                "dailyStepGoal" to profile.dailyStepGoal,
                "todaySteps" to profile.todaySteps,
                "reminderEnabled" to profile.reminderEnabled,
                "streakDays" to profile.streakDays,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (profile.photoUrl != null) {
                payload["photoUrl"] = profile.photoUrl
            }

            if (profile.createdAt != null) {
                payload["createdAt"] = profile.createdAt
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }

            if (profile.fcmToken.isNotBlank()) {
                payload["fcmToken"] = profile.fcmToken
            }

            userRef.set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            com.example.data.WalkCrewLogger.logError("saveUserProfile(${profile.userId})", e)
            Result.failure(e)
        }
    }

    suspend fun updatePhotoUrl(userId: String, photoUrl: String?): Result<Unit> {
        return try {
            com.example.data.WalkCrewLogger.logWrite("users", userId, "updating photoUrl")
            val userRef = db.collection("users").document(userId)
            val updateData = hashMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (photoUrl != null) {
                updateData["photoUrl"] = photoUrl
            } else {
                updateData["photoUrl"] = FieldValue.delete()
            }
            userRef.set(updateData, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            com.example.data.WalkCrewLogger.logError("updatePhotoUrl($userId)", e)
            Result.failure(e)
        }
    }

    suspend fun updateDailyGoal(userId: String, goal: Long): Result<Unit> {
        return try {
            com.example.data.WalkCrewLogger.logWrite("users", userId, "updating dailyStepGoal to $goal")
            val userRef = db.collection("users").document(userId)
            userRef.set(
                mapOf(
                    "dailyStepGoal" to goal,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            com.example.data.WalkCrewLogger.logError("updateDailyGoal($userId)", e)
            Result.failure(e)
        }
    }

    suspend fun updateReminderEnabled(userId: String, enabled: Boolean): Result<Unit> {
        return try {
            com.example.data.WalkCrewLogger.logWrite("users", userId, "updating reminderEnabled to $enabled")
            val userRef = db.collection("users").document(userId)
            userRef.set(
                mapOf(
                    "reminderEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            com.example.data.WalkCrewLogger.logError("updateReminderEnabled($userId)", e)
            Result.failure(e)
        }
    }

    suspend fun updateTodaySteps(userId: String, steps: Long): Result<Unit> {
        return try {
            com.example.data.WalkCrewLogger.logWrite("users", userId, "updating todaySteps to $steps")
            val userRef = db.collection("users").document(userId)
            userRef.set(
                mapOf(
                    "todaySteps" to steps,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            com.example.data.WalkCrewLogger.logError("updateTodaySteps($userId)", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccount(userId: String): Result<Unit> {
        return try {
            com.example.data.WalkCrewLogger.logWrite("users", userId, "deleting user profile document")
            db.collection("users").document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            com.example.data.WalkCrewLogger.logError("deleteUserAccount($userId)", e)
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
