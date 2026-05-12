package com.lowbyte.studio.lbsadssdk.ads.nextgen

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.lowbyte.studio.lbsadssdk.billing.BillingManager
import com.lowbyte.studio.lbsadssdk.remote.RemoteConfigManager
import com.lowbyte.studio.lbsadssdk.utils.AdLoadingDialog

/**
 * Manager for Interstitial Ads using the GMA Next-Gen SDK.
 */
class NextGenInterstitialAdManager(
    private var adUnitId: String? = null,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = null
) {
    private val TAG = "NGAdsManagerInter"
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    interface InterstitialListener {
        fun onAdLoaded() {}
        fun onAdFailedToLoad(error: String) {}
        fun onAdDismissed() {}
        fun onAdClicked() {}
        fun onAdShowed() {}
        fun onAdFailedToShow(error: String) {}
    }

    /**
     * Loads an interstitial ad.
     */
    fun loadAd(activity: Activity, listener: InterstitialListener? = null) {
        if (billingManager?.isUserPro() == true) {
            Log.d(TAG, "Interstitial: User is Pro, ads suppressed.")
            return
        }
        
        val isEnabled = remoteConfigKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            Log.d(TAG, "Interstitial disabled by Remote Config (key: $remoteConfigKey)")
            return
        }

        if (interstitialAd != null || isLoading) return

        isLoading = true
        val finalAdUnitId = adUnitId ?: "ca-app-pub-3940256099942544/1033173712" // Sample ID
        val adRequest = AdRequest.Builder(finalAdUnitId).build()

        InterstitialAd.load(adRequest, object : AdLoadCallback<InterstitialAd> {
            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "Interstitial ad loaded.")
                interstitialAd = ad
                isLoading = false
                listener?.onAdLoaded()

                ad.adEventCallback = object : InterstitialAdEventCallback {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Interstitial ad dismissed.")
                        interstitialAd = null
                        listener?.onAdDismissed()
                        // Preload next ad
                        loadAd(activity)
                    }

                    override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                        Log.e(TAG, "Interstitial ad failed to show: $fullScreenContentError")
                        interstitialAd = null
                        listener?.onAdFailedToShow(fullScreenContentError.toString())
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Interstitial ad showed.")
                        listener?.onAdShowed()
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "Interstitial ad clicked.")
                        listener?.onAdClicked()
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "Interstitial ad impression recorded.")
                    }
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Interstitial ad failed to load: $adError")
                isLoading = false
                interstitialAd = null
                listener?.onAdFailedToLoad(adError.toString())
            }
        })
    }

    /**
     * Shows the interstitial ad if loaded.
     */
    fun showAd(
        activity: Activity,
        showDialog: Boolean = true,
        delayMs: Long = 500,
        listener: InterstitialListener? = null
    ) {
        if (interstitialAd == null) {
            Log.d(TAG, "Ad not loaded. Loading now...")
            loadAd(activity, listener)
            return
        }

        if (showDialog) {
            val dialog = AdLoadingDialog(activity)
            dialog.show()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                interstitialAd?.show(activity)
            }, delayMs)
        } else {
            interstitialAd?.show(activity)
        }
    }
}
