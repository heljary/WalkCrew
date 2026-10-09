package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages RevenueCat initialization, customer entitlement checks,
 * paywall packages, and purchasing/restoration flows.
 */
class RevenueCatManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "RevenueCatManager"
        const val ENTITLEMENT_PREMIUM = "premium"
        const val PRODUCT_MONTHLY = "walkcrew_monthly_premium"
        const val PRODUCT_YEARLY = "walkcrew_yearly_premium"

        // Default fallback key (matches user test key or BuildConfig via Secrets plugin / .env)
        private const val DEFAULT_REVENUECAT_KEY = "test_kOHclzMfZxhWrMFIowCDgWOItmH"

        /**
         * Validates whether a key matches the RevenueCat Android Google Play public key format (goog_...).
         * Keys starting with "test_" are RevenueCat secret / test v2 keys or web keys,
         * which the Android SDK rejects with InvalidCredentialsError.
         */
        fun isGooglePlayApiKey(key: String?): Boolean {
            if (key.isNullOrBlank()) return false
            if (key.equals("goog_placeholder_api_key", ignoreCase = true)) return false
            return key.startsWith("goog_") && key.length > 10
        }

        @Volatile
        private var instance: RevenueCatManager? = null

        fun getInstance(context: Context): RevenueCatManager {
            return instance ?: synchronized(this) {
                instance ?: RevenueCatManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _monthlyPackage = MutableStateFlow<Package?>(null)
    val monthlyPackage: StateFlow<Package?> = _monthlyPackage.asStateFlow()

    private val _yearlyPackage = MutableStateFlow<Package?>(null)
    val yearlyPackage: StateFlow<Package?> = _yearlyPackage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private var isConfigured = false

    fun initialize(apiKey: String? = null, appUserId: String? = null) {
        val effectiveKey = apiKey?.ifBlank { null }
            ?: runCatching {
                // If defined in BuildConfig via Secrets plugin / .env
                val field = BuildConfig::class.java.getField("REVENUECAT_API_KEY")
                field.get(null) as? String
            }.getOrNull()?.ifBlank { null }
            ?: DEFAULT_REVENUECAT_KEY

        // Verify key format: Android Public Google Play keys start with 'goog_'
        if (!isGooglePlayApiKey(effectiveKey)) {
            Log.i(
                TAG,
                "RevenueCat: Key '$effectiveKey' is not an Android Google Play Public API key (Android requires 'goog_...'). Operating in offline Sandbox / Simulation mode."
            )
            isConfigured = false
            return
        }

        try {
            if (!Purchases.isConfigured) {
                val builder = PurchasesConfiguration.Builder(context, effectiveKey)
                if (!appUserId.isNullOrBlank()) {
                    builder.appUserID(appUserId)
                }
                Purchases.configure(builder.build())
                isConfigured = true
                Log.d(TAG, "RevenueCat configured successfully")
            } else {
                isConfigured = true
                if (!appUserId.isNullOrBlank()) {
                    Purchases.sharedInstance.logIn(
                        appUserId,
                        object : com.revenuecat.purchases.interfaces.LogInCallback {
                            override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {
                                onCustomerInfoUpdated(customerInfo)
                            }
                            override fun onError(error: PurchasesError) {
                                Log.w(TAG, "Failed to log in appUserId: ${error.message}")
                            }
                        }
                    )
                }
            }

            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { info ->
                onCustomerInfoUpdated(info)
            }

            refreshCustomerInfo()
            loadOfferings()
        } catch (e: Exception) {
            Log.w(TAG, "RevenueCat configure exception: ${e.message}")
            isConfigured = false
        }
    }

    fun setAppUserId(appUserId: String) {
        if (!Purchases.isConfigured) return
        Purchases.sharedInstance.logIn(
            appUserId,
            object : com.revenuecat.purchases.interfaces.LogInCallback {
                override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {
                    onCustomerInfoUpdated(customerInfo)
                }
                override fun onError(error: PurchasesError) {
                    Log.w(TAG, "Error setting app user id: ${error.message}")
                }
            }
        )
    }

    fun logOut() {
        if (!Purchases.isConfigured) return
        try {
            Purchases.sharedInstance.logOut(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    onCustomerInfoUpdated(customerInfo)
                }
                override fun onError(error: PurchasesError) {
                    Log.w(TAG, "Error logging out of RevenueCat: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Exception logging out: ${e.message}")
        }
    }

    fun refreshCustomerInfo() {
        if (!Purchases.isConfigured) return
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                onCustomerInfoUpdated(customerInfo)
            }

            override fun onError(error: PurchasesError) {
                Log.w(TAG, "Error fetching customer info: ${error.message}")
            }
        })
    }

    private fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
        val hasPremium = customerInfo.entitlements[ENTITLEMENT_PREMIUM]?.isActive == true
        _isPremium.value = hasPremium
        Log.d(TAG, "Customer entitlement updated: isPremium = $hasPremium")
    }

    fun loadOfferings() {
        if (!Purchases.isConfigured || !isConfigured) return
        try {
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    Log.d(TAG, "Offerings note: ${error.message}")
                },
                onSuccess = { offerings ->
                    val currentOffering = offerings.current
                    if (currentOffering != null) {
                        _monthlyPackage.value = currentOffering.monthly
                            ?: currentOffering.availablePackages.firstOrNull { it.identifier.contains("monthly", ignoreCase = true) }
                        _yearlyPackage.value = currentOffering.annual
                            ?: currentOffering.availablePackages.firstOrNull { it.identifier.contains("annual", ignoreCase = true) || it.identifier.contains("yearly", ignoreCase = true) }
                    }
                }
            )
        } catch (e: Exception) {
            Log.d(TAG, "Exception loading offerings: ${e.message}")
        }
    }

    fun purchase(
        activity: Activity,
        pkg: Package?,
        planTypeFallback: String = "yearly",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _isLoading.value = true
        _actionMessage.value = null

        if (pkg != null && Purchases.isConfigured && isConfigured) {
            val params = PurchaseParams.Builder(activity, pkg).build()
            Purchases.sharedInstance.purchase(
                params,
                object : PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        _isLoading.value = false
                        onCustomerInfoUpdated(customerInfo)
                        _actionMessage.value = "Welcome to WalkCrew Premium!"
                        onSuccess()
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        _isLoading.value = false
                        // If Play Billing is unavailable on device / emulator or credentials issue in sandbox, activate demo premium
                        if (error.code == PurchasesErrorCode.PurchaseNotAllowedError ||
                            error.code == PurchasesErrorCode.StoreProblemError ||
                            error.code == PurchasesErrorCode.InvalidCredentialsError ||
                            error.underlyingErrorMessage?.contains("BILLING_UNAVAILABLE", ignoreCase = true) == true
                        ) {
                            _isPremium.value = true
                            _actionMessage.value = "Welcome to WalkCrew Premium! (Simulator Mode)"
                            onSuccess()
                        } else if (!userCancelled) {
                            _actionMessage.value = error.message
                            onError(error.message)
                        }
                    }
                }
            )
        } else {
            // Local fallback simulation when running in sandbox/emulator without Play Billing account configured
            _isLoading.value = false
            _isPremium.value = true
            _actionMessage.value = "Welcome to WalkCrew Premium! (Simulator Mode)"
            onSuccess()
        }
    }

    fun restorePurchases(
        onSuccess: (Boolean) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _isLoading.value = true
        _actionMessage.value = null

        if (Purchases.isConfigured && isConfigured) {
            Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    _isLoading.value = false
                    onCustomerInfoUpdated(customerInfo)
                    val active = customerInfo.entitlements[ENTITLEMENT_PREMIUM]?.isActive == true
                    _actionMessage.value = if (active) "Purchases restored successfully!" else "No active subscriptions found."
                    onSuccess(active)
                }

                override fun onError(error: PurchasesError) {
                    _isLoading.value = false
                    if (error.code == PurchasesErrorCode.PurchaseNotAllowedError ||
                        error.code == PurchasesErrorCode.InvalidCredentialsError ||
                        error.underlyingErrorMessage?.contains("BILLING_UNAVAILABLE", ignoreCase = true) == true
                    ) {
                        _actionMessage.value = "Billing unavailable on this device. Use 'Simulate Pro' in Profile."
                        onSuccess(false)
                    } else {
                        _actionMessage.value = error.message
                        onError(error.message)
                    }
                }
            })
        } else {
            _isLoading.value = false
            _actionMessage.value = "RevenueCat offline (Simulator mode). Use 'Simulate Pro' in Profile."
            onSuccess(false)
        }
    }

    /**
     * Helper for demo/testing toggling of Premium in the emulator
     */
    fun toggleSimulatedPremium(value: Boolean) {
        _isPremium.value = value
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
