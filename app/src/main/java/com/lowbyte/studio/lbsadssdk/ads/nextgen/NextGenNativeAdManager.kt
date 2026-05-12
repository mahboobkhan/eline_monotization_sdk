package com.lowbyte.studio.lbsadssdk.ads.nextgen

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.lowbyte.studio.lbsadssdk.R
import com.lowbyte.studio.lbsadssdk.billing.BillingManager
import com.lowbyte.studio.lbsadssdk.remote.RemoteConfigManager

/**
 * Manager for Native Ads using the GMA Next-Gen SDK.
 * Updated to follow the latest Next-Gen Native implementation patterns.
 */
class NextGenNativeAdManager(
    private var adUnitId: String? = null,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = null
) {
    private val TAG = "NextGenNativeAd"
    private var currentNativeAd: NativeAd? = null

    enum class NativeSize {
        SMALL, MEDIUM, LARGE
    }

    /**
     * Loads and displays a native ad in the provided container.
     */
    fun loadAndShowNativeAd(
        activity: Activity,
        container: ViewGroup,
        size: NativeSize = NativeSize.MEDIUM,
        customLayout: Int? = null,
        customShimmer: Int? = null
    ) {
        // 1. Pro check
        if (billingManager?.isUserPro() == true) {
            container.visibility = View.GONE
            return
        }

        // 2. Remote check
        val isEnabled = remoteConfigKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            container.visibility = View.GONE
            return
        }

        // 3. Show Shimmer
        val shimmerRes = customShimmer ?: when (size) {
            NativeSize.SMALL -> R.layout.shimmer_native_small
            NativeSize.MEDIUM -> R.layout.shimmer_native_medium
            NativeSize.LARGE -> R.layout.shimmer_native_large
        }
        
        activity.runOnUiThread {
            container.removeAllViews()
            LayoutInflater.from(activity).inflate(shimmerRes, container, true)
            container.visibility = View.VISIBLE
        }

        // 4. Load Ad via NativeAdLoader
        val finalAdUnitId = adUnitId ?: "ca-app-pub-3940256099942544/2247696110"
        val adRequest = NativeAdRequest
            .Builder(finalAdUnitId, listOf(NativeAd.NativeAdType.NATIVE))
            .build()

        val adCallback = object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                Log.d(TAG, "Native ad loaded.")
                currentNativeAd?.destroy()
                currentNativeAd = nativeAd

                // Set event callbacks
                nativeAd.adEventCallback = object : NativeAdEventCallback {
                    override fun onAdClicked() {
                        Log.d(TAG, "Native ad recorded a click.")
                    }
                }

                activity.runOnUiThread {
                    val layoutRes = customLayout ?: when (size) {
                        NativeSize.SMALL -> R.layout.layout_native_small
                        NativeSize.MEDIUM -> R.layout.layout_native_medium
                        NativeSize.LARGE -> R.layout.layout_native_large
                    }

                    val adView = LayoutInflater.from(activity).inflate(layoutRes, null) as NativeAdView
                    displayNativeAd(nativeAd, adView)
                    
                    container.removeAllViews()
                    container.addView(adView)
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Native ad failed to load: $adError")
                activity.runOnUiThread {
                    container.visibility = View.GONE
                }
            }

            override fun onAdLoadingCompleted() {
                Log.d(TAG, "Native ad loading completed.")
            }
        }

        NativeAdLoader.load(adRequest, adCallback)
    }

    /**
     * Populates the NativeAdView with assets from the loaded NativeAd.
     */
    private fun displayNativeAd(nativeAd: NativeAd, adView: NativeAdView) {
        // Find views
        val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
        val bodyView = adView.findViewById<TextView>(R.id.ad_body)
        val ctaView = adView.findViewById<Button>(R.id.ad_call_to_action)
        val mediaView = adView.findViewById<com.google.android.libraries.ads.mobile.sdk.nativead.MediaView>(R.id.ad_media)

        // Assign core views to NativeAdView for tracking
        adView.headlineView = headlineView
        adView.bodyView = bodyView
        adView.callToActionView = ctaView

        // Populate assets
        headlineView?.text = nativeAd.headline
        bodyView?.text = nativeAd.body
        ctaView?.text = nativeAd.callToAction

        // Set visibility based on asset presence
        headlineView?.visibility = getAssetViewVisibility(nativeAd.headline)
        bodyView?.visibility = getAssetViewVisibility(nativeAd.body)
        ctaView?.visibility = getAssetViewVisibility(nativeAd.callToAction)

        // Configure MediaView
        mediaView?.imageScaleType = ImageView.ScaleType.CENTER_CROP

        // IMPORTANT: Inform GMA Next-Gen SDK that you have finished populating the views
        adView.registerNativeAd(nativeAd, mediaView)
    }

    /**
     * Determines the visibility of an asset view based on the presence of its asset.
     */
    private fun getAssetViewVisibility(asset: Any?): Int {
        return if (asset == null) View.INVISIBLE else View.VISIBLE
    }

    /**
     * Preloads a native ad.
     */
    fun preloadAd() {
        val finalAdUnitId = adUnitId ?: "ca-app-pub-3940256099942544/2247696110"
        val adRequest = NativeAdRequest
            .Builder(finalAdUnitId, listOf(NativeAd.NativeAdType.NATIVE))
            .build()

        val adCallback = object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                Log.d(TAG, "Native ad preloaded.")
                currentNativeAd?.destroy()
                currentNativeAd = nativeAd
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Native ad preloading failed: $adError")
            }
        }

        NativeAdLoader.load(adRequest, adCallback)
    }

    /**
     * Checks if a native ad is preloaded.
     */
    fun isAdLoaded(): Boolean {
        return currentNativeAd != null
    }

    /**
     * Shows a preloaded native ad in the container.
     */
    fun showPreloadedNativeAd(
        activity: Activity,
        container: ViewGroup,
        size: NativeSize = NativeSize.MEDIUM,
        customLayout: Int? = null
    ) {
        val ad = currentNativeAd
        if (ad == null) {
            Log.e(TAG, "No preloaded native ad found.")
            container.visibility = View.GONE
            return
        }

        activity.runOnUiThread {
            val layoutRes = customLayout ?: when (size) {
                NativeSize.SMALL -> R.layout.layout_native_small
                NativeSize.MEDIUM -> R.layout.layout_native_medium
                NativeSize.LARGE -> R.layout.layout_native_large
            }

            val adView = LayoutInflater.from(activity).inflate(layoutRes, null) as NativeAdView
            displayNativeAd(ad, adView)
            
            container.removeAllViews()
            container.addView(adView)
            container.visibility = View.VISIBLE
            Log.d(TAG, "Preloaded Native ad added to container.")
        }
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        currentNativeAd?.destroy()
        currentNativeAd = null
    }
}
