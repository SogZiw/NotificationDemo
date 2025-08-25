package com.lib.notification.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.lib.notification.reminder.ReminderConfig
import com.lib.notification.reminder.ToolbarManager

class DemoToolbarService : Service() {

    companion object {
        var isToolbarRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        isToolbarRunning = true
    }

    private fun startNotificationMain() {
        runCatching {
            val notification = ToolbarManager.buildNotification()
            startForeground(ReminderConfig.TOOLBAR_NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startNotificationMain()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isToolbarRunning = false
    }

}