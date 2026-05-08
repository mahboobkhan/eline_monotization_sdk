package com.lowbyte.studio.lbsadssdk.ads

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.lowbyte.studio.lbsadssdk.analytics.AnalyticsManager
import com.lowbyte.studio.lbsadssdk.utils.AdLoadingDialog

class RewardedAdManager(private val adUnitId: String) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun loadAd(activity: Activity) {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        AnalyticsManager.logEvent("rewarded_load_start")

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                AnalyticsManager.logEvent("rewarded_loaded")
                rewardedAd = ad
                isLoading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                AnalyticsManager.logEvent("rewarded_load_failed", android.os.Bundle().apply {
                    putString("error", error.message)
                })
                rewardedAd = null
                isLoading = false
            }
        })
    }

    fun showAd(activity: Activity, onRewardEarned: (RewardItem) -> Unit, onDismiss: () -> Unit) {
        if (rewardedAd != null) {
            AnalyticsManager.logEvent("rewarded_show_start")
            val dialog = AdLoadingDialog(activity)
            dialog.show()

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        AnalyticsManager.logEvent("rewarded_dismissed")
                        rewardedAd = null
                        loadAd(activity)
                        onDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        AnalyticsManager.logEvent("rewarded_show_failed", android.os.Bundle().apply {
                            putString("error", error.message)
                        })
                        rewardedAd = null
                        onDismiss()
                    }

                    override fun onAdClicked() {
                        AnalyticsManager.logEvent("rewarded_clicked")
                    }
                }
                rewardedAd?.show(activity) { rewardItem ->
                    AnalyticsManager.logEvent("rewarded_earned", android.os.Bundle().apply {
                        putString("type", rewardItem.type)
                        putInt("amount", rewardItem.amount)
                    })
                    onRewardEarned(rewardItem)
                }
            }, 1000)
        } else {
            AnalyticsManager.logEvent("rewarded_not_loaded")
            loadAd(activity)
            onDismiss()
        }
    }
}
