package com.example.data

import android.util.Log

/**
 * Centralized logging and error tracking for WalkCrew Firestore operations.
 */
object WalkCrewLogger {
    const val TAG = "WalkCrew"

    @Volatile
    var lastFirestoreError: String? = null
        private set

    fun logRead(collection: String, docId: String? = null, details: String = "") {
        val target = if (docId != null) "$collection/$docId" else collection
        Log.d(TAG, "Firestore READ: $target $details".trim())
    }

    fun logWrite(collection: String, docId: String? = null, details: String = "") {
        val target = if (docId != null) "$collection/$docId" else collection
        Log.d(TAG, "Firestore WRITE: $target $details".trim())
    }

    fun logError(operation: String, error: Throwable) {
        // Ignore normal job cancellations
        if (error is kotlinx.coroutines.CancellationException) return

        val msg = error.message ?: "Unknown error"
        
        // Handle offline state gracefully without full stack trace
        if (msg.contains("offline", ignoreCase = true)) {
            Log.w(TAG, "Firestore $operation postponed: client is offline.")
            return
        }

        lastFirestoreError = "[$operation] $msg"
        if (msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
            msg.contains("permission-denied", ignoreCase = true) ||
            msg.contains("Missing or insufficient permissions", ignoreCase = true)
        ) {
            Log.e(TAG, "🚨 PERMISSION DENIED on $operation: $msg", error)
        } else {
            Log.e(TAG, "Firestore error on $operation: $msg", error)
        }
    }

    fun clearLastError() {
        lastFirestoreError = null
    }
}
