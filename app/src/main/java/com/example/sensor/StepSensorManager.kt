package com.example.sensor

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class StepSensorManager private constructor(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _stepCount = MutableStateFlow(loadTodaySteps())
    val stepCount: StateFlow<Long> = _stepCount.asStateFlow()

    private val _isSensorAvailable = MutableStateFlow(false)
    val isSensorAvailable: StateFlow<Boolean> = _isSensorAvailable.asStateFlow()

    private val _hasPermission = MutableStateFlow(checkPermissionGranted())
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private var isRegistered = false
    private var usingStepCounter = false

    init {
        checkSensorAvailability()
        _hasPermission.value = checkPermissionGranted()
    }

    private fun checkPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun updatePermissionStatus(granted: Boolean) {
        _hasPermission.value = granted
        if (granted) {
            startListening()
            startForegroundServiceIfPossible()
        }
    }

    private fun checkSensorAvailability() {
        val sm = sensorManager
        if (sm == null) {
            _isSensorAvailable.value = false
            return
        }
        val hasCounter = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
        val hasDetector = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
        _isSensorAvailable.value = hasCounter || hasDetector
    }

    fun hasPhysicalSensor(): Boolean {
        return _isSensorAvailable.value
    }

    fun startForegroundServiceIfPossible() {
        if (_hasPermission.value && _isSensorAvailable.value) {
            StepTrackingService.start(context)
        }
    }

    @Synchronized
    fun startListening() {
        if (isRegistered) return
        val sm = sensorManager ?: return

        // 1. Prefer TYPE_STEP_COUNTER (cumulative hardware counter)
        val stepCounter = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounter != null) {
            val registered = sm.registerListener(
                this,
                stepCounter,
                SensorManager.SENSOR_DELAY_UI
            )
            if (registered) {
                isRegistered = true
                usingStepCounter = true
                Log.i(TAG, "Registered TYPE_STEP_COUNTER sensor listener successfully")
                return
            }
        }

        // 2. Fallback to TYPE_STEP_DETECTOR (triggers 1 event per step taken)
        val stepDetector = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepDetector != null) {
            val registered = sm.registerListener(
                this,
                stepDetector,
                SensorManager.SENSOR_DELAY_UI
            )
            if (registered) {
                isRegistered = true
                usingStepCounter = false
                Log.i(TAG, "Registered TYPE_STEP_DETECTOR sensor listener successfully")
                return
            }
        }

        Log.w(TAG, "No suitable hardware step sensor could be registered")
    }

    @Synchronized
    fun stopListening() {
        if (!isRegistered) return
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering sensor listener: ${e.message}")
        }
        isRegistered = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalHardwareSteps = event.values.getOrNull(0)?.toLong() ?: return
                handleStepCounterEvent(totalHardwareSteps)
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                handleStepDetectorEvent()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op for step sensors
    }

    private fun handleStepCounterEvent(hardwareTotalSteps: Long) {
        val todayStr = LocalDate.now().toString()
        val savedDate = prefs.getString(KEY_SAVED_DATE, null)

        val startOfDayOffset: Long
        var accumulatedSteps: Long

        if (savedDate != todayStr) {
            // New calendar day detected: archive previous day's steps
            if (savedDate != null) {
                val yesterdaySteps = prefs.getLong(KEY_TODAY_STEPS, 0L)
                archivePastDay(savedDate, yesterdaySteps)
            }

            startOfDayOffset = hardwareTotalSteps
            accumulatedSteps = 0L

            prefs.edit()
                .putString(KEY_SAVED_DATE, todayStr)
                .putLong(KEY_START_OFFSET, startOfDayOffset)
                .putLong(KEY_ACCUMULATED, accumulatedSteps)
                .putLong(KEY_TODAY_STEPS, 0L)
                .apply()
        } else {
            val savedOffset = prefs.getLong(KEY_START_OFFSET, -1L)
            accumulatedSteps = prefs.getLong(KEY_ACCUMULATED, 0L)

            if (savedOffset == -1L) {
                startOfDayOffset = hardwareTotalSteps
                prefs.edit().putLong(KEY_START_OFFSET, startOfDayOffset).apply()
            } else if (hardwareTotalSteps < savedOffset) {
                // Device reboot occurred! Hardware counter reset to 0
                val previousWalkedBeforeReboot = prefs.getLong(KEY_TODAY_STEPS, 0L)
                accumulatedSteps = previousWalkedBeforeReboot
                startOfDayOffset = hardwareTotalSteps

                prefs.edit()
                    .putLong(KEY_START_OFFSET, startOfDayOffset)
                    .putLong(KEY_ACCUMULATED, accumulatedSteps)
                    .apply()
            } else {
                startOfDayOffset = savedOffset
            }
        }

        val todaySteps = maxOf(0L, (hardwareTotalSteps - startOfDayOffset) + accumulatedSteps)

        // Save and emit StateFlow
        prefs.edit().putLong(KEY_TODAY_STEPS, todaySteps).apply()
        _stepCount.value = todaySteps
    }

    private fun handleStepDetectorEvent() {
        val todayStr = LocalDate.now().toString()
        val savedDate = prefs.getString(KEY_SAVED_DATE, null)

        val currentSteps = if (savedDate != todayStr) {
            if (savedDate != null) {
                val yesterdaySteps = prefs.getLong(KEY_TODAY_STEPS, 0L)
                archivePastDay(savedDate, yesterdaySteps)
            }
            prefs.edit().putString(KEY_SAVED_DATE, todayStr).apply()
            1L
        } else {
            prefs.getLong(KEY_TODAY_STEPS, 0L) + 1L
        }

        prefs.edit().putLong(KEY_TODAY_STEPS, currentSteps).apply()
        _stepCount.value = currentSteps
    }

    private fun loadTodaySteps(): Long {
        val todayStr = LocalDate.now().toString()
        val savedDate = prefs.getString(KEY_SAVED_DATE, null)
        return if (savedDate == todayStr) {
            prefs.getLong(KEY_TODAY_STEPS, 0L)
        } else {
            0L
        }
    }

    fun getTodaySteps(): Long {
        return loadTodaySteps()
    }

    fun getArchivedStepsForDate(dateStr: String): Long {
        return prefs.getLong(KEY_ARCHIVE_PREFIX + dateStr, 0L)
    }

    private fun archivePastDay(dateStr: String, steps: Long) {
        prefs.edit().putLong(KEY_ARCHIVE_PREFIX + dateStr, steps).apply()
    }

    /**
     * Manual step adjustment (e.g. testing / demo increment)
     */
    fun addManualSteps(amount: Long) {
        val updated = loadTodaySteps() + amount
        val todayStr = LocalDate.now().toString()
        prefs.edit()
            .putString(KEY_SAVED_DATE, todayStr)
            .putLong(KEY_TODAY_STEPS, updated)
            .apply()
        _stepCount.value = updated
    }

    fun resetSteps() {
        val todayStr = LocalDate.now().toString()
        prefs.edit()
            .putString(KEY_SAVED_DATE, todayStr)
            .putLong(KEY_TODAY_STEPS, 0L)
            .putLong(KEY_ACCUMULATED, 0L)
            .putLong(KEY_START_OFFSET, -1L)
            .apply()
        _stepCount.value = 0L
    }

    companion object {
        private const val TAG = "StepSensorManager"
        private const val PREFS_NAME = "walkcrew_step_sensor_prefs"
        private const val KEY_SAVED_DATE = "saved_date"
        private const val KEY_START_OFFSET = "start_of_day_offset"
        private const val KEY_ACCUMULATED = "accumulated_reboot_steps"
        private const val KEY_TODAY_STEPS = "today_steps_count"
        private const val KEY_ARCHIVE_PREFIX = "history_day_"

        @Volatile
        private var INSTANCE: StepSensorManager? = null

        fun getInstance(context: Context): StepSensorManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StepSensorManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
