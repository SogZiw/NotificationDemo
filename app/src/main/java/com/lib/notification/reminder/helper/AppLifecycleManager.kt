package com.lib.notification.reminder.helper

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import com.lib.notification.MainActivity
import com.lib.notification.reminder.utils.isEnableSpecialMode

object AppLifecycleManager {

    private val activityList = mutableSetOf<Activity>()

    private var foregroundCounts = 0

    fun isAppForeground() = foregroundCounts > 0

    fun init(context: Application) {
        context.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
                synchronized(activityList) { activityList.add(activity) }
            }

            override fun onActivityDestroyed(activity: Activity) {
                synchronized(activityList) { activityList.remove(activity) }
            }

            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) {
                // todo: MainActivity改成广告的activity 或者 非basectivity
                if (activity !is MainActivity) {
                    // 需要cloak+referrer买量用户+admob审核屏蔽
                    if (isEnableSpecialMode) {
                        hideSystemBars(activity)
                    }
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) = Unit

            override fun onActivityStarted(activity: Activity) {
                foregroundCounts++
            }

            override fun onActivityStopped(activity: Activity) {
                foregroundCounts--
            }
        })
    }

    private val adActivityTag = arrayOf("AdActivity", "AppLovin")

    fun finishAdActivity() {
        runCatching {
            activityList.toMutableList().forEach { act ->
                val actClzName = act::class.java.name
                if (adActivityTag.any { actClzName.contains(it) }) {
                    act.finish()
                }
            }
        }
    }

    fun hideSystemBars(activity: Activity) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = activity.window.insetsController ?: return
                controller.hide(WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                activity.window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

}