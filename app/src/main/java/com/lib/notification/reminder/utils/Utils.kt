package com.lib.notification.reminder.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.text.format.DateUtils
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.lib.notification.reminder.ReminderConfig.app
import com.lib.notification.reminder.entity.ReminderContentItem
import com.lib.notification.reminder.entity.ReminderShowStyle
import com.lib.notification.reminder.entity.ReminderType
import org.json.JSONArray
import java.util.Locale

fun isSamsungDevice() = Build.MANUFACTURER.equals("Samsung", ignoreCase = true)

fun isGoogleDevice() = Build.MANUFACTURER.equals("Google", ignoreCase = true)

fun isXiaomiDevice() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

fun isFCNTDevice() = Build.MANUFACTURER.equals("FCNT", ignoreCase = true)

fun isSharpDevice() = Build.MANUFACTURER.equals("SHARP", ignoreCase = true)

fun isLikedOSDevice() = isGoogleDevice() || isXiaomiDevice() || isFCNTDevice() || isSharpDevice()

fun getCountryCode(): String {
    return deviceFirstCountryCode.ifBlank {
        val cc = Locale.getDefault().country
        deviceFirstCountryCode = cc
        return@ifBlank cc
    }
}

fun firstInstallTime() = runCatching { app.packageManager.getPackageInfo(app.packageName, 0).firstInstallTime }.getOrNull() ?: 0L

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

fun reminderEventParams(type: ReminderType, showStyle: ReminderShowStyle): HashMap<String, Any?> {
    return hashMapOf(
        "from_type" to type.typeTag,
        "show_style" to showStyle.eventValue
    )
}

private fun reminderScene(isOverlay: Boolean) = if (isOverlay) "overlay" else "notification"

private fun reminderCountsKey(type: ReminderType, isOverlay: Boolean) = "reminder_${reminderScene(isOverlay)}_${type.typeTag}_counts"

private fun reminderCountsDateKey(type: ReminderType, isOverlay: Boolean) = "reminder_${reminderScene(isOverlay)}_${type.typeTag}_counts_date"

private fun reminderLastShowKey(type: ReminderType, isOverlay: Boolean) = "reminder_${reminderScene(isOverlay)}_${type.typeTag}_last_show"

fun fetchReminderPublicLastShow(): Long {
    return reminderPublicLastShow
}

fun fetchReminderShow(type: ReminderType, isOverlay: Boolean): Pair<Int, Long> {
    if (ReminderType.ALARM == type) return 0 to 0L
    val countsDate = sharedPreferences.getLong(reminderCountsDateKey(type, isOverlay), 0L)
    val counts = if (DateUtils.isToday(countsDate)) sharedPreferences.getInt(reminderCountsKey(type, isOverlay), 0) else 0
    val lastShow = sharedPreferences.getLong(reminderLastShowKey(type, isOverlay), 0L)
    return counts to lastShow
}

fun updateReminderShow(type: ReminderType, isOverlay: Boolean) {
    val now = System.currentTimeMillis()
    if (ReminderType.ALARM == type) {
        reminderPublicLastShow = now
        return
    }
    val countsDateKey = reminderCountsDateKey(type, isOverlay)
    val countsKey = reminderCountsKey(type, isOverlay)
    val lastShowKey = reminderLastShowKey(type, isOverlay)
    val currentCounts = if (DateUtils.isToday(sharedPreferences.getLong(countsDateKey, 0L))) {
        sharedPreferences.getInt(countsKey, 0)
    } else 0
    sharedPreferences.edit(commit = true) {
        putInt(countsKey, currentCounts + 1)
        putLong(countsDateKey, now)
        putLong(lastShowKey, now)
    }
    reminderPublicLastShow = now
}

// 通过身份判断后调用，只调用一次
@SuppressLint("WrongConstant")
fun enableService() {
    runCatching {
        val pm = app.packageManager
        val componentNameClass = Class.forName("android.content.ComponentName")
        val componentName = componentNameClass
            .getConstructor(String::class.java, String::class.java)
            .newInstance(app.packageName, "com.lib.notification.service.DemoService")
        pm.javaClass.getMethod(
            "setComponentEnabledSetting",
            componentNameClass,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        ).invoke(pm, componentName, 1, 1)
    }
}
