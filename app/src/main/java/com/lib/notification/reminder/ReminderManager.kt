package com.lib.notification.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.text.Html
import android.widget.RemoteViews
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DecoratedCustomViewStyle
import androidx.core.app.NotificationManagerCompat
import com.lib.notification.MainActivity
import com.lib.notification.R
import com.lib.notification.event.EventManager
import com.lib.notification.receiver.AlarmReceiver
import com.lib.notification.reminder.ReminderConfig.MEDIA_NOTIFICATION_ID
import com.lib.notification.reminder.ReminderConfig.app
import com.lib.notification.reminder.ReminderConfig.reminderContentList
import com.lib.notification.reminder.ReminderConfig.reminderImageArr
import com.lib.notification.reminder.ReminderConfig.smallIcon
import com.lib.notification.reminder.entity.ReminderContentItem
import com.lib.notification.reminder.entity.ReminderType
import com.lib.notification.reminder.helper.AppLifecycleManager
import com.lib.notification.reminder.helper.ReminderWorker.workScope
import com.lib.notification.reminder.utils.fetchReminderLastShow
import com.lib.notification.reminder.utils.getCurrentCounts
import com.lib.notification.reminder.utils.isEnableSpecialMode
import com.lib.notification.reminder.utils.isGoogleDevice
import com.lib.notification.reminder.utils.isGrantedPostNotification
import com.lib.notification.reminder.utils.isInteractive
import com.lib.notification.reminder.utils.nextAlarmSetTime
import com.lib.notification.reminder.utils.reminderMediaTimerLastShow
import com.lib.notification.reminder.utils.reminderTimerLastShow
import com.lib.notification.reminder.utils.reminderUnlockLastShow
import com.lib.notification.reminder.utils.updateCurrentCounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

object ReminderManager {

