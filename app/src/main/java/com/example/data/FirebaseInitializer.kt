package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseInitializer {
    private const val TAG = "FirebaseInitializer"
    const val FIRESTORE_DATABASE_ID = "ai-studio-android-walkcrew-f78bcbe3-d4e3-4ce3-b319-6ae08a7aead2"

    @Volatile
    private var firestoreInstance: FirebaseFirestore? = null

    fun getFirestore(): FirebaseFirestore {
        return firestoreInstance ?: synchronized(this) {
            firestoreInstance ?: run {
                val app = try {
                    FirebaseApp.getInstance()
                } catch (e: Exception) {
                    null
                }
                val db = if (app != null) {
                    FirebaseFirestore.getInstance(app, FIRESTORE_DATABASE_ID)
                } else {
                    FirebaseFirestore.getInstance(FIRESTORE_DATABASE_ID)
                }

                try {
                    val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(
                            com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build()
                        )
                        .build()
                    db.firestoreSettings = settings
                    Log.d("WalkCrew", "Firestore offline persistence enabled for $FIRESTORE_DATABASE_ID")
                } catch (e: Exception) {
                    Log.d("WalkCrew", "Firestore settings note: ${e.message}")
                }

                firestoreInstance = db
                db
            }
        }
    }

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
        Log.d("WalkCrew", "Application ID (Package): ${app.applicationContext.packageName}")
        Log.d("WalkCrew", "App ID (Firebase): ${options.applicationId}")
        Log.d("WalkCrew", "API Key (suffix): ...${options.apiKey.takeLast(4)}")
        
        // Connectivity check
        getFirestore().run {
            this.addSnapshotsInSyncListener {
                Log.d("WalkCrew", "Firestore network connection available: ${!this.app.isDataCollectionDefaultEnabled}")
                // Note: accurate connection status is harder via SDK, but logging info helps
            }
        }
    }
}
