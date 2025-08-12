package com.app.calllib

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CallLog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class CallLogService : Service() {
    private lateinit var callLogObserver: CallLogObserver
    override fun onCreate() {
        super.onCreate()
        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                onNotificationDismissedReceiver,
                IntentFilter("DISMISSED_ACTION"),
                RECEIVER_NOT_EXPORTED // This is required on Android 14
            )
        }
    }
    private val onNotificationDismissedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            generateForegroundNotification()
        }
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::callLogObserver.isInitialized)
            contentResolver.unregisterContentObserver(callLogObserver)

        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            unregisterReceiver(onNotificationDismissedReceiver)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        generateForegroundNotification()
        callLogObserver = CallLogObserver(this, contentResolver, Handler(Looper.getMainLooper()))
        contentResolver.registerContentObserver(
            CallLog.Calls.CONTENT_URI,
            true,
            callLogObserver
        )
        return START_STICKY
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "dost_call_log_reading"
    }
    private fun generateForegroundNotification() {
        val dismissedIntent = Intent("DISMISSED_ACTION")
        dismissedIntent.setPackage(packageName) // This is required on Android 14
        val dismissedPendingIntent = PendingIntent.getBroadcast(
            this, 111,
            dismissedIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        if (SDK_INT >= Build.VERSION_CODES.O) {

            val mNotificationManager =
                    this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            mNotificationManager.createNotificationChannelGroup(
                NotificationChannelGroup("dost_app", "Elogix")
            )
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Service Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.enableLights(false)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            mNotificationManager.createNotificationChannel(notificationChannel)
            val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)

            builder.setContentTitle(
                StringBuilder("elogiX Service").toString()
            ).setTicker(
                StringBuilder("elogiX is running")
                    .toString()
            )
                .setDeleteIntent(dismissedPendingIntent).setSmallIcon(R.drawable.dost_logo)
                .setPriority(NotificationCompat.PRIORITY_LOW).setWhen(0).setOnlyAlertOnce(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setOngoing(true)
            builder.color = ContextCompat.getColor(this, R.color.app_color)
            val notification = builder.build()
            startForeground(
                7887, notification
            )
        }
    }
}