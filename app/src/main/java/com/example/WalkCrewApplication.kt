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



        // Initialize RevenueCat only when a valid Google Play/Amazon public API key ('goog_...' or 'amzn_...') is provided.
        // If missing, placeholder, or invalid, do not initialize and treat all users as free tier.
        val apiKey = try {
            BuildConfig.REVENUECAT_API_KEY.trim()
        } catch (e: Throwable) {
            ""
        }

        val isValidKey = apiKey.isNotBlank() &&
            apiKey != "placeholder_api_key" &&
            apiKey != "12345" &&
            !apiKey.contains("REVENUECAT_API_KEY") &&
            (apiKey.startsWith("goog_") || apiKey.startsWith("amzn_"))

        if (isValidKey) {
            try {
                Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
                Purchases.configure(
                    PurchasesConfiguration.Builder(this, apiKey)
                        .build()
                )
                RevenueCatManager.getInstance(this).setupAfterConfigure()
                Log.i("WalkCrewApp", "RevenueCat initialized with provided public API key")
            } catch (e: Exception) {
                Log.e("WalkCrewApp", "RevenueCat initialization failed: ${e.message}")
            }
        } else {
            Log.i("WalkCrewApp", "REVENUECAT_API_KEY is missing or not a valid public key ('goog_...' / 'amzn_...'). RevenueCat disabled; treating all users as free.")
        }
    }
}
