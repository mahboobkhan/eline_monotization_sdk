package com.lowbyte.studio.lbsadssdk.ads.nextgen

import android.app.Activity
import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Manager for GDPR/UMP Consent.
 * Ensures ads are only requested after consent is resolved in applicable regions.
 */
object NextGenConsentManager {
    private const val TAG = "NGAdsManagerConsent"
    private const val PREFS_NAME = "lbs_ads_sdk_prefs"
    private const val KEY_CONSENT_RESOLVED = "consent_resolved"

    private val EEA_COUNTRIES = setOf(
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE", "IT",
        "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "GB", "IS", "LI", "NO"
    )

    private var consentInformation: ConsentInformation? = null

    /**
     * Initializes and gathers consent if required.
     */
    fun gatherConsent(activity: Activity, onComplete: (Boolean) -> Unit) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 1. Check if already resolved in previous session
        if (prefs.getBoolean(KEY_CONSENT_RESOLVED, false)) {
            Log.d(TAG, "Consent already resolved in previous session.")
            onComplete(true)
            return
        }

        // 2. Check if country requires consent
        if (!isConsentRequired(activity)) {
            Log.d(TAG, "Consent not required for this country. Marking as resolved.")
            prefs.edit().putBoolean(KEY_CONSENT_RESOLVED, true).apply()
            onComplete(true)
            return
        }

        // 3. Use UMP SDK to gather consent
        Log.d(TAG, "Consent required. Gathering via UMP SDK...")
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation?.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.e(TAG, "Consent form error: ${formError.message}")
                    }
                    
                    val resolved = canRequestAds()
                    if (resolved) {
                        Log.d(TAG, "Consent gathered successfully.")
                        prefs.edit().putBoolean(KEY_CONSENT_RESOLVED, true).apply()
                    }
                    onComplete(resolved)
                }
            },
            { requestError ->
                Log.e(TAG, "Consent update request failed: ${requestError.message}")
                onComplete(canRequestAds())
            }
        )
    }

    /**
     * Checks if ads can be requested based on UMP SDK status.
     */
    fun canRequestAds(context: Context? = null): Boolean {
        // If we don't have activity context, check prefs first
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_CONSENT_RESOLVED, false)) return true
        }

        val info = consentInformation ?: return false
        return info.canRequestAds()
    }

    /**
     * Identifies if the user is in an EEA country using TelephonyManager.
     */
    private fun isConsentRequired(context: Context): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val countryCode = tm.networkCountryIso.uppercase()
            Log.d(TAG, "Detected Country: $countryCode")
            EEA_COUNTRIES.contains(countryCode)
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting country: ${e.message}")
            true // Default to true for safety if detection fails
        }
    }
}
