package com.lib.notification.reminder.helper

import android.app.Activity
import android.app.Application
import android.os.Bundle

object AppLifecycleManager {

    private var foregroundCounts = 0

    fun isAppForeground() = foregroundCounts > 0

    fun init(context: Application) {
        context.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, bundle: Bundle?) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
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

}