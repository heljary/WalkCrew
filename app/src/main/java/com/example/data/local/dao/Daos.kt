package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.LocalCrew
import com.example.data.local.entities.LocalStepLog
import com.example.data.local.entities.LocalUserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface CrewDao {
    @Query("SELECT * FROM crews")
    fun getAllCrews(): Flow<List<LocalCrew>>

    @Query("SELECT * FROM crews WHERE members LIKE '%' || :userId || '%'")
    fun getCrewsForUser(userId: String): Flow<List<LocalCrew>>

    @Query("SELECT * FROM crews WHERE crewId = :crewId")
    fun getCrewById(crewId: String): Flow<LocalCrew?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrews(crews: List<LocalCrew>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrew(crew: LocalCrew)

    @Query("DELETE FROM crews")
    suspend fun clearCrews()
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun getProfile(userId: String): Flow<LocalUserProfile?>

    @Query("SELECT * FROM user_profiles WHERE userId IN (:userIds)")
    suspend fun getProfiles(userIds: List<String>): List<LocalUserProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<LocalUserProfile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: LocalUserProfile)
}

@Dao
interface StepLogDao {
    @Query("SELECT * FROM step_logs WHERE crewId = :crewId")
    fun getLogsForCrew(crewId: String): Flow<List<LocalStepLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<LocalStepLog>)

    @Query("DELETE FROM step_logs WHERE crewId = :crewId")
    suspend fun deleteLogsForCrew(crewId: String)
}
