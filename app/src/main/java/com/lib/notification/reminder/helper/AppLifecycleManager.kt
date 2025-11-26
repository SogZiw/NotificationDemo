package com.lib.notification.reminder.helper

import android.app.Activity
import android.app.Application
import android.os.Bundle

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
            override fun onActivityResumed(activity: Activity) = Unit
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

}