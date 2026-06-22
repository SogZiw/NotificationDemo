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
import com.lib.notification.event.TrustedTimeClientIns
import com.lib.notification.receiver.AlarmReceiver
import com.lib.notification.reminder.ReminderConfig.MEDIA_NOTIFICATION_ID
import com.lib.notification.reminder.ReminderConfig.app
import com.lib.notification.reminder.ReminderConfig.reminderContentList
import com.lib.notification.reminder.ReminderConfig.reminderImageArr
import com.lib.notification.reminder.ReminderConfig.smallIcon
import com.lib.notification.reminder.entity.ReminderContentItem
import com.lib.notification.reminder.entity.ReminderShowStyle
import com.lib.notification.reminder.entity.ReminderType
import com.lib.notification.reminder.helper.AppLifecycleManager
import com.lib.notification.reminder.helper.ReminderWorker.workScope
import com.lib.notification.reminder.utils.fetchReminderPublicLastShow
import com.lib.notification.reminder.utils.fetchReminderShow
import com.lib.notification.reminder.utils.firstInstallTime
import com.lib.notification.reminder.utils.isEnableServerTimeJudge
import com.lib.notification.reminder.utils.isEnableSpecialMode
import com.lib.notification.reminder.utils.isGrantedPostNotification
import com.lib.notification.reminder.utils.isInteractive
import com.lib.notification.reminder.utils.isLikedOSDevice
import com.lib.notification.reminder.utils.isSamsungDevice
import com.lib.notification.reminder.utils.isXiaomiDevice
import com.lib.notification.reminder.utils.nextAlarmSetTime
import com.lib.notification.reminder.utils.reminderEventParams
import com.lib.notification.reminder.utils.updateReminderShow
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
    @Synchronized
    fun show(type: ReminderType) {
        if (canShow(type).not()) return
        val content = reminderContentList.randomOrNull() ?: return
        val imageIcon = reminderImageArr.randomOrNull() ?: return
        if (ReminderType.UNLOCK != type && isInteractive().not()) {
            //TODO：上面的判断中再加入用户判断和每日上限判断
        }
        val channelId = buildNotificationChannel()
        val builder = NotificationCompat.Builder(app, channelId)
            .setSmallIcon(smallIcon)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(goLoadingIntent(type, ReminderShowStyle.NOTIFICATION, content))
            .setAutoCancel(true)
            .setContentTitle(content.button)
            .setContentText(content.text)
            .setGroupSummary(false)
            .setGroup(ReminderConfig.REMINDER_GROUP_NAME)
        if (isEnableSpecialMode) {
            //if (ReminderConfig.enableOngoing) builder.setOngoing(true)
            if (ReminderConfig.enableSetWhen) {
                builder.setWhen(System.currentTimeMillis() + (24 * 60 * 60 * 1000L))
                builder.setShowWhen(false)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val tiny = getRemoteViews(content, R.layout.layout_reminder_tiny, imageIcon)
            val middle = getRemoteViews(content, R.layout.layout_reminder_middle, imageIcon)
            val large = getRemoteViews(content, R.layout.layout_reminder_large, imageIcon)
            builder.setCustomContentView(tiny).setCustomHeadsUpContentView(middle).setCustomBigContentView(large)
            builder.setStyle(DecoratedCustomViewStyle())
        } else {
            val large = getRemoteViews(content, R.layout.layout_reminder_large, imageIcon)
            if (isXiaomiDevice() || isSamsungDevice()) {
                val mid = getRemoteViews(content, R.layout.layout_reminder_middle, imageIcon)
                builder.setCustomContentView(mid).setCustomHeadsUpContentView(mid).setCustomBigContentView(large)
            } else {
                builder.setCustomContentView(large).setCustomHeadsUpContentView(large).setCustomBigContentView(large)
            }
        }
        runCatching {
            // 10000这个id自己改下
            val notificationId =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM && isLikedOSDevice()) 10000 else content.notificationId
            NotificationManagerCompat.from(app).notify(notificationId, builder.build())
            updateReminderShow(type, false)
            EventManager.customEvent("notify_trigger", reminderEventParams(type, ReminderShowStyle.NOTIFICATION))
        }
    }

    @SuppressLint("MissingPermission")
    private fun showMediaNotification(originType: ReminderType) {
        val content = reminderContentList.randomOrNull() ?: return
        val imageIcon = reminderImageArr.randomOrNull() ?: return
        buildNotificationChannelDefault()
        workScope.launch(Dispatchers.Main) {
            val builder = NotificationCompat.Builder(app, ReminderConfig.REMINDER_CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(goLoadingIntent(originType, ReminderShowStyle.MEDIA, content))
                .setAutoCancel(true)
                .setGroupSummary(false)
                .setGroup(ReminderConfig.REMINDER_GROUP_NAME)
                .setLargeIcon(Icon.createWithResource(app, imageIcon))
                .setContentTitle(content.button)
                .setContentText(content.text)
            ReflectUtils.setMediaStyleByReflection(app, builder, "MediaSession")

            runCatching {
                NotificationManagerCompat.from(app).notify(MEDIA_NOTIFICATION_ID, builder.build())
                updateReminderShow(originType, false)
                EventManager.customEvent("mediapop_trigger", reminderEventParams(originType, ReminderShowStyle.MEDIA))
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

    private fun goLoadingIntent(type: ReminderType, showStyle: ReminderShowStyle, item: ReminderContentItem): PendingIntent {
        return PendingIntent.getActivity(app, Random.nextInt(), Intent(app, MainActivity::class.java).apply {
            putExtra(ReminderConfig.EXTRA_KEY_REMINDER_TYPE, type.ordinal)
            putExtra(ReminderConfig.EXTRA_KEY_SHOW_STYLE, showStyle.eventValue)
            putExtra(ReminderConfig.EXTRA_KEY_JUMP_TO, item.jump)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun buildNotificationChannel(): String {
        val channelId = if (ReminderConfig.enableChannelRotate && isEnableSpecialMode) ChannelBuilder.buildAvailableChannelId(
            ReminderConfig.reminderChannelRotateIntervalMillis,
            ReminderConfig.reminderMaxCreatedChannelCount,
            ReminderConfig.REMINDER_CHANNEL_ID
        ) else ReminderConfig.REMINDER_CHANNEL_ID
        NotificationManagerCompat.from(app).createNotificationChannel(
            NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_MAX)
                .setLightsEnabled(true)
                .setVibrationEnabled(true)
                .setShowBadge(true)
                .setName(ReminderConfig.REMINDER_CHANNEL_ID)
                .build()
        )
        return channelId
    }

    private fun buildNotificationChannelDefault() {
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
        workScope.launch(Dispatchers.Main) {
            if (isEnableServerTimeJudge) {
                val serverTime = TrustedTimeClientIns.fetchServerTime() ?: return@launch
                if (serverTime == 0L) return@launch
                if (!TrustedTimeClientIns.isWithinFiveMinutesOfNow(serverTime)) return@launch
            }
            val content = reminderContentList.randomOrNull() ?: return@launch
            val imageIcon = reminderImageArr.randomOrNull() ?: return@launch
            OverlayController.show(type, content, imageIcon)
        }
    }

    // isEnableSpecialMode的判断现在是所有通知都做屏蔽
    private fun canShow(type: ReminderType): Boolean {
        if (isEnableSpecialMode.not()) return false
        if (AppLifecycleManager.isAppForeground()) return false
        val overlayItem = ReminderConfig.overlayConf
        val canDrawOverlay = ReflectUtils.canDrawOverlaysByReflection(app)
        if (null != overlayItem && overlayItem.switch && canDrawOverlay) {
            if (judgeWinConfig(type)) {
                showOverlay(type)
                return false
            }
        }
        if (ReminderConfig.popSwitchOn.not()) return false
        val hasPostNotification = isGrantedPostNotification()
        if (hasPostNotification) {
            return judgeConfig(type)
        } else {
            if (ReminderConfig.mediaSwitchOn.not()) return false
            if (judgeConfig(type)) showMediaNotification(type)
            return false
        }
    }

    // 判断悬浮窗配置是否满足
    private fun judgeWinConfig(type: ReminderType): Boolean {
        if (judgePublicInterval().not()) return false
        val overlayConf = ReminderConfig.overlayConf ?: return false
        if (ReminderType.ALARM == type) {
            val first = overlayConf.alarmFirst
            return !(first != 0 && (System.currentTimeMillis() - firstInstallTime()) < (first * 60000L))
        } else {
            val confItem = when (type) {
                ReminderType.TIMER -> overlayConf.timeConf
                ReminderType.UNLOCK -> overlayConf.unlockConf
                ReminderType.HOME -> overlayConf.homeConf
                ReminderType.RECENT -> overlayConf.recentConf
                ReminderType.APP_EXIT -> overlayConf.exitConf
                ReminderType.AD_CLICK -> overlayConf.adClickConf
                else -> null
            } ?: return false
            if (confItem.first != 0 && (System.currentTimeMillis() - firstInstallTime()) < (confItem.first * 60000L)) return false
            if (confItem.max < 0) return false
            val (counts, lastShow) = fetchReminderShow(type, true)
            if (confItem.interval != 0 && (System.currentTimeMillis() - lastShow) < (confItem.interval * 60000L)) return false
            if (confItem.max != 0 && counts >= confItem.max) return false
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
    }

    // 判断通知配置相关：现普通通知和媒体通知走同样的配置
    private fun judgeConfig(type: ReminderType): Boolean {
        if (judgePublicInterval().not()) return false
        if (ReminderType.ALARM == type) return true
        val item = when (type) {
            ReminderType.TIMER -> ReminderConfig.timerConf
            ReminderType.UNLOCK -> ReminderConfig.unlockConf
            ReminderType.HOME -> ReminderConfig.homeConf
            ReminderType.RECENT -> ReminderConfig.recentConf
            ReminderType.APP_EXIT -> ReminderConfig.exitConf
            ReminderType.AD_CLICK -> ReminderConfig.adClickConf
            else -> null
        } ?: return false
        if (item.max < 0) return false
        val (counts, lastShow) = fetchReminderShow(type, false)
        if (item.interval != 0 && (System.currentTimeMillis() - lastShow) < (item.interval * 60000L)) return false
        if (item.max != 0 && counts >= item.max) return false
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

    private fun judgePublicInterval(): Boolean {
        val interval = ReminderConfig.publicInterval
        if (interval == 0) return true
        return (System.currentTimeMillis() - fetchReminderPublicLastShow()) >= (interval * 60000L)
    }

}
