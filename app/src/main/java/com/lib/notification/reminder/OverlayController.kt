package com.lib.notification.reminder

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.text.Html
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import com.lib.notification.MainActivity
import com.lib.notification.databinding.LayoutOverlayBinding
import com.lib.notification.event.EventManager
import com.lib.notification.reminder.ReminderConfig.app
import com.lib.notification.reminder.entity.ReminderContentItem
import com.lib.notification.reminder.entity.ReminderType
import com.lib.notification.reminder.utils.isFirstMistouch
import com.lib.notification.reminder.utils.reminderTimerWinLastShow
import com.lib.notification.reminder.utils.reminderUnlockWinLastShow
import com.lib.notification.reminder.utils.updateCurrentCounts
import kotlin.random.Random

@SuppressLint("StaticFieldLeak")
object OverlayController {

    private var overlayView: View? = null
    private val marginOffset by lazy { (app.resources.displayMetrics.density * 20).toInt() }
    private var isNeedMistouch = false

    fun show(type: ReminderType, content: ReminderContentItem, imageIcon: Int) {
        if (overlayView != null) return
        when (type) {
            ReminderType.TIMER -> reminderTimerWinLastShow = System.currentTimeMillis()
            ReminderType.UNLOCK -> reminderUnlockWinLastShow = System.currentTimeMillis()
            else -> Unit
        }
        updateCurrentCounts(type, true)
        isNeedMistouch = Random.nextInt(0, 101) <= (ReminderConfig.overlayConf?.rate ?: 50)
        if (isFirstMistouch || isNeedMistouch) {
            // 广告预加载
        }
        val viewBinding by lazy { LayoutOverlayBinding.inflate(LayoutInflater.from(app)) }
        overlayView = viewBinding.root
        ReflectUtils.addViewByReflection(app, overlayView, marginOffset)
        viewBinding.imageReminder.setImageResource(imageIcon)
        viewBinding.textContent.text = Html.fromHtml(content.text)
        viewBinding.textButton.text = Html.fromHtml(content.button)
        viewBinding.root.setOnClickListener {
            dismissOverlayView()
            goLoadingIntent(type, content)
        }
        viewBinding.actionClose.setOnClickListener {
            if (isFirstMistouch) {
                isFirstMistouch = false
                dismissOverlayView()
                goLoadingIntent(type, content)
                return@setOnClickListener
            }
            if (isNeedMistouch) {
                isNeedMistouch = false
                dismissOverlayView()
                goLoadingIntent(type, content)
                return@setOnClickListener
            }
            dismissOverlayView()
        }
        startScaleAnimation(viewBinding.textButton)
        EventManager.customEvent(
            "winpop_show", hashMapOf(
                "from_type" to when (type) {
                    ReminderType.TIMER -> "time"
                    ReminderType.UNLOCK -> "unlock"
                    ReminderType.ALARM -> "alarm"
                    else -> ""
                }
            )
        )
    }

    private fun goLoadingIntent(type: ReminderType, item: ReminderContentItem) {
        EventManager.customEvent(
            "winpop_click", hashMapOf(
                "from_type" to when (type) {
                    ReminderType.TIMER -> "time"
                    ReminderType.UNLOCK -> "unlock"
                    ReminderType.ALARM -> "alarm"
                    else -> ""
                }
            )
        )
        app.startActivity(Intent(app, MainActivity::class.java).apply {
            putExtra(ReminderConfig.EXTRA_KEY_JUMP_TO, item.jump)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }

    private fun dismissOverlayView() {
        val view = overlayView ?: return
        ReflectUtils.removeViewByReflection(app, view)
        overlayView = null
    }

    private fun startScaleAnimation(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.1f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.1f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 1000L
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }


    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val width = app.resources.displayMetrics.widthPixels - marginOffset
        return WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = marginOffset
        }
    }


}