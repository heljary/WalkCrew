package com.example.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.data.DailyStepData
import com.example.data.HealthConnectState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class HealthConnectManager(private val context: Context) {

    val healthConnectClient by lazy {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            Log.e(TAG, "HealthConnectClient not available: ${e.message}")
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    fun checkAvailability(): HealthConnectState {
        val status = HealthConnectClient.getSdkStatus(context)
        return when (status) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectState.NeedsPermission
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectState.NotInstalled
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectState.NotSupported
            else -> HealthConnectState.NotSupported
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions: ${e.message}")
            false
        }
    }

    /**
     * Read today's total steps aggregate
     */
    suspend fun readTodaySteps(): Long {
        val client = healthConnectClient ?: return 0L
        return try {
            val now = Instant.now()
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            response[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied reading today's steps (likely background access): ${e.message}")
            0L
        } catch (e: Exception) {
            Log.e(TAG, "Error reading today's steps: ${e.message}")
            0L
        }
    }

    /**
     * Read past 7 days step totals
     */
    suspend fun readPast7DaysSteps(): List<DailyStepData> {
        val client = healthConnectClient ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val result = mutableListOf<DailyStepData>()

        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val start = date.atStartOfDay(zone).toInstant()
            val end = if (i == 0) Instant.now() else date.plusDays(1).atStartOfDay(zone).toInstant()

            val steps = try {
                val response = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )
                response[StepsRecord.COUNT_TOTAL] ?: 0L
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission denied reading steps for $date (likely background access): ${e.message}")
                0L
            } catch (e: Exception) {
                Log.e(TAG, "Error reading steps for $date: ${e.message}")
                0L
            }

            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val dayInitial = dayName.take(1).uppercase(Locale.getDefault())

            result.add(
                DailyStepData(
                    date = date,
                    dayInitial = dayInitial,
                    dayName = dayName,
                    steps = steps,
                    isToday = (i == 0),
                    goal = 8000L
                )
            )
        }

        return result
    }

    fun openHealthConnectPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            setPackage("com.android.vending")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web browser
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    companion object {
        private const val TAG = "HealthConnectManager"
    }
}
