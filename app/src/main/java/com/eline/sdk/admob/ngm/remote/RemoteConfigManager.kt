package com.eline.sdk.admob.ngm.remote

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.eline.sdk.admob.ngm.BuildConfig

import java.lang.ref.WeakReference

object RemoteConfigManager {
    private val remoteConfigRef: WeakReference<FirebaseRemoteConfig> = WeakReference(FirebaseRemoteConfig.getInstance())

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
            .build()
        
        val defaults = mapOf(
            "ads_enabled" to true,
            "banner_enabled" to true,
            "interstitial_enabled" to true,
            "interstitial_counter_enabled" to true,
            "interstitial_interval_enabled" to true,
            "rewarded_enabled" to true,
            "native_enabled" to true,
            "app_open_enabled" to true,
            "interstitial_threshold" to 2L,
            "interstitial_interval" to 60L
        )
        
        remoteConfigRef.get()?.apply {
            setConfigSettingsAsync(configSettings)
            setDefaultsAsync(defaults)
        }
    }

    fun fetchAndActivate(onComplete: (Boolean) -> Unit) {
        remoteConfigRef.get()?.fetchAndActivate()?.addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        } ?: onComplete(false)
    }

    fun getString(key: String): String = remoteConfigRef.get()?.getString(key) ?: ""
    fun getBoolean(key: String): Boolean = remoteConfigRef.get()?.getBoolean(key) ?: true
    fun getLong(key: String): Long = remoteConfigRef.get()?.getLong(key) ?: 0L
}
