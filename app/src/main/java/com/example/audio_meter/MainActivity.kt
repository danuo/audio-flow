package com.example.audio_meter

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    lateinit var audioRecorder: AudioRecorder
    lateinit var databaseHandler: DatabaseHandler
    lateinit var uiHandler: UiHandler
    val handler = Handler(Looper.getMainLooper())
    var dbShift: Float = -70f
    var showMilliseconds: Long = 3600 * 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioRecorder = AudioRecorder(this)
        setContentView(R.layout.activity_main)
        uiHandler = UiHandler(this)
        databaseHandler = DatabaseHandler(context = this, uiHandler = uiHandler)
        startServer(this, databaseHandler)
    }

    companion object {
        const val REFRESH_RATE = 10
    }

    private fun startServer(context: ComponentActivity, databaseHandler: DatabaseHandler) {
        CoroutineScope(Dispatchers.IO).launch {
            Server(context, databaseHandler)
        }
    }
}