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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioRecorder = AudioRecorder(this)
        setContentView(R.layout.activity_main)
        uiHandler = UiHandler(this)
        initDB(uiHandler)
        CoroutineScope(Dispatchers.IO).launch {
            initServer(databaseHandler)
        }
    }

    companion object {
        private const val REFRESH_RATE = 10
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = (SAMPLE_RATE / REFRESH_RATE)  // before: 1024
    }

    private fun initDB(uiHandler: UiHandler) {
        databaseHandler =
            DatabaseHandler(context = this, uiHandler = uiHandler)
    }

    private fun initServer(databaseHandler: DatabaseHandler) {
        val server = Server(this, databaseHandler)
        server.startServer()
    }
}