package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.example.data.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Handles image downscaling/compression and Firebase Storage profile picture operations.
 */
class ProfilePhotoRepository(
    private val context: Context,
    private val userRepository: UserRepository = UserRepository.getInstance(context),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val storage: FirebaseStorage by lazy {
        com.example.data.FirebaseInitializer.ensureInitialized(context)
        FirebaseStorage.getInstance()
    }

    /**
     * Safely reads, downscales, auto-rotates EXIF, and compresses an image to JPEG bytes.
     * Prevents OOM by sampling large pictures.
     */
    suspend fun compressAndDownscale(
        uri: Uri,
        maxDimension: Int = 1024,
        quality: Int = 85
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // 1. Calculate inSampleSize
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: return@withContext null

            val originalWidth = boundsOptions.outWidth
            val originalHeight = boundsOptions.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) return@withContext null

            var sampleSize = 1
            while ((originalWidth / sampleSize) > maxDimension * 2 || (originalHeight / sampleSize) > maxDimension * 2) {
                sampleSize *= 2
            }

            // 2. Decode sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            // 3. Check and apply EXIF orientation
            val orientation = getExifOrientation(uri)
            val matrix = Matrix()
            if (orientation != 0f) {
                matrix.postRotate(orientation)
            }

            // 4. Scale down to maxDimension if still larger
            val currentWidth = sampledBitmap.width
            val currentHeight = sampledBitmap.height
            val scale = if (currentWidth > maxDimension || currentHeight > maxDimension) {
                val maxCurr = maxOf(currentWidth, currentHeight).toFloat()
                maxDimension.toFloat() / maxCurr
            } else {
                1f
            }
            if (scale < 1f) {
                matrix.postScale(scale, scale)
            }

            val finalBitmap = if (!matrix.isIdentity) {
                Bitmap.createBitmap(
                    sampledBitmap,
                    0,
                    0,
                    sampledBitmap.width,
                    sampledBitmap.height,
                    matrix,
                    true
                )
            } else {
                sampledBitmap
            }

            // 5. Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()

            if (finalBitmap != sampledBitmap) {
                finalBitmap.recycle()
            }
            sampledBitmap.recycle()

            bytes
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image: ${e.message}", e)
            null
        }
    }

    private fun getExifOrientation(uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exifInterface = ExifInterface(stream)
                when (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Uploads the JPEG image bytes to Firebase Storage under `profile_pictures/{userId}.jpg`.
     * Upon success, updates both Firebase Auth and Firestore user profile.
     */
    suspend fun uploadProfilePhoto(userId: String, imageBytes: ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val storageRef = storage.reference.child("profile_pictures/$userId.jpg")
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCacheControl("public, max-age=86400")
                    .build()

                storageRef.putBytes(imageBytes, metadata).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                // 1. Update Firebase Auth user profile
                val currentUser = auth.currentUser
                if (currentUser != null && currentUser.uid == userId) {
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setPhotoUri(Uri.parse(downloadUrl))
                        .build()
                    currentUser.updateProfile(profileUpdate).await()
                }

                // 2. Update Firestore user document
                val firestoreResult = userRepository.updatePhotoUrl(userId, downloadUrl)
                if (firestoreResult.isFailure) {
                    Log.w(TAG, "Firestore updatePhotoUrl warning: ${firestoreResult.exceptionOrNull()?.message}")
                }

                Result.success(downloadUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload profile photo: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Deletes the profile picture from Firebase Storage, clears Firebase Auth photoUri,
     * and resets `photoUrl` in Firestore to null.
     */
    suspend fun deleteProfilePhoto(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val storageRef = storage.reference.child("profile_pictures/$userId.jpg")
                try {
                    storageRef.delete().await()
                } catch (e: Exception) {
                    // Ignore not-found errors so users can reset even if storage file was missing
                    Log.d(TAG, "Storage delete note (might not exist): ${e.message}")
                }

                // 1. Update Firebase Auth user profile
                val currentUser = auth.currentUser
                if (currentUser != null && currentUser.uid == userId) {
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setPhotoUri(null)
                        .build()
                    currentUser.updateProfile(profileUpdate).await()
                }

                // 2. Update Firestore user document
                userRepository.updatePhotoUrl(userId, null)

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete profile photo: ${e.message}", e)
                Result.failure(e)
            }
        }

    companion object {
        private const val TAG = "ProfilePhotoRepo"

        @Volatile
        private var INSTANCE: ProfilePhotoRepository? = null

        fun getInstance(context: Context): ProfilePhotoRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProfilePhotoRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
