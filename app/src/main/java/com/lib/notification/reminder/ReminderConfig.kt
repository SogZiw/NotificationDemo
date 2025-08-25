package com.lib.notification.reminder

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.lib.notification.App
import com.lib.notification.R
import com.lib.notification.reminder.entity.ReminderConfItem
import com.lib.notification.reminder.entity.ReminderContentItem
import com.lib.notification.reminder.entity.ToolbarConfItem
import com.lib.notification.reminder.utils.getCountryCode
import com.lib.notification.reminder.utils.isSamsungDevice
import com.lib.notification.reminder.utils.parseReminderContent
import com.lib.notification.service.DemoToolbarService
import org.json.JSONObject

object ReminderConfig {

    // job service job id > 1000
    const val JOB_INTENT_ID = 10000

    // 常驻通知channel id 以及 notificationId
    const val TOOLBAR_CHANNEL_ID = "Toolbar"
    const val TOOLBAR_NOTIFICATION_ID = 100001

    // 普通通知channel id
    const val REMINDER_CHANNEL_ID = "important_message"

    // 普通通知group name
    const val REMINDER_GROUP_NAME = "important"

    const val EXTRA_KEY_JUMP_TO = "EXTRA_JUMP_TO"
    const val EXTRA_KEY_REMINDER_TYPE = "EXTRA_KEY_REMINDER_TYPE"

    // 打包时改为false
    val isDebugMode by lazy { true }
    lateinit var app: App
    val firstCountryCode by lazy { getCountryCode() }

    // small icon: 纯色
    val smallIcon by lazy { R.drawable.ic_test_small_icon }

    // toolbar 样式设置: 需要配置，默认4个，自行删减，少于4个布局文件需删减
    val toolbarContent by lazy {
        listOf(
            ToolbarConfItem(
                R.drawable.ic_test_toolbar_icon,
                text = R.string.test_name,
                jump = 0
            ),
            ToolbarConfItem(
                R.drawable.ic_test_toolbar_icon,
                text = R.string.test_name,
                jump = 1
            ),
            ToolbarConfItem(
                R.drawable.ic_test_toolbar_icon,
                text = R.string.test_name,
                jump = 2
            ),
            ToolbarConfItem(
                R.drawable.ic_test_toolbar_icon,
                text = R.string.test_name,
                jump = 3
            ),
        )
    }

    // 普通通知icon
    val reminderImageArr by lazy { arrayOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round) }

    var popSwitchOn = false
    var popStartHour = 0
    var popEndHour = 0

    // 定时通知配置
    var timerConf: ReminderConfItem? = null

    // 解锁通知配置
    var unlockConf: ReminderConfItem? = null

    // 下发文案
    var reminderContentList = mutableListOf<ReminderContentItem>()

    // 常驻展示
    fun showToolbar(context: Context) {
        if ("KR" == firstCountryCode && isSamsungDevice()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context is Application) {
            ToolbarManager.buildNotification()
        } else {
            runCatching {
                if (DemoToolbarService.isToolbarRunning) return
                ContextCompat.startForegroundService(context, Intent(context, DemoToolbarService::class.java))
            }
        }
    }

    // 格式化通知配置
    fun formatPopConf(json: String?) {
        if (json.isNullOrBlank()) return
        runCatching {
            JSONObject(json).run {
                popSwitchOn = 1 == optInt("fl_on", 0)
                popStartHour = optInt("fl_pop_start", 0)
                popEndHour = optInt("fl_pop_end", 0)
                timerConf = ReminderConfItem(optInt("fl_t", 30), optInt("fl_t_limit", 10))
                unlockConf = ReminderConfItem(optInt("fl_u", 30), optInt("fl_u_limit", 10))
            }
        }
    }

    // 格式化下发文案
    fun formatContent(json: String?) {
        if (json.isNullOrBlank()) return
        reminderContentList = parseReminderContent(json)
    }
}