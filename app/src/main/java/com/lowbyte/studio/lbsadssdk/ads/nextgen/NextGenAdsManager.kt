package com.lowbyte.studio.lbsadssdk.ads.nextgen

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.lowbyte.studio.lbsadssdk.billing.BillingManager
import com.lowbyte.studio.lbsadssdk.remote.RemoteConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main Manager for GMA Next-Gen SDK.
 * Handles initialization and provides access to specialized ad managers.
 */
object NextGenAdsManager {
    private const val TAG = "NextGenAdsManager"
    private var isInitialized = false
    private lateinit var billingManager: BillingManager

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
                onComplete?.invoke()
            }
            
            Log.d(TAG, "MobileAds.initialize called.")
        }
    }

    /**
     * Creates a new Banner manager with optional configuration.
     */
    fun getBannerManager(
        adUnitId: String? = null,
        remoteConfigKey: String? = "banner_enabled"
    ): NextGenBannerAdManager {
        return NextGenBannerAdManager(
            adUnitId = adUnitId,
            billingManager = if (::billingManager.isInitialized) billingManager else null,
            remoteConfigKey = remoteConfigKey
        )
    }

    /**
     * Creates a new Interstitial manager with optional configuration.
     */
    fun getInterstitialManager(
        adUnitId: String? = null,
        remoteConfigKey: String? = "interstitial_enabled"
    ): NextGenInterstitialAdManager {
        return NextGenInterstitialAdManager(
            adUnitId = adUnitId,
            billingManager = if (::billingManager.isInitialized) billingManager else null,
            remoteConfigKey = remoteConfigKey
        )
    }

    /**
     * Creates a new Counter-based Interstitial manager.
     */
    fun getInterstitialCounterManager(
        adUnitId: String,
        remoteConfigKey: String? = "interstitial_counter_enabled",
        thresholdKey: String? = "interstitial_threshold"
    ): NextGenInterstitialCounterManager {
        return NextGenInterstitialCounterManager(
            adUnitId = adUnitId,
            billingManager = if (::billingManager.isInitialized) billingManager else null,
            remoteConfigKey = remoteConfigKey,
            thresholdKey = thresholdKey
        )
    }

    /**
     * Creates a new Interval-based Interstitial manager.
     */
    fun getInterstitialIntervalManager(
        adUnitId: String,
        remoteConfigKey: String? = "interstitial_interval_enabled",
        intervalKey: String? = "interstitial_interval"
    ): NextGenInterstitialIntervalManager {
        return NextGenInterstitialIntervalManager(
            adUnitId = adUnitId,
            billingManager = if (::billingManager.isInitialized) billingManager else null,
            remoteConfigKey = remoteConfigKey,
            intervalKey = intervalKey
        )
    }

    /**
     * Starts preloading Interstitial ads globally.
     */
    fun startPreloadingInterstitial(adUnitId: String) {
        val adRequest = AdRequest.Builder(adUnitId).build()
        val preloadConfig = PreloadConfiguration(adRequest)
        InterstitialAdPreloader.start(adUnitId, preloadConfig)
        Log.d(TAG, "Global preloading started for: $adUnitId")
    }

    /**
     * Creates a new Rewarded manager with optional configuration.
     */
    fun getRewardedManager(
        adUnitId: String? = null,
        remoteConfigKey: String? = "rewarded_enabled"
    ): NextGenRewardedAdManager {
        return NextGenRewardedAdManager(
            adUnitId = adUnitId,
            billingManager = if (::billingManager.isInitialized) billingManager else null,
            remoteConfigKey = remoteConfigKey
        )
    }

    /**
     * Creates a new Native manager with optional configuration.
     */
    fun getNativeAdManager(
        adUnitId: String? = null,
        remoteConfigKey: String? = "native_enabled"
    ): NextGenNativeAdManager {
        return NextGenNativeAdManager(
            adUnitId = adUnitId,
            billingManager = if (::billingManager.isInitialized) billingManager else null,
            remoteConfigKey = remoteConfigKey
        )
    }

    /**
     * Creates a new App Open manager with optional configuration.
     */
    fun getAppOpenAdManager(
        application: Application,
        adUnitId: String,
        remoteConfigKey: String? = "app_open_enabled"
    ): NextGenAppOpenAdManager {
        return NextGenAppOpenAdManager(
            application = application,
            adUnitId = adUnitId,
            billingManager = if (::billingManager.isInitialized) billingManager else null,
            remoteConfigKey = remoteConfigKey
        )
    }

    /**
     * Checks if ads are enabled globally.
     */
    fun isAdsEnabled(): Boolean {
        return RemoteConfigManager.getBoolean("ads_enabled") && 
               !(if (::billingManager.isInitialized) billingManager.isUserPro() else false)
    }
}
