package com.example.audio_flow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainApplication : Application() {
    private lateinit var preferences: SharedPreferences

    var wifiOn = false
    var recordingOn = false

    var permissionNotification = false
    var permissionAudio = false
    var serviceStarted = false

    var htmlString = ""

    private var _dbShift: Float = 0f
    var dbShift: Float
        get() = _dbShift
        set(value) {
            _dbShift = value
            val editor = preferences.edit()
            editor.putFloat("dbShift", value)
            editor.apply()
        }

    private var _dbTarget: Float = 0f
    var dbTarget: Float
        get() = _dbTarget
        set(value) {
            _dbTarget = value
            val editor = preferences.edit()
            editor.putFloat("dbTarget", value)
            editor.apply()
        }

    var showMilliseconds: Long = 3600 * 1000


    private val notificationEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (intent.action == "toggleRecord") {
                    toggleRecording()
                }
                if (intent.action == "toggleWifi") {
                    toggleWifi()
                }
            }
            Log.d("MainApplication", "intent received, ${intent?.action}")
        }
    }

    companion object {
        const val REFRESH_RATE = 10
        private lateinit var instance: MainApplication
        fun getInstance(): MainApplication {
            return instance
        }
    }

    private fun updateUiBroadcast() {
        Log.d("MainApplication", "updateUiBroadcast")
        val intent = Intent("updateUi")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initSharedPrefs()  // init shared prefs
        ValueDatabase.loadDatabase(this)  // init database
        htmlString =
            loadHtmlResourceToString(context = this, R.raw.index).trimIndent()

        // create notification channel
        val channel = NotificationChannel(
            "server_channel",
            "Server Notification",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        this.registerReceiver(
            notificationEventReceiver,
            IntentFilter("toggleRecord"),
        )

        this.registerReceiver(
            notificationEventReceiver,
            IntentFilter("toggleWifi"),
        )

    }

    private fun initSharedPrefs() {
        preferences = this.getSharedPreferences("com.example.audio_flow", Context.MODE_PRIVATE)
        dbShift = preferences.getFloat("dbShift", -70f)
        dbTarget = preferences.getFloat("dbTarget", 10f)
    }

    fun toggleRecording() {
        recordingOn = !recordingOn
        updateUiBroadcast()
        updateService()
    }

    fun toggleWifi() {
        wifiOn = !wifiOn
        updateUiBroadcast()
        updateService()
    }

    private fun updateService() {
        if (wifiOn or recordingOn) {
            val intent = Intent(applicationContext, ServerService::class.java)
            intent.action = "refresh"
            startService(intent)
        }

        if ((!wifiOn) and (!recordingOn)) {
            stopService()
        }
    }

    private fun stopService() {
        Log.d("MainActivity", "stopService()")
        val intent = Intent(applicationContext, ServerService::class.java)
        intent.action = "stop"
        startService(intent)
    }

    private fun loadHtmlResourceToString(context: Context, resourceId: Int): String {
        val inputStream = context.resources.openRawResource(resourceId)
        return inputStream.readBytes().toString(Charsets.UTF_8)
    }
}
