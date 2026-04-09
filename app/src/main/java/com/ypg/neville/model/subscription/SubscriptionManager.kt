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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SubscriptionUiState(
    val isActive: Boolean = false,
    val isBillingReady: Boolean = false,
    val isLoading: Boolean = true,
    val productTitle: String = "Suscripción anual",
    val productPrice: String? = null,
    val lastMessage: String? = null
)

object SubscriptionManager : PurchasesUpdatedListener {

    private const val PREFS_NAME = "subscription_status"
    private const val KEY_IS_ACTIVE = "subscription_is_active"

    const val PRODUCT_ID_ANNUAL = "premium_anual"
    private const val BASE_PLAN_ID_ANNUAL = "anual"

    private lateinit var appContext: Context
    private var billingClient: BillingClient? = null
    private var annualDetails: ProductDetails? = null

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val cachedActive = hasActiveSubscription(appContext)
        _uiState.value = _uiState.value.copy(isActive = cachedActive, isLoading = true, lastMessage = null)

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

    fun hasActiveSubscriptionNow(): Boolean = _uiState.value.isActive

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
                    _uiState.value = _uiState.value.copy(
                        isBillingReady = false,
                        isLoading = false,
                        lastMessage = "No se pudo conectar con Google Play"
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _uiState.value = _uiState.value.copy(isBillingReady = false)
            }
        })
    }

    private fun onBillingReady() {
        _uiState.value = _uiState.value.copy(isBillingReady = true, isLoading = true, lastMessage = null)
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
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val owned = purchases.any { purchase ->
            purchase.products.contains(PRODUCT_ID_ANNUAL) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        saveActiveFlag(owned)
        _uiState.value = _uiState.value.copy(isActive = owned, isLoading = false)

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

    private fun saveActiveFlag(value: Boolean) {
        if (!::appContext.isInitialized) return
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_ACTIVE, value)
            .apply()
    }

    fun hasActiveSubscription(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_ACTIVE, false)
    }
}
