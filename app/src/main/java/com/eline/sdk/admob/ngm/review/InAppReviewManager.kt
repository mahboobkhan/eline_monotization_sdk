package com.eline.sdk.admob.ngm.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.eline.sdk.admob.ngm.analytics.AnalyticsManager

object InAppReviewManager {
    fun launchReviewFlow(activity: Activity) {
        AnalyticsManager.logEvent("review_flow_attempt")
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                AnalyticsManager.logEvent("review_info_received")
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    AnalyticsManager.logEvent("review_flow_finished")
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown.
                }
            } else {
                AnalyticsManager.logEvent("review_info_failed")
            }
        }
    }
}
