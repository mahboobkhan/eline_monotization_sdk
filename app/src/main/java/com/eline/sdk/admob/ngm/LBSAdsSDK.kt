package com.eline.sdk.admob.ngm

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.eline.sdk.admob.ngm.analytics.AnalyticsManager
import java.lang.ref.WeakReference

object LBSAdsSDK {
    private var isInitialized = false
    private var mixpanelRef: WeakReference<MixpanelAPI>? = null

    fun init(context: Context, mixpanelToken: String? = null) {
        if (isInitialized) return
        val appContext = context.applicationContext

        FirebaseApp.initializeApp(appContext)
        
        mixpanelToken?.let {
            val mp = MixpanelAPI.getInstance(appContext, it, true)
            mixpanelRef = WeakReference(mp)
        }
        
        AnalyticsManager.init(appContext, mixpanelRef?.get())
        
        isInitialized = true
    }

    fun getMixpanel() = mixpanelRef?.get()
}
