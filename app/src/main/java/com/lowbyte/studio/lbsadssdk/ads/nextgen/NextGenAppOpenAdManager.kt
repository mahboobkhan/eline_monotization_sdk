package com.lowbyte.studio.lbsadssdk.ads.nextgen

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.lowbyte.studio.lbsadssdk.billing.BillingManager
import com.lowbyte.studio.lbsadssdk.remote.RemoteConfigManager
import com.lowbyte.studio.lbsadssdk.utils.AdLoadingDialog
import java.lang.ref.WeakReference
import java.util.Date

/**
 * Manager for App Open Ads using the GMA Next-Gen SDK.
 */
class NextGenAppOpenAdManager(
    private val application: Application,
    private val adUnitId: String,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = "app_open_enabled"
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private val TAG = "NextGenAppOpen"
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivityRef: WeakReference<Activity>? = null

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * Loads an App Open ad.
     */
    fun loadAd() {
        if (billingManager?.isUserPro() == true) return
        
        val isEnabled = remoteConfigKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) return

        if (isLoadingAd || isAdAvailable()) return

        isLoadingAd = true
        Log.d(TAG, "Loading App Open ad: $adUnitId")
        
        val adRequest = AdRequest.Builder(adUnitId).build()

        AppOpenAd.load(adRequest, object : AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(ad: AppOpenAd) {
                Log.i(TAG, "App Open ad loaded.")
                appOpenAd = ad
                isLoadingAd = false
                loadTime = Date().time
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "App Open ad failed to load: $error")
                isLoadingAd = false
            }
        })
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && (Date().time - loadTime) < 3600000 * 4 // 4 hours
    }

    /**
     * Shows the ad if available.
     */
    fun showAdIfAvailable(
        activity: Activity,
        showDialog: Boolean = true,
        delayMs: Long = 500,
        onComplete: (() -> Unit)? = null
    ) {
        if (isShowingAd) return

        if (billingManager?.isUserPro() == true) {
            onComplete?.invoke()
            return
        }

        if (!isAdAvailable()) {
            loadAd()
            onComplete?.invoke()
            return
        }

        Log.d(TAG, "Showing App Open ad...")

        if (showDialog) {
            val dialog = AdLoadingDialog(activity)
            dialog.show()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                showActualAd(activity, onComplete)
            }, delayMs)
        } else {
            showActualAd(activity, onComplete)
        }
    }

    private fun showActualAd(activity: Activity, onComplete: (() -> Unit)?) {
        appOpenAd?.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open ad dismissed.")
                appOpenAd = null
                isShowingAd = false
                loadAd()
                onComplete?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                Log.e(TAG, "App Open ad failed to show: $error")
                appOpenAd = null
                isShowingAd = false
                loadAd()
                onComplete?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open ad showed.")
                isShowingAd = true
            }
            
            override fun onAdClicked() {
                Log.d(TAG, "App Open ad clicked.")
            }
            
            override fun onAdImpression() {
                Log.d(TAG, "App Open ad impression.")
            }
        }
        appOpenAd?.show(activity)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        currentActivityRef?.get()?.let { showAdIfAvailable(it) }
    }

    // Activity Lifecycle Callbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) { currentActivityRef = WeakReference(activity) }
    override fun onActivityResumed(activity: Activity) { currentActivityRef = WeakReference(activity) }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() == activity) currentActivityRef = null
    }
}
