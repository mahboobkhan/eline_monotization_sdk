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
import com.lowbyte.studio.lbsadssdk.utils.AdShowHelper

/**
 * Type 3: Time Interval-based Ad
 * Show ad only after a certain time interval has passed since last ad.
 */
class InterstitialIntervalManager(private val adUnitId: String) {
    private val TAG = "InterstitialInterval"
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastShowTime: Long = 0

    /**
     * @param reload If true, reload ad if not currently loading. Default false (Pre-Cache).
     */
    fun loadAd(activity: Activity, reload: Boolean = false, overrideAdUnitId: String? = null) {
        if (isLoading) {
            Log.d(TAG, "Ad is already loading, skipping.")
            return
        }
        if (!reload && interstitialAd != null) {
            Log.d(TAG, "Ad already cached, skipping load.")
            AnalyticsManager.logEvent("interval_interstitial_precache_hit")
            return
        }

        val unitId = overrideAdUnitId ?: adUnitId
        Log.i(TAG, "Loading interval-based ad for ID: $unitId")
        isLoading = true
        AnalyticsManager.logEvent("interval_interstitial_load_start")
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity, unitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                Log.i(TAG, "Ad loaded successfully.")
                interstitialAd = ad
                isLoading = false
                AnalyticsManager.logEvent("interval_interstitial_loaded")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "Ad failed to load: ${error.message}")
                interstitialAd = null
                isLoading = false
                AnalyticsManager.logEvent("interval_interstitial_load_failed", Bundle().apply {
                    putString("error", error.message)
                })
            }
        })
    }

    /**
     * @param intervalSeconds Minimum time in seconds between ads.
     * @param remote If true, respect the interval. If false, show immediately if available.
     * @param preloadAfterShow If true, start preloading after ad is shown.
     * @param showDialog If true, show a loading dialog before ad.
     * @param delayMs Delay before showing ad. Default 500ms.
     */
    fun showAd(
        activity: Activity,
        intervalSeconds: Long,
        remote: Boolean = true,
        preloadAfterShow: Boolean = true,
        showDialog: Boolean = true,
        delayMs: Long = 500,
        overrideAdUnitId: String? = null,
        onDismiss: () -> Unit
    ) {
        if (remote) {
            val currentTime = System.currentTimeMillis()
            val timePassed = (currentTime - lastShowTime) / 1000
            
            Log.d(TAG, "Time passed since last ad: $timePassed s, Required: $intervalSeconds s")
            AnalyticsManager.logEvent("interval_interstitial_check", Bundle().apply {
                putLong("time_passed", timePassed)
                putLong("required", intervalSeconds)
            })

            if (timePassed < intervalSeconds) {
                Log.i(TAG, "Interval not reached. Ad will not show.")
                AnalyticsManager.logEvent("interval_interstitial_too_soon")
                onDismiss()
                return
            }
        }

        if (interstitialAd != null) {
            Log.d(TAG, "Showing ad...")
            AnalyticsManager.logEvent("interval_interstitial_show_start")
            
            AdShowHelper.showAdWithOptionalDialog(activity, showDialog, delayMs) {
                interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.i(TAG, "Ad dismissed. Updating last show time.")
                        AnalyticsManager.logEvent("interval_interstitial_dismissed")
                        interstitialAd = null
                        lastShowTime = System.currentTimeMillis() // Save time
                        if (preloadAfterShow) {
                            Log.d(TAG, "Preloading next ad.")
                            loadAd(activity, overrideAdUnitId = overrideAdUnitId)
                        }
                        onDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.e(TAG, "Ad failed to show: ${error.message}")
                        AnalyticsManager.logEvent("interval_interstitial_show_failed")
                        interstitialAd = null
                        if (preloadAfterShow) loadAd(activity, overrideAdUnitId = overrideAdUnitId)
                        onDismiss()
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "Ad clicked.")
                        AnalyticsManager.logEvent("interval_interstitial_clicked")
                    }
                }
                interstitialAd?.show(activity)
            }
        } else {
            Log.w(TAG, "Ad not loaded when show requested. Loading...")
            AnalyticsManager.logEvent("interval_interstitial_not_loaded")
            loadAd(activity, overrideAdUnitId = overrideAdUnitId)
            onDismiss()
        }
    }
}
