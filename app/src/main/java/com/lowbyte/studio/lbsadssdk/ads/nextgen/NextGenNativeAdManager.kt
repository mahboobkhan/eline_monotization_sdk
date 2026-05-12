package com.lowbyte.studio.lbsadssdk.ads.nextgen

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
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
 * Standardized parameters and ad unit ID callbacks.
 */
class NextGenNativeAdManager(
    private var adUnitId: String? = null,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = null
) {
    private val TAG = "NGAdsManagerNativeAd"
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
        customAdUnitId: String? = null,
        customRemoteConfigKey: String? = null,
        size: NativeSize = NativeSize.MEDIUM,
        customLayout: Int? = null,
        customShimmer: Int? = null,
        listener: NextGenAdListener? = null
    ) {
        val finalAdUnitId = customAdUnitId ?: adUnitId ?: "ca-app-pub-3940256099942544/2247696110"
        val finalRemoteKey = customRemoteConfigKey ?: remoteConfigKey

        // 1. Pro check
        if (billingManager?.isUserPro() == true) {
            Log.d(TAG, "Native: User is Pro, ads suppressed.")
            container.visibility = View.GONE
            return
        }

        // 2. Remote check
        val isEnabled = finalRemoteKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            Log.d(TAG, "Native ad disabled by Remote Config (key: $finalRemoteKey)")
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
        val adRequest = NativeAdRequest
            .Builder(finalAdUnitId, listOf(NativeAd.NativeAdType.NATIVE))
            .build()

        val adCallback = object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                Log.d(TAG, "Native ad loaded: $finalAdUnitId")
                currentNativeAd?.destroy()
                currentNativeAd = nativeAd
                listener?.onAdLoaded(finalAdUnitId)

                // Set event callbacks
                nativeAd.adEventCallback = object : NativeAdEventCallback {
                    override fun onAdClicked() {
                        Log.d(TAG, "Native ad recorded a click.")
                        listener?.onAdClicked(finalAdUnitId)
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
                Log.e(TAG, "Native ad failed to load: ${adError.message}")
                listener?.onAdFailedToLoad(finalAdUnitId, adError.toString())
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

    private fun displayNativeAd(nativeAd: NativeAd, adView: NativeAdView) {
        val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
        val bodyView = adView.findViewById<TextView>(R.id.ad_body)
        val ctaView = adView.findViewById<Button>(R.id.ad_call_to_action)
        val mediaView = adView.findViewById<com.google.android.libraries.ads.mobile.sdk.nativead.MediaView>(R.id.ad_media)

        adView.headlineView = headlineView
        adView.bodyView = bodyView
        adView.callToActionView = ctaView

        headlineView?.text = nativeAd.headline
        bodyView?.text = nativeAd.body
        ctaView?.text = nativeAd.callToAction

        headlineView?.visibility = if (nativeAd.headline == null) View.INVISIBLE else View.VISIBLE
        bodyView?.visibility = if (nativeAd.body == null) View.INVISIBLE else View.VISIBLE
        ctaView?.visibility = if (nativeAd.callToAction == null) View.INVISIBLE else View.VISIBLE

        mediaView?.imageScaleType = ImageView.ScaleType.CENTER_CROP
        adView.registerNativeAd(nativeAd, mediaView)
    }

    fun preloadAd(customAdUnitId: String? = null, listener: NextGenAdListener? = null) {
        val finalAdUnitId = customAdUnitId ?: adUnitId ?: "ca-app-pub-3940256099942544/2247696110"
        val adRequest = NativeAdRequest
            .Builder(finalAdUnitId, listOf(NativeAd.NativeAdType.NATIVE))
            .build()

        val adCallback = object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                Log.d(TAG, "Native ad preloaded: $finalAdUnitId")
                currentNativeAd?.destroy()
                currentNativeAd = nativeAd
                listener?.onAdLoaded(finalAdUnitId)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Native ad preloading failed: ${adError.message}")
                listener?.onAdFailedToLoad(finalAdUnitId, adError.toString())
            }
        }

        NativeAdLoader.load(adRequest, adCallback)
    }

    fun destroy() {
        currentNativeAd?.destroy()
        currentNativeAd = null
    }
}
