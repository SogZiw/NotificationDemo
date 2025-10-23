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

enum class ReminderType {
    TIMER,
    UNLOCK,
    ALARM
}