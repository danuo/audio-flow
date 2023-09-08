package com.example.audio_meter

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ServerService : Service() {
    private val server by lazy {
        ServerNew()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (intent.hasExtra("html")) {
                server.htmlString = intent.getStringExtra("html")!!
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
        startForeground(1, notification)
        server.startServer()
    }
}