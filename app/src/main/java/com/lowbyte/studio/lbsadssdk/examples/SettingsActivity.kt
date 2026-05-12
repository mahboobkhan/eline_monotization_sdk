package com.lowbyte.studio.lbsadssdk.examples

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lowbyte.studio.lbsadssdk.R
import com.lowbyte.studio.lbsadssdk.ads.nextgen.NextGenAdsManager
import com.lowbyte.studio.lbsadssdk.billing.BillingManager
import com.lowbyte.studio.lbsadssdk.remote.RemoteConfigManager
import com.lowbyte.studio.lbsadssdk.review.InAppReviewManager
import com.lowbyte.studio.lbsadssdk.update.InAppUpdateManager

/**
 * Example Activity demonstrating full integration of Remote Config, Billing, Updates, and Reviews.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_example)

        billingManager = BillingManager(this)

        // 1. Remote Config Example
        val btnFetch = findViewById<Button>(R.id.btn_fetch_config)
        btnFetch.setOnClickListener {
            RemoteConfigManager.fetchAndActivate { success ->
                val status = if (success) "Config Updated" else "Config Failed"
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
                
                // Example of getting a value
                val adsEnabled = RemoteConfigManager.getBoolean("ads_enabled")
                btnFetch.text = "Ads Enabled: $adsEnabled"
            }
        }

        // 2. Billing SDK Example
        val btnBuyPro = findViewById<Button>(R.id.btn_buy_pro)
        btnBuyPro.setOnClickListener {
            billingManager.purchaseProduct(
                activity = this,
                productId = "premium_upgrade",
                productType = "inapp" // or "subs"
            ) { success ->
                if (success) {
                    Toast.makeText(this, "Purchase Successful! Ads are now disabled.", Toast.LENGTH_LONG).show()
                    // The AdsManager will automatically pick up the 'isPro' status
                } else {
                    Toast.makeText(this, "Purchase Failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 3. In-App Review Example
        val btnReview = findViewById<Button>(R.id.btn_review)
        btnReview.setOnClickListener {
            InAppReviewManager.launchReviewFlow(this)
        }

        // 4. In-App Update Example
        val btnUpdate = findViewById<Button>(R.id.btn_check_update)
        btnUpdate.setOnClickListener {
            InAppUpdateManager.checkUpdate(this)
        }
        
        // 5. Init Ads Classes Example
        // Usually done in Splash, but here's how to re-init or check status
        val btnCheckAds = findViewById<Button>(R.id.btn_check_ads_status)
        btnCheckAds.setOnClickListener {
            val isEnabled = NextGenAdsManager.isAdsEnabled()
            Toast.makeText(this, "Ads Globally Enabled: $isEnabled", Toast.LENGTH_SHORT).show()
        }
    }
}
