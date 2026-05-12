package com.lowbyte.studio.lbsadssdk.examples

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.lowbyte.studio.lbsadssdk.R
import com.lowbyte.studio.lbsadssdk.ads.nextgen.NextGenAdsManager
import com.lowbyte.studio.lbsadssdk.billing.BillingManager

/**
 * Example Splash Activity demonstrating SDK initialization and App Open Ad.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_example)

        val billingManager = BillingManager(this)

        // 1. Initialize GMA Next-Gen SDK
        NextGenAdsManager.initialize(
            context = this as android.content.Context,
            appId = "ca-app-pub-3940256099942544~3347511713", // Sample App ID
            billing = billingManager
        ) {
            // SDK Initialized
            
            // 2. Start preloading Interstitial ads globally
            NextGenAdsManager.startPreloadingInterstitial("ca-app-pub-3940256099942544/1033173712")
            
            // 3. Initialize and load App Open Ad
            val appOpenManager = NextGenAdsManager.getAppOpenAdManager(
                application = application ?: (applicationContext as android.app.Application),
                adUnitId = "ca-app-pub-3940256099942544/9257395921"
            )
            appOpenManager.loadAd()
            
            // 4. Move to Home after a short delay
            Handler(Looper.getMainLooper()).postDelayed({
                appOpenManager.showAdIfAvailable(this as Activity) {
                    val intent = Intent(this as android.content.Context, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }, 3000)
        }
    }
}
