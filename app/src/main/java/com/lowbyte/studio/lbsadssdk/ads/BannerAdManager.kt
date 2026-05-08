package com.lowbyte.studio.lbsadssdk.ads

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.lowbyte.studio.lbsadssdk.R
import com.lowbyte.studio.lbsadssdk.analytics.AnalyticsManager

class BannerAdManager(private val adUnitId: String) {

    fun loadBanner(activity: Activity, container: ViewGroup, adSize: AdSize = AdSize.BANNER) {
        // Show Shimmer
        val shimmer = activity.layoutInflater.inflate(R.layout.layout_banner_shimmer, container, false)
        container.removeAllViews()
        container.addView(shimmer)

        val adView = AdView(activity)
        adView.adUnitId = adUnitId
        adView.setAdSize(adSize)

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                container.removeAllViews()
                container.addView(adView)
                AnalyticsManager.logAdImpression(adUnitId, "Banner")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                container.removeAllViews()
                container.visibility = View.GONE
            }
        }

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }
}
