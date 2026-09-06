package com.wyrmwhelp.idlehoard.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.wyrmwhelp.idlehoard.domain.model.PLATINUM_PURCHASE_OPTIONS
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** The outcome of a completed (or failed) Platinum Pieces pack purchase — see [BillingManager.purchaseEvents]. */
sealed class PlatinumPurchaseResult {
    /** [productId]'s pack was charged, consumed, and should credit [platinumPieces] to the save. */
    data class Granted(val productId: String, val platinumPieces: Long) : PlatinumPurchaseResult()

    /** The purchase didn't go through — [message] is short enough to show directly in the Shop. */
    data class Failed(val message: String) : PlatinumPurchaseResult()
}

/**
 * Thin wrapper around Google Play Billing for the Shop's "Buy Platinum
 * Pieces" packs (`domain/model/PlatinumPurchases.kt`). App-scoped
 * singleton (like `AdManager`/`GameEngine`) — the billing connection and
 * any in-flight purchase should survive a screen rotation or navigating
 * away from the Shop, not tear down with a composable.
 *
 * Every pack is a **consumable**: `handlePurchase` calls `consumeAsync`
 * immediately once a purchase reaches [Purchase.PurchaseState.PURCHASED],
 * which both acknowledges it and clears it so the same product can be
 * bought again later — Play Billing has no separate "restore" concept for
 * a consumable once it's been consumed, unlike a permanent unlock.
 * [queryExistingPurchases] runs once on every successful connection
 * specifically to catch a purchase that completed but never got consumed
 * (e.g. the app was killed mid-flow) — without it, that Platinum would be
 * paid for but never granted, and the product would stay stuck
 * "already owned" until Play Billing itself expires it.
 *
 * Requires the products in [PLATINUM_PURCHASE_OPTIONS] to actually exist
 * as consumable in-app products in the Google Play Console under this
 * app's listing — see CLAUDE.md's Monetization section. Without that,
 * [queryProductDetails] simply returns nothing for every id and every
 * buy button in the Shop stays disabled (see [productDetails]/
 * [formattedPrices]) rather than crashing or silently failing later.
 *
 * **[connect] is deliberately lazy, not called from `init`** — measured on
 * a real device/emulator, letting Play Billing's own connection handshake
 * run at app launch roughly doubled cold-start time (an already-slow
 * ~15s from `AdManager`'s ads SDK init, per CLAUDE.md, became 30-75s+ with
 * Billing initializing eagerly too — see that file's build-environment
 * notes). Since the Shop's IAP rows are the only thing that needs a live
 * connection, and buying Platinum is a rare, deliberate action rather
 * than something that must feel instant like a rewarded ad, `connect()`
 * is called once the Shop section actually opens (`GameViewModel.ensureBillingConnected`)
 * instead of unconditionally at app start the way `AdManager` preloads ads.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : PurchasesUpdatedListener {

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())

    /** Live [ProductDetails] per product id, once Play Billing has resolved them — see [formattedPrices] for the UI-friendly view. */
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails

    private val _formattedPrices = MutableStateFlow<Map<String, String>>(emptyMap())

    /** Play Store's own formatted price string (e.g. "$4.99", localized) per product id — empty until [productDetails] loads. */
    val formattedPrices: StateFlow<Map<String, String>> = _formattedPrices

    private val _purchaseEvents = MutableSharedFlow<PlatinumPurchaseResult>(replay = 0, extraBufferCapacity = 1)

    /** One-shot purchase outcomes — collect this to credit Platinum and show a Shop message; see [PlatinumPurchaseResult]. */
    val purchaseEvents: SharedFlow<PlatinumPurchaseResult> = _purchaseEvents

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var connectionStarted = false

    /**
     * Starts the Play Billing connection if it hasn't already been started —
     * safe to call repeatedly (e.g. every time the Shop section opens).
     * See this class's doc for why this isn't called from `init` instead.
     */
    fun connect() {
        if (connectionStarted) return
        connectionStarted = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    queryExistingPurchases()
                } else {
                    Log.w(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Play Billing reconnects lazily on the next call that needs
                // a live connection; nothing to proactively retry here.
            }
        })
    }

    private fun queryProductDetails() {
        val products = PLATINUM_PURCHASE_OPTIONS.map { option ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(option.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = productDetailsList.associateBy { it.productId }
                _productDetails.value = details
                _formattedPrices.value = details.mapValues { (_, d) -> d.oneTimePurchaseOfferDetails?.formattedPrice.orEmpty() }
            } else {
                Log.w(TAG, "queryProductDetailsAsync failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach(::handlePurchase)
            }
        }
    }

    /** Launches Play's own purchase sheet for [productId]. No-ops if [productDetails] hasn't resolved it yet. */
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val details = _productDetails.value[productId] ?: return
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach(::handlePurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _purchaseEvents.tryEmit(
                PlatinumPurchaseResult.Failed(billingResult.debugMessage.ifBlank { "Purchase failed." }),
            )
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val productId = purchase.products.firstOrNull() ?: return
        val option = PLATINUM_PURCHASE_OPTIONS.firstOrNull { it.productId == productId } ?: return
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.consumeAsync(consumeParams) { billingResult, _ ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _purchaseEvents.tryEmit(PlatinumPurchaseResult.Granted(productId, option.platinumPieces))
            } else {
                Log.w(TAG, "consumeAsync failed for $productId: ${billingResult.debugMessage}")
            }
        }
    }

    private companion object {
        const val TAG = "BillingManager"
    }
}
