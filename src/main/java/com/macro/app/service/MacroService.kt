package com.macro.app.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.macro.app.R
import com.macro.app.capture.ScreenCapture
import kotlin.math.abs

class MacroService : AccessibilityService() {

    companion object {
        var instance: MacroService? = null
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "macro_service_channel"
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var statusText: TextView? = null
    private var screenCapture: ScreenCapture? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        startForegroundService()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFloatingWidget()
        instance = null
        stopForeground(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "매크로 서비스",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "매크로 앱이 실행 중입니다"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("매크로 서비스 실행 중")
            .setContentText("화면 캡처 준비됨")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun setScreenCapture(capture: ScreenCapture) {
        screenCapture = capture

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("매크로 서비스 실행 중")
            .setContentText("화면 캡처 활성화됨 ✅")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun getScreenCapture(): ScreenCapture? {
        return screenCapture
    }

    fun showFloatingWidget(message: String) {
        if (floatingView != null) {
            updateFloatingWidget(message)
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutId = resources.getIdentifier(
            "floating_status_widget",
            "layout",
            packageName
        )

        floatingView = if (layoutId != 0) {
            LayoutInflater.from(this).inflate(layoutId, null)
        } else {
            TextView(this).apply {
                text = message
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xCC000000.toInt())
                setPadding(24, 12, 24, 12)
                textSize = 14f
            }
        }

        statusText = floatingView?.findViewById(
            resources.getIdentifier("floating_status_text", "id", packageName)
        ) ?: (floatingView as? TextView)

        statusText?.text = message

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 100
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var lastClickTime = 0L

        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (initialTouchX - event.rawX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = abs(event.rawX - initialTouchX)
                    val deltaY = abs(event.rawY - initialTouchY)

                    if (deltaX < 10 && deltaY < 10) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < 300) {
                            showFloatingMenu()
                        }
                        lastClickTime = currentTime
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showFloatingMenu() {
        val menuItems = arrayOf("정지", "숨기기")

        android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("매크로 제어")
            .setItems(menuItems) { _, which ->
                when (which) {
                    0 -> {
                        com.macro.app.MainActivity.instance?.runOnUiThread {
                            val stopButton = com.macro.app.MainActivity.instance?.findViewById<android.widget.Button>(
                                R.id.btnStop
                            )
                            stopButton?.performClick()
                        }
                    }
                    1 -> hideFloatingWidget()
                }
            }
            .setNegativeButton("취소", null)
            .create()
            .apply {
                window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
            }
            .show()
    }

    fun updateFloatingWidget(message: String) {
        statusText?.text = message
    }

    fun hideFloatingWidget() {
        floatingView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        floatingView = null
        statusText = null
    }
}