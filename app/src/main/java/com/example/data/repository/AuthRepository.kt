package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.example.data.api.WorkerApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

data class AccountCollisionInfo(
    val email: String,
    val existingProvider: String, // e.g. "password", "google.com", "facebook.com"
    val pendingCredential: AuthCredential
)

class AuthRepository(
    private val context: Context,
    val auth: FirebaseAuth = run {
        com.example.data.FirebaseInitializer.ensureInitialized(context)
        FirebaseAuth.getInstance()
    }
) {
    private val credentialManager = CredentialManager.create(context)
    private val facebookCallbackManager = CallbackManager.Factory.create()
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // Store pending credential for account linking across collision resolution
    var pendingCollision: AccountCollisionInfo? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    val currentUserId: String?
        get() = auth.currentUser?.uid

    fun handleFacebookActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return facebookCallbackManager.onActivityResult(requestCode, resultCode, data)
    }

    // --- Email & Password Sign Up ---
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: return Result.failure(Exception("Failed to create user account"))

            if (displayName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
                user.updateProfile(profileUpdates).await()
            }

            // Send initial 6-digit verification code
            try {
                sendVerificationCode()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send initial verification code: ${e.message}")
            }

            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up with email failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- Email & Password Sign In ---
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: return Result.failure(Exception("Sign-in failed"))
            _currentUser.value = user

            // Check if there is a pending collision credential to link
            linkPendingCredentialIfAny(user)

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in with email failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- Password Reset ---
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send password reset email: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- Email Verification (OTP via Cloudflare Worker) ---
    private val workerApi = WorkerApi.getInstance()

    suspend fun sendVerificationCode(): Result<Unit> {
        return workerApi.sendCode()
    }

    suspend fun verifyEmailCode(code: String): Result<Unit> {
        val result = workerApi.verifyCode(code)
        if (result.isSuccess) {
            val user = auth.currentUser
            user?.reload()?.await()
            user?.getIdToken(true)?.await()
            _currentUser.value = auth.currentUser
        }
        return result
    }

    suspend fun reloadUser(): Result<FirebaseUser> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            user.reload().await()
            _currentUser.value = auth.currentUser
            Result.success(auth.currentUser ?: user)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reload user: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- Google Sign-In ---
    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            val clientId = if (resId != 0) {
                context.getString(resId)
            } else {
                "walkcrew-client-id.apps.googleusercontent.com"
            }
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                try {
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        _currentUser.value = user
                        linkPendingCredentialIfAny(user)
                        Result.success(user)
                    } else {
                        Result.failure(Exception("Failed to obtain user profile from Google credential"))
                    }
                } catch (collision: FirebaseAuthUserCollisionException) {
                    val collisionInfo = handleCollisionException(collision, authCredential)
                    Result.failure(collision)
                }
            } else {
                Result.failure(Exception("Unexpected credential type returned from CredentialManager"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled: ${e.message}")
            Result.failure(e)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun isFacebookConfigured(): Boolean {
        val resId = context.resources.getIdentifier("facebook_app_id", "string", context.packageName)
        if (resId == 0) return false
        val appId = context.getString(resId)
        return appId.isNotBlank() && appId != "123456789012345" && appId.length >= 8 && appId.all { it.isDigit() }
    }

    // --- Facebook Login ---
    suspend fun signInWithFacebook(activity: Activity): Result<FirebaseUser> =
        suspendCancellableCoroutine { continuation ->
            if (!isFacebookConfigured()) {
                continuation.resume(
                    Result.failure(
                        Exception("Facebook Login requires a valid Facebook App ID. Please configure your App ID in strings.xml.")
                    )
                )
                return@suspendCancellableCoroutine
            }

            try {
                com.facebook.FacebookSdk.sdkInitialize(activity.applicationContext)
            } catch (e: Throwable) {
                Log.w(TAG, "Facebook SDK init error: ${e.message}")
            }

            val loginManager = LoginManager.getInstance()

            loginManager.registerCallback(
                facebookCallbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        val token = result.accessToken.token
                        val authCredential = FacebookAuthProvider.getCredential(token)

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val authResult = auth.signInWithCredential(authCredential).await()
                                val user = authResult.user
                                if (user != null) {
                                    _currentUser.value = user
                                    linkPendingCredentialIfAny(user)
                                    continuation.resume(Result.success(user))
                                } else {
                                    continuation.resume(Result.failure(Exception("Facebook sign-in returned empty user")))
                                }
                            } catch (collision: FirebaseAuthUserCollisionException) {
                                handleCollisionException(collision, authCredential)
                                continuation.resume(Result.failure(collision))
                            } catch (e: Exception) {
                                continuation.resume(Result.failure(e))
                            }
                        }
                    }

                    override fun onCancel() {
                        continuation.resume(Result.failure(Exception("Facebook sign-in was cancelled")))
                    }

                    override fun onError(error: FacebookException) {
                        continuation.resume(Result.failure(error))
                    }
                }
            )

            try {
                loginManager.logInWithReadPermissions(activity, listOf("public_profile", "email"))
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }

    // --- Account Linking ---
    private fun handleCollisionException(
        collision: FirebaseAuthUserCollisionException,
        pendingCredential: AuthCredential
    ): AccountCollisionInfo {
        val email = collision.email ?: auth.currentUser?.email ?: ""
        // Determine existing provider from exception or fallback
        val existingProvider = when {
            collision.message?.contains("password", ignoreCase = true) == true -> "Email & Password"
            collision.message?.contains("google", ignoreCase = true) == true -> "Google"
            collision.message?.contains("facebook", ignoreCase = true) == true -> "Facebook"
            else -> "another sign-in method"
        }
        val info = AccountCollisionInfo(email, existingProvider, pendingCredential)
        pendingCollision = info
        return info
    }

    private suspend fun linkPendingCredentialIfAny(user: FirebaseUser) {
        val collision = pendingCollision ?: return
        try {
            user.linkWithCredential(collision.pendingCredential).await()
            Log.i(TAG, "Successfully linked pending credential to user ${user.uid}")
            pendingCollision = null
        } catch (e: Exception) {
            Log.w(TAG, "Could not link pending credential: ${e.message}")
        }
    }

    // --- Re-Authentication ---
    suspend fun reauthenticateWithPassword(password: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            val email = user.email ?: return Result.failure(Exception("No email associated with account"))
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Re-authentication with password failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun reauthenticateWithGoogle(activity: Activity): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            val clientId = if (resId != 0) context.getString(resId) else "walkcrew-client-id.apps.googleusercontent.com"
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                user.reauthenticate(authCredential).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unexpected credential type"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Re-authentication with Google failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun reauthenticateWithFacebook(activity: Activity): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            if (!isFacebookConfigured()) {
                continuation.resume(
                    Result.failure(
                        Exception("Facebook Login requires a valid Facebook App ID. Please configure your App ID in strings.xml.")
                    )
                )
                return@suspendCancellableCoroutine
            }
            val user = auth.currentUser
            if (user == null) {
                continuation.resume(Result.failure(Exception("No user logged in")))
                return@suspendCancellableCoroutine
            }
            try {
                com.facebook.FacebookSdk.sdkInitialize(activity.applicationContext)
            } catch (e: Throwable) {
                Log.w(TAG, "Facebook SDK init error: ${e.message}")
            }
            val loginManager = LoginManager.getInstance()
            loginManager.registerCallback(
                facebookCallbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        val token = result.accessToken.token
                        val authCredential = FacebookAuthProvider.getCredential(token)
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                user.reauthenticate(authCredential).await()
                                continuation.resume(Result.success(Unit))
                            } catch (e: Exception) {
                                continuation.resume(Result.failure(e))
                            }
                        }
                    }

                    override fun onCancel() {
                        continuation.resume(Result.failure(Exception("Re-authentication was cancelled")))
                    }

                    override fun onError(error: FacebookException) {
                        continuation.resume(Result.failure(error))
                    }
                }
            )
            loginManager.logInWithReadPermissions(activity, listOf("public_profile", "email"))
        }

    // --- Sign Out ---
    fun signOut(scope: CoroutineScope, onComplete: () -> Unit = {}) {
        auth.signOut()
        _currentUser.value = null
        try {
            LoginManager.getInstance().logOut()
        } catch (e: Exception) {
            Log.w(TAG, "Facebook logOut failed: ${e.message}")
        }
        scope.launch(Dispatchers.IO) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                onComplete()
            }
        }
    }

    // --- Delete Account ---
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            user.delete().await()
            _currentUser.value = null
            try {
                LoginManager.getInstance().logOut()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Could not clear credential state: ${e.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete account: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Helper to determine the primary auth provider for current user
    fun getCurrentUserProvider(): String {
        val user = auth.currentUser ?: return "none"
        for (info in user.providerData) {
            when (info.providerId) {
                GoogleAuthProvider.PROVIDER_ID -> return "google.com"
                FacebookAuthProvider.PROVIDER_ID -> return "facebook.com"
                EmailAuthProvider.PROVIDER_ID -> return "password"
            }
        }
        return "password"
    }

    fun isEmailVerified(): Boolean {
        val user = auth.currentUser ?: return false
        // Google and Facebook users are automatically treated as verified
        val isFederated = user.providerData.any {
            it.providerId == GoogleAuthProvider.PROVIDER_ID || it.providerId == FacebookAuthProvider.PROVIDER_ID
        }
        return isFederated || user.isEmailVerified
    }

    companion object {
        private const val TAG = "AuthRepository"

        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        // --- Friendly Error Mapping (Never raw exception strings) ---
        fun getFriendlyErrorMessage(throwable: Throwable?): String {
            if (throwable == null) return "An unexpected error occurred."
            if (throwable is WorkerApi.WorkerException) {
                return WorkerApi.getFriendlyMessage(throwable)
            }
            val msg = throwable.message ?: ""
            return when {
                throwable is FirebaseAuthInvalidUserException ->
                    "No account found with this email. Please check your spelling or sign up."
                throwable is FirebaseAuthInvalidCredentialsException ->
                    "Incorrect password or invalid credentials. Please try again."
                throwable is FirebaseAuthUserCollisionException ->
                    "This email is already registered with another sign-in method."
                throwable is FirebaseAuthWeakPasswordException ->
                    "Password is too weak. Please use at least 8 characters with numbers."
                throwable is FirebaseAuthRecentLoginRequiredException ->
                    "For your security, please re-authenticate before making this change."
                throwable is FirebaseNetworkException || msg.contains("network", ignoreCase = true) ->
                    "Network connection issue. Please check your internet connection."
                throwable is GetCredentialCancellationException || msg.contains("cancelled", ignoreCase = true) ->
                    "Sign-in was cancelled."
                msg.contains("TOO_MANY_ATTEMPTS_TRY_LATER", ignoreCase = true) || msg.contains("too many", ignoreCase = true) ->
                    "Too many attempts. Please wait a few minutes before trying again."
                msg.contains("Cannot get application info", ignoreCase = true) ||
                msg.contains("Error validating application", ignoreCase = true) ||
                msg.contains("Object with ID", ignoreCase = true) ||
                msg.contains("Facebook Login requires a valid Facebook App ID", ignoreCase = true) ||
                msg.contains("GraphMethodException", ignoreCase = true) ||
                msg.contains("OAuthException", ignoreCase = true) ->
                    "Facebook Login is not configured yet. Please configure your registered Facebook App ID in strings.xml."
                msg.contains("EMAIL_EXISTS", ignoreCase = true) ->
                    "An account with this email already exists. Please sign in."
                msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ->
                    "This sign-in method is currently disabled. Please contact support."
                msg.contains("USER_DISABLED", ignoreCase = true) ->
                    "This account has been disabled. Please contact support."
                else ->
                    throwable.localizedMessage?.takeIf { it.isNotBlank() && !it.contains("com.google") }
                        ?: "Unable to complete request. Please try again."
            }
        }
    }
}
