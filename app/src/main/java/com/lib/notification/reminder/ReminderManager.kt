package com.lib.notification.reminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.text.Html
import android.widget.RemoteViews
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DecoratedCustomViewStyle
import androidx.core.app.NotificationManagerCompat
import com.lib.notification.MainActivity
import com.lib.notification.R
import com.lib.notification.reminder.ReminderConfig.app
import com.lib.notification.reminder.ReminderConfig.reminderContentList
import com.lib.notification.reminder.ReminderConfig.reminderImageArr
import com.lib.notification.reminder.ReminderConfig.smallIcon
import com.lib.notification.reminder.entity.ReminderContentItem
import com.lib.notification.reminder.entity.ReminderType
import com.lib.notification.reminder.helper.AppLifecycleManager
import com.lib.notification.reminder.utils.getCurrentCounts
import com.lib.notification.reminder.utils.isGrantedPostNotification
import com.lib.notification.reminder.utils.isInteractive
import com.lib.notification.reminder.utils.reminderTimerLastShow
import com.lib.notification.reminder.utils.reminderUnlockLastShow
import com.lib.notification.reminder.utils.updateCurrentCounts
import java.util.Calendar
import kotlin.random.Random

object ReminderManager {

    @SuppressLint("MissingPermission")
    fun show(type: ReminderType) {
        if (canShow(type).not()) return
        val content = reminderContentList.randomOrNull() ?: return
        val imageIcon = reminderImageArr.randomOrNull() ?: return
        // TODO 触发打点
        buildNotificationChannel()
        val builder = NotificationCompat.Builder(app, ReminderConfig.REMINDER_CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(goLoadingIntent(type, content))
            .setAutoCancel(true)
            .setGroupSummary(false)
            .setGroup(ReminderConfig.REMINDER_GROUP_NAME)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val tiny = getRemoteViews(content, R.layout.layout_reminder_tiny, imageIcon)
            val large = getRemoteViews(content, R.layout.layout_reminder_large, imageIcon)
            builder.setCustomContentView(tiny).setCustomHeadsUpContentView(tiny).setCustomBigContentView(large)
            builder.setStyle(DecoratedCustomViewStyle())
        } else {
            val mid = getRemoteViews(content, R.layout.layout_reminder_middle, imageIcon)
            val large = getRemoteViews(content, R.layout.layout_reminder_large, imageIcon)
            builder.setCustomContentView(mid).setCustomHeadsUpContentView(mid).setCustomBigContentView(large)
        }
        runCatching {
            NotificationManagerCompat.from(app).notify(content.notificationId, builder.build())
            if (ReminderType.TIMER == type) reminderTimerLastShow = System.currentTimeMillis() else reminderUnlockLastShow = System.currentTimeMillis()
            updateCurrentCounts(type)
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

    private fun canShow(type: ReminderType): Boolean {
        if (AppLifecycleManager.isAppForeground()) return false
        if (ReminderConfig.popSwitchOn.not()) return false
        if (isGrantedPostNotification().not()) return false
        val item = (if (ReminderType.TIMER == type) ReminderConfig.timerConf else ReminderConfig.unlockConf) ?: return false
        val lastShow = if (ReminderType.TIMER == type) reminderTimerLastShow else reminderUnlockLastShow
        if (item.interval != 0 && (System.currentTimeMillis() - lastShow) < (item.interval * 60000L)) return false
        if (item.max != 0 && getCurrentCounts(type) >= item.max) return false
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