package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.CrewDao
import com.example.data.local.dao.StepLogDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entities.LocalCrew
import com.example.data.local.entities.LocalStepLog
import com.example.data.local.entities.LocalUserProfile

@Database(
    entities = [
        LocalCrew::class,
        LocalUserProfile::class,
        LocalStepLog::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun crewDao(): CrewDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun stepLogDao(): StepLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "walkcrew_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
