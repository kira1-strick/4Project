
package com.mlbb.bot

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }

        val title = TextView(this).apply {
            text = "MLBB AUTO PLAYER"
            textSize = 25f
        }

        val description = TextView(this).apply {
            text = "Floating Assistant v0.1.0"
            textSize = 15f
        }

        val button = Button(this).apply {
            text = "ENABLE FLOATING BUBBLE"

            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    startOverlay()
                }
            }
        }

        layout.addView(title)
        layout.addView(description)
        layout.addView(button)

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()

        if (Settings.canDrawOverlays(this)) {
            // Permission available.
        }
    }

    private fun startOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        startForegroundService(intent)
    }
}
