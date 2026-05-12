package com.lowbyte.studio.lbsadssdk.ads.nextgen

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.lowbyte.studio.lbsadssdk.billing.BillingManager
import com.lowbyte.studio.lbsadssdk.remote.RemoteConfigManager
import com.lowbyte.studio.lbsadssdk.utils.AdLoadingDialog

/**
 * Manager for Interstitial Ads using the GMA Next-Gen SDK.
 * Standardized parameters and context-aware dismissal logic.
 */
class NextGenInterstitialAdManager(
    private var adUnitId: String? = null,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = null
) {
    private val TAG = "NGAdsManagerInter"
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    /**
     * Loads an interstitial ad.
     */
    fun loadAd(
        activity: Activity,
        customAdUnitId: String? = null,
        customRemoteConfigKey: String? = null,
        listener: NextGenAdListener? = null
    ) {
        val finalAdUnitId = customAdUnitId ?: adUnitId ?: "ca-app-pub-3940256099942544/1033173712"
        val finalRemoteKey = customRemoteConfigKey ?: remoteConfigKey

        if (billingManager?.isUserPro() == true) {
            Log.d(TAG, "Interstitial: User is Pro, ads suppressed.")
            return
        }
        
        val isEnabled = finalRemoteKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            Log.d(TAG, "Interstitial disabled by Remote Config (key: $finalRemoteKey)")
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
                        listener?.onAdDismissed(finalAdUnitId)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                        Log.e(TAG, "Interstitial ad failed to show: ${error.message}")
                        interstitialAd = null
                        listener?.onAdFailedToShow(finalAdUnitId, error.toString())
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Interstitial ad showed.")
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
        isFragment: Boolean = false,
        dialogStyle: AdLoadingDialog.Style = AdLoadingDialog.Style.SMALL,
        listener: NextGenAdListener? = null,
        onDismiss: () -> Unit
    ) {
        val finalAdUnitId = adUnitId ?: "ca-app-pub-3940256099942544/1033173712"
        val ad = interstitialAd

        if (ad == null) {
            Log.d(TAG, "Ad not loaded. Loading now...")
            loadAd(activity, listener = listener)
            onDismiss()
            return
        }

        // Setup dismissal behavior
        val originalCallback = ad.adEventCallback
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                originalCallback?.onAdDismissedFullScreenContent()
                if (reloadOnDismiss) loadAd(activity, listener = listener)
                if (!isFragment) {
                    Log.d(TAG, "isFragment is false, dismissing via onAdDismissed.")
                    onDismiss()
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                originalCallback?.onAdFailedToShowFullScreenContent(error)
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
