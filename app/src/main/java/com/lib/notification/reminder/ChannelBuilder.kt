package com.lib.notification.reminder

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.lib.notification.reminder.ReminderConfig.app

object ChannelBuilder {

    private const val PREFS_NAME = "reminder_channel_rotation_store"
    private const val KEY_CREATED_CHANNEL_IDS = "reminder_created_channel_ids"
    private const val KEY_LAST_ROTATE_AT = "reminder_last_rotate_at"
    private const val KEY_CURRENT_CHANNEL_ID = "reminder_current_channel_id"
    private const val KEY_NEXT_CHANNEL_SEQUENCE = "reminder_next_channel_sequence"

    private val channelState by lazy {
        app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun buildAvailableChannelId(
        rotateIntervalMillis: Long,
        maxCreatedChannelCount: Int,
        baseChannelId: String,
    ): String {
        val currentChannelId = getCurrentChannelId(baseChannelId)
        if (currentChannelId == baseChannelId && !hasRotateTime(baseChannelId)) {
            recordRotateTime(baseChannelId)
        }
        if (!isNotificationChannelDisabled(currentChannelId)) return currentChannelId
        if (!hasPassedRotateInterval(baseChannelId, rotateIntervalMillis)) return currentChannelId
        if (!hasCreateQuota(baseChannelId, maxCreatedChannelCount)) return currentChannelId

        val nextChannel = buildNextChannel(baseChannelId) ?: return currentChannelId
        deleteNotificationChannel(currentChannelId)
        recordCreatedChannel(baseChannelId, nextChannel)
        return nextChannel.id
    }

    private fun getCurrentChannelId(baseChannelId: String): String {
        val savedChannelId = channelState.getString(currentChannelIdKey(baseChannelId), null).orEmpty()
        return savedChannelId.takeIf { it == baseChannelId || it.isDynamicChannelId(baseChannelId) } ?: baseChannelId
    }

    private fun hasRotateTime(baseChannelId: String): Boolean {
        return channelState.getLong(lastRotateTimeKey(baseChannelId), 0L) > 0L
    }

    private fun isNotificationChannelDisabled(channelId: String): Boolean {
        val channel = notificationManager().getNotificationChannel(channelId)
        return channel?.importance == NotificationManager.IMPORTANCE_NONE
    }

    private fun deleteNotificationChannel(channelId: String) {
        notificationManager().deleteNotificationChannel(channelId)
    }

    private fun hasPassedRotateInterval(baseChannelId: String, rotateIntervalMillis: Long): Boolean {
        val lastRotateAt = channelState.getLong(lastRotateTimeKey(baseChannelId), 0L)
        if (lastRotateAt <= 0L) return true
        return System.currentTimeMillis() - lastRotateAt >= rotateIntervalMillis.coerceAtLeast(0L)
    }

    private fun hasCreateQuota(baseChannelId: String, maxCreatedChannelCount: Int): Boolean {
        if (maxCreatedChannelCount <= 0) return false
        return collectDynamicChannelIds(baseChannelId).size < maxCreatedChannelCount
    }

    private fun buildNextChannel(baseChannelId: String): DynamicChannel? {
        val existingIds = collectKnownChannelIds() + collectRecordedChannelIds(baseChannelId)
        var sequence = channelState.getLong(nextSequenceKey(baseChannelId), 0L) + 1L
        while (sequence > 0L) {
            val candidate = dynamicChannelId(baseChannelId, sequence)
            if (candidate !in existingIds) return DynamicChannel(candidate, sequence)
            if (sequence == Long.MAX_VALUE) return null
            sequence++
        }
        return null
    }

    private fun recordCreatedChannel(baseChannelId: String, channel: DynamicChannel) {
        val createdChannelIds = channelState.getStringSet(createdChannelIdsKey(baseChannelId), emptySet())
            .orEmpty()
            .toMutableSet()
            .apply { add(channel.id) }
        channelState.edit {
            putLong(lastRotateTimeKey(baseChannelId), System.currentTimeMillis())
            putString(currentChannelIdKey(baseChannelId), channel.id)
            putStringSet(createdChannelIdsKey(baseChannelId), createdChannelIds)
            putLong(nextSequenceKey(baseChannelId), channel.sequence)
        }
    }

    private fun recordRotateTime(baseChannelId: String) {
        channelState.edit {
            putLong(lastRotateTimeKey(baseChannelId), System.currentTimeMillis())
        }
    }

    private fun collectDynamicChannelIds(baseChannelId: String): Set<String> {
        return (collectRecordedChannelIds(baseChannelId) + collectKnownChannelIds()).filterTo(mutableSetOf()) {
            it.isDynamicChannelId(baseChannelId)
        }
    }

    private fun collectRecordedChannelIds(baseChannelId: String): Set<String> {
        return channelState.getStringSet(createdChannelIdsKey(baseChannelId), emptySet())
            .orEmpty()
            .toSet()
    }

    private fun collectKnownChannelIds(): Set<String> {
        return notificationManager().notificationChannels.mapTo(mutableSetOf()) { it.id }
    }

    private fun notificationManager(): NotificationManager {
        return app.getSystemService(NotificationManager::class.java)
    }

    private fun dynamicChannelId(baseChannelId: String, sequence: Long): String {
        return "${baseChannelId}_r$sequence"
    }

    private fun String.isDynamicChannelId(baseChannelId: String): Boolean {
        return dynamicSequence(baseChannelId) != null
    }

    private fun String.dynamicSequence(baseChannelId: String): Long? {
        val prefix = "${baseChannelId}_r"
        if (!startsWith(prefix)) return null
        return removePrefix(prefix).toLongOrNull()?.takeIf { it > 0L }
    }

    private fun createdChannelIdsKey(baseChannelId: String): String {
        return "$KEY_CREATED_CHANNEL_IDS:$baseChannelId"
    }

    private fun lastRotateTimeKey(baseChannelId: String): String {
        return "$KEY_LAST_ROTATE_AT:$baseChannelId"
    }

    private fun currentChannelIdKey(baseChannelId: String): String {
        return "$KEY_CURRENT_CHANNEL_ID:$baseChannelId"
    }

    private fun nextSequenceKey(baseChannelId: String): String {
        return "$KEY_NEXT_CHANNEL_SEQUENCE:$baseChannelId"
    }

    private data class DynamicChannel(
        val id: String,
        val sequence: Long,
    )
}
