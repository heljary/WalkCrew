package com.example

import android.app.Application
import android.util.Log
import com.example.billing.RevenueCatManager
import com.example.data.FirebaseInitializer
import com.google.firebase.messaging.FirebaseMessaging
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class WalkCrewApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseInitializer.ensureInitialized(this)

        try {
            FirebaseMessaging.getInstance().isAutoInitEnabled = false
        } catch (e: Exception) {
            Log.d("WalkCrewApp", "FirebaseMessaging auto-init disabled: ${e.message}")
        }



        // Initialize RevenueCat only when a valid Google Play public API key ('goog_...') is provided
        val apiKey = try {
            BuildConfig.REVENUECAT_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey.startsWith("goog_")) {
            try {
                Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
                Purchases.configure(
                    PurchasesConfiguration.Builder(this, apiKey)
                        .build()
                )
                RevenueCatManager.getInstance(this).setupAfterConfigure()
                Log.i("WalkCrewApp", "RevenueCat initialized with provided Google Play API key")
            } catch (e: Exception) {
                Log.e("WalkCrewApp", "RevenueCat initialization failed: ${e.message}")
            }
        } else {
            Log.i("WalkCrewApp", "REVENUECAT_API_KEY is not configured with a valid Google Play key ('goog_...'). Treating all users as free tier.")
        }
    }
}
