package com.eline.sdk.admob.ngm.ads.nextgen

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.eline.sdk.admob.ngm.billing.BillingManager
import com.eline.sdk.admob.ngm.remote.RemoteConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main Manager for GMA Next-Gen SDK.
 * Handles initialization and provides access to specialized ad managers.
 */
object NextGenAdsManager {
    private const val TAG = "NGAdsManager"
    private var isInitialized = false
    private lateinit var billingManager: BillingManager

    private var bannerManager: NextGenBannerAdManager? = null
    private var interstitialManager: NextGenInterstitialAdManager? = null
    private var appOpenAdManager: NextGenAppOpenAdManager? = null
    private val nativeManagers = mutableMapOf<String, NextGenNativeAdManager>()
    private val rewardedManagers = mutableMapOf<String, NextGenRewardedAdManager>()
    private val interstitialCounterManagers = mutableMapOf<String, NextGenInterstitialCounterManager>()
    private val interstitialIntervalManagers = mutableMapOf<String, NextGenInterstitialIntervalManager>()

    /**
     * Initializes the GMA Next-Gen SDK on a background thread.
     * 
     * @param context Application context.
     * @param appId The AdMob App ID.
     * @param billing The billing manager for pro checks.
     * @param onComplete Optional callback when initialization is complete.
     */
    fun initialize(
        context: Context,
        appId: String? = null,
        billing: BillingManager,
        onComplete: (() -> Unit)? = null
    ) {
        if (isInitialized) return
        billingManager = billing

        val finalAppId = appId ?: "ca-app-pub-3940256099942544~3347511713" // Sample App ID

        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            Log.d(TAG, "Initializing GMA Next-Gen SDK...")
            
            val config = InitializationConfig.Builder(finalAppId).build()
            
            MobileAds.initialize(context, config) {
                Log.d(TAG, "Adapter initialization complete.")
                isInitialized = true
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete?.invoke()
                }
            }
            
            Log.d(TAG, "MobileAds.initialize called.")
        }
    }

    fun getBillingManager(): BillingManager? = if (::billingManager.isInitialized) billingManager else null

    /**
     * Creates a new Banner manager with optional configuration.
     */
    fun getBannerManager(
        adUnitId: String? = null,
        remoteConfigKey: String? = "banner_enabled"
    ): NextGenBannerAdManager {
        if (bannerManager == null) {
            bannerManager = NextGenBannerAdManager(
                adUnitId = adUnitId,
                billingManager = if (::billingManager.isInitialized) billingManager else null,
                remoteConfigKey = remoteConfigKey
            )
        }
        return bannerManager!!
    }

    /**
     * Creates a new Interstitial manager with optional configuration.
     */
    fun getInterstitialManager(): NextGenInterstitialAdManager {
        if (interstitialManager == null) {
            interstitialManager = NextGenInterstitialAdManager(
                billingManager = if (::billingManager.isInitialized) billingManager else null
            )
        }
        return interstitialManager!!
    }

    /**
     * Creates a new Counter-based Interstitial manager.
     */
    fun getInterstitialCounterManager(
        adUnitId: String,
        remoteConfigKey: String? = "interstitial_counter_enabled",
        thresholdKey: String? = "interstitial_threshold"
    ): NextGenInterstitialCounterManager {
        return interstitialCounterManagers.getOrPut(adUnitId) {
            NextGenInterstitialCounterManager(
                adUnitId = adUnitId,
                billingManager = if (::billingManager.isInitialized) billingManager else null,
                remoteConfigKey = remoteConfigKey,
                thresholdKey = thresholdKey
            )
        }
    }

    /**
     * Creates a new Interval-based Interstitial manager.
     */
    fun getInterstitialIntervalManager(
        intervalKey: String = "interstitial_interval"
    ): NextGenInterstitialIntervalManager {
        return interstitialIntervalManagers.getOrPut(intervalKey) {
            NextGenInterstitialIntervalManager(
                billingManager = if (::billingManager.isInitialized) billingManager else null,
                intervalKey = intervalKey
            )
        }
    }

//    /**
//     * Starts preloading Interstitial ads globally.
//     */
//    fun startPreloadingInterstitial(adUnitId: String,remoteConfigKey: Boolean = true) {
//        if (remoteConfigKey){
//            val adRequest = AdRequest.Builder(adUnitId).build()
//            val preloadConfig = PreloadConfiguration(adRequest)
//            InterstitialAdPreloader.start(adUnitId, preloadConfig)
//            Log.d(TAG, "Global preloading started for: remote $adUnitId")
//        }else{
//            Log.d(TAG, "Global preloading not requested remote False")
//
//        }
//    }

    /**
     * Preloads a Banner ad using the global singleton access.
     */
    fun preloadBanner(activity: android.app.Activity, adUnitId: String, width: Int = 320) {
        getBannerManager(adUnitId).preloadAdaptiveBanner(activity, width)
    }

    /**
     * Preloads a Native ad using the global singleton access.
     */
    fun preloadNative(adUnitId: String) {
        getNativeAdManager(adUnitId).preloadAd()
    }

    /**
     * Creates a new Rewarded manager with optional configuration.
     */
    fun getRewardedManager(
        adUnitId: String? = null,
        remoteConfigKey: String? = "rewarded_enabled"
    ): NextGenRewardedAdManager {
        val key = adUnitId ?: "default"
        return rewardedManagers.getOrPut(key) {
            NextGenRewardedAdManager(
                adUnitId = adUnitId,
                billingManager = if (::billingManager.isInitialized) billingManager else null,
                remoteConfigKey = remoteConfigKey
            )
        }
    }

    /**
     * Creates a new Native manager with optional configuration.
     */
    fun getNativeAdManager(
        adUnitId: String? = null,
    ): NextGenNativeAdManager {
        val key = adUnitId ?: "default"
        return nativeManagers.getOrPut(key) {
            NextGenNativeAdManager(
                billingManager = if (::billingManager.isInitialized) billingManager else null
            )
        }
    }

    /**
     * Creates a new App Open manager with optional configuration.
     */
    fun getAppOpenAdManager(
        application: Application,
        adUnitId: String,
        remoteConfigKey: String? = "openAppAdIsEnable"
    ): NextGenAppOpenAdManager {
        if (appOpenAdManager == null) {
            appOpenAdManager = NextGenAppOpenAdManager(
                application = application,
                adUnitId = adUnitId,
                billingManager = if (::billingManager.isInitialized) billingManager else null,
                remoteConfigKey = remoteConfigKey
            )
        }
        return appOpenAdManager!!
    }

    /**
     * Checks if ads are enabled globally.
     */
    fun isAdsEnabled(): Boolean {
        return RemoteConfigManager.getBoolean("ads_enabled") && 
               !(if (::billingManager.isInitialized) billingManager.isUserPro() else false)
    }
}
