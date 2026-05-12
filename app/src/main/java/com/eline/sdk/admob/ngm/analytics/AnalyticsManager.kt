package com.eline.sdk.admob.ngm.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject
import java.lang.ref.WeakReference

object AnalyticsManager {
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var mixpanelRef: WeakReference<MixpanelAPI>? = null

    fun init(context: Context, mixpanelInstance: MixpanelAPI?) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
        mixpanelRef = mixpanelInstance?.let { WeakReference(it) }
    }

    fun logEvent(eventName: String, params: Bundle? = null) {
        // Log to Firebase
        firebaseAnalytics?.logEvent(eventName, params)

        // Log to Mixpanel (only if initialized)
        mixpanelRef?.get()?.let { mp ->
            val json = JSONObject()
            params?.let { bundle ->
                for (key in bundle.keySet()) {
                    val value = bundle.get(key)
                    if (value != null) {
                        json.put(key, value)
                    }
                }
            }
            mp.track(eventName, json)
        }
    }

    fun logAdImpression(adUnitId: String, adFormat: String) {
        val bundle = Bundle().apply {
            putString("ad_unit_id", adUnitId)
            putString("ad_format", adFormat)
        }
        logEvent("ad_impression_custom", bundle)
    }

    fun logException(e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
    }
}
