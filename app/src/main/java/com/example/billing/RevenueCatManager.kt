package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
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
 * Manages RevenueCat Billing in Test Store mode and production.
 * Handles customer entitlements, product offerings, purchases, and restoration.
 */
class RevenueCatManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "RevenueCatManager"
        const val ENTITLEMENT_PREMIUM = "premium"
        const val PRODUCT_MONTHLY = "walkcrew_monthly_premium"
        const val PRODUCT_YEARLY = "walkcrew_yearly_premium"

        // RevenueCat Test Store public API key
        const val REVENUECAT_TEST_STORE_KEY = "test_jWPmZHHsWTAOnILNsMOCKUwXmjN"

        // Production Google Play public API key fallback
        const val REVENUECAT_GOOGLE_API_KEY = "goog_cInkmqVIgHVeSVMSBFAXpzbmOtB"

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

    private val _isOfferingsLoading = MutableStateFlow(false)
    val isOfferingsLoading: StateFlow<Boolean> = _isOfferingsLoading.asStateFlow()

    private val _monthlyPackage = MutableStateFlow<Package?>(null)
    val monthlyPackage: StateFlow<Package?> = _monthlyPackage.asStateFlow()

    private val _yearlyPackage = MutableStateFlow<Package?>(null)
    val yearlyPackage: StateFlow<Package?> = _yearlyPackage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun setupAfterConfigure(appUserId: String? = null) {
        if (!Purchases.isConfigured) return

        try {
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

            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { info ->
                onCustomerInfoUpdated(info)
            }

            refreshCustomerInfo()
            loadOfferings()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RevenueCat listeners: ${e.message}")
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

    fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
        val hasPremium = customerInfo.entitlements[ENTITLEMENT_PREMIUM]?.isActive == true
        _isPremium.value = hasPremium
        Log.i(TAG, "RevenueCat entitlement updated: isPremium = $hasPremium")
    }

    fun updatePackages(monthly: Package?, yearly: Package?) {
        _monthlyPackage.value = monthly
        _yearlyPackage.value = yearly
    }

    fun loadOfferings() {
        if (!Purchases.isConfigured) return
        _isOfferingsLoading.value = true
        try {
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    _isOfferingsLoading.value = false
                    Log.w(TAG, "Offerings error: ${error.message}")
                },
                onSuccess = { offerings ->
                    _isOfferingsLoading.value = false
                    val currentOffering = offerings.current
                    if (currentOffering != null) {
                        val monthly = currentOffering.monthly
                            ?: currentOffering.availablePackages.firstOrNull {
                                it.packageType == PackageType.MONTHLY ||
                                it.identifier.contains("monthly", ignoreCase = true) ||
                                it.identifier.contains("month", ignoreCase = true)
                            }
                            ?: currentOffering.availablePackages.firstOrNull()

                        val yearly = currentOffering.annual
                            ?: currentOffering.availablePackages.firstOrNull {
                                it.packageType == PackageType.ANNUAL ||
                                it.identifier.contains("annual", ignoreCase = true) ||
                                it.identifier.contains("yearly", ignoreCase = true) ||
                                it.identifier.contains("year", ignoreCase = true)
                            }
                            ?: currentOffering.availablePackages.getOrNull(1)

                        _monthlyPackage.value = monthly
                        _yearlyPackage.value = yearly
                        Log.i(TAG, "Loaded offerings successfully: monthly=${monthly?.identifier}, yearly=${yearly?.identifier}")
                    } else {
                        Log.w(TAG, "Current offering is null")
                    }
                }
            )
        } catch (e: Exception) {
            _isOfferingsLoading.value = false
            Log.e(TAG, "Exception loading offerings: ${e.message}")
        }
    }

    /**
     * Executes In-App Purchase through RevenueCat SDK.
     */
    fun purchase(
        activity: Activity,
        pkg: Package?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (_isOfferingsLoading.value) {
            // Still loading offerings, avoid showing package error
            return
        }

        _isLoading.value = true
        _actionMessage.value = null

        if (pkg != null && Purchases.isConfigured) {
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
                        if (userCancelled) {
                            Log.i(TAG, "User cancelled purchase")
                        } else {
                            val friendlyMsg = mapPurchasesError(error)
                            _actionMessage.value = friendlyMsg
                            onError(friendlyMsg)
                        }
                    }
                }
            )
        } else {
            _isLoading.value = false
            val errorMsg = if (pkg == null) {
                "Unable to load subscription package. Please check your internet connection."
            } else {
                "Store is not configured."
            }
            _actionMessage.value = errorMsg
            onError(errorMsg)
        }
    }

    /**
     * Restores existing purchases for the account.
     */
    fun restorePurchases(
        onSuccess: (Boolean) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _isLoading.value = true
        _actionMessage.value = null

        if (Purchases.isConfigured) {
            Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    _isLoading.value = false
                    onCustomerInfoUpdated(customerInfo)
                    val active = customerInfo.entitlements[ENTITLEMENT_PREMIUM]?.isActive == true
                    _actionMessage.value = if (active) {
                        "Purchases restored successfully!"
                    } else {
                        "No active subscriptions found for this account."
                    }
                    onSuccess(active)
                }

                override fun onError(error: PurchasesError) {
                    _isLoading.value = false
                    val friendlyMsg = mapPurchasesError(error)
                    _actionMessage.value = friendlyMsg
                    onError(friendlyMsg)
                }
            })
        } else {
            _isLoading.value = false
            _actionMessage.value = "Store is not ready."
            onError("Store is not ready.")
        }
    }

    private fun mapPurchasesError(error: PurchasesError): String {
        return when (error.code) {
            PurchasesErrorCode.PurchaseCancelledError -> "Purchase was cancelled."
            PurchasesErrorCode.StoreProblemError -> "Store service error. Please try again later."
            PurchasesErrorCode.PurchaseNotAllowedError -> "Purchases are not allowed on this device."
            PurchasesErrorCode.PurchaseInvalidError -> "Invalid purchase details."
            PurchasesErrorCode.ProductNotAvailableForPurchaseError -> "Subscription is currently not available."
            PurchasesErrorCode.NetworkError -> "Network connection problem. Please check your connection."
            PurchasesErrorCode.InvalidCredentialsError -> "Invalid store credentials."
            else -> error.message
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
