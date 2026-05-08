package com.lowbyte.studio.lbsadssdk.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.lowbyte.studio.lbsadssdk.analytics.AnalyticsManager
import com.lowbyte.studio.lbsadssdk.utils.AdLoadingDialog
import java.util.Date
import com.lowbyte.studio.lbsadssdk.billing.BillingManager
import com.lowbyte.studio.lbsadssdk.remote.RemoteConfigManager
import java.lang.ref.WeakReference

class AppOpenAdManager(
    private val application: Application,
    private val adUnitId: String
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivityRef: WeakReference<Activity>? = null
    private var billingManager: BillingManager? = null

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun setBillingManager(manager: BillingManager) {
        this.billingManager = manager
    }

    fun loadAd() {
        if (!RemoteConfigManager.getBoolean("ads_on") || !RemoteConfigManager.getBoolean("app_open_on")) return
        if (billingManager?.isUserPro() == true) return
        
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true
        AnalyticsManager.logEvent("app_open_load_start")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            application, adUnitId, request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    AnalyticsManager.logEvent("app_open_loaded")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    AnalyticsManager.logEvent("app_open_load_failed", Bundle().apply {
                        putString("error", loadAdError.message)
                    })
                    isLoadingAd = false
                }
            })
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    fun showAdIfAvailable(activity: Activity, onComplete: (() -> Unit)? = null) {
        if (isShowingAd) return
        
        if (billingManager?.isUserPro() == true || 
            !RemoteConfigManager.getBoolean("ads_on") || 
            !RemoteConfigManager.getBoolean("app_open_on")) {
            onComplete?.invoke()
            return
        }

        if (!isAdAvailable()) {
            loadAd()
            onComplete?.invoke()
            return
        }

        val dialog = AdLoadingDialog(activity)
        dialog.show()

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    AnalyticsManager.logEvent("app_open_dismissed")
                    appOpenAd = null
                    isShowingAd = false
                    loadAd()
                    onComplete?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    AnalyticsManager.logEvent("app_open_show_failed", Bundle().apply {
                        putString("error", adError.message)
                    })
                    appOpenAd = null
                    isShowingAd = false
                    loadAd()
                    onComplete?.invoke()
                }

                override fun onAdShowedFullScreenContent() {
                    AnalyticsManager.logEvent("app_open_show_start")
                    isShowingAd = true
                }

                override fun onAdClicked() {
                    AnalyticsManager.logEvent("app_open_clicked")
                }
            }
            appOpenAd?.show(activity)
        }, 500)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        currentActivityRef?.get()?.let { showAdIfAvailable(it) }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) { currentActivityRef = WeakReference(activity) }
    override fun onActivityResumed(activity: Activity) { currentActivityRef = WeakReference(activity) }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() == activity) {
            currentActivityRef = null
        }
    }
}
