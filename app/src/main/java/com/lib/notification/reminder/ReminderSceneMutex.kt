package com.lib.notification.reminder

import android.os.SystemClock

internal object ReminderSceneMutex {
    private const val LOCK_WINDOW_MS = 2_000L

    private var lastAcquireAt = 0L

    @Synchronized
    fun tryAcquire(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAcquireAt < LOCK_WINDOW_MS) return false
        lastAcquireAt = now
        return true
    }
}
