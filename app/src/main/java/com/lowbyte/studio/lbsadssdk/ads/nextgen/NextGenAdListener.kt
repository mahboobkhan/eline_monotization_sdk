package com.lowbyte.studio.lbsadssdk.ads.nextgen

/**
 * Standard listener for all Next-Gen Ads.
 */
interface NextGenAdListener {
    fun onAdLoaded(adUnitId: String) {}
    fun onAdFailedToLoad(adUnitId: String, error: String) {}
    fun onAdShowed(adUnitId: String) {}
    fun onAdClicked(adUnitId: String) {}
    fun onAdDismissed(adUnitId: String) {}
    fun onAdFailedToShow(adUnitId: String, error: String) {}
    fun onAdImpression(adUnitId: String) {}
    fun onUserEarnedReward(adUnitId: String, amount: Int, type: String) {}
}
