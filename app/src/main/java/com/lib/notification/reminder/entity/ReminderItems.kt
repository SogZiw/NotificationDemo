package com.lib.notification.reminder.entity

data class ReminderConfItem(
    val interval: Int,
    val max: Int
)

data class ReminderContentItem(
    val text: String,
    val button: String,
    val jump: Int,
    val notificationId: Int
)

data class ToolbarConfItem(
    val imageId: Int,
    val text: Int,
    val jump: Int,
)

data class OverlayConfItem(
    val switch: Boolean,
    val rate: Int,
    val timeInterval: Int,
    val timeMax: Int,
    val unlockInterval: Int,
    val unlockMax: Int
)

enum class ReminderType {
    TIMER,
    UNLOCK,
    ALARM,
    MEDIA,
}