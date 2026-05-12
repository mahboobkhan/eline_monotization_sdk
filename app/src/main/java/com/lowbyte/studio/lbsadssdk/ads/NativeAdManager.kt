package com.lowbyte.studio.lbsadssdk.ads

import android.app.Activity
import android.util.Log
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
            .forNativeAd { nativeAd ->
                val adView = activity.layoutInflater.inflate(layoutResId, null) as NativeAdView
                populateNativeAdView(nativeAd, adView)
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
            .forNativeAd { nativeAd ->
                Log.d(TAG, "Native ad pre-fetched.")
                preloadedNativeAd = nativeAd
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
            
            // Note: In legacy SDK, it's often better to load a fresh one after use 
            // or keep it if it's reusable. We'll clear it for now.
            preloadedNativeAd = null
            prefetchNativeAd(activity)
        } else {
            container.visibility = View.GONE
            prefetchNativeAd(activity)
        }
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        adView.mediaView = adView.findViewById(R.id.ad_media)

        (adView.headlineView as? TextView)?.text = nativeAd.headline
        nativeAd.mediaContent?.let { adView.mediaView?.setMediaContent(it) }

        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as? TextView)?.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as? Button)?.text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
            adView.iconView?.visibility = View.VISIBLE
        }

        if (nativeAd.price == null) {
            adView.priceView?.visibility = View.INVISIBLE
        } else {
            adView.priceView?.visibility = View.VISIBLE
            (adView.priceView as? TextView)?.text = nativeAd.price
        }

        if (nativeAd.store == null) {
            adView.storeView?.visibility = View.INVISIBLE
        } else {
            adView.storeView?.visibility = View.VISIBLE
            (adView.storeView as? TextView)?.text = nativeAd.store
        }

        if (nativeAd.starRating == null) {
            adView.starRatingView?.visibility = View.INVISIBLE
        } else {
            (adView.starRatingView as? RatingBar)?.rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView?.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            adView.advertiserView?.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as? TextView)?.text = nativeAd.advertiser
            adView.advertiserView?.visibility = View.VISIBLE
        }

        adView.setNativeAd(nativeAd)
    }
}
