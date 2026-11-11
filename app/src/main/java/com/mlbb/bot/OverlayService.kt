package com.mlbb.bot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {

    companion object {
        const val ACTION_STOP = "com.mlbb.bot.action.STOP_OVERLAY"
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIF_ID = 1001

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var expandedView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        isRunning = true
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        expandedView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
        expandedView = null
    }

    // ---------- Notification ----------

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Floating Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Overlay assistant status" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL_ID)
        else
            @Suppress("DEPRECATION") Notification.Builder(this)

        return builder
            .setContentTitle("MLBB Auto Player")
            .setContentText("Floating assistant active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    // ---------- Floating bubble ----------

    private fun showBubble() {
        val view = LayoutInflater.from(this).inflate(
            android.R.layout.simple_list_item_1, null
        ) as TextView
        view.text = "🎮"
        view.textSize = 22f
        view.setBackgroundColor(0xCC000000.toInt())
        view.setTextColor(0xFFFFFFFF.toInt())
        view.setPadding(30, 30, 30, 30)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 60
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var touchX = 0
        var touchY = 0

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX.toInt()
                    touchY = event.rawY.toInt()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX.toInt() - touchX)
                    params.y = initialY + (event.rawY.toInt() - touchY)
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Kalau tidak banyak bergerak → anggap tap → toggle expand
                    if (kotlin.math.abs(event.rawX - touchX) < 15 &&
                        kotlin.math.abs(event.rawY - touchY) < 15
                    ) toggleExpanded()
                    true
                }
                else -> false
            }
        }

        bubbleView = view
        windowManager.addView(view, params)
    }

    private fun toggleExpanded() {
        if (expandedView != null) {
            runCatching { windowManager.removeView(expandedView) }
            expandedView = null
            return
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEE1E1E1E.toInt())
            setPadding(40, 40, 40, 40)
        }

        val label = TextView(this).apply {
            text = "MLBB Assistant"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            setPadding(0, 0, 0, 20)
        }

        val stopBtn = Button(this).apply {
            text = "STOP"
            setOnClickListener {
                stopSelf()
            }
        }

        layout.addView(label)
        layout.addView(stopBtn)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        expandedView = layout
        windowManager.addView(layout, params)
    }
}
