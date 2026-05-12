package com.eline.sdk.admob.ngm.utils

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.eline.sdk.admob.ngm.analytics.AnalyticsManager

object AdShowHelper {
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Handles the transition from a loading dialog to a fullscreen ad.
     * Ensures the ad is shown immediately if the dialog is dismissed early.
     */
    fun showAdWithOptionalDialog(
        activity: Activity,
        showDialog: Boolean,
        delayMs: Long,
        onShowAd: () -> Unit
    ) {
        if (!showDialog) {
            onShowAd()
            return
        }

        val dialog = AdLoadingDialog(activity)
        var hasProceeded = false

        val proceedToAd = {
            if (!hasProceeded) {
                hasProceeded = true
                handler.removeCallbacksAndMessages(null)
                if (dialog.isShowing) {
                    try {
                        dialog.dismiss()
                    } catch (e: Exception) {
                        // Activity might be finished
                    }
                }
                onShowAd()
            }
        }

        // Handle case where dialog is dismissed manually or via system (though setCancelable(false))
        dialog.setOnDismissListener {
            proceedToAd()
        }

        try {
            dialog.show()
            handler.postDelayed({
                proceedToAd()
            }, delayMs)
        } catch (e: Exception) {
            AnalyticsManager.logException(e)
            onShowAd() // Fallback if dialog fails
        }
    }
}
