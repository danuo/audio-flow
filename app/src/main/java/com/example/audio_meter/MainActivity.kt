package com.example.audio_meter

import android.content.Context
import android.content.SharedPreferences

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private lateinit var preferences: SharedPreferences

    lateinit var audioRecorder: AudioRecorder
    lateinit var databaseHandler: DatabaseHandler
    lateinit var uiHandler: UiHandler
    val handler = Handler(Looper.getMainLooper())

    private var _dbShift: Float = -70f
    var dbShift: Float
        get() = _dbShift
        set(value) {
            _dbShift = value
            val editor = preferences.edit()
            editor.putFloat("dbShift", value)
            editor.apply()
        }

    var showMilliseconds: Long = 3600 * 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSharedPrefs()
        setContentView(R.layout.activity_main)
        audioRecorder = AudioRecorder(this)
        uiHandler = UiHandler(this)
        databaseHandler = DatabaseHandler(context = this, uiHandler = uiHandler)
        startServer(this, databaseHandler)
    }

    companion object {
        const val REFRESH_RATE = 10
    }

    private fun startServer(context: MainActivity, databaseHandler: DatabaseHandler) {
        CoroutineScope(Dispatchers.IO).launch {
            Server(context, databaseHandler)
        }
    }

    private fun initSharedPrefs() {
        preferences = this.getSharedPreferences("com.example.audio_meter", Context.MODE_PRIVATE)
        dbShift = preferences.getFloat("dbShift", -70f)
    }
}