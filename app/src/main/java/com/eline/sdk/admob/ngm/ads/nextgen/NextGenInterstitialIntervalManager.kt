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
 * Interval-based Interstitial Ad Manager for Next-Gen SDK.
 * Shows ad only after a certain time interval has passed or if startDelay is false.
 */
class NextGenInterstitialIntervalManager(
    private val billingManager: BillingManager? = null,
    private val intervalKey: String? = null
) {
    private val TAG = "NGAdsManagerInterInterval"
    private var lastShowTime: Long = 0
    private var isLoading = false

    /**
     * Starts preloading ads for this ad unit with success/failure logging.
     */
    fun startPreloading(finalAdUnitId: String = "", finalRemoteKey: String? = null, listener: NextGenAdListener? = null) {
        val isEnabled = finalRemoteKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            Log.d(TAG, "Interstitial Interval disabled by Remote Config (key: $finalRemoteKey)")

            return
        }

        if (isLoading) return
        isLoading = true
        val adRequest = AdRequest.Builder(finalAdUnitId).build()
        val preloadConfig = PreloadConfiguration(adRequest)
        
        Log.d(TAG, "Initiating preload request for: $finalAdUnitId")
        
        // Use a one-time load to provide immediate feedback/logging
        com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd.load(
            adRequest,
            object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd> {
                override fun onAdLoaded(ad: com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd) {
                    Log.d(TAG, "Preload Success: Ad cached for $finalAdUnitId")
                    isLoading = false
                    listener?.onAdLoaded(finalAdUnitId)
                    // Once verified, let the global preloader take over for future refills
                    InterstitialAdPreloader.start(finalAdUnitId, preloadConfig)
                }

                override fun onAdFailedToLoad(error: com.google.android.libraries.ads.mobile.sdk.common.LoadAdError) {
                    Log.e(TAG, "Preload Failed for $finalAdUnitId: ${error.message} (Code: ${error.code})")
                    isLoading = false
                    listener?.onAdFailedToLoad(finalAdUnitId, error.toString())
                }
            }
        )
    }

    /**
     * Shows the ad if interval has passed or if startDelay is false.
     * 
     * @param activity The activity context.
     * @param intervalSeconds Minimum time in seconds between ads.
     * @param startDelay If false, shows ad immediately without checking interval or showing dialog.
     * @param reloadOnDismiss If true, starts preloading next ad after dismiss.
     * @param isFragment If true, calls onDismiss callback on onAdImpression instead of onAdDismissed.
     * @param listener Optional listener for ad events.
     * @param onDismiss Callback when ad process is complete.
     */
    fun showAd(
        activity: Activity,
        finalAdUnitId: String = "",
        finalRemoteKey: String? = null,
        intervalSeconds: Long = 60,
        startDelay: Boolean = true,
        reloadOnDismiss: Boolean = true,
        isFragment: Boolean = false,
        dialogStyle: AdLoadingDialog.Style = AdLoadingDialog.Style.SMALL,
        listener: NextGenAdListener? = null,
        onDismiss: () -> Unit
    ) {
      //  val finalAdUnitId = customAdUnitId ?: adUnitId

        // 1. Pro check
        if (billingManager?.isUserPro() == true) {
            Log.d(TAG, "Interstitial Interval: User is Pro, ads suppressed.")
            onDismiss()
            return
        }

        // 2. Remote check
        val isEnabled = finalRemoteKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            Log.d(TAG, "Interstitial Interval disabled by Remote Config (key: $finalRemoteKey)")
            onDismiss()
            return
        }

        // 3. Interval check
        if (startDelay) {
            val finalInterval = if (intervalKey != null) {
                val remoteVal = RemoteConfigManager.getLong(intervalKey)
                if (remoteVal > 0) remoteVal else intervalSeconds
            } else {
                intervalSeconds
            }

            val currentTime = System.currentTimeMillis()
            val timePassed = (currentTime - lastShowTime) / 1000
            Log.d(TAG, "Time passed: $timePassed s, Required: $finalInterval s")

            if (timePassed < finalInterval) {
                Log.d(TAG, "Interval not reached.")
                onDismiss()
                return
            }
        } else {
            Log.d(TAG, "startDelay is false, ignoring interval check.")
        }

        // 4. Poll for ad
        val interstitialAd = InterstitialAdPreloader.pollAd(finalAdUnitId)
        if (interstitialAd != null) {
            Log.d(TAG, "Ad found in preloader. Showing...")
            listener?.onAdLoaded(finalAdUnitId)
            
            interstitialAd.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed. Updating last show time.")
                    lastShowTime = System.currentTimeMillis()
                    listener?.onAdDismissed(finalAdUnitId)
                    if (reloadOnDismiss) startPreloading(finalAdUnitId)
                    if (!isFragment) activity.runOnUiThread { onDismiss() }
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError) {
                    Log.e(TAG, "Ad failed to show: ${error.message}")
                    listener?.onAdFailedToShow(finalAdUnitId, error.toString())
                    activity.runOnUiThread { onDismiss() }
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
                        activity.runOnUiThread { onDismiss() }
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
