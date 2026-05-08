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

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                isLoading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedAd = null
                isLoading = false
            }
        })
    }

    fun showAd(activity: Activity, onReward: (Boolean) -> Unit) {
        if (rewardedAd != null) {
            val dialog = AdLoadingDialog(activity)
            dialog.show()

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                rewardedAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewardedAd = null
                        loadAd(activity)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                        rewardedAd = null
                        onReward(false)
                    }
                }
                rewardedAd?.show(activity) { rewardItem ->
                    onReward(true)
                }
            }, 800)
        } else {
            loadAd(activity)
            onReward(false)
        }
    }
}
