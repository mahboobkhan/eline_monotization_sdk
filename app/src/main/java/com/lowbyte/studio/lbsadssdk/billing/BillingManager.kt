package com.lowbyte.studio.lbsadssdk.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.lowbyte.studio.lbsadssdk.analytics.AnalyticsManager


class BillingManager(context: Context) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext

    private var billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var onPurchaseComplete: ((Boolean) -> Unit)? = null
    private var isPro = false

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                startConnection()
            }
        })
    }

    fun isUserPro(): Boolean = isPro

    fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                isPro = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                // Also check in-app products if needed
                queryInAppPurchases()
            }
        }
    }

    private fun queryInAppPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (!isPro) {
                    isPro = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                }
            }
        }
    }

    fun purchaseProduct(activity: Activity, productId: String, productType: String, onComplete: (Boolean) -> Unit) {
        this.onPurchaseComplete = onComplete
        
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params
        ) { billingResult, result ->
            val productDetailsList = result.productDetailsList
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails: ProductDetails = productDetailsList[0]
                var offerToken = ""

                val offers = productDetails.subscriptionOfferDetails
                if (!offers.isNullOrEmpty()) {
                    offerToken = offers.get(0).offerToken
                }

                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )
                    )
                    .build()

                billingClient.launchBillingFlow(activity, flowParams)
            } else {
                onComplete(false)
            }
        }
    }

    fun consumePurchase(purchase: Purchase, onComplete: (Boolean) -> Unit) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { billingResult, _ ->
            onComplete(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
            onPurchaseComplete?.invoke(true)
        } else {
            onPurchaseComplete?.invoke(false)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        isPro = true
                        AnalyticsManager.logEvent("purchase_success")
                    }
                }
            } else {
                isPro = true
            }
        }
    }

    fun restorePurchases(onComplete: (Boolean) -> Unit) {
        queryPurchases()
        // Wait a bit or use a more robust way to signal completion
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            onComplete(isPro)
        }, 2000)
    }
}
