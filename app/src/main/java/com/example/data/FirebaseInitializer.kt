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
                .setApplicationId("1:1070356437895:android:fc65797eb9c0f045ec5122")
                .setProjectId("gen-lang-client-0778467524")
                .setApiKey("AIzaSyAN3YF8LoFnhZKHIieo4wURR4sF8vK_4Pw")
                .setStorageBucket("gen-lang-client-0778467524.firebasestorage.app")
                .setGcmSenderId("1070356437895")
                .build()
            FirebaseApp.initializeApp(appContext, options)
        }
    }
}
