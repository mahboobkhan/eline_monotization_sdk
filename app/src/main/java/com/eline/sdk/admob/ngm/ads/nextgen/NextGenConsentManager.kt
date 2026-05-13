package com.eline.sdk.admob.ngm.ads.nextgen

import android.app.Activity
import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import androidx.core.content.edit

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
    private var isGatheringConsent = false

    /**
     * Initializes and gathers consent if required.
     * 
     * @param debug If true, forces EEA geography for testing the consent form.
     */
    fun gatherConsent(activity: Activity, debug: Boolean = false, onComplete: (Boolean) -> Unit) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 1. Check if already gathering
        if (isGatheringConsent) {
            Log.d(TAG, "Consent gathering already in progress. Skipping duplicate call.")
            return
        }

        // 2. Check if already resolved in previous session (Only if not in debug mode)
        if (!debug && prefs.getBoolean(KEY_CONSENT_RESOLVED, false)) {
            Log.d(TAG, "Consent already resolved in previous session.")
            onComplete(true)
            return
        }

        // 3. Check if country requires consent (Skip if in debug mode)
        if (!debug && !isConsentRequired(activity)) {
            Log.d(TAG, "Consent not required for this country. Marking as resolved.")
            prefs.edit { putBoolean(KEY_CONSENT_RESOLVED, true) }
            onComplete(true)
            return
        }

        isGatheringConsent = true

        // 4. Setup Debug Settings if enabled
        val debugSettings = if (debug) {
            Log.d(TAG, "Debug mode enabled: Forcing EEA geography for testing.")
            ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .build()
        } else null

        // 5. Use UMP SDK to gather consent
        Log.d(TAG, "Gathering consent via UMP SDK...")
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .setConsentDebugSettings(debugSettings)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        
        // Reset consent state in debug mode to ensure form shows up
        if (debug) {
            consentInformation?.reset()
        }

        consentInformation?.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    isGatheringConsent = false
                    if (formError != null) {
                        Log.e(TAG, "Consent form error: ${formError.message}")
                    }
                    
                    val resolved = canRequestAds()
                    if (resolved) {
                        Log.d(TAG, "Consent resolved successfully.")
                        prefs.edit { putBoolean(KEY_CONSENT_RESOLVED, true) }
                    }
                    onComplete(canRequestAds())
                }
            },
            { requestError ->
                isGatheringConsent = false
                Log.e(TAG, "Consent update request failed: [${requestError.errorCode}] ${requestError.message}")
                if (requestError.errorCode == 3) {
                    Log.e(TAG, "TIP: A '3' error code often means a network issue or missing Test Device Hashed ID for debug mode.")
                }
                onComplete(canRequestAds())
            }
        )
    }

    /**
     * Checks if ads can be requested based on UMP SDK status.
     */
    fun canRequestAds(context: Context? = null): Boolean {
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
            true // Default to true for safety
        }
    }
}
