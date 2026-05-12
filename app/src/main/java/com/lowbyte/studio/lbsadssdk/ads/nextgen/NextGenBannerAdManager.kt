package com.lowbyte.studio.lbsadssdk.ads.nextgen

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdSize
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.lowbyte.studio.lbsadssdk.billing.BillingManager
import com.lowbyte.studio.lbsadssdk.remote.RemoteConfigManager
import java.lang.ref.WeakReference

/**
 * Manager for Banner Ads using the GMA Next-Gen SDK.
 * 
 * @param adUnitId The ad unit ID for the banner.
 * @param billingManager Optional billing manager for Pro user checks.
 * @param remoteConfigKey Optional key for remote config to check if ad is enabled.
 */
class NextGenBannerAdManager(
    private var adUnitId: String? = null,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = null
) {
    private val TAG = "NextGenBannerAd"
    private var bannerAd: BannerAd? = null

    /**
     * Interface for banner ad events.
     */
    interface BannerListener {
        fun onAdLoaded() {}
        fun onAdFailedToLoad(error: String) {}
        fun onAdClicked() {}
        fun onAdImpression() {}
        fun onAdDismissed() {}
        fun onAdShowed() {}
        fun onAdFailedToShow(error: String) {}
        fun onAdRefreshed() {}
        fun onAdFailedToRefresh(error: String) {}
    }

    /**
     * Loads and shows a banner ad in the provided container.
     * 
     * @param activity The activity context.
     * @param container The ViewGroup to add the banner view to.
     * @param width The width for the adaptive banner (default is 320).
     * @param isCollapsible Whether the banner should be collapsible (optional).
     * @param collapsibleType "top" or "bottom" (default is "bottom").
     * @param maxHeight Optional max height for inline adaptive banner.
     * @param listener Optional listener for ad events.
     */
    fun loadAndShowBanner(
        activity: Activity,
        container: ViewGroup,
        width: Int = 320,
        isCollapsible: Boolean = false,
        collapsibleType: String = "bottom",
        maxHeight: Int? = null,
        listener: BannerListener? = null
    ) {
        // 1. Pro check
        if (billingManager?.isUserPro() == true) {
            Log.d(TAG, "User is Pro. Skipping banner ad.")
            container.visibility = android.view.View.GONE
            return
        }

        // 2. Remote Config check
        val isEnabled = remoteConfigKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            Log.d(TAG, "Banner ad is disabled via remote config.")
            container.visibility = android.view.View.GONE
            return
        }

        val finalAdUnitId = adUnitId ?: "ca-app-pub-3940256099942544/9214589741" // Sample ID if null

        // 3. Determine Ad Size
        val adSize = if (maxHeight != null) {
            AdSize.getInlineAdaptiveBannerAdSize(width, maxHeight)
        } else {
            AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, width)
        }

        // 4. Build Request
        val requestBuilder = BannerAdRequest.Builder(finalAdUnitId, adSize)
        
        if (isCollapsible) {
            val extras = Bundle()
            extras.putString("collapsible", collapsibleType)
            requestBuilder.setGoogleExtrasBundle(extras)
        }

        val adRequest = requestBuilder.build()

        // 5. Load Ad
        BannerAd.load(
            adRequest,
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    Log.d(TAG, "Banner ad loaded. Collapsible: ${ad.isCollapsible()}")
                    bannerAd = ad
                    listener?.onAdLoaded()

                    // Set Refresh Callback
                    ad.bannerAdRefreshCallback = object : BannerAdRefreshCallback {
                        override fun onAdRefreshed() {
                            Log.d(TAG, "Banner ad refreshed.")
                            listener?.onAdRefreshed()
                        }

                        override fun onAdFailedToRefresh(loadAdError: LoadAdError) {
                            Log.e(TAG, "Banner ad failed to refresh: $loadAdError")
                            listener?.onAdFailedToRefresh(loadAdError.toString())
                        }
                    }

                    // Set Event Callback
                    ad.adEventCallback = object : BannerAdEventCallback {
                        override fun onAdImpression() {
                            Log.d(TAG, "Banner ad recorded an impression.")
                            listener?.onAdImpression()
                        }

                        override fun onAdClicked() {
                            Log.d(TAG, "Banner ad clicked.")
                            listener?.onAdClicked()
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Banner ad showed full screen content.")
                            listener?.onAdShowed()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Banner ad dismissed full screen content.")
                            listener?.onAdDismissed()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                            Log.e(TAG, "Banner ad failed to show full screen content: $error")
                            listener?.onAdFailedToShow(error.toString())
                        }
                    }

                    // Add to UI
                    activity.runOnUiThread {
                        container.removeAllViews()
                        container.addView(ad.getView(activity))
                        container.visibility = android.view.View.VISIBLE
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Banner ad failed to load: $loadAdError")
                    bannerAd = null
                    listener?.onAdFailedToLoad(loadAdError.toString())
                    activity.runOnUiThread {
                        container.visibility = android.view.View.GONE
                    }
                }
            }
        )
    }

    /**
     * Preloads an adaptive banner for a specific orientation.
     */
    fun preloadAdaptiveBanner(
        activity: Activity,
        width: Int,
        orientation: String = "current", // "current", "portrait", "landscape"
        listener: BannerListener? = null
    ) {
        val adSize = when (orientation) {
            "portrait" -> AdSize.getPortraitInlineAdaptiveBannerAdSize(activity, width)
            "landscape" -> AdSize.getLandscapeInlineAdaptiveBannerAdSize(activity, width)
            else -> AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, width)
        }

        val finalAdUnitId = adUnitId ?: "ca-app-pub-3940256099942544/9214589741"
        val adRequest = BannerAdRequest.Builder(finalAdUnitId, adSize).build()

        BannerAd.load(adRequest, object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                bannerAd = ad
                listener?.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                listener?.onAdFailedToLoad(error.toString())
            }
        })
    }

    /**
     * Destroys the current banner ad.
     */
    fun destroy(container: ViewGroup? = null) {
        container?.removeAllViews()
        bannerAd?.destroy()
        bannerAd = null
        Log.d(TAG, "Banner ad destroyed.")
    }
}
