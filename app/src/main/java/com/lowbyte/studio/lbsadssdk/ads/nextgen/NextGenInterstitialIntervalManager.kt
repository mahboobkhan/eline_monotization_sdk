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
 * Shows ad only after a certain time interval has passed.
 */
class NextGenInterstitialIntervalManager(
    private val adUnitId: String,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = null,
    private val intervalKey: String? = null
) {
    private val TAG = "NGAdsManagerInterInterval"
    private var lastShowTime: Long = 0

    /**
     * Starts preloading ads for this ad unit.
     */
    fun startPreloading() {
        val adRequest = AdRequest.Builder(adUnitId).build()
        val preloadConfig = PreloadConfiguration(adRequest)
        InterstitialAdPreloader.start(adUnitId, preloadConfig)
        Log.d(TAG, "Started preloading for: $adUnitId")
    }

    /**
     * Shows the ad if interval has passed and ad is available.
     * 
     * @param activity The activity context.
     * @param intervalSeconds Minimum time in seconds between ads.
     * @param showDialog Whether to show a loading dialog.
     * @param delayMs Delay for the loading dialog.
     * @param onDismiss Callback when ad is dismissed or skipped.
     */
    fun showAd(
        activity: Activity,
        intervalSeconds: Long = 60,
        showDialog: Boolean = true,
        delayMs: Long = 500,
        onDismiss: () -> Unit
    ) {
        // 1. Pro check
        if (billingManager?.isUserPro() == true) {
            onDismiss()
            return
        }

        // 2. Remote check
        val isEnabled = remoteConfigKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            onDismiss()
            return
        }

        // 3. Interval check
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

        // 4. Poll for ad
        val interstitialAd = InterstitialAdPreloader.pollAd(adUnitId)
        if (interstitialAd != null) {
            Log.d(TAG, "Ad found in preloader. Showing...")
            
            interstitialAd.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed. Updating last show time.")
                    lastShowTime = System.currentTimeMillis()
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError) {
                    Log.e(TAG, "Ad failed to show: $fullScreenContentError")
                    onDismiss()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed.")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Ad clicked.")
                }
                
                override fun onAdImpression() {
                    Log.d(TAG, "Ad impression.")
                }
            }

            if (showDialog) {
                val dialog = AdLoadingDialog(activity)
                dialog.show()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    dialog.dismiss()
                    interstitialAd.show(activity)
                }, delayMs)
            } else {
                interstitialAd.show(activity)
            }
        } else {
            Log.w(TAG, "Ad not available in preloader yet.")
            onDismiss()
        }
    }
}
