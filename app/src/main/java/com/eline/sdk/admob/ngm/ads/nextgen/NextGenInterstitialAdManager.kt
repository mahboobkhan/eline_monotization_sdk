package com.eline.sdk.admob.ngm.ads.nextgen

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.eline.sdk.admob.ngm.billing.BillingManager
import com.eline.sdk.admob.ngm.remote.RemoteConfigManager
import com.eline.sdk.admob.ngm.utils.AdLoadingDialog

/**
 * Manager for Interstitial Ads using the GMA Next-Gen SDK.
 * Standardized parameters and context-aware dismissal logic.
 */
class NextGenInterstitialAdManager(
    private val billingManager: BillingManager? = null
) {
    private val TAG = "NGAdsManagerInter"
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    /**
     * Loads an interstitial ad.
     */
    fun loadAd(
        customAdUnitId: String? = null,
        customRemoteConfigKey: String = "fullscreen_enabled",
        listener: NextGenAdListener? = null
    ) {
        val finalAdUnitId = customAdUnitId ?: "ca-app-pub-3940256099942544/1033173712"

        if (billingManager?.isUserPro() == true) {
            Log.d(TAG, "Interstitial: User is Pro, ads suppressed.")
            return
        }
        
        val isEnabled = customRemoteConfigKey.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            Log.d(TAG, "Interstitial disabled by Remote Config (key: $customRemoteConfigKey)")
            return
        }

        if (interstitialAd != null || isLoading) return

        isLoading = true
        val adRequest = AdRequest.Builder(finalAdUnitId).build()

        InterstitialAd.load(adRequest, object : AdLoadCallback<InterstitialAd> {
            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "Interstitial ad loaded: $finalAdUnitId")
                interstitialAd = ad
                isLoading = false
                listener?.onAdLoaded(finalAdUnitId)

                ad.adEventCallback = object : InterstitialAdEventCallback {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Interstitial ad dismissed.")
                        interstitialAd = null
                        AdStateController.isInterstitialShowing = false
                        listener?.onAdDismissed(finalAdUnitId)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                        Log.e(TAG, "Interstitial ad failed to show: ${error.message}")
                        interstitialAd = null
                        AdStateController.isInterstitialShowing = false
                        listener?.onAdFailedToShow(finalAdUnitId, error.toString())
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Interstitial ad showed.")
                        AdStateController.isInterstitialShowing = true
                        listener?.onAdShowed(finalAdUnitId)
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "Interstitial ad clicked.")
                        listener?.onAdClicked(finalAdUnitId)
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "Interstitial ad impression recorded.")
                        listener?.onAdImpression(finalAdUnitId)
                    }
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Interstitial ad failed to load: ${adError.message}")
                isLoading = false
                interstitialAd = null
                listener?.onAdFailedToLoad(finalAdUnitId, adError.toString())
            }
        })
    }

    /**
     * Shows the interstitial ad if loaded.
     */
    fun showAd(
        activity: Activity,
        showDialog: Boolean = true,
        delayMs: Long = 500,
        reloadOnDismiss: Boolean = true,
        remoteConfig: String = "fullscreen_enabled",
        adUnitId: String? = null,
        isFragment: Boolean = false,
        dialogStyle: AdLoadingDialog.Style = AdLoadingDialog.Style.SMALL,
        listener: NextGenAdListener? = null,
        onDismiss: () -> Unit
    ) {
        val isEnabled = remoteConfig.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            Log.d(TAG, "Interstitial disabled by Remote Config (key: $remoteConfig)")
            onDismiss()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "Ad not loaded. Loading now...")
            loadAd( adUnitId,listener = listener)
            onDismiss()
            return
        }

        // Setup dismissal behavior
        val originalCallback = ad.adEventCallback
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "dismissing via onAdDismissed.")
                originalCallback?.onAdDismissedFullScreenContent()
                AdStateController.isInterstitialShowing = false
                if (reloadOnDismiss) loadAd(adUnitId,listener = listener)
                if (!isFragment) {
                    onDismiss()
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                Log.d(TAG, "fullscreen onAdFailedToShowFullScreenContent ${error.message}")
                originalCallback?.onAdFailedToShowFullScreenContent(error)
                AdStateController.isInterstitialShowing = false
                onDismiss()
            }

            override fun onAdShowedFullScreenContent() {
                originalCallback?.onAdShowedFullScreenContent()
            }

            override fun onAdClicked() {
                originalCallback?.onAdClicked()
            }

            override fun onAdImpression() {
                originalCallback?.onAdImpression()
                if (isFragment) {
                    Log.d(TAG, "isFragment is true, dismissing via onAdImpression.")
                    onDismiss()
                }
            }
        }

        if (showDialog) {
            val dialog = AdLoadingDialog(activity, dialogStyle)
            dialog.show()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                ad.show(activity)
            }, delayMs)
        } else {
            ad.show(activity)
        }
    }
}
