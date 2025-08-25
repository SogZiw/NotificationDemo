package com.lib.notification.reminder

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DecoratedCustomViewStyle
import androidx.core.app.NotificationManagerCompat
import com.lib.notification.MainActivity
import com.lib.notification.R
import com.lib.notification.reminder.ReminderConfig.app
import com.lib.notification.reminder.ReminderConfig.smallIcon
import com.lib.notification.reminder.entity.ToolbarConfItem
import kotlin.random.Random

object ToolbarManager {

    @SuppressLint("MissingPermission")
    fun buildNotification(): Notification {
        buildNotificationChannel()
        val builder = NotificationCompat.Builder(app, ReminderConfig.TOOLBAR_CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSound(null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        val largeRemoteViews = buildRemoteViews(R.layout.layout_toolbar, ReminderConfig.toolbarContent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val smallRemoteViews = buildRemoteViews(R.layout.layout_toolbar_tiny, ReminderConfig.toolbarContent)
            builder.setCustomContentView(smallRemoteViews).setCustomBigContentView(largeRemoteViews).setCustomHeadsUpContentView(largeRemoteViews)
            builder.setStyle(DecoratedCustomViewStyle())
        } else {
            builder.setCustomContentView(largeRemoteViews).setCustomBigContentView(largeRemoteViews).setCustomHeadsUpContentView(largeRemoteViews)
        }
        val notification = builder.build()
        runCatching {
            NotificationManagerCompat.from(app).notify(ReminderConfig.TOOLBAR_NOTIFICATION_ID, notification)
        }
        return notification
    }

    private fun buildRemoteViews(layoutId: Int, content: List<ToolbarConfItem>): RemoteViews {
        return RemoteViews(app.packageName, layoutId).apply {
            setImageViewResource(R.id.image_toolbar_1, content.getOrNull(0)?.imageId ?: 0)
            setImageViewResource(R.id.image_toolbar_2, content.getOrNull(1)?.imageId ?: 0)
            setImageViewResource(R.id.image_toolbar_3, content.getOrNull(2)?.imageId ?: 0)
            setImageViewResource(R.id.image_toolbar_4, content.getOrNull(3)?.imageId ?: 0)

            setTextViewText(R.id.text_toolbar_1, app.getString(content.getOrNull(0)?.text ?: 0))
            setTextViewText(R.id.text_toolbar_2, app.getString(content.getOrNull(1)?.text ?: 0))
            setTextViewText(R.id.text_toolbar_3, app.getString(content.getOrNull(2)?.text ?: 0))
            setTextViewText(R.id.text_toolbar_4, app.getString(content.getOrNull(3)?.text ?: 0))

            setOnClickPendingIntent(R.id.layout_toolbar_1, goLoadingIntent(content.getOrNull(0)?.jump ?: 0))
            setOnClickPendingIntent(R.id.layout_toolbar_2, goLoadingIntent(content.getOrNull(1)?.jump ?: 0))
            setOnClickPendingIntent(R.id.layout_toolbar_3, goLoadingIntent(content.getOrNull(2)?.jump ?: 0))
            setOnClickPendingIntent(R.id.layout_toolbar_4, goLoadingIntent(content.getOrNull(3)?.jump ?: 0))
        }
    }

    private fun goLoadingIntent(jump: Int): PendingIntent {
        return PendingIntent.getActivity(app, Random.nextInt(), Intent(app, MainActivity::class.java).apply {
            putExtra(ReminderConfig.EXTRA_KEY_JUMP_TO, jump)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun buildNotificationChannel() {
        NotificationManagerCompat.from(app).createNotificationChannel(
            NotificationChannelCompat.Builder(ReminderConfig.TOOLBAR_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setSound(null, null)
                .setLightsEnabled(false)
                .setVibrationEnabled(false)
                .setShowBadge(false)
                .setName(ReminderConfig.TOOLBAR_CHANNEL_ID)
                .build()
        )
    }

}