package com.lowbyte.studio.lbsadssdk.ads

import android.app.Activity
import android.app.Application
import android.view.ViewGroup
import com.google.android.gms.ads.rewarded.RewardItem
import com.lowbyte.studio.lbsadssdk.billing.BillingManager
import com.lowbyte.studio.lbsadssdk.remote.RemoteConfigManager
import com.lowbyte.studio.lbsadssdk.utils.NetworkUtils

object AdsManager {
    private lateinit var interstitialManager: InterstitialAdManager
    private lateinit var interstitialSimple: InterstitialSimpleManager
    private lateinit var interstitialCounter: InterstitialCounterManager
    private lateinit var interstitialInterval: InterstitialIntervalManager
    private lateinit var rewardedManager: RewardedAdManager
    private lateinit var appOpenManager: AppOpenAdManager
    private lateinit var billingManager: BillingManager

    var adsEnabled = true

    fun init(application: Application, billing: BillingManager) {
        billingManager = billing
        
        // Load IDs from Remote Config or use default/test
        val interstitialId = AdIds.getInterstitialId(RemoteConfigManager.getString("interstitial_id"))
        val rewardedId = AdIds.getRewardedId(RemoteConfigManager.getString("rewarded_id"))
        val appOpenId = AdIds.getAppOpenId(RemoteConfigManager.getString("app_open_id"))

        interstitialManager = InterstitialAdManager(interstitialId)
        interstitialSimple = InterstitialSimpleManager(interstitialId)
        interstitialCounter = InterstitialCounterManager(interstitialId)
        interstitialInterval = InterstitialIntervalManager(interstitialId)
        
        rewardedManager = RewardedAdManager(rewardedId)
        appOpenManager = AppOpenAdManager(application, appOpenId)
        appOpenManager.setBillingManager(billingManager)
        
        adsEnabled = RemoteConfigManager.getBoolean("ads_enabled")
    }

    private fun canShowAds(activity: Activity): Boolean {
        return adsEnabled && !billingManager.isUserPro() && NetworkUtils.isNetworkAvailable(activity)
    }

    fun loadInterstitial(activity: Activity) {
        if (canShowAds(activity)) interstitialManager.loadAd(activity)
    }

    fun showInterstitial(
        activity: Activity,
        showDialog: Boolean = true,
        delayMs: Long = 500,
        onDismiss: () -> Unit
    ) {
        if (canShowAds(activity)) {
            interstitialManager.showAd(activity, showDialog, delayMs, onDismiss)
        } else {
            onDismiss()
        }
    }

    fun loadRewarded(activity: Activity) {
        if (canShowAds(activity)) rewardedManager.loadAd(activity)
    }

    fun showRewarded(
        activity: Activity,
        showDialog: Boolean = true,
        delayMs: Long = 500,
        onRewardEarned: (RewardItem) -> Unit,
        onDismiss: () -> Unit
    ) {
        if (canShowAds(activity)) {
            rewardedManager.showAd(activity, showDialog, delayMs, onRewardEarned, onDismiss)
        } else {
            // If pro/no-ads, provide a dummy reward item so the flow can continue
            onRewardEarned(object : RewardItem {
                override fun getType(): String = "pro_reward"
                override fun getAmount(): Int = 1
            })
            onDismiss()
        }
    }

    fun showBanner(activity: Activity, container: ViewGroup) {
        if (canShowAds(activity)) {
            val bannerId = AdIds.getBannerId(RemoteConfigManager.getString("banner_id"))
            BannerAdManager(bannerId).loadBanner(activity, container)
        } else {
            container.removeAllViews()
            container.visibility = android.view.View.GONE
        }
    }

    fun showNative(activity: Activity, container: ViewGroup, layoutResId: Int) {
        if (canShowAds(activity)) {
            val nativeId = AdIds.getNativeId(RemoteConfigManager.getString("native_id"))
            NativeAdManager(nativeId).loadNativeAd(activity, container, layoutResId)
        } else {
            container.removeAllViews()
            container.visibility = android.view.View.GONE
        }
    }

    fun getAppOpenManager() = appOpenManager
    fun getBillingManager() = billingManager
    
    fun getInterstitialSimple() = interstitialSimple
    fun getInterstitialCounter() = interstitialCounter
    fun getInterstitialInterval() = interstitialInterval
}
