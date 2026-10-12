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



        // Test Store Billing initialization with RevenueCat
        try {
            Purchases.logLevel = LogLevel.DEBUG
            Purchases.configure(
                PurchasesConfiguration.Builder(this, "test_jWPmZHHsWTAOnILNsMOCKUwXmjN")
                    .build()
            )
            RevenueCatManager.getInstance(this).setupAfterConfigure()
            Log.i("WalkCrewApp", "RevenueCat configured with Test Store API key in LogLevel.DEBUG")
        } catch (e: Exception) {
            Log.e("WalkCrewApp", "RevenueCat initialization failed: ${e.message}")
        }
    }
}
