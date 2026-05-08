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
import android.util.Log
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

    private val TAG = "AppOpenAd"
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
        if (!RemoteConfigManager.getBoolean("ads_on") || !RemoteConfigManager.getBoolean("app_open_on")) {
            Log.d(TAG, "App Open ads disabled via remote config.")
            return
        }
        if (billingManager?.isUserPro() == true) {
            Log.d(TAG, "User is pro, skipping app open load.")
            return
        }
        
        if (isLoadingAd || isAdAvailable()) {
            Log.d(TAG, "Ad already loading or available.")
            return
        }
        
        Log.i(TAG, "Loading App Open ad for ID: $adUnitId")
        isLoadingAd = true
        AnalyticsManager.logEvent("app_open_load_start")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            application, adUnitId, request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.i(TAG, "App Open ad loaded successfully.")
                    AnalyticsManager.logEvent("app_open_loaded")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "App Open ad failed to load: ${loadAdError.message}")
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
        if (isShowingAd) {
            Log.d(TAG, "Ad already showing.")
            return
        }
        
        if (billingManager?.isUserPro() == true || 
            !RemoteConfigManager.getBoolean("ads_on") || 
            !RemoteConfigManager.getBoolean("app_open_on")) {
            Log.d(TAG, "Skipping ad show: Pro status or disabled.")
            onComplete?.invoke()
            return
        }

        if (!isAdAvailable()) {
            Log.w(TAG, "Ad not available when show requested. Loading...")
            loadAd()
            onComplete?.invoke()
            return
        }

        Log.d(TAG, "Showing App Open ad...")

        val dialog = AdLoadingDialog(activity)
        dialog.show()

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.i(TAG, "App Open ad dismissed.")
                    AnalyticsManager.logEvent("app_open_dismissed")
                    appOpenAd = null
                    isShowingAd = false
                    loadAd()
                    onComplete?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "App Open ad failed to show: ${adError.message}")
                    AnalyticsManager.logEvent("app_open_show_failed", Bundle().apply {
                        putString("error", adError.message)
                    })
                    appOpenAd = null
                    isShowingAd = false
                    loadAd()
                    onComplete?.invoke()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "App Open ad shown.")
                    AnalyticsManager.logEvent("app_open_show_start")
                    isShowingAd = true
                }

                override fun onAdClicked() {
                    Log.d(TAG, "App Open ad clicked.")
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
