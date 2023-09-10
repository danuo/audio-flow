package com.example.audio_meter

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log

class MainApplication : Application() {
    private lateinit var preferences: SharedPreferences


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

    companion object {
        private lateinit var instance: MainApplication

        fun getInstance(): MainApplication {
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initSharedPrefs()
        ValueDatabase.loadDatabase(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "server_channel",
                "Server Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initSharedPrefs() {
        preferences = this.getSharedPreferences("com.example.audio_meter", Context.MODE_PRIVATE)
        dbShift = preferences.getFloat("dbShift", -70f)
        dbTarget = preferences.getFloat("dbTarget", 10f)
    }
}