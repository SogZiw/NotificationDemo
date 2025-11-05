package com.lib.notification.reminder.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.lib.notification.event.EventManager
import com.lib.notification.reminder.ReminderConfig.app
import com.lib.notification.reminder.ReminderManager
import com.lib.notification.reminder.entity.ReminderType
import com.lib.notification.service.ExtraJobIntentService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

object ReminderWorker {

    val workScope by lazy { CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, _ -> }) }

    fun init() {
        workScope.launch {
            tickerFlow(0L, 5 * 60000L).collect {
                EventManager.customEvent("sessionback")
            }
        }
        app.registerReceiver(unlockReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
        })
        workScope.launch {
            tickerFlow(60000L, 60000L).collect {
                ReminderManager.show(ReminderType.TIMER)
            }
        }
        workScope.launch {
            tickerFlow(60000L, 60000L).collect {
                ReminderManager.show(ReminderType.MEDIA)
            }
        }
    }

    private val unlockReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                workScope.launch {
                    delay(1000L)
                    ReminderManager.show(ReminderType.UNLOCK)
                    context?.let {
                        runCatching {
                            ExtraJobIntentService.startWork(context)
                        }
                    }
                }
            }
        }
    }

    private fun tickerFlow(first: Long, interval: Long) = flow {
        delay(first)
        while (true) {
            emit(Unit)
            delay(interval)
        }
    }

}