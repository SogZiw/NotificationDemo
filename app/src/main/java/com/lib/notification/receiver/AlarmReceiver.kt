package com.lib.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lib.notification.reminder.ReminderManager
import com.lib.notification.reminder.entity.ReminderType

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        runCatching {
            ReminderManager.show(ReminderType.ALARM)
        }
        ReminderManager.scheduleNextAlarm()
    }

}