package com.example.audio_meter

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainApplication : Application() {
    private lateinit var preferences: SharedPreferences

    var wifiOn = false
    var recordingOn = false

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

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("MainApplication", "received action intent")
            if (intent?.action == "toggleRecord") {
                toggleRecording()
            }
            if (intent?.action == "toggleWifi") {
                toggleWifi()
            }
        }
    }

    companion object {
        private lateinit var instance: MainApplication
        fun getInstance(): MainApplication {
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // init shared prefs
        initSharedPrefs()

        // init database
        ValueDatabase.loadDatabase(this)

        // create notification channel
        val channel = NotificationChannel(
            "server_channel",
            "Server Notification",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            actionReceiver,
            IntentFilter("actionData")
        )
    }

    private fun initSharedPrefs() {
        preferences = this.getSharedPreferences("com.example.audio_meter", Context.MODE_PRIVATE)
        dbShift = preferences.getFloat("dbShift", -70f)
        dbTarget = preferences.getFloat("dbTarget", 10f)
    }


    fun toggleRecording() {
        // 1. change value
        // 2. update ui
        // 3. update services
        recordingOn = !recordingOn
    }

    fun toggleWifi() {
        wifiOn != wifiOn
    }
}