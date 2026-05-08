package com.lowbyte.studio.lbsadssdk.remote

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.lowbyte.studio.lbsadssdk.BuildConfig

import java.lang.ref.WeakReference

object RemoteConfigManager {
    private val remoteConfigRef: WeakReference<FirebaseRemoteConfig> = WeakReference(FirebaseRemoteConfig.getInstance())

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
            .build()
        remoteConfigRef.get()?.setConfigSettingsAsync(configSettings)
    }

    fun fetchAndActivate(onComplete: (Boolean) -> Unit) {
        remoteConfigRef.get()?.fetchAndActivate()?.addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        } ?: onComplete(false)
    }

    fun getString(key: String): String = remoteConfigRef.get()?.getString(key) ?: ""
    fun getBoolean(key: String): Boolean = remoteConfigRef.get()?.getBoolean(key) ?: false
    fun getLong(key: String): Long = remoteConfigRef.get()?.getLong(key) ?: 0L
}
