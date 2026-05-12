package com.eline.sdk.admob.ngm.examples

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.eline.sdk.admob.ngm.R
import com.eline.sdk.admob.ngm.ads.nextgen.NextGenAdsManager
import com.eline.sdk.admob.ngm.billing.BillingManager
import com.eline.sdk.admob.ngm.remote.RemoteConfigManager
import com.eline.sdk.admob.ngm.review.InAppReviewManager
import com.eline.sdk.admob.ngm.update.InAppUpdateManager

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
                Toast.makeText(this as android.content.Context, status, Toast.LENGTH_SHORT).show()
                
                // Example of getting a value
                val adsEnabled = RemoteConfigManager.getBoolean("ads_enabled")
                btnFetch.text = "Ads Enabled: $adsEnabled"
            }
        }

        // 2. Billing SDK Example
        val btnBuyPro = findViewById<Button>(R.id.btn_buy_pro)
        btnBuyPro.setOnClickListener {
            billingManager.purchaseProduct(
                activity = this as Activity,
                productId = "premium_upgrade",
                productType = "inapp" // or "subs"
            ) { success ->
                if (success) {
                    Toast.makeText(this as android.content.Context, "Purchase Successful! Ads are now disabled.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this as android.content.Context, "Purchase Failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 3. In-App Review Example
        val btnReview = findViewById<Button>(R.id.btn_review)
        btnReview.setOnClickListener {
            InAppReviewManager.launchReviewFlow(this as Activity)
        }

        // 4. In-App Update Example
        val btnUpdate = findViewById<Button>(R.id.btn_check_update)
        btnUpdate.setOnClickListener {
            InAppUpdateManager.checkUpdate(this as Activity)
        }
        
        // 5. Init Ads Classes Example
        val btnCheckAds = findViewById<Button>(R.id.btn_check_ads_status)
        btnCheckAds.setOnClickListener {
            val isEnabled = NextGenAdsManager.isAdsEnabled()
            Toast.makeText(this as android.content.Context, "Ads Globally Enabled: $isEnabled", Toast.LENGTH_SHORT).show()
        }
    }
}
