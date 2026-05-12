package com.lowbyte.studio.lbsadssdk.ads

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.lowbyte.studio.lbsadssdk.R
import com.lowbyte.studio.lbsadssdk.analytics.AnalyticsManager

/**
 * Legacy Banner Manager with Preloading support.
 */
class BannerAdManager(private val adUnitId: String) {
    private var adView: AdView? = null
    private var isAdLoaded = false
    private val TAG = "BannerAdManager"

    /**
     * Loads a banner ad normally.
     */
    fun loadBanner(activity: Activity, container: ViewGroup, adSize: AdSize = AdSize.BANNER) {
        AnalyticsManager.logEvent("banner_load_start")
        // Show Shimmer
        val shimmer = activity.layoutInflater.inflate(R.layout.layout_banner_shimmer, container, false)
        container.removeAllViews()
        container.addView(shimmer)

        val localAdView = AdView(activity)
        localAdView.adUnitId = adUnitId
        localAdView.setAdSize(adSize)

        localAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                container.removeAllViews()
                container.addView(localAdView)
                AnalyticsManager.logEvent("banner_loaded")
                AnalyticsManager.logAdImpression(adUnitId, "Banner")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                AnalyticsManager.logEvent("banner_load_failed", android.os.Bundle().apply {
                    putString("error", error.message)
                })
                container.removeAllViews()
                container.visibility = View.GONE
            }

            override fun onAdOpened() {
                AnalyticsManager.logEvent("banner_clicked")
            }
        }

        val adRequest = AdRequest.Builder().build()
        localAdView.loadAd(adRequest)
    }

    /**
     * Prefetches a banner ad in the background.
     */
    fun prefetchBanner(activity: Activity, adSize: AdSize = AdSize.BANNER) {
        if (isAdLoaded) return

        adView = AdView(activity.applicationContext)
        adView?.adUnitId = adUnitId
        adView?.setAdSize(adSize)

        adView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Banner pre-fetched successfully.")
                isAdLoaded = true
                AnalyticsManager.logEvent("banner_prefetched")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "Banner pre-fetch failed: ${error.message}")
                isAdLoaded = false
                adView = null
            }
        }

        adView?.loadAd(AdRequest.Builder().build())
    }

    fun isLoaded(): Boolean = isAdLoaded

    /**
     * Adds the pre-fetched banner to the container.
     */
    fun addBannerToContainer(container: ViewGroup) {
        val currentAdView = adView
        if (isAdLoaded && currentAdView != null) {
            // Remove from previous parent if any
            (currentAdView.parent as? ViewGroup)?.removeView(currentAdView)
            container.removeAllViews()
            container.addView(currentAdView)
            container.visibility = View.VISIBLE
            Log.d(TAG, "Pre-fetched banner added to container.")
            AnalyticsManager.logAdImpression(adUnitId, "Banner_Preloaded")
        } else {
            container.visibility = View.GONE
        }
    }
}
