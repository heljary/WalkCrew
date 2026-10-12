package com.example.ui.paywall

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.billing.RevenueCatManager
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
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
import kotlinx.coroutines.launch

class PaywallViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PaywallViewModel"
    }

    private val revenueCatManager = RevenueCatManager.getInstance(application)

    private val _isOfferingsLoading = MutableStateFlow(true)
    val isOfferingsLoading: StateFlow<Boolean> = _isOfferingsLoading.asStateFlow()

    private val _monthlyPackage = MutableStateFlow<Package?>(null)
    val monthlyPackage: StateFlow<Package?> = _monthlyPackage.asStateFlow()

    private val _yearlyPackage = MutableStateFlow<Package?>(null)
    val yearlyPackage: StateFlow<Package?> = _yearlyPackage.asStateFlow()

    private val _selectedPlan = MutableStateFlow(SubscriptionPlan.YEARLY)
    val selectedPlan: StateFlow<SubscriptionPlan> = _selectedPlan.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    val isPremium: StateFlow<Boolean> = revenueCatManager.isPremium

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        // Register real-time entitlement listener
        if (Purchases.isConfigured) {
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
                handleCustomerInfo(customerInfo)
            }
            refreshCustomerInfo()
        }
        fetchOfferings()
    }

    fun selectPlan(plan: SubscriptionPlan) {
        _selectedPlan.value = plan
    }

    /**
     * Asynchronously fetches offerings from RevenueCat (offerings.current).
     */
    fun fetchOfferings() {
        if (!Purchases.isConfigured) {
            _isOfferingsLoading.value = false
            return
        }

        _isOfferingsLoading.value = true
        _actionMessage.value = null

        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                _isOfferingsLoading.value = false
                Log.w(TAG, "Failed to load offerings: ${error.message}")
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
                        } ?: currentOffering.availablePackages.firstOrNull()

                    val yearly = currentOffering.annual
                        ?: currentOffering.availablePackages.firstOrNull {
                            it.packageType == PackageType.ANNUAL ||
                            it.identifier.contains("annual", ignoreCase = true) ||
                            it.identifier.contains("yearly", ignoreCase = true) ||
                            it.identifier.contains("year", ignoreCase = true)
                        } ?: currentOffering.availablePackages.getOrNull(1)

                    _monthlyPackage.value = monthly
                    _yearlyPackage.value = yearly
                    revenueCatManager.updatePackages(monthly, yearly)
                    Log.i(TAG, "Offerings fetched: monthly=${monthly?.identifier}, yearly=${yearly?.identifier}")
                } else {
                    Log.w(TAG, "Offerings.current is null. Available offerings: ${offerings.all.keys}")
                }
            }
        )
    }

    fun refreshCustomerInfo() {
        if (!Purchases.isConfigured) return
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                handleCustomerInfo(customerInfo)
            }
            override fun onError(error: PurchasesError) {
                Log.w(TAG, "Failed to retrieve customer info: ${error.message}")
            }
        })
    }

    private fun handleCustomerInfo(customerInfo: CustomerInfo) {
        revenueCatManager.onCustomerInfoUpdated(customerInfo)
        val hasPremium = customerInfo.entitlements[RevenueCatManager.ENTITLEMENT_PREMIUM]?.isActive == true
        Log.i(TAG, "Entitlement 'premium' active status: $hasPremium")
    }

    /**
     * Triggers purchase flow with RevenueCat SDK.
     */
    fun purchase(
        activity: Activity,
        onSuccess: () -> Unit = {}
    ) {
        if (_isOfferingsLoading.value) {
            // Still loading offerings, avoid showing error message
            return
        }

        val pkgToBuy = if (_selectedPlan.value == SubscriptionPlan.YEARLY) {
            _yearlyPackage.value
        } else {
            _monthlyPackage.value
        }

        if (pkgToBuy == null) {
            _actionMessage.value = "Selected subscription plan is not loaded yet. Please wait a moment."
            return
        }

        if (!Purchases.isConfigured) {
            _actionMessage.value = "Store is not configured."
            return
        }

        _isPurchasing.value = true
        _actionMessage.value = null

        val params = PurchaseParams.Builder(activity, pkgToBuy).build()
        Purchases.sharedInstance.purchase(
            params,
            object : PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                    _isPurchasing.value = false
                    handleCustomerInfo(customerInfo)
                    val active = customerInfo.entitlements[RevenueCatManager.ENTITLEMENT_PREMIUM]?.isActive == true
                    if (active) {
                        _actionMessage.value = "Welcome to WalkCrew Premium!"
                        onSuccess()
                    } else {
                        _actionMessage.value = "Purchase completed."
                    }
                }

                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    _isPurchasing.value = false
                    if (userCancelled) {
                        Log.i(TAG, "Purchase cancelled by user")
                    } else {
                        val friendlyMsg = mapPurchasesError(error)
                        _actionMessage.value = friendlyMsg
                    }
                }
            }
        )
    }

    fun restorePurchases(onSuccess: (Boolean) -> Unit = {}) {
        _isPurchasing.value = true
        _actionMessage.value = null

        if (Purchases.isConfigured) {
            Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    _isPurchasing.value = false
                    handleCustomerInfo(customerInfo)
                    val active = customerInfo.entitlements[RevenueCatManager.ENTITLEMENT_PREMIUM]?.isActive == true
                    _actionMessage.value = if (active) {
                        "Purchases restored successfully!"
                    } else {
                        "No active subscriptions found for this account."
                    }
                    onSuccess(active)
                }

                override fun onError(error: PurchasesError) {
                    _isPurchasing.value = false
                    _actionMessage.value = mapPurchasesError(error)
                }
            })
        } else {
            _isPurchasing.value = false
            _actionMessage.value = "Store is not configured."
        }
    }

    private fun mapPurchasesError(error: PurchasesError): String {
        return when (error.code) {
            PurchasesErrorCode.PurchaseCancelledError -> "Purchase was cancelled."
            PurchasesErrorCode.StoreProblemError -> "Store service error. Please try again."
            PurchasesErrorCode.PurchaseNotAllowedError -> "Purchases are not allowed on this device."
            PurchasesErrorCode.PurchaseInvalidError -> "Invalid purchase details."
            PurchasesErrorCode.ProductNotAvailableForPurchaseError -> "Subscription is currently not available."
            PurchasesErrorCode.NetworkError -> "Network connection problem. Please check your connection."
            else -> error.message
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
