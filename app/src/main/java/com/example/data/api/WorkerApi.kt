package com.example.data.api

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class WorkerApi private constructor() {
    private val client = OkHttpClient.Builder().build()
    private val baseUrl = "https://walkcrew-verify.eljaryhamza850.workers.dev"

    suspend fun sendCode(): Result<Unit> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/send-code"
        try {
            val user = FirebaseAuth.getInstance().currentUser
                ?: return@withContext Result.failure(WorkerException("AUTH_REQUIRED", "No user logged in", null, 401))
            val tokenResult = user.getIdToken(true).await()
            val token = tokenResult.token
                ?: return@withContext Result.failure(WorkerException("AUTH_TOKEN_MISSING", "Failed to obtain auth token", null, 401))

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .post(ByteArray(0).toRequestBody(null))
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                val bodyString = response.body?.string() ?: ""
                Log.d(TAG, "POST $url -> Status: $code, Body: $bodyString")

                val json = try {
                    if (bodyString.isNotBlank()) JSONObject(bodyString) else JSONObject()
                } catch (e: Exception) {
                    JSONObject()
                }

                val ok = json.optBoolean("ok", response.isSuccessful)
                if (ok && response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorCode = json.optString("error", if (response.code >= 500) "SERVER_ERROR" else "REQUEST_FAILED")
                    val message = json.optString("message", if (response.code == 429) "Too many attempts. Please wait." else "Failed to send code.")
                    val remaining = if (json.has("remaining")) json.getInt("remaining") else null
                    Result.failure(WorkerException(errorCode, "HTTP $code: $errorCode - $message", remaining, code))
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "POST $url -> Network IOException: ${e.message}", e)
            Result.failure(WorkerException("NETWORK", "Network error: ${e.localizedMessage ?: "Connection failed"}", null, 0))
        } catch (e: Exception) {
            Log.e(TAG, "POST $url -> Exception: ${e.message}", e)
            Result.failure(WorkerException("EXCEPTION", "Exception: ${e.localizedMessage ?: "Unexpected error"}", null, 500))
        }
    }

    suspend fun verifyCode(codeStr: String): Result<Unit> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/verify-code"
        try {
            val user = FirebaseAuth.getInstance().currentUser
                ?: return@withContext Result.failure(WorkerException("AUTH_REQUIRED", "No user logged in", null, 401))
            val tokenResult = user.getIdToken(true).await()
            val token = tokenResult.token
                ?: return@withContext Result.failure(WorkerException("AUTH_TOKEN_MISSING", "Failed to obtain auth token", null, 401))

            val jsonBody = JSONObject().put("code", codeStr.trim()).toString()
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                val bodyString = response.body?.string() ?: ""
                Log.d(TAG, "POST $url -> Status: $code, Body: $bodyString")

                val json = try {
                    if (bodyString.isNotBlank()) JSONObject(bodyString) else JSONObject()
                } catch (e: Exception) {
                    JSONObject()
                }

                val ok = json.optBoolean("ok", response.isSuccessful)
                if (ok && response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorCode = json.optString("error", "INVALID_CODE")
                    val message = json.optString("message", "Invalid verification code.")
                    val remaining = if (json.has("remaining")) json.getInt("remaining") else null
                    Result.failure(WorkerException(errorCode, "HTTP $code: $errorCode - $message", remaining, code))
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "POST $url -> Network IOException: ${e.message}", e)
            Result.failure(WorkerException("NETWORK", "Network error: ${e.localizedMessage ?: "Connection failed"}", null, 0))
        } catch (e: Exception) {
            Log.e(TAG, "POST $url -> Exception: ${e.message}", e)
            Result.failure(WorkerException("EXCEPTION", "Exception: ${e.localizedMessage ?: "Unexpected error"}", null, 500))
        }
    }

    class WorkerException(
        val errorCode: String,
        message: String,
        val remaining: Int? = null,
        val httpStatus: Int = 0
    ) : Exception(message)

    companion object {
        private const val TAG = "WorkerApi"

        @Volatile
        private var INSTANCE: WorkerApi? = null

        fun getInstance(): WorkerApi {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WorkerApi().also { INSTANCE = it }
            }
        }

        fun getFriendlyMessage(throwable: Throwable?): String {
            if (throwable is WorkerException) {
                return throwable.message ?: "An unexpected error occurred."
            }
            return throwable?.localizedMessage ?: "An unexpected error occurred."
        }
    }
}