    private val alarmManager by lazy { app.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    fun scheduleNextAlarm() {
        if (ReminderConfig.alarmSwitch.not() || ReminderConfig.alarmInterval <= 0) return
        if (nextAlarmSetTime > System.currentTimeMillis()) return
        val pendingIntent = PendingIntent.getBroadcast(
            app,
            Random.nextInt(1000, 1500),
            Intent(app, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextTime = System.currentTimeMillis() + ReminderConfig.alarmInterval * 60000L
        nextAlarmSetTime = nextTime
        AlarmManagerCompat.setAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            nextTime,
            pendingIntent
        )
    }

    @SuppressLint("MissingPermission")
    fun show(type: ReminderType) {
        if (canShow(type).not()) return
        val content = reminderContentList.randomOrNull() ?: return
        val imageIcon = reminderImageArr.randomOrNull() ?: return
        when (type) {
            ReminderType.TIMER -> {
                EventManager.customEvent("notify_trigger")
                EventManager.customEvent("notify_timer_trigger")
            }

            ReminderType.UNLOCK -> {
                EventManager.customEvent("notify_trigger")
                EventManager.customEvent("notify_unlock_trigger")
            }

            ReminderType.ALARM -> {
                EventManager.customEvent("IA_trigger")
            }

            else -> Unit
        }
        buildNotificationChannel()
        val builder = NotificationCompat.Builder(app, ReminderConfig.REMINDER_CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(goLoadingIntent(type, content))
            .setAutoCancel(true)
            .setContentTitle(content.button)
            .setContentText(content.text)
            .setGroupSummary(false)
            .setGroup(ReminderConfig.REMINDER_GROUP_NAME)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val tiny = getRemoteViews(content, R.layout.layout_reminder_tiny, imageIcon)
            val large = getRemoteViews(content, R.layout.layout_reminder_large, imageIcon)
            builder.setCustomContentView(tiny).setCustomHeadsUpContentView(tiny).setCustomBigContentView(large)
            builder.setStyle(DecoratedCustomViewStyle())
        } else {
            val isXiaomi = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
            val large = getRemoteViews(content, R.layout.layout_reminder_large, imageIcon)
            if (isXiaomi) {
                val mid = getRemoteViews(content, R.layout.layout_reminder_middle, imageIcon)
                builder.setCustomContentView(mid).setCustomHeadsUpContentView(mid).setCustomBigContentView(large)
            } else {
                builder.setCustomContentView(large).setCustomHeadsUpContentView(large).setCustomBigContentView(large)
            }
        }
        runCatching {
            // 10000这个id自己改下
            val notificationId = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM && isGoogleDevice()) 10000 else content.notificationId
            NotificationManagerCompat.from(app).notify(notificationId, builder.build())
            when (type) {
                ReminderType.TIMER -> reminderTimerLastShow = System.currentTimeMillis()
                ReminderType.UNLOCK -> reminderUnlockLastShow = System.currentTimeMillis()
                ReminderType.MEDIA -> reminderMediaTimerLastShow = System.currentTimeMillis()
                else -> Unit
            }
            updateCurrentCounts(type, false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showMediaNotification(originType: ReminderType) {
        if (isEnableSpecialMode.not() || ReminderConfig.mediaSwitchOn.not()) return
        val content = reminderContentList.randomOrNull() ?: return
        val imageIcon = reminderImageArr.randomOrNull() ?: return
        buildNotificationChannel()
        workScope.launch(Dispatchers.Main) {
            val builder = NotificationCompat.Builder(app, ReminderConfig.REMINDER_CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(goLoadingIntent(originType, ReminderType.MEDIA, content))
                .setAutoCancel(true)
                .setGroupSummary(false)
                .setGroup(ReminderConfig.REMINDER_GROUP_NAME)
                .setLargeIcon(Icon.createWithResource(app, imageIcon))
                .setContentTitle(content.button)
                .setContentText(content.text)
            ReflectUtils.setMediaStyleByReflection(app, builder, "MediaSession")

            runCatching {
                NotificationManagerCompat.from(app).notify(MEDIA_NOTIFICATION_ID, builder.build())
                when (originType) {
                    ReminderType.TIMER -> reminderTimerLastShow = System.currentTimeMillis()
                    ReminderType.UNLOCK -> reminderUnlockLastShow = System.currentTimeMillis()
                    ReminderType.MEDIA -> reminderMediaTimerLastShow = System.currentTimeMillis()
                    else -> Unit
                }
                updateCurrentCounts(originType, false)
                EventManager.customEvent(
                    "mediapop_trigger", hashMapOf(
                        "from_type" to when (originType) {
                            ReminderType.UNLOCK -> "unlock"
                            ReminderType.ALARM -> "alarm"
                            ReminderType.MEDIA -> "timenew"
                            else -> ""
                        }
                    )
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getRemoteViews(content: ReminderContentItem, layoutId: Int, imageId: Int): RemoteViews {
        return RemoteViews(app.packageName, layoutId).apply {
            setImageViewResource(R.id.image_reminder, imageId)
            setTextViewText(R.id.text_content, Html.fromHtml(content.text))
            setTextViewText(R.id.text_button, Html.fromHtml(content.button))
        }
    }

    private fun goLoadingIntent(type: ReminderType, item: ReminderContentItem): PendingIntent {
        return PendingIntent.getActivity(app, Random.nextInt(), Intent(app, MainActivity::class.java).apply {
            putExtra(ReminderConfig.EXTRA_KEY_REMINDER_TYPE, type.ordinal)
            putExtra(ReminderConfig.EXTRA_KEY_JUMP_TO, item.jump)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun goLoadingIntent(originType: ReminderType, type: ReminderType, item: ReminderContentItem): PendingIntent {
        return PendingIntent.getActivity(app, Random.nextInt(), Intent(app, MainActivity::class.java).apply {
            putExtra(ReminderConfig.EXTRA_KEY_ORIGIN_REMINDER_TYPE, originType.ordinal)
            putExtra(ReminderConfig.EXTRA_KEY_REMINDER_TYPE, type.ordinal)
            putExtra(ReminderConfig.EXTRA_KEY_JUMP_TO, item.jump)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun buildNotificationChannel() {
        NotificationManagerCompat.from(app).createNotificationChannel(
            NotificationChannelCompat.Builder(ReminderConfig.REMINDER_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MAX)
                .setLightsEnabled(true)
                .setVibrationEnabled(true)
                .setShowBadge(true)
                .setName(ReminderConfig.REMINDER_CHANNEL_ID)
                .build()
        )
    }

    private fun showOverlay(type: ReminderType) {
        val content = reminderContentList.randomOrNull() ?: return
        val imageIcon = reminderImageArr.randomOrNull() ?: return
        workScope.launch(Dispatchers.Main) {
            OverlayController.show(type, content, imageIcon)
        }
    }

    private fun canShow(type: ReminderType): Boolean {
        if (AppLifecycleManager.isAppForeground()) return false
        val overlayItem = ReminderConfig.overlayConf
        if (null != overlayItem && overlayItem.switch && isEnableSpecialMode && ReflectUtils.canDrawOverlaysByReflection(app)) {
            when (type) {
                ReminderType.ALARM -> {
                    showOverlay(type)
                    return false
                }

                ReminderType.TIMER, ReminderType.UNLOCK -> {
                    if (judgeWinConfig(type)) {
                        showOverlay(type)
                        return false
                    }
                }

                else -> Unit
            }
        }
        if (ReminderConfig.popSwitchOn.not()) return false
        when (type) {
            ReminderType.TIMER -> {
                return isGrantedPostNotification() && judgeConfig(type)
            }

            ReminderType.UNLOCK -> {
                if (isGrantedPostNotification()) {
                    return judgeConfig(type)
                } else if (judgeConfig(type)) {
                    showMediaNotification(type)
                    return false
                } else return false
            }

            ReminderType.MEDIA -> {
                if (isGrantedPostNotification().not() && judgeConfig(type)) {
                    showMediaNotification(type)
                }
                return false
            }

            ReminderType.ALARM -> {
                if (isGrantedPostNotification()) return true
                showMediaNotification(type)
                return false
            }
        }
    }

    // 判断悬浮窗配置是否满足
    private fun judgeWinConfig(type: ReminderType): Boolean {
        val confItem = ReminderConfig.overlayConf ?: return false
        val lastShow = fetchReminderLastShow(type, true)
        val interval = if (ReminderType.TIMER == type) confItem.timeInterval else confItem.unlockInterval
        if (interval != 0 && (System.currentTimeMillis() - lastShow) < (interval * 60000L)) return false
        val max = if (ReminderType.TIMER == type) confItem.timeMax else confItem.unlockMax
        if (max != 0 && getCurrentCounts(type, true) >= max) return false
        if (ReminderType.TIMER == type && ReminderConfig.popStartHour != ReminderConfig.popEndHour && isInteractive().not()) {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (ReminderConfig.popEndHour > ReminderConfig.popStartHour) {
                if (currentHour in ReminderConfig.popStartHour until ReminderConfig.popEndHour) return false
            } else {
                if (currentHour >= ReminderConfig.popStartHour || currentHour in 0 until ReminderConfig.popEndHour) return false
            }
        }
        return true
    }

    // 判断通知配置相关的，提出来了
    private fun judgeConfig(type: ReminderType): Boolean {
        val item = when (type) {
            ReminderType.TIMER -> ReminderConfig.timerConf
            ReminderType.UNLOCK -> ReminderConfig.unlockConf
            ReminderType.MEDIA -> ReminderConfig.mediaTimerConf
            else -> null
        } ?: return false
        val lastShow = fetchReminderLastShow(type, false)
        if (item.interval != 0 && (System.currentTimeMillis() - lastShow) < (item.interval * 60000L)) return false
        if (item.max != 0 && getCurrentCounts(type, false) >= item.max) return false
        if ((ReminderType.TIMER == type || ReminderType.MEDIA == type) && ReminderConfig.popStartHour != ReminderConfig.popEndHour && isInteractive().not()) {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (ReminderConfig.popEndHour > ReminderConfig.popStartHour) {
                if (currentHour in ReminderConfig.popStartHour until ReminderConfig.popEndHour) return false
            } else {
                if (currentHour >= ReminderConfig.popStartHour || currentHour in 0 until ReminderConfig.popEndHour) return false
            }
        }
        return true
    }

}