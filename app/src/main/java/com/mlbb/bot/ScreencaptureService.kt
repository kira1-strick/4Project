package com.mlbb.bot

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager

class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCapture"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"

        const val ACTION_STOP = "com.mlbb.bot.action.STOP_CAPTURE"

        private const val CHANNEL_ID = "capture_channel"
        private const val NOTIF_ID = 2001

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var width = 0
    private var height = 0
    private var densityDpi = 0

    private val handler = Handler(Looper.getMainLooper())

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.d(TAG, "MediaProjection stopped by system/user")
            cleanup()
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            cleanup()
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_DATA)
        }

        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.e(TAG, "Missing permission result, stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        startProjection(resultCode, data)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        cleanup()
    }

    // ---------- Projection ----------

    private fun startProjection(resultCode: Int, data: Intent) {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager

        val projection = mpm.getMediaProjection(resultCode, data)
        if (projection == null) {
            Log.e(TAG, "MediaProjection null")
            stopSelf()
            return
        }
        mediaProjection = projection
        projection.registerCallback(projectionCallback, handler)

        val metrics = DisplayMetrics()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
            densityDpi = resources.displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            width = metrics.widthPixels
            height = metrics.heightPixels
            densityDpi = metrics.densityDpi
        }

        imageReader = ImageReader.newInstance(
            width, height, PixelFormat.RGBA_8888, 2
        ).apply {
            setOnImageAvailableListener({ reader -> onFrame(reader) }, handler)
        }

        virtualDisplay = projection.createVirtualDisplay(
            "MLBB-Capture",
            width, height, densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null, handler
        )

        Log.d(TAG, "Capture started ${width}x$height")
    }

    private fun onFrame(reader: ImageReader) {
        val image: Image? = reader.acquireLatestImage()
        if (image == null) return
        try {
            // v0.2.0: hanya log ukuran frame. Analyzer di tahap berikutnya.
            Log.d(TAG, "Frame ${image.width}x${image.height}")
            // TODO(v0.3.0): FrameAnalyzer.analyze(image)
        } finally {
            image.close()
        }
    }

    private fun cleanup() {
        virtualDisplay?.release(); virtualDisplay = null
        imageReader?.close();     imageReader = null
        mediaProjection?.stop();  mediaProjection = null
    }

    // ---------- Notification ----------

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
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
            .setContentTitle("Screen Capture Active")
            .setContentText("MLBB Auto Player sedang membaca layar")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }
}
