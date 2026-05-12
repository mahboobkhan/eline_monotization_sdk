package com.lowbyte.studio.lbsadssdk.ads

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.lowbyte.studio.lbsadssdk.R
import com.lowbyte.studio.lbsadssdk.analytics.AnalyticsManager

/**
 * Legacy Native Manager with Preloading support.
 */
class NativeAdManager(private val adUnitId: String) {
    private var preloadedNativeAd: NativeAd? = null
    private var isLoading = false
    private val TAG = "NativeAdManager"

    /**
     * Loads a native ad and shows it immediately.
     */
    fun loadNativeAd(activity: Activity, container: ViewGroup, layoutResId: Int) {
        AnalyticsManager.logEvent("native_load_start")
        // Show Shimmer
        val shimmer = activity.layoutInflater.inflate(R.layout.layout_native_shimmer, container, false)
        container.removeAllViews()
        container.addView(shimmer)

        val adLoader = AdLoader.Builder(activity, adUnitId)
            .forNativeAd { ad : NativeAd ->
                val adView = activity.layoutInflater.inflate(layoutResId, null) as NativeAdView
                populateNativeAdView(ad, adView)
                container.removeAllViews()
                container.addView(adView)
                AnalyticsManager.logEvent("native_loaded")
                AnalyticsManager.logAdImpression(adUnitId, "Native")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    AnalyticsManager.logEvent("native_load_failed", android.os.Bundle().apply {
                        putString("error", error.message)
                    })
                    container.removeAllViews()
                    container.visibility = View.GONE
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * Pre-fetches a native ad in the background.
     */
    fun prefetchNativeAd(activity: Activity) {
        if (isLoading || preloadedNativeAd != null) return
        isLoading = true

        val adLoader = AdLoader.Builder(activity.applicationContext, adUnitId)
            .forNativeAd { ad : NativeAd ->
                Log.d(TAG, "Native ad pre-fetched.")
                preloadedNativeAd = ad
                isLoading = false
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Native pre-fetch failed: ${error.message}")
                    isLoading = false
                    preloadedNativeAd = null
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun isLoaded(): Boolean = preloadedNativeAd != null

    /**
     * Populates the container with the preloaded native ad.
     */
    fun showPreloadedNativeAd(activity: Activity, container: ViewGroup, layoutResId: Int) {
        val nativeAd = preloadedNativeAd
        if (nativeAd != null) {
            val adView = activity.layoutInflater.inflate(layoutResId, null) as NativeAdView
            populateNativeAdView(nativeAd, adView)
            container.removeAllViews()
            container.addView(adView)
            container.visibility = View.VISIBLE
            Log.d(TAG, "Pre-fetched native ad added to container.")
            AnalyticsManager.logAdImpression(adUnitId, "Native_Preloaded")
            
            preloadedNativeAd = null
            prefetchNativeAd(activity)
        } else {
            container.visibility = View.GONE
            prefetchNativeAd(activity)
        }
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the native ad view elements.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        adView.mediaView = adView.findViewById(R.id.ad_media)

        // Set the view element with the native ad assets.
        (adView.headlineView as? TextView)?.text = nativeAd.headline
        nativeAd.mediaContent?.let { adView.mediaView?.setMediaContent(it) }

        (adView.bodyView as? TextView)?.text = nativeAd.body
        (adView.callToActionView as? Button)?.text = nativeAd.callToAction
        (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        (adView.priceView as? TextView)?.text = nativeAd.price
        (adView.storeView as? TextView)?.text = nativeAd.store
        nativeAd.starRating?.let { rating ->
            (adView.starRatingView as? RatingBar)?.rating = rating.toFloat()
        }
        (adView.advertiserView as? TextView)?.text = nativeAd.advertiser

        // Handle Visibility
        adView.headlineView?.visibility = getAssetViewVisibility(nativeAd.headline)
        adView.bodyView?.visibility = getAssetViewVisibility(nativeAd.body)
        adView.callToActionView?.visibility = getAssetViewVisibility(nativeAd.callToAction)
        adView.iconView?.visibility = getAssetViewVisibility(nativeAd.icon)
        adView.priceView?.visibility = getAssetViewVisibility(nativeAd.price)
        adView.starRatingView?.visibility = getAssetViewVisibility(nativeAd.starRating)
        adView.storeView?.visibility = getAssetViewVisibility(nativeAd.store)
        adView.advertiserView?.visibility = getAssetViewVisibility(nativeAd.advertiser)

        // Assign the ad to the view.
        adView.setNativeAd(nativeAd)
    }

    private fun getAssetViewVisibility(asset: Any?): Int {
        return if (asset == null) View.INVISIBLE else View.VISIBLE
    }
    
    fun destroy() {
        preloadedNativeAd?.destroy()
        preloadedNativeAd = null
    }
}
