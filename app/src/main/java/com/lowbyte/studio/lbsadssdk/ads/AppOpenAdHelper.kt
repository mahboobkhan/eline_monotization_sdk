package com.lowbyte.studio.lbsadssdk.ads

import android.app.Activity
import android.content.Intent
import com.lowbyte.studio.lbsadssdk.remote.RemoteConfigManager

object AppOpenAdHelper {

    /**
     * Shows the App Open Ad and proceeds to the next activity.
     * Handles Pro status, Remote Config toggles, and optional completion logic.
     *
     * @param activity The current activity (Splash).
     * @param nextIntent The intent to start after the ad is finished.
     * @param finishCurrent Whether to finish the current activity.
     */
    fun showAdAndProceed(
        activity: Activity,
        nextIntent: Intent,
        finishCurrent: Boolean = true
    ) {
        val appOpenManager = AdsManager.getAppOpenManager()
        
        appOpenManager.showAdIfAvailable(activity) {
            // Ad is finished or ad was not available
            activity.startActivity(nextIntent)
            if (finishCurrent) {
                activity.finish()
            }
        }
    }
}
