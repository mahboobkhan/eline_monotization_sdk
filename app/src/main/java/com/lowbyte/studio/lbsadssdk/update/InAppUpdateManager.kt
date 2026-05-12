package com.lowbyte.studio.lbsadssdk.update

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.lowbyte.studio.lbsadssdk.analytics.AnalyticsManager

/**
 * Manager for handling Play Store In-App Updates.
 */
object InAppUpdateManager {
    private const val TAG = "InAppUpdate"
    const val UPDATE_REQUEST_CODE = 999

    fun checkUpdate(activity: Activity) {
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                AnalyticsManager.logEvent("update_available")
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    activity,
                    UPDATE_REQUEST_CODE
                )
            } else {
                Log.d(TAG, "No update available or immediate update not allowed.")
            }
        }
    }
}
