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
 * Updated with standardized parameters and ad unit ID callbacks.
 */
class NextGenBannerAdManager(
    private var adUnitId: String? = null,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = null
) {
    private val TAG = "NGAdsManagerBannerAd"
    private var bannerAd: BannerAd? = null

    /**
     * Loads and displays a banner ad.
     * 
     * @param adUnitId Override default ad unit ID.
     * @param remoteConfigKey Override default remote config key.
     */
    fun loadAndShowBanner(
        activity: Activity,
        container: ViewGroup,
        customAdUnitId: String? = null,
        customRemoteConfigKey: String? = null,
        width: Int = 320,
        isCollapsible: Boolean = false,
        collapsibleType: String = "bottom",
        maxHeight: Int? = null,
        listener: NextGenAdListener? = null
    ) {
        val finalAdUnitId = customAdUnitId ?: adUnitId ?: "ca-app-pub-3940256099942544/9214589741"
        val finalRemoteKey = customRemoteConfigKey ?: remoteConfigKey

        if (billingManager?.isUserPro() == true) {
            Log.d(TAG, "Banner: User is Pro, ads suppressed.")
            container.visibility = android.view.View.GONE
            return
        }

        if (!NextGenConsentManager.canRequestAds(activity)) {
            val status = if (com.google.android.ump.UserMessagingPlatform.getConsentInformation(activity).consentStatus == com.google.android.ump.ConsentInformation.ConsentStatus.REQUIRED) "REQUIRED" else "UNKNOWN/FAILED"
            Log.w(TAG, "Banner: First Resolve Consent then Try to load Ads. Current Status: $status")
            container.visibility = android.view.View.GONE
            return
        }

        val isEnabled = finalRemoteKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            Log.d(TAG, "Banner ad disabled by Remote Config (key: $finalRemoteKey)")
            container.visibility = android.view.View.GONE
            return
        }

        Log.d(TAG, "Loading and showing Banner ad: $finalAdUnitId")

        val adSize = if (maxHeight != null) {
            AdSize.getInlineAdaptiveBannerAdSize(width, maxHeight)
        } else {
            AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, width)
        }

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
                    Log.d(TAG, "Banner ad loaded: $finalAdUnitId")
                    bannerAd = ad
                    listener?.onAdLoaded(finalAdUnitId)

                    ad.bannerAdRefreshCallback = object : BannerAdRefreshCallback {
                        override fun onAdRefreshed() {
                            Log.d(TAG, "Banner ad refreshed.")
                            listener?.onAdLoaded(finalAdUnitId)
                        }
                        override fun onAdFailedToRefresh(adError: LoadAdError) {
                            Log.e(TAG, "Banner ad failed to refresh: ${adError.message}")
                            listener?.onAdFailedToLoad(finalAdUnitId, adError.toString())
                        }
                    }

                    ad.adEventCallback = object : BannerAdEventCallback {
                        override fun onAdImpression() { 
                            Log.d(TAG, "Banner ad impression.")
                            listener?.onAdImpression(finalAdUnitId) 
                        }
                        override fun onAdClicked() { 
                            Log.d(TAG, "Banner ad clicked.")
                            listener?.onAdClicked(finalAdUnitId) 
                        }
                        override fun onAdShowedFullScreenContent() { 
                            Log.d(TAG, "Banner ad showed full screen.")
                            listener?.onAdShowed(finalAdUnitId) 
                        }
                        override fun onAdDismissedFullScreenContent() { 
                            Log.d(TAG, "Banner ad dismissed full screen.")
                            listener?.onAdDismissed(finalAdUnitId) 
                        }
                        override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                            Log.e(TAG, "Banner ad failed to show: ${error.message}")
                            listener?.onAdFailedToShow(finalAdUnitId, error.toString())
                        }
                    }

                    adView.registerBannerAd(ad, activity)
                    container.visibility = android.view.View.VISIBLE
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Banner ad failed to load: ${adError.message} (Code: ${adError.code})")
                    bannerAd = null
                    listener?.onAdFailedToLoad(finalAdUnitId, adError.toString())
                    container.visibility = android.view.View.GONE
                }
            })
        }
    }

    fun isAdLoaded(): Boolean = bannerAd != null

    fun preloadAdaptiveBanner(
        activity: Activity,
        width: Int,
        orientation: String = "current",
        listener: NextGenAdListener? = null
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
                    listener?.onAdLoaded(finalAdUnitId)
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    listener?.onAdFailedToLoad(finalAdUnitId, adError.toString())
                }
            })
        }
    }

    fun destroy() {
        bannerAd?.destroy()
        bannerAd = null
    }
}
