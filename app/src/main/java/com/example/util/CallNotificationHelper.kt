package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Individual

object CallNotificationHelper {

    const val CHANNEL_ID = "striker_call_alerts_channel"
    private const val CHANNEL_NAME = "Striker Incoming Calls"
    private const val CHANNEL_DESC = "Notifications when a registered striker calls your device"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Displays a notification when an incoming call matches a registered individual.
     */
    fun showStrikerCallNotification(context: Context, individual: Individual) {
        createNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_INDIVIDUAL_ID", individual.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            individual.id.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.striker_calling_title) + ": " + individual.fullName
        val content = context.getString(
            R.string.striker_calling_msg,
            individual.fullName,
            individual.boost.toString(),
            individual.strikeCount
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$content\nTel: ${individual.phoneNumber}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationId = 2000 + (individual.id % 1000).toInt()
        notificationManager.notify(notificationId, notification)
    }

    /**
     * For manual or test trigger from UI
     */
    fun showTestNotification(
        context: Context,
        name: String = "Tariq Al-Mansoor",
        phone: String = "+1 555-0198",
        boost: Double = 242.0,
        strikes: Int = 42
    ) {
        val testIndividual = Individual(
            id = 999L,
            fullName = name,
            phoneNumber = phone,
            type = "M",
            length = 24.5,
            width = 12.0,
            size = 294.0,
            sScore = 88.0,
            fScore = 76.5,
            cScore = 92.0,
            boost = boost,
            strikeCount = strikes
        )
        showStrikerCallNotification(context, testIndividual)
    }
}
