package com.example.audio_meter

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.os.Build
import android.util.Log

class ServerService : Service() {
    private lateinit var audioRecorder: AudioRecorder
    private val application: MainApplication = MainApplication.getInstance()
    private var serverRunnable: ServerRunner? = null
    private var serverThread: Thread? = null
    private var htmlString: String = ""


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        audioRecorder = AudioRecorder(this)
        Log.d("ServerService", "inside onCreate()")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (intent.hasExtra("html")) {
                htmlString = intent.getStringExtra("html")!!
            }
        }
        when (intent?.action) {
            "start" -> {
                initService()
                startSubServices()
            }

            "refresh" -> startSubServices()
            "stop" -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWifiServer()
    }

    private fun initService() {
        val notification = getNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } else {
            startForeground(1, notification)
        }
    }

    private fun getNotification(): Notification {
        val recordToggleIntent = Intent("actionData")
        recordToggleIntent.action = "toggleRecord"
        val recordTogglePendingIntent = PendingIntent.getBroadcast(
            this, 0, recordToggleIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val wifiToggleIntent = Intent("actionData")
        wifiToggleIntent.action = "toggleWifi"
        val wifiTogglePendingIntent =
            PendingIntent.getBroadcast(
                this, 0, wifiToggleIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

        val recorderString = if (application.recordingOn) {
            "Record running"
        } else {
            "Record stopped"
        }
        val serverString = if (application.wifiOn) {
            "Server running"
        } else {
            "Server stopped"
        }
        val combinedString = recorderString + "\n" + serverString
        return NotificationCompat.Builder(this, "server_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Audio Flow Service")
            .setContentText(combinedString)
            .setOngoing(true)
            .addAction(
                androidx.appcompat.R.drawable.abc_btn_radio_material,
                "stop record",
                recordTogglePendingIntent
            ).addAction(
                androidx.appcompat.R.drawable.abc_text_select_handle_middle_mtrl,
                "stop wifi",
                wifiTogglePendingIntent
            )
            .build()
    }

    private fun startSubServices() {
        Log.d("ServerService", "executing startSubServices")
        if (application.wifiOn) {
            Log.d("ServerService", "executing startSubServices, startWifi")
            startWifiServer()
        } else {
            Log.d("ServerService", "executing startSubServices, stopWifi")
            stopWifiServer()
        }
        if (application.recordingOn) {
            audioRecorder.startRecordingThread()
        }
    }

    private fun startWifiServer() {
        if (serverThread == null) {
            serverRunnable = ServerRunner(htmlString)
            serverThread = Thread(serverRunnable)
            serverThread?.start()
        }
    }

    private fun stopWifiServer() {
        serverRunnable?.stopServer()
        serverThread?.join()
        serverRunnable = null
        serverThread = null
        Log.d("ServerService", "old thread finished, everything nulled")
    }
}