package com.mlbb.bot

import android.app.*
import android.os.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import android.content.Context

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var dashboard: LinearLayout? = null
    private var status: TextView? = null

    private val channelId = "mlbb_overlay"

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onBind(intent: android.content.Intent?) = null

    override fun onCreate() {
        super.onCreate()

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotification()

        startForeground(
            1,
            Notification.Builder(this, channelId)
                .setContentTitle("MLBB Auto Player")
                .setContentText("Floating assistant aktif")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        )

        createBubble()
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId,
                "Floating Assistant",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun params(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(20)
            y = dp(250)
        }
    }

    private fun createBubble() {
        val view = TextView(this).apply {
            text = "F"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER

            background = GradientDrawable().apply {
                setColor(Color.rgb(35, 190, 130))
                shape = GradientDrawable.OVAL
            }
        }

        val lp = params(dp(58), dp(58))

        view.setOnTouchListener(object : View.OnTouchListener {

            var startX = 0
            var startY = 0
            var touchX = 0f
            var touchY = 0f
            var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {

                    MotionEvent.ACTION_DOWN -> {
                        startX = lp.x
                        startY = lp.y
                        touchX = event.rawX
                        touchY = event.rawY
                        moved = false
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - touchX
                        val dy = event.rawY - touchY

                        if (kotlin.math.abs(dx) > dp(5) ||
                            kotlin.math.abs(dy) > dp(5)) {
                            moved = true
                        }

                        lp.x = startX + dx.toInt()
                        lp.y = startY + dy.toInt()

                        wm.updateViewLayout(view, lp)
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!moved) toggleDashboard()
                        return true
                    }
                }

                return false
            }
        })

        bubble = view
        wm.addView(view, lp)
    }

    private fun toggleDashboard() {
        if (dashboard != null) {
            wm.removeView(dashboard)
            dashboard = null
        } else {
            showDashboard()
        }
    }

    private fun showDashboard() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))

            background = GradientDrawable().apply {
                setColor(Color.rgb(20, 22, 29))
                cornerRadius = dp(18).toFloat()
            }
        }

        val title = TextView(this).apply {
            text = "MLBB AUTO PLAYER"
            textSize = 18f
            setTextColor(Color.WHITE)
        }

        status = TextView(this).apply {
            text = "Status: READY"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(15), 0, dp(15))
        }

        val start = Button(this).apply {
            text = "START"
            setOnClickListener {
                status?.text = "Status: ASSISTANT ACTIVE"
            }
        }

        val stop = Button(this).apply {
            text = "STOP"
            setOnClickListener {
                status?.text = "Status: STOPPED"
            }
        }

        val close = Button(this).apply {
            text = "CLOSE PANEL"
            setOnClickListener {
                toggleDashboard()
            }
        }

        panel.addView(title)
        panel.addView(status)
        panel.addView(start)
        panel.addView(stop)
        panel.addView(close)

        dashboard = panel

        wm.addView(
            panel,
            params(dp(280), WindowManager.LayoutParams.WRAP_CONTENT)
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        bubble?.let {
            wm.removeView(it)
        }

        dashboard?.let {
            wm.removeView(it)
        }

        bubble = null
        dashboard = null
    }
}
