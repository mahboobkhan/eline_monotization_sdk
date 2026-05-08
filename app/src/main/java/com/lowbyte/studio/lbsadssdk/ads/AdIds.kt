package com.lowbyte.studio.lbsadssdk.ads

import com.lowbyte.studio.lbsadssdk.BuildConfig

object AdIds {
    // Test IDs
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395923"

    fun getBannerId(realId: String): String = if (BuildConfig.DEBUG) TEST_BANNER else realId
    fun getInterstitialId(realId: String): String = if (BuildConfig.DEBUG) TEST_INTERSTITIAL else realId
    fun getRewardedId(realId: String): String = if (BuildConfig.DEBUG) TEST_REWARDED else realId
    fun getNativeId(realId: String): String = if (BuildConfig.DEBUG) TEST_NATIVE else realId
    fun getAppOpenId(realId: String): String = if (BuildConfig.DEBUG) TEST_APP_OPEN else realId
}
