package com.example.notifications

import android.util.Log
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WalkCrewMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM registration token received: $token")
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "WalkCrew Nudge"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "A teammate nudged you to get your steps in today!"

        NotificationHelper.showLocalNotification(
            context = applicationContext,
            title = title,
            message = body,
            channelId = NotificationHelper.CHANNEL_NUDGES,
            notificationId = System.currentTimeMillis().toInt()
        )
    }

    private fun saveTokenToFirestore(token: String) {
        val authRepo = AuthRepository.getInstance(applicationContext)
        val uid = authRepo.currentUserId ?: return
        val userRepo = UserRepository.getInstance(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                userRepo.db.collection("users").document(uid).update(
                    mapOf("fcmToken" to token)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save FCM token: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "WalkCrewMessaging"
    }
}
