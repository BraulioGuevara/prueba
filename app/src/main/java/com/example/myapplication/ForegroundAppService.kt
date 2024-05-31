package com.example.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.util.*

class ForegroundAppService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            val currentApp = getForegroundApp()
            val intent = Intent("com.example.myapplication.FOREGROUND_APP_UPDATED")
            intent.putExtra("appName", currentApp)
            sendBroadcast(intent)
            handler.postDelayed(this, 5000) // Verificar cada 5 segundos
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        handler.post(runnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getForegroundApp(): String {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 5000 // 5 segundos atrás

        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, beginTime, endTime
        )

        if (usageStatsList.isNullOrEmpty()) {
            return "Unknown"
        }

        var foregroundApp: UsageStats? = null
        for (usageStats in usageStatsList) {
            if (foregroundApp == null || usageStats.lastTimeUsed > foregroundApp.lastTimeUsed) {
                foregroundApp = usageStats
            }
        }

        return foregroundApp?.packageName ?: "Unknown"
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "ForegroundAppServiceChannel"
            val channelName = "Foreground App Service Channel"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Foreground App Service")
                .setContentText("Monitoring foreground app usage")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
            startForeground(1, notification)
        }
    }
}
