package com.lowbyte.studio.lbsadssdk.ads.nextgen

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.lowbyte.studio.lbsadssdk.billing.BillingManager
import com.lowbyte.studio.lbsadssdk.remote.RemoteConfigManager
import com.lowbyte.studio.lbsadssdk.utils.AdLoadingDialog

/**
 * Interval-based Interstitial Ad Manager for Next-Gen SDK.
 * Shows ad only after a certain time interval has passed or if startDelay is false.
 */
class NextGenInterstitialIntervalManager(
    private var adUnitId: String,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = null,
    private val intervalKey: String? = null
) {
    private val TAG = "NGAdsManagerInterInterval"
    private var lastShowTime: Long = 0

    /**
     * Starts preloading ads for this ad unit.
     */
    fun startPreloading(customAdUnitId: String? = null) {
        val finalAdUnitId = customAdUnitId ?: adUnitId
        val adRequest = AdRequest.Builder(finalAdUnitId).build()
        val preloadConfig = PreloadConfiguration(adRequest)
        InterstitialAdPreloader.start(finalAdUnitId, preloadConfig)
        Log.d(TAG, "Started preloading for: $finalAdUnitId")
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
        customAdUnitId: String? = null,
        customRemoteConfigKey: String? = null,
        intervalSeconds: Long = 60,
        startDelay: Boolean = true,
        reloadOnDismiss: Boolean = true,
        isFragment: Boolean = false,
        listener: NextGenAdListener? = null,
        onDismiss: () -> Unit
    ) {
        val finalAdUnitId = customAdUnitId ?: adUnitId
        val finalRemoteKey = customRemoteConfigKey ?: remoteConfigKey

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
                val dialog = AdLoadingDialog(activity)
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
