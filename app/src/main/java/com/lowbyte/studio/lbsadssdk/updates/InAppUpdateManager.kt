package com.lowbyte.studio.lbsadssdk.updates

import android.app.Activity
import android.content.Intent
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.lowbyte.studio.lbsadssdk.analytics.AnalyticsManager

class InAppUpdateManager(private val activity: Activity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private val UPDATE_REQUEST_CODE = 123

    fun checkForUpdate(activity: Activity) {
        AnalyticsManager.logEvent("update_check_start")
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                AnalyticsManager.logEvent("update_available", android.os.Bundle().apply {
                    putInt("version_code", appUpdateInfo.availableVersionCode())
                })
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    AnalyticsManager.logEvent("update_start_immediate")
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        activity,
                        UPDATE_REQUEST_CODE
                    )
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    AnalyticsManager.logEvent("update_start_flexible")
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        activity,
                        UPDATE_REQUEST_CODE
                    )
                }
            } else {
                AnalyticsManager.logEvent("update_not_available")
            }
        }
    }

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // After the update is downloaded, show a notification and request user confirmation to restart the app.
            appUpdateManager.completeUpdate()
        }
    }

    fun registerListener() {
        appUpdateManager.registerListener(listener)
    }

    fun unregisterListener() {
        appUpdateManager.unregisterListener(listener)
    }
}
