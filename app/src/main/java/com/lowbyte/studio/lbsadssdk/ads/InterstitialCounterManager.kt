package com.lowbyte.studio.lbsadssdk.ads

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.lowbyte.studio.lbsadssdk.analytics.AnalyticsManager
import com.lowbyte.studio.lbsadssdk.utils.AdLoadingDialog

/**
 * Type 2: Counter-based Ad
 * Show ad only after a certain number of calls (threshold).
 */
class InterstitialCounterManager(private val adUnitId: String) {
    private val TAG = "InterstitialCounter"
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var currentCount = 0

    /**
     * @param reload If true, reload ad if not currently loading. Default false (Pre-Cache).
     */
    fun loadAd(activity: Activity, reload: Boolean = false) {
        if (isLoading) {
            Log.d(TAG, "Ad is already loading, skipping.")
            return
        }
        if (!reload && interstitialAd != null) {
            Log.d(TAG, "Ad already cached, skipping load.")
            AnalyticsManager.logEvent("counter_interstitial_precache_hit")
            return
        }

        Log.i(TAG, "Loading counter-based ad for ID: $adUnitId")
        isLoading = true
        AnalyticsManager.logEvent("counter_interstitial_load_start")
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                Log.i(TAG, "Ad loaded successfully.")
                interstitialAd = ad
                isLoading = false
                AnalyticsManager.logEvent("counter_interstitial_loaded")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "Ad failed to load: ${error.message}")
                interstitialAd = null
                isLoading = false
                AnalyticsManager.logEvent("counter_interstitial_load_failed", Bundle().apply {
                    putString("error", error.message)
                })
            }
        })
    }

    /**
     * @param threshold Number of calls required to show ad.
     * @param remote If true, respect the counter. If false, show immediately if available.
     * @param startCountOnDismiss If true, increment on dismiss. Default true.
     * @param preloadAfterShow If true, start preloading after ad is shown.
     */
    fun showAd(
        activity: Activity,
        threshold: Int,
        remote: Boolean = true,
        startCountOnDismiss: Boolean = true,
        preloadAfterShow: Boolean = true,
        onDismiss: () -> Unit
    ) {
        if (remote) {
            currentCount++
            Log.d(TAG, "Current count: $currentCount, Threshold: $threshold")
            AnalyticsManager.logEvent("counter_interstitial_count", Bundle().apply {
                putInt("current", currentCount)
                putInt("threshold", threshold)
            })

            if (currentCount < threshold) {
                Log.i(TAG, "Threshold not reached. Ad will not show.")
                AnalyticsManager.logEvent("counter_interstitial_threshold_not_reached")
                onDismiss()
                return
            }
        }

        if (interstitialAd != null) {
            Log.d(TAG, "Showing ad...")
            AnalyticsManager.logEvent("counter_interstitial_show_start")
            val dialog = AdLoadingDialog(activity)
            dialog.show()

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.i(TAG, "Ad dismissed. Resetting counter.")
                        AnalyticsManager.logEvent("counter_interstitial_dismissed")
                        interstitialAd = null
                        if (remote) currentCount = 0 // Reset
                        if (preloadAfterShow) {
                            Log.d(TAG, "Preloading next ad.")
                            loadAd(activity)
                        }
                        onDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.e(TAG, "Ad failed to show: ${error.message}")
                        AnalyticsManager.logEvent("counter_interstitial_show_failed")
                        interstitialAd = null
                        if (preloadAfterShow) loadAd(activity)
                        onDismiss()
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "Ad clicked.")
                        AnalyticsManager.logEvent("counter_interstitial_clicked")
                    }
                }
                interstitialAd?.show(activity)
            }, 800)
        } else {
            Log.w(TAG, "Ad not loaded when show requested.")
            AnalyticsManager.logEvent("counter_interstitial_not_loaded")
            loadAd(activity)
            onDismiss()
        }
    }
}
