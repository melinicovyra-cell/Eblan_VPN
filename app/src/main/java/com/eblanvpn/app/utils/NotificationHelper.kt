package com.eblanvpn.app.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.eblanvpn.app.MainActivity
import com.eblanvpn.app.R
import com.eblanvpn.app.data.model.TrafficStats
import com.eblanvpn.app.service.EblanVpnService

object NotificationHelper {

    const val VPN_NOTIFICATION_ID = 1001
    const val CHANNEL_ID_VPN = "eblan_vpn_service"
    const val CHANNEL_ID_GENERAL = "eblan_vpn_general"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // VPN service channel (ongoing)
        NotificationChannel(
            CHANNEL_ID_VPN,
            "VPN Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Показывает статус VPN подключения"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }.also { nm.createNotificationChannel(it) }

        // General notifications
        NotificationChannel(
            CHANNEL_ID_GENERAL,
            "Уведомления",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Общие уведомления приложения"
        }.also { nm.createNotificationChannel(it) }
    }

    fun buildVpnNotification(
        context: Context,
        serverName: String,
        stats: TrafficStats,
        showTraffic: Boolean = true
    ): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val disconnectIntent = Intent(context, EblanVpnService::class.java).apply {
            action = EblanVpnService.ACTION_STOP
        }
        val disconnectPending = PendingIntent.getService(
            context, 1, disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val uploadSpeed = TrafficStats.formatSpeed(stats.uploadSpeed)
        val downloadSpeed = TrafficStats.formatSpeed(stats.downloadSpeed)
        val totalUp = TrafficStats.formatBytes(stats.totalUpload)
        val totalDown = TrafficStats.formatBytes(stats.totalDownload)

        val contentText = if (showTraffic) {
            "↑ $uploadSpeed  ↓ $downloadSpeed"
        } else {
            "Подключено"
        }

        val bigText = if (showTraffic) {
            "↑ $uploadSpeed  ↓ $downloadSpeed\nВсего: ↑ $totalUp  ↓ $totalDown"
        } else {
            "VPN активен"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID_VPN)
            .setSmallIcon(R.drawable.ic_vpn_shield)
            .setContentTitle("$serverName — Подключено")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(openPending)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                R.drawable.ic_vpn_shield,
                "Отключить",
                disconnectPending
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun buildConnectingNotification(context: Context, serverName: String): Notification {
        val openIntent = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_VPN)
            .setSmallIcon(R.drawable.ic_vpn_shield)
            .setContentTitle("Подключение...")
            .setContentText(serverName)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setProgress(0, 0, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateVpnNotification(
        context: Context,
        serverName: String,
        stats: TrafficStats,
        showTraffic: Boolean = true
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(
            VPN_NOTIFICATION_ID,
            buildVpnNotification(context, serverName, stats, showTraffic)
        )
    }
}
