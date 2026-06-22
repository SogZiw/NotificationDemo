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
    val alarmFirst: Int,
    var timeConf: ReminderConfItem? = null,
    var unlockConf: ReminderConfItem? = null,
    var homeConf: ReminderConfItem? = null,
    var recentConf: ReminderConfItem? = null,
    var exitConf: ReminderConfItem? = null,
    var adClickConf: ReminderConfItem? = null,
)

enum class ReminderType(val typeTag: String) {
    TIMER("time"),
    UNLOCK("unlock"),
    ALARM("alarm"),
    HOME("home"),
    RECENT("recent"),
    APP_EXIT("appexit"),
    AD_CLICK("adclick")
}

enum class ReminderShowStyle(val eventValue: String) {
    NOTIFICATION("notification"),
    MEDIA("media"),
    OVERLAY("overlay")
}