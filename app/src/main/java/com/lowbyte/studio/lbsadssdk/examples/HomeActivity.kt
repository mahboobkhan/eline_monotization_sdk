package com.lowbyte.studio.lbsadssdk.examples

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.lowbyte.studio.lbsadssdk.R
import com.lowbyte.studio.lbsadssdk.ads.nextgen.NextGenAdsManager
import com.lowbyte.studio.lbsadssdk.ads.nextgen.NextGenNativeAdManager

/**
 * Example Home Activity demonstrating Banner and Native ads.
 */
class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_example)

        val bannerContainer = findViewById<FrameLayout>(R.id.banner_container)
        val nativeContainer = findViewById<FrameLayout>(R.id.native_container)
        val btnShowInterstitial = findViewById<Button>(R.id.btn_show_interstitial)

        // 1. Show Banner Ad (Adaptive & Collapsible)
        val bannerManager = NextGenAdsManager.getBannerManager("ca-app-pub-3940256099942544/9214589741")
        bannerManager.loadAndShowBanner(
            activity = this,
            container = bannerContainer,
            isCollapsible = true,
            collapsibleType = "bottom"
        )

        // 2. Show Medium Native Ad with Shimmer
        val nativeManager = NextGenAdsManager.getNativeAdManager("ca-app-pub-3940256099942544/2247696110")
        nativeManager.loadAndShowNativeAd(
            activity = this,
            container = nativeContainer,
            size = NextGenNativeAdManager.NativeSize.MEDIUM
        )

        // 3. Setup Interstitial Counter Manager for a button click
        val counterManager = NextGenAdsManager.getInterstitialCounterManager("ca-app-pub-3940256099942544/1033173712")
        btnShowInterstitial.setOnClickListener {
            counterManager.showAd(this, threshold = 3) {
                // Logic after ad dismissed or if threshold not reached
            }
        }
    }
}
