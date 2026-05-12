package com.lowbyte.studio.lbsadssdk.ads.nextgen

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.lowbyte.studio.lbsadssdk.billing.BillingManager
import com.lowbyte.studio.lbsadssdk.remote.RemoteConfigManager
import com.lowbyte.studio.lbsadssdk.utils.AdLoadingDialog

/**
 * Manager for Rewarded Ads using the GMA Next-Gen SDK.
 */
class NextGenRewardedAdManager(
    private var adUnitId: String? = null,
    private val billingManager: BillingManager? = null,
    private val remoteConfigKey: String? = null
) {
    private val TAG = "NGAdsManagerRewarded"
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    interface RewardedListener {
        fun onAdLoaded() {}
        fun onAdFailedToLoad(error: String) {}
        fun onAdDismissed() {}
        fun onAdClicked() {}
        fun onAdShowed() {}
        fun onAdFailedToShow(error: String) {}
        fun onUserEarnedReward(rewardItem: RewardItem)
    }

    /**
     * Loads a rewarded ad.
     */
    fun loadAd(activity: Activity, listener: RewardedListener? = null) {
        if (billingManager?.isUserPro() == true) {
            Log.d(TAG, "Rewarded: User is Pro, ads suppressed.")
            return
        }

        val isEnabled = remoteConfigKey?.let { RemoteConfigManager.getBoolean(it) } ?: true
        if (!isEnabled) {
            Log.d(TAG, "Rewarded disabled by Remote Config (key: $remoteConfigKey)")
            return
        }

        if (rewardedAd != null || isLoading) return

        isLoading = true
        val finalAdUnitId = adUnitId ?: "ca-app-pub-3940256099942544/5224354917" // Sample ID
        val adRequest = AdRequest.Builder(finalAdUnitId).build()

        RewardedAd.load(adRequest, object : AdLoadCallback<RewardedAd> {
            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Rewarded ad loaded.")
                rewardedAd = ad
                isLoading = false
                listener?.onAdLoaded()

                ad.adEventCallback = object : RewardedAdEventCallback {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Rewarded ad dismissed.")
                        rewardedAd = null
                        listener?.onAdDismissed()
                        // Preload next ad
                        loadAd(activity)
                    }

                    override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                        Log.e(TAG, "Rewarded ad failed to show: $fullScreenContentError")
                        rewardedAd = null
                        listener?.onAdFailedToShow(fullScreenContentError.toString())
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Rewarded ad showed.")
                        listener?.onAdShowed()
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "Rewarded ad clicked.")
                        listener?.onAdClicked()
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "Rewarded ad impression recorded.")
                    }
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Rewarded ad failed to load: $adError")
                isLoading = false
                rewardedAd = null
                listener?.onAdFailedToLoad(adError.toString())
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
        listener: RewardedListener
    ) {
        if (rewardedAd == null) {
            Log.d(TAG, "Ad not loaded. Loading now...")
            loadAd(activity, listener)
            return
        }

        if (showDialog) {
            val dialog = AdLoadingDialog(activity)
            dialog.show()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                rewardedAd?.show(activity) { rewardItem ->
                    Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                    listener.onUserEarnedReward(rewardItem)
                }
            }, delayMs)
        } else {
            rewardedAd?.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                listener.onUserEarnedReward(rewardItem)
            }
        }
    }
}
