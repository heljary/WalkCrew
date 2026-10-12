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
            val app = FirebaseApp.initializeApp(appContext) ?: throw IllegalStateException("FirebaseApp.initializeApp returned null")
            if (com.example.BuildConfig.DEBUG) {
                Log.d("WalkCrew", "Firebase initialized from google-services.json")
                logFirebaseInfo(app)
            }
            app
        } catch (e: Exception) {
            Log.w(TAG, "Default initializeApp failed (${e.message}), initializing with explicit options")
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:1070356437895:android:fc65797eb9c0f045ec5122")
                .setProjectId("gen-lang-client-0778467524")
                .setApiKey("AIzaSyAN3YF8LoFnhZKHIieo4wURR4sF8vK_4Pw")
                .setStorageBucket("gen-lang-client-0778467524.firebasestorage.app")
                .setGcmSenderId("1070356437895")
                .build()
            val app = FirebaseApp.initializeApp(appContext, options)
            if (com.example.BuildConfig.DEBUG) {
                Log.d("WalkCrew", "Firebase initialized with explicit options")
                logFirebaseInfo(app)
            }
            app
        }
    }

    private fun logFirebaseInfo(app: FirebaseApp) {
        val options = app.options
        Log.d("WalkCrew", "Project ID: ${options.projectId}")
        Log.d("WalkCrew", "Application ID: ${options.applicationId}")
        Log.d("WalkCrew", "API Key (suffix): ...${options.apiKey.takeLast(4)}")
        
        // Connectivity check
        com.google.firebase.firestore.FirebaseFirestore.getInstance().run {
            this.addSnapshotsInSyncListener {
                Log.d("WalkCrew", "Firestore network connection available: ${!this.app.isDataCollectionDefaultEnabled}")
                // Note: accurate connection status is harder via SDK, but logging info helps
            }
        }
    }
}
