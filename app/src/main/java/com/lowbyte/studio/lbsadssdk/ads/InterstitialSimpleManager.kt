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
 * Type 1: Simple Load & Show (No Cache)
 * Load ad and show if available. Every load starts fresh.
 */
class InterstitialSimpleManager(private val adUnitId: String) {
    private val TAG = "AdsLogInterSimple"
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun loadAd(activity: Activity, overrideAdUnitId: String? = null, onComplete: ((Boolean) -> Unit)? = null) {
        if (isLoading) {
            Log.d(TAG, "Ad is already loading, skipping.")
            return
        }
        isLoading = true
        interstitialAd = null // Force fresh load
        
        val unitId = overrideAdUnitId ?: adUnitId
        Log.i(TAG, "Starting fresh ad load for ID: $unitId")
        AnalyticsManager.logEvent("simple_interstitial_load_start")
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity, unitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                Log.i(TAG, "Ad loaded successfully.${ad.adUnitId}")
                interstitialAd = ad
                isLoading = false
                AnalyticsManager.logEvent("simple_interstitial_loaded")
                onComplete?.invoke(true)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "Ad failed to load: ${error.message}")
                interstitialAd = null
                isLoading = false
                AnalyticsManager.logEvent("simple_interstitial_load_failed", Bundle().apply {
                    putString("error", error.message)
                })
                onComplete?.invoke(false)
            }
        })
    }

    /**
     * @param showDialog If true, show a loading dialog before ad.
     * @param delayMs Delay before showing ad to allow dialog to appear. Default 500ms.
     */
    fun showAd(
        activity: Activity,
        showDialog: Boolean = true,
        delayMs: Long = 500,
        overrideAdUnitId: String? = null,
        onDismiss: () -> Unit
    ) {
        if (interstitialAd != null) {
            Log.d(TAG, "Showing ad...")
            AnalyticsManager.logEvent("simple_interstitial_show_start")
            
            AdShowHelper.showAdWithOptionalDialog(activity, showDialog, delayMs) {
                interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.i(TAG, "Ad dismissed by user.")
                        AnalyticsManager.logEvent("simple_interstitial_dismissed")
                        interstitialAd = null
                        onDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.e(TAG, "Ad failed to show: ${error.message}")
                        AnalyticsManager.logEvent("simple_interstitial_show_failed", Bundle().apply {
                            putString("error", error.message)
                        })
                        interstitialAd = null
                        onDismiss()
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "Ad clicked.")
                        AnalyticsManager.logEvent("simple_interstitial_clicked")
                    }
                }
                interstitialAd?.show(activity)
            }
        } else {
            Log.w(TAG, "Attempted to show ad, but none was loaded. Trying to load...")
            loadAd(activity, overrideAdUnitId) { success ->
                onDismiss()
            }
        }
    }
}
