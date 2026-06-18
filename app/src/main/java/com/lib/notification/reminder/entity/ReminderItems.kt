package com.lib.notification.reminder.entity

data class ReminderConfItem(
    val first: Int = 0,
    val interval: Int,
    val max: Int,
    val delay: Int = 0
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
    val unlockMax: Int,
    val timeFirst: Int = 0,
    val unlockFirst: Int = 0,
    val alarmFirst: Int = 0
)

enum class ReminderType(val typeTag: String) {
    TIMER("time"),
    UNLOCK("unlock"),
    ALARM("alarm"),
    MEDIA("media"),
    HOME("home"),
    RECENT("recent"),
    APP_EXIT("appexit"),
    AD_CLICK("adclick")
}