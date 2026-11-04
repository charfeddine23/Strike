package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Individual

object StrikerNotificationHelper {

    const val CHANNEL_ID = "striker_call_alerts"
    private const val CHANNEL_NAME = "Striker Call Alerts"
    private const val CHANNEL_DESC = "Notifications when an individual registered in StrikePulse is calling"
    private const val NOTIFICATION_ID = 9001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300, 200, 500)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun notifyStrikerCalling(
        context: Context,
        individual: Individual,
        incomingNumber: String
    ) {
        createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TARGET_STRIKER_ID", individual.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            individual.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚡ A striker is calling!")
            .setContentText("${individual.fullName} (${individual.strikeCount} strikes • BOOST ${individual.boost})")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "⚡ A striker is calling!\n" +
                        "Name: ${individual.fullName}\n" +
                        "Phone: ${if (individual.phoneNumber.isNotBlank()) individual.phoneNumber else incomingNumber}\n" +
                        "Total Strikes: ${individual.strikeCount} | BOOST: ${individual.boost} | Type: ${individual.type}"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300, 200, 500))
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun testStrikerCallNotification(context: Context) {
        val sample = Individual(
            id = 999,
            fullName = "Sample Striker",
            type = "M",
            length = 45.0,
            width = 30.0,
            size = 1350.0,
            sScore = 18.0,
            fScore = 16.0,
            cScore = 19.0,
            boost = 66.5,
            strikeCount = 42,
            phoneNumber = "+1 555-0199"
        )
        notifyStrikerCalling(context, sample, "+1 555-0199")
    }
}
