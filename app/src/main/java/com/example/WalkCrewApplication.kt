package com.example

import android.app.Application
import android.util.Log
import com.example.data.FirebaseInitializer
import com.google.firebase.messaging.FirebaseMessaging

class WalkCrewApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseInitializer.ensureInitialized(this)
        FirebaseInitializer.getFirestore()

        try {
            FirebaseMessaging.getInstance().isAutoInitEnabled = false
        } catch (e: Exception) {
            Log.d("WalkCrewApp", "FirebaseMessaging auto-init disabled: ${e.message}")
        }
    }
}
