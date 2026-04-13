package com.ypg.neville.model.subscription

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.PendingPurchasesParams
import com.ypg.neville.model.preferences.DbPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SubscriptionUiState(
    val isActive: Boolean = false,
    val isEntitlementVerified: Boolean = false,
    val isBillingReady: Boolean = false,
    val isLoading: Boolean = true,
    val productTitle: String = "Suscripción anual",
    val productPrice: String? = null,
    val lastMessage: String? = null
)

object SubscriptionManager : PurchasesUpdatedListener {

    const val PRODUCT_ID_ANNUAL = "premium_anual"
    private const val BASE_PLAN_ID_ANNUAL = "anual"

    private lateinit var appContext: Context
    private var billingClient: BillingClient? = null
    private var annualDetails: ProductDetails? = null

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext

        val now = System.currentTimeMillis()
        val local = readBillingFallbackLease(now)

        _uiState.value = _uiState.value.copy(
            isActive = local != null,
            isEntitlementVerified = local != null,
            isLoading = true,
            lastMessage = null
        )

        if (billingClient == null) {
            billingClient = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build()
                )
                .build()
        }
        ensureConnected()
    }

    fun refreshStatus() {
        if (!::appContext.isInitialized) return
        ensureConnected()
    }

    fun launchPurchase(activity: Activity): Boolean {
        val client = billingClient ?: return false
        val details = annualDetails ?: return false
        val offer = selectAnnualOffer(details) ?: return false
        val offerToken = offer.offerToken

        val product = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(product))
            .build()

        val result = client.launchBillingFlow(activity, params)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    fun restorePurchases() {
        queryActivePurchases()
    }

    fun hasActiveSubscriptionNow(): Boolean {
        val state = _uiState.value
        return state.isEntitlementVerified && state.isActive
    }

    fun hasVerifiedActiveSubscriptionNow(): Boolean = hasActiveSubscriptionNow()

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                processPurchases(purchases.orEmpty())
                queryActivePurchases()
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _uiState.value = _uiState.value.copy(lastMessage = "Compra cancelada")
            }

            else -> {
                _uiState.value = _uiState.value.copy(
                    lastMessage = "Error de compra: ${billingResult.debugMessage.ifBlank { billingResult.responseCode.toString() }}"
                )
            }
        }
    }

    private fun ensureConnected() {
        val client = billingClient ?: return
        if (client.isReady) {
            onBillingReady()
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true)
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onBillingReady()
                } else {
                    val now = System.currentTimeMillis()
                    val canFallback = readBillingFallbackLease(now) != null
                    _uiState.value = _uiState.value.copy(
                        isActive = canFallback,
                        isEntitlementVerified = canFallback,
                        isBillingReady = false,
                        isLoading = false,
                        lastMessage = "No se pudo conectar con Google Play"
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                val now = System.currentTimeMillis()
                val canFallback = readBillingFallbackLease(now) != null
                _uiState.value = _uiState.value.copy(
                    isActive = canFallback,
                    isEntitlementVerified = canFallback,
                    isBillingReady = false
                )
            }
        })
    }

    private fun onBillingReady() {
        _uiState.value = _uiState.value.copy(
            isActive = false,
            isEntitlementVerified = false,
            isBillingReady = true,
            isLoading = true,
            lastMessage = null
        )
        queryProductDetails()
        queryActivePurchases()
    }

    private fun queryProductDetails() {
        val client = billingClient ?: return
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID_ANNUAL)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        client.queryProductDetailsAsync(params) { billingResult, queryResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val detailsList = queryResult.productDetailsList
                annualDetails = detailsList.firstOrNull()
                val detail = annualDetails
                val offer = detail?.let { selectAnnualOffer(it) }
                val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    productTitle = detail?.name ?: "Suscripción anual",
                    productPrice = phase?.formattedPrice,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastMessage = "No se pudo cargar el precio de la suscripción"
                )
            }
        }
    }

    private fun queryActivePurchases() {
        val client = billingClient ?: return
        if (!client.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            } else {
                _uiState.value = _uiState.value.copy(
                    isActive = false,
                    isEntitlementVerified = false,
                    isLoading = false
                )
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val ownedPurchases = purchases.filter { purchase ->
            purchase.products.contains(PRODUCT_ID_ANNUAL) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        if (ownedPurchases.isEmpty()) {
            clearBillingFallbackLease()
            _uiState.value = _uiState.value.copy(
                isActive = false,
                isEntitlementVerified = true,
                isLoading = false
            )
            return
        }

        val now = System.currentTimeMillis()
        val leaseUntil = now + BILLING_OFFLINE_LEASE_MS
        saveBillingFallbackLease(leaseUntil)
        _uiState.value = _uiState.value.copy(
            isActive = true,
            isEntitlementVerified = true,
            isLoading = false
        )

        purchases
            .filter {
                it.products.contains(PRODUCT_ID_ANNUAL) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    !it.isAcknowledged
            }
            .forEach { purchase ->
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(params) { }
            }
    }

    private fun selectAnnualOffer(details: ProductDetails): ProductDetails.SubscriptionOfferDetails? {
        return details.subscriptionOfferDetails
            ?.firstOrNull { offer ->
                offer.basePlanId == BASE_PLAN_ID_ANNUAL
            }
            ?: details.subscriptionOfferDetails?.firstOrNull { offer ->
                offer.pricingPhases.pricingPhaseList.any { phase ->
                    phase.billingPeriod.equals("P1Y", ignoreCase = true)
                }
            }
            ?: details.subscriptionOfferDetails?.firstOrNull()
    }

    fun hasActiveSubscription(context: Context): Boolean {
        if (!::appContext.isInitialized) {
            initialize(context.applicationContext)
            return false
        }
        return hasActiveSubscriptionNow()
    }

    private fun saveBillingFallbackLease(leaseUntilMs: Long) {
        if (!::appContext.isInitialized) return
        DbPreferences.named(appContext, BILLING_FALLBACK_PREFS).edit()
            .putLong(KEY_BILLING_LEASE_UNTIL_MS, leaseUntilMs)
            .putBoolean(KEY_BILLING_CACHED_ACTIVE, true)
            .apply()
    }

    private fun readBillingFallbackLease(nowMs: Long): Any? {
        if (!::appContext.isInitialized) return null
        val prefs = DbPreferences.named(appContext, BILLING_FALLBACK_PREFS)
        val isActive = prefs.getBoolean(KEY_BILLING_CACHED_ACTIVE, false)
        val leaseUntil = prefs.getLong(KEY_BILLING_LEASE_UNTIL_MS, 0L)
        if (isActive && nowMs <= leaseUntil) return Any()
        return null
    }

    private fun clearBillingFallbackLease() {
        if (!::appContext.isInitialized) return
        DbPreferences.named(appContext, BILLING_FALLBACK_PREFS).edit()
            .remove(KEY_BILLING_LEASE_UNTIL_MS)
            .remove(KEY_BILLING_CACHED_ACTIVE)
            .apply()
    }

    private const val BILLING_FALLBACK_PREFS = "billing_fallback_entitlement"
    private const val KEY_BILLING_LEASE_UNTIL_MS = "billing_lease_until_ms"
    private const val KEY_BILLING_CACHED_ACTIVE = "billing_cached_active"
    private const val BILLING_OFFLINE_LEASE_MS = 7L * 24L * 60L * 60L * 1000L
}
