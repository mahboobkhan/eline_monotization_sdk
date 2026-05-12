package com.eline.sdk.admob.ngm.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.eline.sdk.admob.ngm.R
import com.eline.sdk.admob.ngm.analytics.AnalyticsManager
import java.net.HttpURLConnection
import java.net.URL

class LBSFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        AnalyticsManager.logEvent("notification_received")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            showNotification(data)
        }
    }

    private fun showNotification(data: Map<String, String>) {
        val title = data["title"] ?: ""
        val body = data["body"] ?: ""
        val iconUrl = data["icon_url"]
        val featureImageUrl = data["feature_image_url"]
        val isAd = data["is_ad"] == "true"
        val link = data["link"]

        val channelId = "lbs_default_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Small View
        val smallView = RemoteViews(packageName, R.layout.layout_notification_small)
        smallView.setTextViewText(R.id.notification_title, title)
        smallView.setTextViewText(R.id.notification_body, body)
        
        // Expanded View
        val expandedView = RemoteViews(packageName, R.layout.layout_notification_expanded)
        expandedView.setTextViewText(R.id.notification_title, title)
        expandedView.setTextViewText(R.id.notification_body, body)
        
        if (isAd) {
            expandedView.setViewVisibility(R.id.notification_ad_badge, View.VISIBLE)
        }

        // Load Images
        iconUrl?.let {
            val bitmap = getBitmapFromUrl(it)
            bitmap?.let { b ->
                smallView.setImageViewBitmap(R.id.notification_icon, b)
                expandedView.setImageViewBitmap(R.id.notification_icon, b)
            }
        }

        featureImageUrl?.let {
            val bitmap = getBitmapFromUrl(it)
            bitmap?.let { b ->
                expandedView.setImageViewBitmap(R.id.notification_feature_image, b)
                expandedView.setViewVisibility(R.id.notification_feature_image, View.VISIBLE)
            }
        }

        val intent = if (!link.isNullOrEmpty()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(link))
        } else {
            packageManager.getLaunchIntentForPackage(packageName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Fallback small icon
            .setCustomContentView(smallView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun getBitmapFromUrl(urlString: String): Bitmap? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AnalyticsManager.logEvent("notification_token_new")
        // You could send this to your server or log it
    }
}
