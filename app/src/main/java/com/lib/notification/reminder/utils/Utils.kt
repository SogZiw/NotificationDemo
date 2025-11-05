package com.lib.notification.reminder.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.text.format.DateUtils
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lib.notification.reminder.ReminderConfig.app
import com.lib.notification.reminder.entity.ReminderContentItem
import com.lib.notification.reminder.entity.ReminderType
import org.json.JSONArray
import java.util.Locale

fun isSamsungDevice() = Build.MANUFACTURER.equals("Samsung", ignoreCase = true)

fun getCountryCode(): String {
    return deviceFirstCountryCode.ifBlank {
        val cc = Locale.getDefault().country
        deviceFirstCountryCode = cc
        return@ifBlank cc
    }
}

fun parseReminderContent(jsonString: String): MutableList<ReminderContentItem> {
    val result = mutableListOf<ReminderContentItem>()
    runCatching {
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val jump = obj.optInt("jump", 0)
            val text = obj.optString("text", "")
            val button = obj.optString("button", "")
            val notifiId = obj.optInt("notifi_id")
            result.add(
                ReminderContentItem(
                    text = text,
                    button = button,
                    jump = jump,
                    notificationId = notifiId
                )
            )
        }
    }
    return result
}

fun isGrantedPostNotification(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(app, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else NotificationManagerCompat.from(app).areNotificationsEnabled()
}

fun isInteractive() = runCatching { (app.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive }.getOrNull() ?: false

fun fetchReminderLastShow(type: ReminderType): Long {
    return when (type) {
        ReminderType.TIMER -> reminderTimerLastShow
        ReminderType.UNLOCK -> reminderUnlockLastShow
        ReminderType.MEDIA -> reminderMediaTimerLastShow
        else -> 0L
    }
}

fun fetchReminderCounts(type: ReminderType): Int {
    return when (type) {
        ReminderType.TIMER -> reminderTimerCounts
        ReminderType.UNLOCK -> reminderUnlockCounts
        ReminderType.MEDIA -> reminderMediaTimerCounts
        else -> 0
    }
}

fun updateReminderCounts(type: ReminderType, newCounts: Int) {
    when (type) {
        ReminderType.TIMER -> reminderTimerCounts = newCounts
        ReminderType.UNLOCK -> reminderUnlockCounts = newCounts
        ReminderType.MEDIA -> reminderMediaTimerCounts = newCounts
        else -> Unit
    }
}

fun updateCurrentCounts(type: ReminderType) {
    val currentTime = reminderDailyTime
    if (DateUtils.isToday(currentTime)) {
        val counts = fetchReminderCounts(type)
        updateReminderCounts(type, counts + 1)
    } else {
        updateReminderCounts(type, 1)
    }
}

fun getCurrentCounts(type: ReminderType): Int {
    val currentTime = reminderDailyTime
    val counts: Int
    if (DateUtils.isToday(currentTime)) {
        counts = fetchReminderCounts(type)
    } else {
        updateReminderCounts(type, 0)
        counts = 0
    }
    reminderDailyTime = System.currentTimeMillis()
    return counts
}