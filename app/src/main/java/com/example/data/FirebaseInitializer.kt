package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseInitializer {
    private const val TAG = "FirebaseInitializer"

    fun ensureInitialized(context: Context): FirebaseApp {
        val appContext = context.applicationContext ?: context
        if (FirebaseApp.getApps(appContext).isNotEmpty()) {
            return FirebaseApp.getInstance()
        }

        return try {
            FirebaseApp.initializeApp(appContext) ?: throw IllegalStateException("FirebaseApp.initializeApp returned null")
        } catch (e: Exception) {
            Log.w(TAG, "Default initializeApp failed (${e.message}), initializing with explicit options")
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:918401212822:android:69e9e8fbcc75d9331c7db7")
                .setProjectId("gen-lang-client-0840698630")
                .setApiKey("AIzaSyAPiQQarRoTlA-9hqRpj9DtJSTUUhR5tug")
                .setStorageBucket("gen-lang-client-0840698630.firebasestorage.app")
                .setGcmSenderId("918401212822")
                .build()
            FirebaseApp.initializeApp(appContext, options)
        }
    }
}
