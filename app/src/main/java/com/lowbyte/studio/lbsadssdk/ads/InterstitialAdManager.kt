package com.lowbyte.studio.lbsadssdk.ads

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.lowbyte.studio.lbsadssdk.analytics.AnalyticsManager
import com.lowbyte.studio.lbsadssdk.utils.AdLoadingDialog

class InterstitialAdManager(private val adUnitId: String) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun loadAd(activity: Activity) {
        if (isLoading || interstitialAd != null) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                isLoading = false
                AnalyticsManager.logEvent("interstitial_loaded", null)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialAd = null
                isLoading = false
                AnalyticsManager.logEvent("interstitial_failed", null)
            }
        })
    }

    fun showAd(activity: Activity, onDismiss: () -> Unit) {
        if (interstitialAd != null) {
            val dialog = AdLoadingDialog(activity)
            dialog.show()

            // Artificial delay to show dialog as requested "Handle loading Ads Dialoges"
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        loadAd(activity) // Preload next
                        onDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                        interstitialAd = null
                        onDismiss()
                    }
                }
                interstitialAd?.show(activity)
            }, 800)
        } else {
            loadAd(activity)
            onDismiss()
        }
    }
}
