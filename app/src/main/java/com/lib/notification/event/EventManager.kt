package com.lib.notification.event

import android.os.Build
import android.util.Log
import androidx.core.content.edit
import com.lib.notification.BuildConfig
import com.lib.notification.reminder.ReminderConfig
import com.lib.notification.reminder.helper.ReminderWorker
import com.lib.notification.reminder.utils.sharedPreferences
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.UUID

object EventManager {

    // tba base url
    private val baseUrl by lazy {
        if (ReminderConfig.isDebugMode) "https://test-pastime.pdfviewerpro.com/ar/freddie/hackney" else "https://pastime.pdfviewerpro.com/duluth/dactylic/gullet"
    }

    // sp存储的distinctId的key
    const val KEY_DISTINCT_ID = "DistinctId"
    private val okHttpClient by lazy { OkHttpClient.Builder().build() }
    private var userDeviceId: String
        get() = sharedPreferences.getString(KEY_DISTINCT_ID, "") ?: ""
        set(value) {
            sharedPreferences.edit(commit = true) { putString(KEY_DISTINCT_ID, value) }
        }
    private val distinctId by lazy { getOrBuildDeviceId() }

    fun customEvent(eventName: String, params: HashMap<String, Any?> = hashMapOf()) {
        val rootObj = buildCommonObj()
        rootObj.put("but", eventName)
        params.forEach { (key, value) ->
            rootObj.put("$key%gauche", value)
        }
        val jsonString = rootObj.toString()
        ReminderWorker.workScope.launch {
            val request = Request.Builder().post(jsonString.toRequestBody("application/json".toMediaTypeOrNull())).url(baseUrl).build()
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (ReminderConfig.isDebugMode) Log.e("Test", "onResponse: ${response.body.string()}")
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (ReminderConfig.isDebugMode) Log.e("Test", "onFailure: ${e.message}")
                }
            })
        }
    }

    private fun buildCommonObj(): JSONObject {
        return JSONObject().also {
            it.put("montana", JSONObject().apply {
                put("holiday", BuildConfig.VERSION_NAME)
                put("rare", distinctId)
                put("drayman", "dump")
                put("becalm", "")
                put("whereof", Build.VERSION.RELEASE ?: "")
                put("heroic", System.currentTimeMillis())
                put("ryder", "")
                put("portent", Build.MANUFACTURER ?: "")
                put("built", ReminderConfig.app.packageName)
                put("abrasion", Locale.getDefault().toString())
                put("dirge", ReminderConfig.firstCountryCode)
                put("asphalt", Build.BRAND ?: "")
                put("adele", Build.MODEL ?: "")
                put("grease", UUID.randomUUID().toString())
            })
        }
    }

    private fun getOrBuildDeviceId(): String {
        return userDeviceId.ifBlank {
            val id = UUID.randomUUID().toString()
            userDeviceId = id
            return@ifBlank id
        }
    }

}