package com.example

import android.app.Application
import android.util.Log
import com.example.billing.RevenueCatManager
import com.example.data.FirebaseInitializer
import com.google.firebase.messaging.FirebaseMessaging

class WalkCrewApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseInitializer.ensureInitialized(this)

        try {
            FirebaseMessaging.getInstance().isAutoInitEnabled = false
        } catch (e: Exception) {
            Log.d("WalkCrewApp", "FirebaseMessaging auto-init disabled: ${e.message}")
        }

        try {
            if (com.facebook.FacebookSdk.isInitialized()) {
                com.facebook.FacebookSdk.setAutoLogAppEventsEnabled(false)
                com.facebook.FacebookSdk.setAdvertiserIDCollectionEnabled(false)
            }
        } catch (e: Throwable) {
            // Ignore
        }

        try {
            RevenueCatManager.getInstance(this).initialize()
        } catch (e: Exception) {
            Log.w("WalkCrewApp", "RevenueCat initialization: ${e.message}")
        }
    }
}
