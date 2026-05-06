package com.lib.notification.event

import android.content.Context
import com.google.android.gms.time.TrustedTime
import com.google.android.gms.time.TrustedTimeClient
import com.lib.notification.reminder.ReminderConfig.app
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs

object TrustedTimeClientIns {
    @Volatile
    private var client: TrustedTimeClient? = null
    private var isSuccess: Boolean = false
    private const val FIVE_MINUTES_MILLIS = 5 * 60 * 1000L

    suspend fun getClient(context: Context): TrustedTimeClient? = client ?: suspendCancellableCoroutine { cont ->
        TrustedTime.createClient(context.applicationContext)
            .addOnCompleteListener { task ->
                if (!cont.isActive) return@addOnCompleteListener
                val result = if (task.isSuccessful) task.result else null
                client = result
                cont.resume(result)
            }
    }

    fun isWithinFiveMinutesOfNow(time: Long): Boolean {
        val now = System.currentTimeMillis()
        return abs(time - now) <= FIVE_MINUTES_MILLIS
    }

    suspend fun fetchServerTime(): Long? {
        if (isSuccess.not()) EventManager.customEvent("time_real_ask")
        val client = getClient(app) ?: return null
        val time = runCatching { client.computeCurrentUnixEpochMillis() }.getOrNull()
        if (time != null && time != 0L && isSuccess.not()) {
            isSuccess = true
            EventManager.customEvent("time_real_suss", hashMapOf("tim" to time))
        }
        return time
    }

}