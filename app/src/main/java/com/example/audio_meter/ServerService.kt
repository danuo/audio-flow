package com.example.audio_meter

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
        Log.d("in service", "we made it here to oncreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (intent.hasExtra("html")) {
                htmlString = intent.getStringExtra("html")!!
            }
        }
        when (intent?.action) {
            "start" -> initService()
            "refresh" -> startSubServices()
            "stop" -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWifi()
    }

    private fun initService() {
        val notification = NotificationCompat.Builder(this, "server_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Server Running")
            .setContentText("Server can be stopped in the app")
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } else {
            startForeground(1, notification)
        }
        startSubServices()  // does nothing if wifiOn = false
    }

    private fun startSubServices() {
        Log.d("ServerService", "executing startSubServices")
        if (application.wifiOn) {
            Log.d("ServerService", "executing startSubServices, startWifi")
            startWifi()
        } else {
            Log.d("ServerService", "executing startSubServices, stopWifi")
            stopWifi()
        }
        if (application.recordingOn) {
            audioRecorder.startRecordingThread()
        }
    }

    private fun startWifi() {
        if (serverThread == null) {
            serverRunnable = ServerRunner(htmlString)
            serverThread = Thread(serverRunnable)
            serverThread?.start()
        }
    }

    private fun stopWifi() {
        serverRunnable?.stopServer()
//        serverThread?.interrupt()
        serverThread?.join()
        serverRunnable = null
        serverThread = null
        Log.d("ServerService", "old thread finished, everything nulled")
    }
}