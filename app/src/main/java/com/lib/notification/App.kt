package com.lib.notification

import android.app.Application
import com.lib.notification.reminder.ReminderConfig
import com.lib.notification.reminder.helper.AppLifecycleManager
import com.lib.notification.reminder.helper.ReminderWorker

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        ReminderConfig.app = this
        AppLifecycleManager.init(this)
        ReminderWorker.init()
        ReminderConfig.showToolbar(this)
        initTestData()
    }

    private fun initTestData() {
        ReminderConfig.formatPopConf(
            """
            {
              "fl_pop_start":23,
              "fl_pop_end":6,
              "fl_on": 1,
              "fl_t": 2,
              "fl_t_limit": 30,
              "fl_u": 0,
              "fl_u_limit":30
            }
        """.trimIndent()
        )
        ReminderConfig.formatContent(
            """
            [
                {
                    "jump": 1,
                    "text": "Secret file leaked!",
                    "button": "View now",
                    "notifi_id":31231
                },
                {
                    "jump": 2,
                    "text": "Warning: Files unsecured!",
                    "button": "Protect",
                    "notifi_id":31232
                },
                {
                    "jump": 3,
                    "text": "Hidden trick inside!",
                    "button": "Reveal",
                    "notifi_id":31233
                },
                {
                    "jump": 4,
                    "text": "Limited access available!",
                    "button": "Claim",
                    "notifi_id":31234
                },
                {
                    "jump": 5,
                    "text": "VIP feature unlocked!",
                    "button": "Use now",
                    "notifi_id":31235
                },
                {
                    "jump": 6,
                    "text": "Urgent action required!",
                    "button": "Fix now",
                    "notifi_id":31236
                }
            ]
        """.trimIndent()
        )
    }

}