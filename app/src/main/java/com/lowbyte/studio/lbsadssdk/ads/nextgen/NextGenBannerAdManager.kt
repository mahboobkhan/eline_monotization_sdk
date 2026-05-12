package com.lowbyte.studio.lbsadssdk.ads.nextgen

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.lowbyte.studio.lbsadssdk.billing.BillingManager
import com.lowbyte.studio.lbsadssdk.remote.RemoteConfigManager

/**
 * Manager for Banner Ads using the GMA Next-Gen SDK.
 * Fixed imports and updated BannerAdRequest/AdView patterns for 1.0.1 SDK.
 */
class NextGenBannerAdManager(
    private var adUnitId: String? = null,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = null
) {
    private val TAG = "NextGenBannerAd"
    private var bannerAd: BannerAd? = null

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

    fun loadAndShowBanner(
        activity: Activity,
        container: ViewGroup,
        width: Int = 320,
        isCollapsible: Boolean = false,
        collapsibleType: String = "bottom",
        maxHeight: Int? = null,
        listener: BannerListener? = null
    ) {
        if (billingManager?.isUserPro() == true) {
            container.visibility = android.view.View.GONE
            return
        }

        val isEnabled = remoteConfigKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            container.visibility = android.view.View.GONE
            return
        }

        val finalAdUnitId = adUnitId ?: "ca-app-pub-3940256099942544/9214589741"
        val adSize = if (maxHeight != null) {
            AdSize.getInlineAdaptiveBannerAdSize(width, maxHeight)
        } else {
            AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, width)
        }

        // Corrected BannerAdRequest creation for Next-Gen 1.0.1
        val adRequest = BannerAdRequest.Builder(finalAdUnitId, adSize).apply {
            if (isCollapsible) {
                val extras = Bundle()
                extras.putString("collapsible", collapsibleType)
                setGoogleExtrasBundle(extras)
            }
        }.build()

        activity.runOnUiThread {
            val adView = AdView(activity)
            container.removeAllViews()
            container.addView(adView)

            adView.loadAd(adRequest, object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    bannerAd = ad
                    listener?.onAdLoaded()

                    ad.bannerAdRefreshCallback = object : BannerAdRefreshCallback {
                        override fun onAdRefreshed() {
                            listener?.onAdRefreshed()
                        }
                        override fun onAdFailedToRefresh(adError: LoadAdError) {
                            listener?.onAdFailedToRefresh(adError.toString())
                        }
                    }

                    ad.adEventCallback = object : BannerAdEventCallback {
                        override fun onAdImpression() { listener?.onAdImpression() }
                        override fun onAdClicked() { listener?.onAdClicked() }
                        override fun onAdShowedFullScreenContent() { listener?.onAdShowed() }
                        override fun onAdDismissedFullScreenContent() { listener?.onAdDismissed() }
                        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                            listener?.onAdFailedToShow(fullScreenContentError.toString())
                        }
                    }

                    adView.registerBannerAd(ad, activity)
                    container.visibility = android.view.View.VISIBLE
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    bannerAd = null
                    listener?.onAdFailedToLoad(adError.toString())
                    container.visibility = android.view.View.GONE
                }
            })
        }
    }

    fun preloadAdaptiveBanner(
        activity: Activity,
        width: Int,
        orientation: String = "current",
        listener: BannerListener? = null
    ) {
        val adSize = when (orientation) {
            "portrait" -> AdSize.getPortraitInlineAdaptiveBannerAdSize(activity, width)
            "landscape" -> AdSize.getLandscapeInlineAdaptiveBannerAdSize(activity, width)
            else -> AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, width)
        }

        val finalAdUnitId = adUnitId ?: "ca-app-pub-3940256099942544/9214589741"
        val adRequest = BannerAdRequest.Builder(finalAdUnitId, adSize).build()

        activity.runOnUiThread {
            val adView = AdView(activity)
            adView.loadAd(adRequest, object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    bannerAd = ad
                    listener?.onAdLoaded()
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    listener?.onAdFailedToLoad(adError.toString())
                }
            })
        }
    }

    fun isAdLoaded(): Boolean = bannerAd != null

    fun showPreloadedBanner(activity: Activity, container: ViewGroup, listener: BannerListener? = null) {
        val ad = bannerAd
        if (ad == null) {
            container.visibility = android.view.View.GONE
            return
        }

        ad.adEventCallback = object : BannerAdEventCallback {
            override fun onAdImpression() { listener?.onAdImpression() }
            override fun onAdClicked() { listener?.onAdClicked() }
            override fun onAdShowedFullScreenContent() { listener?.onAdShowed() }
            override fun onAdDismissedFullScreenContent() { listener?.onAdDismissed() }
            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                listener?.onAdFailedToShow(fullScreenContentError.toString())
            }
        }

        activity.runOnUiThread {
            val adView = AdView(activity)
            container.removeAllViews()
            container.addView(adView)
            adView.registerBannerAd(ad, activity)
            container.visibility = android.view.View.VISIBLE
        }
    }

    fun destroy() {
        bannerAd?.destroy()
        bannerAd = null
    }
}
