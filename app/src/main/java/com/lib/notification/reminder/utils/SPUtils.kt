package com.lib.notification.reminder.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.lib.notification.reminder.ReminderConfig.app
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

var deviceFirstCountryCode by SpString()
var reminderDailyTime by SpLong(0L)
var reminderTimerCounts by SPInt(0)
var reminderUnlockCounts by SPInt(0)
var reminderTimerLastShow by SpLong(0L)
var reminderUnlockLastShow by SpLong(0L)

val sharedPreferences: SharedPreferences by lazy { app.getSharedPreferences("default_prefs", Context.MODE_PRIVATE) }

class SPInt(private val default: Int) : ReadWriteProperty<Any?, Int> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getInt(property.name, default)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        sharedPreferences.edit(commit = true) { putInt(property.name, value) }
    }
}

class SpLong(private val default: Long) : ReadWriteProperty<Any?, Long> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getLong(property.name, default)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
        sharedPreferences.edit(commit = true) { putLong(property.name, value) }
    }
}

class SpString(private val default: String? = null) : ReadWriteProperty<Any?, String> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getString(property.name, default) ?: default ?: ""
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        sharedPreferences.edit(commit = true) { putString(property.name, value) }
    }
}