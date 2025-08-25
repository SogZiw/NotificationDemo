package com.lib.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.lib.notification.reminder.utils.isSamsungDevice
import com.lib.notification.service.DemoToolbarService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (isSamsungDevice()) return
        runCatching {
            context?.let {
                ContextCompat.startForegroundService(context, Intent(context, DemoToolbarService::class.java))
            }
        }
    }

}