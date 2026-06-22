package com.lib.notification.reminder.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.lib.notification.event.EventManager
import com.lib.notification.reminder.ReminderConfig
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
        ContextCompat.registerReceiver(app, reasonReceiver, IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS), ContextCompat.RECEIVER_NOT_EXPORTED)
        workScope.launch {
            tickerFlow(10000L, 60000L).collect {
                ReminderManager.show(ReminderType.TIMER)
            }
        }
    }

    // show广告点击通知
    fun showAdClickNotification() {
        workScope.launch {
            delay((ReminderConfig.adClickConf?.delay ?: 3) * 1000L)
            ReminderManager.show(ReminderType.AD_CLICK)
        }
    }

    private val reasonReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                workScope.launch {
                    delay(1000L)
                    if (intent?.action != Intent.ACTION_CLOSE_SYSTEM_DIALOGS) return@launch
                    when (intent.getStringExtra("reason").orEmpty()) {
                        "homekey", "fs_gesture" -> ReminderManager.show(ReminderType.HOME)
                        else -> ReminderManager.show(ReminderType.RECENT)
                    }
                }
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