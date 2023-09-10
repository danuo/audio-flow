package com.example.audio_meter

import android.content.Context
import android.content.SharedPreferences

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat


class MainActivity : ComponentActivity() {
    private lateinit var preferences: SharedPreferences

    lateinit var audioRecorder: AudioRecorder
    lateinit var databaseHandler: DatabaseHandler
    lateinit var uiHandler: UiHandler
    val handler = Handler(Looper.getMainLooper())

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

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("main activity", "now")
        super.onCreate(savedInstanceState)
        initSharedPrefs()
        setContentView(R.layout.activity_main)
        audioRecorder = AudioRecorder(this)
        uiHandler = UiHandler(this)
        databaseHandler = DatabaseHandler(context = this, uiHandler = uiHandler)
        // startServerOld(this, databaseHandler)
        startThing()
    }

    private fun startThing() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }
        val intent = Intent(applicationContext, ServerService::class.java)
        val htmlString =
            loadHtmlResourceToString(context = this, R.raw.index).trimIndent()
        intent.putExtra("html", htmlString)
        intent.action = "start"
        startService(intent)
    }

    private fun stopThing() {
        val intent = Intent(applicationContext, ServerService::class.java)
        intent.action = "stop"
        startService(intent)
    }

    companion object {
        const val REFRESH_RATE = 10
    }

    private fun startServerOld(context: MainActivity, databaseHandler: DatabaseHandler) {
        CoroutineScope(Dispatchers.IO).launch {
            ServerOld(context, databaseHandler)
        }
    }

    private fun loadHtmlResourceToString(context: Context, resourceId: Int): String {
        val inputStream = context.resources.openRawResource(resourceId)
        return inputStream.readBytes().toString(Charsets.UTF_8)
    }

    private fun initSharedPrefs() {
        preferences = this.getSharedPreferences("com.example.audio_meter", Context.MODE_PRIVATE)
        dbShift = preferences.getFloat("dbShift", -70f)
        dbTarget = preferences.getFloat("dbTarget", 10f)
    }
}