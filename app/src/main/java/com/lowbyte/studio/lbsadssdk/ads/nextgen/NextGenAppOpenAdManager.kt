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
 * Standardized parameters and context-aware dismissal logic.
 */
class NextGenAppOpenAdManager(
    private val application: Application,
    private var adUnitId: String,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = "app_open_enabled"
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private val TAG = "NGAdsManagerAppOpen"
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivityRef: WeakReference<Activity>? = null

    init {
        application.registerActivityLifecycleCallbacks(this)
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }
    }

    /**
     * Loads an App Open ad.
     */
    fun loadAd(
        customAdUnitId: String? = null,
        customRemoteConfigKey: String? = null,
        listener: NextGenAdListener? = null
    ) {
        val finalAdUnitId = customAdUnitId ?: adUnitId
        val finalRemoteKey = customRemoteConfigKey ?: remoteConfigKey

        if (billingManager?.isUserPro() == true) {
            Log.d(TAG, "App Open: User is Pro, ads suppressed.")
            return
        }
        
        if (!NextGenConsentManager.canRequestAds(application)) {
            Log.w(TAG, "App Open: First Resolve Consent then Try to load Ads.")
            return
        }
        
        val isEnabled = finalRemoteKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            Log.d(TAG, "App Open disabled by Remote Config (key: $finalRemoteKey)")
            return
        }

        if (isLoadingAd || isAdAvailable()) return

        isLoadingAd = true
        Log.d(TAG, "Loading App Open ad: $finalAdUnitId")
        
        val adRequest = AdRequest.Builder(finalAdUnitId).build()

        AppOpenAd.load(adRequest, object : AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(ad: AppOpenAd) {
                Log.i(TAG, "App Open ad loaded: $finalAdUnitId")
                appOpenAd = ad
                isLoadingAd = false
                loadTime = Date().time
                listener?.onAdLoaded(finalAdUnitId)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "App Open ad failed to load: ${error.message}")
                isLoadingAd = false
                listener?.onAdFailedToLoad(finalAdUnitId, error.toString())
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
        reloadOnDismiss: Boolean = true,
        isFragment: Boolean = false,
        dialogStyle: AdLoadingDialog.Style = AdLoadingDialog.Style.FULLSCREEN,
        listener: NextGenAdListener? = null,
        onComplete: (() -> Unit)? = null
    ) {
        if (isShowingAd) return

        if (billingManager?.isUserPro() == true) {
            onComplete?.invoke()
            return
        }

        if (!isAdAvailable()) {
            loadAd(listener = listener)
            onComplete?.invoke()
            return
        }

        Log.d(TAG, "Showing App Open ad...")

        if (showDialog) {
            val dialog = AdLoadingDialog(activity, dialogStyle)
            dialog.show()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                showActualAd(activity, reloadOnDismiss, isFragment, listener, onComplete)
            }, delayMs)
        } else {
            showActualAd(activity, reloadOnDismiss, isFragment, listener, onComplete)
        }
    }

    private fun showActualAd(
        activity: Activity,
        reloadOnDismiss: Boolean,
        isFragment: Boolean,
        listener: NextGenAdListener?,
        onComplete: (() -> Unit)?
    ) {
        val finalAdUnitId = adUnitId
        appOpenAd?.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open ad dismissed.")
                appOpenAd = null
                isShowingAd = false
                listener?.onAdDismissed(finalAdUnitId)
                if (reloadOnDismiss) loadAd(listener = listener)
                if (!isFragment) onComplete?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                Log.e(TAG, "App Open ad failed to show: ${error.message}")
                appOpenAd = null
                isShowingAd = false
                listener?.onAdFailedToShow(finalAdUnitId, error.toString())
                onComplete?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open ad showed.")
                isShowingAd = true
                listener?.onAdShowed(finalAdUnitId)
            }
            
            override fun onAdClicked() {
                Log.d(TAG, "App Open ad clicked.")
                listener?.onAdClicked(finalAdUnitId)
            }
            
            override fun onAdImpression() {
                Log.d(TAG, "App Open ad impression.")
                listener?.onAdImpression(finalAdUnitId)
                if (isFragment) {
                    Log.d(TAG, "isFragment is true, dismissing via onAdImpression.")
                    onComplete?.invoke()
                }
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
