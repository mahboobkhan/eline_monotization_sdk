package com.eline.sdk.admob.ngm.ads.nextgen

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.eline.sdk.admob.ngm.billing.BillingManager
import com.eline.sdk.admob.ngm.remote.RemoteConfigManager
import com.eline.sdk.admob.ngm.utils.AdLoadingDialog

/**
 * Counter-based Interstitial Ad Manager for Next-Gen SDK.
 * Supports threshold-based display and immediate display options.
 */
class NextGenInterstitialCounterManager(
    private var adUnitId: String,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = null,
    private val thresholdKey: String? = null
) {
    private val TAG = "NGAdsManagerInterCounter"
    private var currentCount = 0

    /**
     * Starts preloading ads for this ad unit with success/failure logging.
     */
    fun startPreloading(customAdUnitId: String? = null, listener: NextGenAdListener? = null) {
        if (!NextGenConsentManager.canRequestAds()) {
            Log.w(TAG, "Preload: First Resolve Consent then Try to load Ads.")
            return
        }
        val finalAdUnitId = customAdUnitId ?: adUnitId
        val adRequest = AdRequest.Builder(finalAdUnitId).build()
        val preloadConfig = PreloadConfiguration(adRequest)
        
        Log.d(TAG, "Initiating preload request for: $finalAdUnitId")
        
        // Use a one-time load to provide immediate feedback/logging
        com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd.load(
            adRequest,
            object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd> {
                override fun onAdLoaded(ad: com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd) {
                    Log.d(TAG, "Preload Success: Ad cached for $finalAdUnitId")
                    listener?.onAdLoaded(finalAdUnitId)
                    // Once verified, let the global preloader take over for future refills
                    InterstitialAdPreloader.start(finalAdUnitId, preloadConfig)
                }

                override fun onAdFailedToLoad(error: com.google.android.libraries.ads.mobile.sdk.common.LoadAdError) {
                    Log.e(TAG, "Preload Failed for $finalAdUnitId: ${error.message} (Code: ${error.code})")
                    listener?.onAdFailedToLoad(finalAdUnitId, error.toString())
                }
            }
        )
    }

    /**
     * Shows the ad if threshold is reached or if startCounting is false.
     * 
     * @param activity The activity context.
     * @param threshold Number of times showAd must be called before showing.
     * @param startCounting If false, shows ad immediately without checking counter.
     * @param startDelay If false, shows ad immediately without dialog delay.
     * @param reloadOnDismiss If true, starts preloading next ad after dismiss.
     * @param isFragment If true, calls onDismiss callback on onAdImpression instead of onAdDismissed.
     * @param listener Optional listener for ad events.
     * @param onDismiss Callback when ad process is complete.
     */
    fun showAd(
        activity: Activity,
        customAdUnitId: String? = null,
        customRemoteConfigKey: String? = null,
        threshold: Int = 2,
        startCounting: Boolean = true,
        startDelay: Boolean = true,
        reloadOnDismiss: Boolean = true,
        isFragment: Boolean = false,
        dialogStyle: AdLoadingDialog.Style = AdLoadingDialog.Style.SMALL,
        listener: NextGenAdListener? = null,
        onDismiss: () -> Unit
    ) {
        val finalAdUnitId = customAdUnitId ?: adUnitId
        val finalRemoteKey = customRemoteConfigKey ?: remoteConfigKey

        // 1. Pro check
        if (billingManager?.isUserPro() == true) {
            Log.d(TAG, "Interstitial Counter: User is Pro, ads suppressed.")
            onDismiss()
            return
        }

        // 2. Remote check
        val isEnabled = finalRemoteKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            Log.d(TAG, "Interstitial Counter disabled by Remote Config (key: $finalRemoteKey)")
            onDismiss()
            return
        }

        // 3. Handle Counting
        if (startCounting) {
            currentCount++
            val finalThreshold = if (thresholdKey != null) {
                val remoteVal = RemoteConfigManager.getLong(thresholdKey).toInt()
                if (remoteVal > 0) remoteVal else threshold
            } else {
                threshold
            }

            Log.d(TAG, "Current count: $currentCount, Threshold: $finalThreshold")

            if (currentCount < finalThreshold) {
                Log.d(TAG, "Threshold not reached.")
                onDismiss()
                return
            }
        } else {
            Log.d(TAG, "startCounting is false, showing ad immediately.")
        }

        // 4. Poll for ad
        val interstitialAd = InterstitialAdPreloader.pollAd(finalAdUnitId)
        if (interstitialAd != null) {
            Log.d(TAG, "Ad found in preloader. Showing...")
            listener?.onAdLoaded(finalAdUnitId)
            
            interstitialAd.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed. Resetting counter.")
                    currentCount = 0
                    listener?.onAdDismissed(finalAdUnitId)
                    if (reloadOnDismiss) startPreloading(finalAdUnitId)
                    if (!isFragment) onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError) {
                    Log.e(TAG, "Ad failed to show: ${error.message}")
                    listener?.onAdFailedToShow(finalAdUnitId, error.toString())
                    onDismiss()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed.")
                    listener?.onAdShowed(finalAdUnitId)
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Ad clicked.")
                    listener?.onAdClicked(finalAdUnitId)
                }
                
                override fun onAdImpression() {
                    Log.d(TAG, "Ad impression.")
                    listener?.onAdImpression(finalAdUnitId)
                    if (isFragment) {
                        Log.d(TAG, "isFragment is true, dismissing via onAdImpression.")
                        onDismiss()
                    }
                }
            }

            if (startDelay) {
                val dialog = AdLoadingDialog(activity, dialogStyle)
                dialog.show()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    dialog.dismiss()
                    interstitialAd.show(activity)
                }, 500)
            } else {
                interstitialAd.show(activity)
            }
        } else {
            Log.w(TAG, "Ad not available in preloader yet.")
            listener?.onAdFailedToLoad(finalAdUnitId, "Ad not available in preloader")
            onDismiss()
        }
    }
}
