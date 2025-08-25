package com.lib.notification.service

import android.content.Context
import android.content.Intent
import androidx.core.app.BaseJobIntentService
import com.lib.notification.reminder.ReminderConfig
import com.lib.notification.reminder.utils.isSamsungDevice

class ExtraJobIntentService : BaseJobIntentService() {

    companion object {
        fun startWork(context: Context) {
            enqueueWork(context, ExtraJobIntentService::class.java, ReminderConfig.JOB_INTENT_ID, Intent(context, ExtraJobIntentService::class.java))
        }
    }

    override fun onHandleWork(intent: Intent) {
        if (isSamsungDevice()) return
        ReminderConfig.showToolbar(ReminderConfig.app)
    }

}