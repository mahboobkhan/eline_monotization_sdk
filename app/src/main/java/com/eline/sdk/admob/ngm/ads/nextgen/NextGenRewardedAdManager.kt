package com.eline.sdk.admob.ngm.ads.nextgen

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.eline.sdk.admob.ngm.billing.BillingManager
import com.eline.sdk.admob.ngm.remote.RemoteConfigManager
import com.eline.sdk.admob.ngm.utils.AdLoadingDialog

/**
 * Manager for Rewarded Ads using the GMA Next-Gen SDK.
 * Standardized parameters and context-aware dismissal logic.
 */
class NextGenRewardedAdManager(
    private var adUnitId: String? = null,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = null
) {
    private val TAG = "NGAdsManagerRewarded"
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    /**
     * Loads a rewarded ad.
     */
    fun loadAd(
        activity: Activity,
        customAdUnitId: String? = null,
        customRemoteConfigKey: String? = null,
        listener: NextGenAdListener? = null
    ) {
        val finalAdUnitId = customAdUnitId ?: adUnitId ?: "ca-app-pub-3940256099942544/5224354917"
        val finalRemoteKey = customRemoteConfigKey ?: remoteConfigKey

        if (billingManager?.isUserPro() == true) {
            Log.d(TAG, "Rewarded: User is Pro, ads suppressed.")
            return
        }

        val isEnabled = finalRemoteKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            Log.d(TAG, "Rewarded disabled by Remote Config (key: $finalRemoteKey)")
            return
        }

        if (rewardedAd != null || isLoading) return

        isLoading = true
        val adRequest = AdRequest.Builder(finalAdUnitId).build()

        RewardedAd.load(adRequest, object : AdLoadCallback<RewardedAd> {
            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Rewarded ad loaded: $finalAdUnitId")
                rewardedAd = ad
                isLoading = false
                listener?.onAdLoaded(finalAdUnitId)

                ad.adEventCallback = object : RewardedAdEventCallback {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Rewarded ad dismissed.")
                        rewardedAd = null
                        listener?.onAdDismissed(finalAdUnitId)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                        Log.e(TAG, "Rewarded ad failed to show: ${error.message}")
                        rewardedAd = null
                        listener?.onAdFailedToShow(finalAdUnitId, error.toString())
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Rewarded ad showed.")
                        listener?.onAdShowed(finalAdUnitId)
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "Rewarded ad clicked.")
                        listener?.onAdClicked(finalAdUnitId)
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "Rewarded ad impression recorded.")
                        listener?.onAdImpression(finalAdUnitId)
                    }
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Rewarded ad failed to load: ${adError.message}")
                isLoading = false
                rewardedAd = null
                listener?.onAdFailedToLoad(finalAdUnitId, adError.toString())
            }
        })
    }

    /**
     * Shows the rewarded ad if loaded.
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
        val finalAdUnitId = adUnitId ?: "ca-app-pub-3940256099942544/5224354917"
        val ad = rewardedAd

        if (ad == null) {
            Log.d(TAG, "Ad not loaded. Loading now...")
            loadAd(activity, listener = listener)
            onDismiss()
            return
        }

        val originalCallback = ad.adEventCallback
        ad.adEventCallback = object : RewardedAdEventCallback {
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
                ad.show(activity) { rewardItem ->
                    Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                    listener?.onUserEarnedReward(finalAdUnitId, rewardItem.amount, rewardItem.type)
                }
            }, delayMs)
        } else {
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                listener?.onUserEarnedReward(finalAdUnitId, rewardItem.amount, rewardItem.type)
            }
        }
    }
}
