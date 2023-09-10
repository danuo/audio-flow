package com.example.audio_meter

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.os.Build
import android.util.Log

class ServerService : Service() {
    private var serverThread: Thread? = null
    var htmlString: String = ""

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("in service", "we made it here to oncreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (intent.hasExtra("html")) {
                htmlString = intent.getStringExtra("html")!!
            }
        }
        when (intent?.action) {
            "start" -> start()
            "stop" -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
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
        startForeground(1, notification)
        serverThread = Thread(ServerRunner(htmlString))
        serverThread?.start()
        Log.d("in service", "and also here to start")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serverThread is Thread) {
            serverThread?.interrupt()
            try {
                serverThread?.join()
            } catch (e: InterruptedException) {
                Log.d("serverservice", e.toString())
            }
        }
    }
}